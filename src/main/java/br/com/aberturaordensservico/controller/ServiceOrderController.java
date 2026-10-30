package br.com.aberturaordensservico.controller;

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

import br.com.aberturaordensservico.dto.OrderAssignmentRequest;
import br.com.aberturaordensservico.dto.OrderCancellationRequest;
import br.com.aberturaordensservico.dto.ServiceOrderRequest;
import br.com.aberturaordensservico.model.AuditEvent;
import br.com.aberturaordensservico.model.ServiceOrder;
import br.com.aberturaordensservico.model.enums.OrderStatus;
import br.com.aberturaordensservico.model.enums.OriginChannel;
import br.com.aberturaordensservico.security.CustomUserDetails;
import br.com.aberturaordensservico.service.AuditService;
import br.com.aberturaordensservico.service.ServiceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders Management", description = "Endpoints de gerenciamento e supervisão de ordens de serviço (EP-008 a EP-010)")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;
    private final AuditService auditService;

    public ServiceOrderController(ServiceOrderService serviceOrderService, AuditService auditService) {
        this.serviceOrderService = serviceOrderService;
        this.auditService = auditService;
    }

    /**
     * EP-008: Criar Nova Ordem de Serviço
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "EP-008: Criar nova ordem de serviço", description = "Abre uma nova ordem no estado OPEN com equipamento e prioridade")
    public ResponseEntity<ServiceOrder> createOrder(
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
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Listar todas as ordens de serviço", description = "Visão geral administrativa de ordens")
    public List<ServiceOrder> listOrders() {
        return serviceOrderService.listAll();
    }

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Listar ordens de serviço abertas", description = "Filtro de ordens no estado OPEN")
    public List<ServiceOrder> listOpenOrders() {
        return serviceOrderService.listByStatus(OrderStatus.OPEN);
    }

    /**
     * EP-009: Atribuir Ordem a um Técnico
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "EP-009: Atribuir ordem a um técnico", description = "Designa um técnico responsável (OPEN/ASSIGNED -> ASSIGNED)")
    public ResponseEntity<ServiceOrder> assignOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderAssignmentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel
    ) {
        ServiceOrder assigned = serviceOrderService.assignOrder(
                id,
                request,
                userDetails != null ? userDetails.getUser() : null,
                originChannel != null ? originChannel : OriginChannel.API_CLIENT,
                idempotencyKey
        );
        return ResponseEntity.ok(assigned);
    }

    /**
     * EP-010: Cancelar Ordem de Serviço
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "EP-010: Cancelar ordem de serviço", description = "Cancela ordem não terminal com justificativa obrigatória")
    public ResponseEntity<ServiceOrder> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderCancellationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel
    ) {
        ServiceOrder cancelled = serviceOrderService.cancelOrder(
                id,
                userDetails != null ? userDetails.getUser() : null,
                request,
                originChannel != null ? originChannel : OriginChannel.API_CLIENT,
                idempotencyKey
        );
        return ResponseEntity.ok(cancelled);
    }

    /**
     * Trilha de Auditoria Transacional (BR-008)
     */
    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Trilha de auditoria da ordem", description = "Retorna o histórico imutável de transições e eventos")
    public ResponseEntity<List<AuditEvent>> getOrderAuditTrail(@PathVariable Long id) {
        List<AuditEvent> trail = auditService.getOrderAuditTrail(id);
        return ResponseEntity.ok(trail);
    }
}
