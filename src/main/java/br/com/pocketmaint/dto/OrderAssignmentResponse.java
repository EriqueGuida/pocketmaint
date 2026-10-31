package br.com.pocketmaint.dto;

import java.time.Instant;

import br.com.pocketmaint.model.enums.OrderStatus;

public record OrderAssignmentResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        Long assignedTechnicianId,
        String assignedTechnicianName,
        Instant assignedAt,
        Long version
) {}
