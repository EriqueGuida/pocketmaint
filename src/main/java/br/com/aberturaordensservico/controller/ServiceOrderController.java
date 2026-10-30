package br.com.aberturaordensservico.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import br.com.aberturaordensservico.dto.OrderCompletionRequest;
import br.com.aberturaordensservico.dto.ServiceOrderRequest;
import br.com.aberturaordensservico.model.AuditEvent;
import br.com.aberturaordensservico.model.ServiceOrder;
import br.com.aberturaordensservico.model.enums.OrderStatus;
import br.com.aberturaordensservico.model.enums.OriginChannel;
import br.com.aberturaordensservico.security.CustomUserDetails;
import br.com.aberturaordensservico.service.AuditService;
import br.com.aberturaordensservico.service.ServiceOrderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;
    private final AuditService auditService;

    public ServiceOrderController(ServiceOrderService serviceOrderService, AuditService auditService) {
        this.serviceOrderService = serviceOrderService;
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<ServiceOrder> createOrder(
            @Valid @RequestBody ServiceOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel
    ) {
        ServiceOrder created = serviceOrderService.createOrder(
                request,
                userDetails != null ? userDetails.getUser() : null,
                originChannel,
                idempotencyKey
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrder> getOrderById(@PathVariable Long id) {
        return serviceOrderService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<ServiceOrder> listOrders() {
        return serviceOrderService.listAll();
    }

    @GetMapping("/open")
    public List<ServiceOrder> listOpenOrders() {
        return serviceOrderService.listByStatus(OrderStatus.OPEN);
    }

    @PostMapping("/{id}/assign")
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
                originChannel,
                idempotencyKey
        );
        return ResponseEntity.ok(assigned);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ServiceOrder> startOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel
    ) {
        // Validação estrita de identidade via JWT: técnico autenticado (SEC-01, BR-003)
        Long techId = userDetails.getId();
        ServiceOrder started = serviceOrderService.startOrder(id, techId, originChannel, idempotencyKey);
        return ResponseEntity.ok(started);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ServiceOrder> completeOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderCompletionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel
    ) {
        Long techId = userDetails.getId();
        ServiceOrder completed = serviceOrderService.completeOrder(id, techId, request, originChannel, idempotencyKey);
        return ResponseEntity.ok(completed);
    }

    @PostMapping("/{id}/cancel")
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
                originChannel,
                idempotencyKey
        );
        return ResponseEntity.ok(cancelled);
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditEvent>> getOrderAuditTrail(@PathVariable Long id) {
        List<AuditEvent> trail = auditService.getOrderAuditTrail(id);
        return ResponseEntity.ok(trail);
    }
}
