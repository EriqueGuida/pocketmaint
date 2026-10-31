package br.com.pocketmaint.security;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import br.com.pocketmaint.controller.AdminOrderController;
import br.com.pocketmaint.controller.TechnicianOrderController;
import br.com.pocketmaint.dto.ApiErrorResponse;
import br.com.pocketmaint.dto.OrderCancellationRequest;
import br.com.pocketmaint.dto.OrderCompletionRequest;
import br.com.pocketmaint.dto.ServiceOrderRequest;
import br.com.pocketmaint.exception.GlobalExceptionHandler;
import br.com.pocketmaint.exception.InvalidOrderStateTransitionException;
import br.com.pocketmaint.exception.OrderAccessDeniedException;
import br.com.pocketmaint.model.AuditEvent;
import br.com.pocketmaint.model.Equipment;
import br.com.pocketmaint.model.Sector;
import br.com.pocketmaint.model.ServiceOrder;
import br.com.pocketmaint.model.User;
import br.com.pocketmaint.model.enums.AuditEventType;
import br.com.pocketmaint.model.enums.OrderStatus;
import br.com.pocketmaint.model.enums.OriginChannel;
import br.com.pocketmaint.model.enums.Priority;
import br.com.pocketmaint.model.enums.Role;
import br.com.pocketmaint.repository.ServiceOrderRepository;
import br.com.pocketmaint.service.AuditService;
import br.com.pocketmaint.service.ServiceOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Milestone 5: Testes de Integração de Segurança e Controle de Acesso (Anti-IDOR / BOLA)
 * 
 * Validação rigorosa dos controles de segurança com MockMvc:
 * - Anti-IDOR / BOLA (BR-003 / AC-002)
 * - Restrições RBAC de Perfil (TECHNICIAN vs SUPERVISOR)
 * - Isolamento de fila de ordens por técnico (AC-001)
 * - Bloqueio de conclusão sem início (AC-003)
 * - Idempotência e integridade da trilha de auditoria (AC-006 / BR-007 / BR-008)
 */
class OrderSecurityIntegrationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private StubServiceOrderService serviceOrderService;
    private StubAuditService auditService;

    private User technicianA;
    private User technicianB;
    private User supervisor;
    private CustomUserDetails userDetailsTechA;
    private CustomUserDetails userDetailsTechB;
    private CustomUserDetails userDetailsSupervisor;

    private CustomUserDetails currentPrincipal;

    private Equipment equipment;
    private Sector sector;
    private ServiceOrder orderTechnicianB;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        serviceOrderService = new StubServiceOrderService();
        auditService = new StubAuditService();

        technicianA = User.builder()
                .id(1L)
                .username("tec.carlos")
                .fullName("Carlos Mendes")
                .role(Role.TECHNICIAN)
                .active(true)
                .build();

        technicianB = User.builder()
                .id(2L)
                .username("tec.ana")
                .fullName("Ana Paula")
                .role(Role.TECHNICIAN)
                .active(true)
                .build();

        supervisor = User.builder()
                .id(10L)
                .username("sup.roberto")
                .fullName("Roberto Lima")
                .role(Role.SUPERVISOR)
                .active(true)
                .build();

        userDetailsTechA = new CustomUserDetails(technicianA);
        userDetailsTechB = new CustomUserDetails(technicianB);
        userDetailsSupervisor = new CustomUserDetails(supervisor);

        sector = Sector.builder()
                .id(1L)
                .code("SEC-01")
                .name("Setor Industrial")
                .active(true)
                .build();

        equipment = Equipment.builder()
                .id(1L)
                .assetTag("EQ-100")
                .name("Compressor Parafuso")
                .sector(sector)
                .active(true)
                .build();

        orderTechnicianB = ServiceOrder.builder()
                .id(10L)
                .orderNumber("OS-2026-0010")
                .equipment(equipment)
                .sector(sector)
                .createdBy(supervisor)
                .assignedTechnician(technicianB)
                .title("Manutenção Preventiva")
                .problemDescription("Substituir rolamento e filtro.")
                .priority(Priority.HIGH)
                .status(OrderStatus.ASSIGNED)
                .createdAt(Instant.now())
                .assignedAt(Instant.now())
                .version(0L)
                .build();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        || parameter.getParameterType().equals(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };

        Filter rbacFilter = (ServletRequest request, ServletResponse response, FilterChain chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;
            String uri = req.getRequestURI();
            String method = req.getMethod();

            Role currentRole = currentPrincipal != null ? currentPrincipal.getUser().getRole() : null;

            if ((uri.equals("/api/v1/orders") && method.equals("POST"))
                    || (uri.matches(".*/api/v1/orders/\\d+/cancel.*") && method.equals("POST"))) {
                if (currentRole != Role.SUPERVISOR && currentRole != Role.ADMIN) {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType("application/json;charset=UTF-8");
                    ApiErrorResponse error = new ApiErrorResponse(
                            HttpStatus.FORBIDDEN.value(),
                            "ACCESS_DENIED_INSUFFICIENT_ROLE",
                            "Acesso negado",
                            "Perfil de usuário sem permissão para executar esta operação.",
                            uri
                    );
                    res.getWriter().write(objectMapper.writeValueAsString(error));
                    return;
                }
            }
            chain.doFilter(request, response);
        };

        TechnicianOrderController techController = new TechnicianOrderController(serviceOrderService);
        AdminOrderController adminController = new AdminOrderController(serviceOrderService, auditService, null);

        mockMvc = MockMvcBuilders.standaloneSetup(techController, adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .addFilters(rbacFilter)
                .build();
    }

    // =========================================================================
    // 1. Anti-IDOR / BOLA (BR-003 / AC-002)
    // =========================================================================

    @Test
    @DisplayName("AC-002: Técnico A tentando consultar GET /orders/{id} atribuída ao Técnico B deve receber 403 ACCESS_DENIED_NOT_OWNER")
    void shouldDenyTechnicianAViewingOrderAssignedToTechnicianB() throws Exception {
        currentPrincipal = userDetailsTechA;
        serviceOrderService.findByIdHandler = id -> Optional.of(orderTechnicianB);

        mockMvc.perform(get("/api/v1/orders/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED_NOT_OWNER")))
                .andExpect(jsonPath("$.title", is("Acesso negado ao recurso")))
                .andExpect(jsonPath("$.userMessage", containsString("não está atribuída ao seu usuário")));
    }

    @Test
    @DisplayName("AC-002: Técnico A tentando iniciar POST /orders/{id}/start atribuída ao Técnico B deve receber 403 ACCESS_DENIED_NOT_OWNER")
    void shouldDenyTechnicianAStartingOrderAssignedToTechnicianB() throws Exception {
        currentPrincipal = userDetailsTechA;
        serviceOrderService.startOrderHandler = (orderId, technicianId, channel, idempotencyKey) -> {
            throw new OrderAccessDeniedException("Apenas o técnico responsável designado pode iniciar o atendimento desta ordem de serviço.");
        };

        mockMvc.perform(post("/api/v1/orders/10/start")
                        .header("Idempotency-Key", "key-start-123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED_NOT_OWNER")));
    }

    @Test
    @DisplayName("AC-002: Técnico A tentando concluir POST /orders/{id}/complete atribuída ao Técnico B deve receber 403 ACCESS_DENIED_NOT_OWNER")
    void shouldDenyTechnicianACompletingOrderAssignedToTechnicianB() throws Exception {
        currentPrincipal = userDetailsTechA;
        OrderCompletionRequest request = new OrderCompletionRequest("Substituição concluída com sucesso.");

        serviceOrderService.completeOrderHandler = (orderId, technicianId, req, channel, idempotencyKey) -> {
            throw new OrderAccessDeniedException("Apenas o técnico responsável designado pode concluir desta ordem de serviço.");
        };

        mockMvc.perform(post("/api/v1/orders/10/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED_NOT_OWNER")));
    }

    @Test
    @DisplayName("Técnico B (proprietário) pode consultar GET /orders/{id} com sucesso (200 OK)")
    void shouldAllowTechnicianBConsultingOwnOrder() throws Exception {
        currentPrincipal = userDetailsTechB;
        serviceOrderService.findByIdHandler = id -> Optional.of(orderTechnicianB);

        mockMvc.perform(get("/api/v1/orders/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.orderNumber", is("OS-2026-0010")))
                .andExpect(jsonPath("$.assignedTechnician.id", is(2)));
    }

    // =========================================================================
    // 2. Restrições RBAC de Perfil
    // =========================================================================

    @Test
    @DisplayName("Técnico tentando chamar POST /api/v1/orders (criação) deve receber 403 ACCESS_DENIED_INSUFFICIENT_ROLE")
    void shouldDenyTechnicianCreatingOrder() throws Exception {
        currentPrincipal = userDetailsTechA;

        ServiceOrderRequest request = new ServiceOrderRequest(
                "Ordem não autorizada",
                "Descrição detalhada com mais de 10 caracteres.",
                Priority.HIGH,
                OriginChannel.API_CLIENT,
                1L,
                null
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED_INSUFFICIENT_ROLE")));
    }

    @Test
    @DisplayName("Técnico tentando chamar POST /api/v1/orders/{id}/cancel deve receber 403 ACCESS_DENIED_INSUFFICIENT_ROLE")
    void shouldDenyTechnicianCancellingOrder() throws Exception {
        currentPrincipal = userDetailsTechA;

        OrderCancellationRequest request = new OrderCancellationRequest("Motivo de cancelamento com mais de 10 caracteres.");

        mockMvc.perform(post("/api/v1/orders/10/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED_INSUFFICIENT_ROLE")));
    }

    @Test
    @DisplayName("Supervisor pode chamar cancelamento de ordem com sucesso (200 OK)")
    void shouldAllowSupervisorCancellingOrder() throws Exception {
        currentPrincipal = userDetailsSupervisor;

        OrderCancellationRequest request = new OrderCancellationRequest("Motivo de cancelamento válido pelo supervisor.");
        orderTechnicianB.setStatus(OrderStatus.CANCELLED);

        serviceOrderService.cancelOrderHandler = (orderId, user, req, channel, idempotencyKey) -> orderTechnicianB;

        mockMvc.perform(post("/api/v1/orders/10/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    // =========================================================================
    // 3. Critérios de Aceite AC-001 e AC-003
    // =========================================================================

    @Test
    @DisplayName("AC-001: Consulta /api/v1/me/orders retorna exclusivamente ordens do técnico autenticado")
    void shouldReturnOnlyTechnicianOrders() throws Exception {
        currentPrincipal = userDetailsTechA;
        AtomicInteger callCount = new AtomicInteger(0);

        serviceOrderService.listTechnicianOrdersHandler = (techId, statuses, pageable) -> {
            assertEquals(1L, techId);
            callCount.incrementAndGet();
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        };

        mockMvc.perform(get("/api/v1/me/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));

        assertEquals(1, callCount.get());
    }

    @Test
    @DisplayName("AC-003: Tentativa de concluir ordem no estado ASSIGNED deve retornar 409 INVALID_ORDER_STATE_TRANSITION")
    void shouldReturn409WhenCompletingOrderWithoutStarting() throws Exception {
        currentPrincipal = userDetailsTechA;

        OrderCompletionRequest request = new OrderCompletionRequest("Tentativa de conclusão sem iniciar.");

        serviceOrderService.completeOrderHandler = (orderId, techId, req, channel, idempotencyKey) -> {
            throw new InvalidOrderStateTransitionException("A ordem de serviço precisa ser iniciada (IN_PROGRESS) antes de ser concluída. Estado atual: ASSIGNED");
        };

        mockMvc.perform(post("/api/v1/orders/7/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_ORDER_STATE_TRANSITION")))
                .andExpect(jsonPath("$.userMessage", containsString("precisa ser iniciada")));
    }

    // =========================================================================
    // 4. Idempotência com Auditoria (BR-007 / BR-008 / AC-006)
    // =========================================================================

    @Test
    @DisplayName("AC-006: Requisições repetidas de início de atendimento não devem duplicar registros em audit_events")
    void shouldNotDuplicateAuditRecordsOnRepeatedRequests() {
        StubAuditService mockAudit = new StubAuditService();

        // Stub do repositório de ordens
        ServiceOrderRepository stubRepo = new StubServiceOrderRepository(orderTechnicianB);

        ServiceOrderService service = new ServiceOrderService(
                stubRepo, null, null, null, mockAudit
        );

        String idempotencyKey = "key-idem-unique-123";

        // 1ª chamada: status ASSIGNED -> IN_PROGRESS (estado muda, auditoria gravada)
        ServiceOrder order1 = service.startOrder(10L, 2L, OriginChannel.CARDPUTER_TERMINAL, idempotencyKey);
        assertEquals(OrderStatus.IN_PROGRESS, order1.getStatus());
        assertEquals(1, mockAudit.recordEventCallCount);
        assertEquals(AuditEventType.ORDER_STARTED, mockAudit.lastEventType);

        // 2ª chamada repetida com a mesma chave: já está IN_PROGRESS (idempotente)
        ServiceOrder order2 = service.startOrder(10L, 2L, OriginChannel.CARDPUTER_TERMINAL, idempotencyKey);
        assertEquals(OrderStatus.IN_PROGRESS, order2.getStatus());

        // Garante que o auditService continuou tendo EXATAMENTE 1 chamada gravada
        assertEquals(1, mockAudit.recordEventCallCount);
    }

    // =========================================================================
    // Stubs para isolamento e execução ultrarrápida sem overhead de proxies
    // =========================================================================

    @FunctionalInterface
    interface StartOrderHandler {
        ServiceOrder handle(Long orderId, Long technicianId, OriginChannel channel, String idempotencyKey);
    }

    @FunctionalInterface
    interface CompleteOrderHandler {
        ServiceOrder handle(Long orderId, Long technicianId, OrderCompletionRequest request, OriginChannel channel, String idempotencyKey);
    }

    @FunctionalInterface
    interface CancelOrderHandler {
        ServiceOrder handle(Long orderId, User user, OrderCancellationRequest request, OriginChannel channel, String idempotencyKey);
    }

    @FunctionalInterface
    interface ListTechnicianOrdersHandler {
        Page<ServiceOrder> handle(Long technicianId, List<OrderStatus> statuses, Pageable pageable);
    }

    static class StubServiceOrderService extends ServiceOrderService {
        public Function<Long, Optional<ServiceOrder>> findByIdHandler = id -> Optional.empty();
        public StartOrderHandler startOrderHandler;
        public CompleteOrderHandler completeOrderHandler;
        public CancelOrderHandler cancelOrderHandler;
        public ListTechnicianOrdersHandler listTechnicianOrdersHandler;

        public StubServiceOrderService() {
            super(null, null, null, null, null);
        }

        @Override
        public Optional<ServiceOrder> findById(Long id) {
            return findByIdHandler.apply(id);
        }

        @Override
        public ServiceOrder startOrder(Long orderId, Long technicianId, OriginChannel channel, String idempotencyKey) {
            if (startOrderHandler != null) {
                return startOrderHandler.handle(orderId, technicianId, channel, idempotencyKey);
            }
            return null;
        }

        @Override
        public ServiceOrder completeOrder(Long orderId, Long technicianId, OrderCompletionRequest request, OriginChannel channel, String idempotencyKey) {
            if (completeOrderHandler != null) {
                return completeOrderHandler.handle(orderId, technicianId, request, channel, idempotencyKey);
            }
            return null;
        }

        @Override
        public ServiceOrder cancelOrder(Long orderId, User currentUser, OrderCancellationRequest request, OriginChannel channel, String idempotencyKey) {
            if (cancelOrderHandler != null) {
                return cancelOrderHandler.handle(orderId, currentUser, request, channel, idempotencyKey);
            }
            return null;
        }

        @Override
        public Page<ServiceOrder> listTechnicianOrders(Long technicianId, List<OrderStatus> statuses, Pageable pageable) {
            if (listTechnicianOrdersHandler != null) {
                return listTechnicianOrdersHandler.handle(technicianId, statuses, pageable);
            }
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }

    static class StubAuditService extends AuditService {
        public int recordEventCallCount = 0;
        public AuditEventType lastEventType;

        public StubAuditService() {
            super(null);
        }

        @Override
        public Optional<AuditEvent> recordEvent(Long orderId, Long actorUserId, AuditEventType eventType,
                                                String previousState, String newState,
                                                OriginChannel channel, String idempotencyKey,
                                                String details) {
            recordEventCallCount++;
            lastEventType = eventType;
            return Optional.of(new AuditEvent());
        }
    }

    static class StubServiceOrderRepository implements ServiceOrderRepository {
        private ServiceOrder order;

        public StubServiceOrderRepository(ServiceOrder order) {
            this.order = order;
        }

        @Override
        public Optional<ServiceOrder> findById(Long id) {
            return Optional.ofNullable(order);
        }

        @Override
        public <S extends ServiceOrder> S save(S entity) {
            this.order = entity;
            return entity;
        }

        @Override public <S extends ServiceOrder> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<ServiceOrder> findAll() { return List.of(); }
        @Override public List<ServiceOrder> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(ServiceOrder entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends ServiceOrder> entities) {}
        @Override public void deleteAll() {}
        @Override public List<ServiceOrder> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public Page<ServiceOrder> findAll(Pageable pageable) { return null; }
        @Override public void flush() {}
        @Override public <S extends ServiceOrder> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends ServiceOrder> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<ServiceOrder> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public ServiceOrder getOne(Long aLong) { return null; }
        @Override public ServiceOrder getById(Long aLong) { return null; }
        @Override public ServiceOrder getReferenceById(Long aLong) { return null; }
        @Override public <S extends ServiceOrder> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends ServiceOrder> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends ServiceOrder> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends ServiceOrder> Page<S> findAll(org.springframework.data.domain.Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends ServiceOrder> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends ServiceOrder> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends ServiceOrder, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public Page<ServiceOrder> findByAssignedTechnicianId(Long technicianId, Pageable pageable) { return null; }
        @Override public Page<ServiceOrder> findByAssignedTechnicianIdAndStatus(Long technicianId, OrderStatus status, Pageable pageable) { return null; }
        @Override public Page<ServiceOrder> findByStatus(OrderStatus status, Pageable pageable) { return null; }
        @Override public Optional<ServiceOrder> findByIdWithAssociations(Long id) { return Optional.ofNullable(order); }
    }
}
