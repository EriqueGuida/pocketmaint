package br.com.pocketmaint.dto;

import java.time.Instant;

import br.com.pocketmaint.model.enums.OrderStatus;

public record OrderCancellationResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        Instant cancelledAt,
        String cancellationReason,
        Long version,
        String message
) {}
