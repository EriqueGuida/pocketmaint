package br.com.pocketmaint.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.pocketmaint.dto.AuditEventResponse;
import br.com.pocketmaint.dto.OrderAssignmentRequest;
import br.com.pocketmaint.dto.OrderAssignmentResponse;
import br.com.pocketmaint.dto.OrderCancellationRequest;
import br.com.pocketmaint.dto.OrderCancellationResponse;
import br.com.pocketmaint.dto.OrderCreatedResponse;
import br.com.pocketmaint.dto.OrderSummaryItemDto;
import br.com.pocketmaint.dto.ServiceOrderRequest;
import br.com.pocketmaint.model.AuditEvent;
import br.com.pocketmaint.model.ServiceOrder;
import br.com.pocketmaint.model.enums.OrderStatus;
import br.com.pocketmaint.model.enums.OriginChannel;
import br.com.pocketmaint.security.CustomUserDetails;
import br.com.pocketmaint.service.AuditService;
import br.com.pocketmaint.service.ServiceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Admin Orders", description = "Endpoints administrativos e supervisão de ordens de serviço (EP-008 a EP-010)")
public class AdminOrderController {

    private final ServiceOrderService serviceOrderService;
    private final AuditService auditService;
    private final br.com.pocketmaint.repository.UserRepository userRepository;

    public AdminOrderController(
            ServiceOrderService serviceOrderService,
            AuditService auditService,
            br.com.pocketmaint.repository.UserRepository userRepository
    ) {
        this.serviceOrderService = serviceOrderService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * EP-008: Criar Nova Ordem de Serviço
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "EP-008: Criar nova ordem de serviço", description = "Cadastra uma nova OS vinculada a equipamento ativo no estado OPEN")
    public ResponseEntity<OrderCreatedResponse> createOrder(
            @Valid @RequestBody ServiceOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel
    ) {
        ServiceOrder created = serviceOrderService.createOrder(
                request,
                userDetails != null ? userDetails.getUser() : null,
                originChannel != null ? originChannel : OriginChannel.API_CLIENT,
                idempotencyKey
        );

        OrderCreatedResponse response = new OrderCreatedResponse(
                created.getId(),
                created.getOrderNumber(),
                created.getTitle(),
                created.getProblemDescription(),
                created.getStatus(),
                created.getPriority(),
                created.getEquipment().getId(),
                created.getEquipment().getName(),
                created.getSector().getId(),
                created.getSector().getCode(),
                created.getCreatedAt(),
                created.getVersion(),
                "Ordem de serviço criada com sucesso."
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * EP-009: Atribuir Ordem de Serviço a um Técnico
     */
    @PostMapping("/{orderId}/assign")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "EP-009: Atribuir ordem a um técnico", description = "Designa ou reatribui a ordem a um técnico ativo com papel TECHNICIAN")
    public ResponseEntity<OrderAssignmentResponse> assignOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderAssignmentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel
    ) {
        ServiceOrder assigned = serviceOrderService.assignOrder(
                orderId,
                request,
                userDetails != null ? userDetails.getUser() : null,
                originChannel != null ? originChannel : OriginChannel.API_CLIENT,
                idempotencyKey
        );

        OrderAssignmentResponse response = new OrderAssignmentResponse(
                assigned.getId(),
                assigned.getOrderNumber(),
                assigned.getStatus(),
                assigned.getAssignedTechnician() != null ? assigned.getAssignedTechnician().getId() : null,
                assigned.getAssignedTechnician() != null ? assigned.getAssignedTechnician().getFullName() : null,
                assigned.getAssignedAt(),
                assigned.getVersion()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * EP-010: Cancelar Ordem de Serviço
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "EP-010: Cancelar ordem de serviço", description = "Cancela ordem que não esteja em estado terminal (COMPLETED/CANCELLED)")
    public ResponseEntity<OrderCancellationResponse> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCancellationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel
    ) {
        ServiceOrder cancelled = serviceOrderService.cancelOrder(
                orderId,
                userDetails != null ? userDetails.getUser() : null,
                request,
                originChannel != null ? originChannel : OriginChannel.API_CLIENT,
                idempotencyKey
        );

        OrderCancellationResponse response = new OrderCancellationResponse(
                cancelled.getId(),
                cancelled.getOrderNumber(),
                cancelled.getStatus(),
                cancelled.getCancelledAt(),
                cancelled.getCancellationReason(),
                cancelled.getVersion(),
                "Ordem de serviço cancelada com sucesso."
        );

        return ResponseEntity.ok(response);
    }

    /**
     * EP-010: Trilha Cronológica de Auditoria
     */
    @GetMapping("/{orderId}/audit")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "EP-010: Trilha cronológica de auditoria", description = "Consulta o histórico imutável de transições e eventos da ordem")
    public ResponseEntity<List<AuditEventResponse>> getOrderAuditTrail(@PathVariable Long orderId) {
        List<AuditEvent> trail = auditService.getOrderAuditTrail(orderId);

        List<AuditEventResponse> response = trail.stream()
                .map(event -> {
                    String actorName = null;
                    if (event.getActorUserId() != null) {
                        actorName = userRepository.findById(event.getActorUserId())
                                .map(br.com.pocketmaint.model.User::getFullName)
                                .orElse("User-" + event.getActorUserId());
                    }
                    return new AuditEventResponse(
                            event.getId(),
                            event.getServiceOrderId(),
                            event.getActorUserId(),
                            actorName,
                            event.getEventType(),
                            event.getPreviousStatus(),
                            event.getNewStatus(),
                            event.getOriginChannel(),
                            event.getIdempotencyKey(),
                            event.getDetails(),
                            event.getOccurredAt()
                    );
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Listar todas as ordens de serviço (Supervisão)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Listar todas as ordens", description = "Visão geral administrativa de ordens")
    public ResponseEntity<List<OrderSummaryItemDto>> listOrders() {
        List<ServiceOrder> orders = serviceOrderService.listAll();
        List<OrderSummaryItemDto> items = orders.stream()
                .map(o -> new OrderSummaryItemDto(
                        o.getId(),
                        o.getOrderNumber(),
                        o.getTitle(),
                        o.getStatus(),
                        o.getPriority(),
                        o.getEquipment() != null ? o.getEquipment().getAssetTag() : null,
                        o.getEquipment() != null ? o.getEquipment().getName() : null,
                        o.getSector() != null ? o.getSector().getCode() : null
                ))
                .toList();
        return ResponseEntity.ok(items);
    }

    /**
     * Listar ordens abertas aguardando técnico
     */
    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Listar ordens abertas", description = "Filtro de ordens no estado OPEN")
    public ResponseEntity<List<OrderSummaryItemDto>> listOpenOrders() {
        List<ServiceOrder> orders = serviceOrderService.listByStatus(OrderStatus.OPEN);
        List<OrderSummaryItemDto> items = orders.stream()
                .map(o -> new OrderSummaryItemDto(
                        o.getId(),
                        o.getOrderNumber(),
                        o.getTitle(),
                        o.getStatus(),
                        o.getPriority(),
                        o.getEquipment() != null ? o.getEquipment().getAssetTag() : null,
                        o.getEquipment() != null ? o.getEquipment().getName() : null,
                        o.getSector() != null ? o.getSector().getCode() : null
                ))
                .toList();
        return ResponseEntity.ok(items);
    }
}
