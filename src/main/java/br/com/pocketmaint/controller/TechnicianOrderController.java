package br.com.pocketmaint.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.pocketmaint.dto.EquipmentDetailDto;
import br.com.pocketmaint.dto.OrderActionResponse;
import br.com.pocketmaint.dto.OrderCompletionRequest;
import br.com.pocketmaint.dto.OrderDetailDto;
import br.com.pocketmaint.dto.OrderSummaryItemDto;
import br.com.pocketmaint.dto.PagedOrdersResponse;
import br.com.pocketmaint.dto.SectorDetailDto;
import br.com.pocketmaint.dto.TechnicianDetailDto;
import br.com.pocketmaint.exception.OrderAccessDeniedException;
import br.com.pocketmaint.exception.ResourceNotFoundException;
import br.com.pocketmaint.model.ServiceOrder;
import br.com.pocketmaint.model.User;
import br.com.pocketmaint.model.enums.OrderStatus;
import br.com.pocketmaint.model.enums.OriginChannel;
import br.com.pocketmaint.model.enums.Role;
import br.com.pocketmaint.security.CustomUserDetails;
import br.com.pocketmaint.service.ServiceOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Technician Orders", description = "Operações de campo exclusivas para o perfil TECHNICIAN e consulta de ordem (EP-004 a EP-007)")
public class TechnicianOrderController {

    private final ServiceOrderService serviceOrderService;

    public TechnicianOrderController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    /**
     * EP-004: Listar Ordens Atribuídas ao Técnico Autenticado
     */
    @GetMapping("/api/v1/me/orders")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "EP-004: Listar ordens do técnico autenticado", description = "Retorna lista paginada e enxuta para consumo pelo CardPuter")
    public ResponseEntity<PagedOrdersResponse> getMyOrders(
            @RequestParam(required = false, defaultValue = "ASSIGNED,IN_PROGRESS") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        List<OrderStatus> statuses = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            for (String s : status.split(",")) {
                try {
                    statuses.add(OrderStatus.valueOf(s.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (statuses.isEmpty()) {
            statuses = Arrays.asList(OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS);
        }

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ServiceOrder> orderPage = serviceOrderService.listTechnicianOrders(userDetails.getId(), statuses, pageRequest);

        List<OrderSummaryItemDto> items = orderPage.getContent().stream()
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

        return ResponseEntity.ok(new PagedOrdersResponse(
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                items
        ));
    }

    /**
     * EP-005: Consultar Detalhes de uma Ordem de Serviço (com proteção Anti-IDOR/BOLA)
     */
    @GetMapping("/api/v1/orders/{orderId}")
    @Operation(summary = "EP-005: Consultar detalhes da ordem", description = "Retorna detalhes completos. Se TECHNICIAN, exige ser o responsável atribuído")
    public ResponseEntity<OrderDetailDto> getOrderDetails(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ServiceOrder order = serviceOrderService.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada com ID: " + orderId));

        User currentUser = userDetails.getUser();
        if (currentUser.getRole() == Role.TECHNICIAN) {
            if (order.getAssignedTechnician() == null || !order.getAssignedTechnician().getId().equals(currentUser.getId())) {
                throw new OrderAccessDeniedException("Esta ordem de serviço não está atribuída ao seu usuário.");
            }
        }

        EquipmentDetailDto eqDto = order.getEquipment() != null
                ? new EquipmentDetailDto(order.getEquipment().getId(), order.getEquipment().getAssetTag(), order.getEquipment().getName(), order.getEquipment().getModel())
                : null;

        SectorDetailDto secDto = order.getSector() != null
                ? new SectorDetailDto(order.getSector().getId(), order.getSector().getCode(), order.getSector().getName())
                : null;

        TechnicianDetailDto techDto = order.getAssignedTechnician() != null
                ? new TechnicianDetailDto(order.getAssignedTechnician().getId(), order.getAssignedTechnician().getFullName())
                : null;

        OrderDetailDto response = new OrderDetailDto(
                order.getId(),
                order.getOrderNumber(),
                order.getTitle(),
                order.getProblemDescription(),
                order.getSolutionDescription(),
                order.getStatus(),
                order.getPriority(),
                eqDto,
                secDto,
                techDto,
                order.getCreatedAt(),
                order.getAssignedAt(),
                order.getStartedAt(),
                order.getCompletedAt(),
                order.getAttendanceDurationMinutes(),
                order.getVersion()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * EP-006: Iniciar Atendimento da Ordem de Serviço
     */
    @PostMapping("/api/v1/orders/{orderId}/start")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "EP-006: Iniciar atendimento da ordem", description = "Transiciona de ASSIGNED para IN_PROGRESS com gravação de startedAt")
    public ResponseEntity<OrderActionResponse> startOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ServiceOrder order = serviceOrderService.startOrder(
                orderId,
                userDetails.getId(),
                originChannel != null ? originChannel : OriginChannel.CARDPUTER_TERMINAL,
                idempotencyKey
        );

        return ResponseEntity.ok(new OrderActionResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getStartedAt(),
                null,
                null,
                null,
                order.getVersion(),
                "Atendimento iniciado com sucesso."
        ));
    }

    /**
     * EP-007: Registrar Solução e Concluir Ordem de Serviço
     */
    @PostMapping("/api/v1/orders/{orderId}/complete")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "EP-007: Registrar solução e concluir ordem", description = "Transiciona de IN_PROGRESS para COMPLETED com cálculo de duração")
    public ResponseEntity<OrderActionResponse> completeOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCompletionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Origin-Channel", required = false) OriginChannel originChannel,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ServiceOrder order = serviceOrderService.completeOrder(
                orderId,
                userDetails.getId(),
                request,
                originChannel != null ? originChannel : OriginChannel.CARDPUTER_TERMINAL,
                idempotencyKey
        );

        return ResponseEntity.ok(new OrderActionResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getStartedAt(),
                order.getCompletedAt(),
                order.getAttendanceDurationMinutes(),
                order.getSolutionDescription(),
                order.getVersion(),
                "Ordem de serviço concluída com sucesso."
        ));
    }
}
