package br.com.aberturaordensservico.service;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aberturaordensservico.dto.OrderAssignmentRequest;
import br.com.aberturaordensservico.dto.OrderCancellationRequest;
import br.com.aberturaordensservico.dto.OrderCompletionRequest;
import br.com.aberturaordensservico.dto.ServiceOrderRequest;
import br.com.aberturaordensservico.exception.BusinessValidationException;
import br.com.aberturaordensservico.exception.ResourceNotFoundException;
import br.com.aberturaordensservico.model.Equipment;
import br.com.aberturaordensservico.model.OrderAssignment;
import br.com.aberturaordensservico.model.ServiceOrder;
import br.com.aberturaordensservico.model.User;
import br.com.aberturaordensservico.model.enums.AuditEventType;
import br.com.aberturaordensservico.model.enums.OrderStatus;
import br.com.aberturaordensservico.model.enums.OriginChannel;
import br.com.aberturaordensservico.repository.EquipmentRepository;
import br.com.aberturaordensservico.repository.OrderAssignmentRepository;
import br.com.aberturaordensservico.repository.ServiceOrderRepository;
import br.com.aberturaordensservico.repository.UserRepository;

@Service
public class ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;
    private final AuditService auditService;

    public ServiceOrderService(
            ServiceOrderRepository serviceOrderRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            OrderAssignmentRepository orderAssignmentRepository,
            AuditService auditService
    ) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.auditService = auditService;
    }

    /**
     * T-01: Criar Ordem de Serviço (BR-001, BR-008)
     */
    @Transactional
    public ServiceOrder createOrder(
            ServiceOrderRequest request,
            User creator,
            OriginChannel originChannel,
            String idempotencyKey
    ) {
        Equipment equipment = equipmentRepository.findById(request.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com ID: " + request.equipmentId()));

        if (!equipment.isActive()) {
            throw new BusinessValidationException("Não é possível abrir ordem de serviço para equipamento inativo.");
        }

        long count = serviceOrderRepository.count() + 1;
        String orderNumber = String.format("OS-%d-%04d", Year.now().getValue(), count);

        ServiceOrder order = new ServiceOrder();
        order.setOrderNumber(orderNumber);
        order.setEquipment(equipment);
        order.setSector(equipment.getSector());
        order.setCreatedBy(creator);
        order.setTitle(request.title().trim());
        order.setProblemDescription(request.problemDescription().trim());
        order.setPriority(request.priority());
        order.setStatus(OrderStatus.OPEN);
        order.setCreatedAt(Instant.now());
        order.setVersion(0L);

        ServiceOrder savedOrder = serviceOrderRepository.save(order);

        // Registro atômico de auditoria (BR-008)
        auditService.recordEvent(
                savedOrder.getId(),
                creator != null ? creator.getId() : 1L,
                AuditEventType.ORDER_CREATED,
                null,
                OrderStatus.OPEN.name(),
                originChannel,
                idempotencyKey,
                "Ordem de serviço criada com prioridade " + request.priority()
        );

        return savedOrder;
    }

    /**
     * T-02: Atribuir Ordem a um Técnico (BR-002, BR-007, BR-008)
     */
    @Transactional
    public ServiceOrder assignOrder(
            Long orderId,
            OrderAssignmentRequest request,
            User assignedBy,
            OriginChannel originChannel,
            String idempotencyKey
    ) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada com ID: " + orderId));

        User technician = userRepository.findById(request.technicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Técnico não encontrado com ID: " + request.technicianId()));

        String previousStatus = order.getStatus().name();
        boolean stateChanged = order.assign(technician, assignedBy, request.notes());

        if (stateChanged) {
            // Desativa atribuição anterior se houver
            orderAssignmentRepository.findByServiceOrderIdAndActiveAssignmentTrue(order.getId())
                    .ifPresent(prevAssignment -> {
                        prevAssignment.setActiveAssignment(false);
                        orderAssignmentRepository.save(prevAssignment);
                    });

            // Registra nova atribuição no histórico
            OrderAssignment newAssignment = new OrderAssignment();
            newAssignment.setServiceOrder(order);
            newAssignment.setTechnician(technician);
            newAssignment.setAssignedBy(assignedBy);
            newAssignment.setActiveAssignment(true);
            newAssignment.setNotes(request.notes());
            newAssignment.setAssignedAt(Instant.now());
            orderAssignmentRepository.save(newAssignment);

            // Grava auditoria transacional
            auditService.recordEvent(
                    order.getId(),
                    assignedBy != null ? assignedBy.getId() : 1L,
                    AuditEventType.ORDER_ASSIGNED,
                    previousStatus,
                    OrderStatus.ASSIGNED.name(),
                    originChannel,
                    idempotencyKey,
                    "Atribuída ao técnico " + technician.getFullName() + " (ID: " + technician.getId() + ")"
            );

            return serviceOrderRepository.save(order);
        }

        return order; // Idempotente
    }

    /**
     * T-03: Iniciar Atendimento (BR-003, BR-007, BR-008)
     */
    @Transactional
    public ServiceOrder startOrder(
            Long orderId,
            Long technicianId,
            OriginChannel originChannel,
            String idempotencyKey
    ) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada com ID: " + orderId));

        String previousStatus = order.getStatus().name();
        boolean stateChanged = order.startAttendance(technicianId);

        if (stateChanged) {
            auditService.recordEvent(
                    order.getId(),
                    technicianId,
                    AuditEventType.ORDER_STARTED,
                    previousStatus,
                    OrderStatus.IN_PROGRESS.name(),
                    originChannel,
                    idempotencyKey,
                    "Atendimento iniciado no equipamento " + order.getEquipment().getName()
            );

            return serviceOrderRepository.save(order);
        }

        return order; // Idempotente
    }

    /**
     * T-04: Concluir Ordem de Serviço (BR-003, BR-004, BR-005, BR-006, BR-007, BR-008)
     */
    @Transactional
    public ServiceOrder completeOrder(
            Long orderId,
            Long technicianId,
            OrderCompletionRequest request,
            OriginChannel originChannel,
            String idempotencyKey
    ) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada com ID: " + orderId));

        String previousStatus = order.getStatus().name();
        boolean stateChanged = order.complete(technicianId, request.solutionDescription());

        if (stateChanged) {
            auditService.recordEvent(
                    order.getId(),
                    technicianId,
                    AuditEventType.ORDER_COMPLETED,
                    previousStatus,
                    OrderStatus.COMPLETED.name(),
                    originChannel,
                    idempotencyKey,
                    "Solução registrada: " + request.solutionDescription().trim()
            );

            return serviceOrderRepository.save(order);
        }

        return order; // Idempotente
    }

    /**
     * T-05: Cancelar Ordem de Serviço (BR-006, BR-008)
     */
    @Transactional
    public ServiceOrder cancelOrder(
            Long orderId,
            User user,
            OrderCancellationRequest request,
            OriginChannel originChannel,
            String idempotencyKey
    ) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada com ID: " + orderId));

        String previousStatus = order.getStatus().name();
        boolean stateChanged = order.cancel(user, request.reason());

        if (stateChanged) {
            auditService.recordEvent(
                    order.getId(),
                    user != null ? user.getId() : 1L,
                    AuditEventType.ORDER_CANCELLED,
                    previousStatus,
                    OrderStatus.CANCELLED.name(),
                    originChannel,
                    idempotencyKey,
                    "Motivo do cancelamento: " + request.reason().trim()
            );

            return serviceOrderRepository.save(order);
        }

        return order; // Idempotente
    }

    public Optional<ServiceOrder> findById(Long id) {
        return serviceOrderRepository.findById(id);
    }

    public Optional<ServiceOrder> findByOrderNumber(String orderNumber) {
        return serviceOrderRepository.findByOrderNumber(orderNumber);
    }

    public List<ServiceOrder> listAll() {
        return serviceOrderRepository.findAll();
    }

    public List<ServiceOrder> listByStatus(OrderStatus status) {
        return serviceOrderRepository.findByStatus(status);
    }

    public Page<ServiceOrder> listTechnicianOrders(Long technicianId, List<OrderStatus> statuses, Pageable pageable) {
        return serviceOrderRepository.findByAssignedTechnicianIdAndStatusIn(technicianId, statuses, pageable);
    }
}
