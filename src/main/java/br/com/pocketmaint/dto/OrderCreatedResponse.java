package br.com.pocketmaint.dto;

import java.time.Instant;

import br.com.pocketmaint.model.enums.OrderStatus;
import br.com.pocketmaint.model.enums.Priority;

public record OrderCreatedResponse(
        Long id,
        String orderNumber,
        String title,
        String problemDescription,
        OrderStatus status,
        Priority priority,
        Long equipmentId,
        String equipmentName,
        Long sectorId,
        String sectorCode,
        Instant createdAt,
        Long version,
        String message
) {}
