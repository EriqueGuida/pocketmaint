package br.com.pocketmaint.dto;

import br.com.pocketmaint.model.enums.OrderStatus;
import br.com.pocketmaint.model.enums.Priority;

public record OrderSummaryItemDto(
        Long id,
        String orderNumber,
        String title,
        OrderStatus status,
        Priority priority,
        String equipmentAssetTag,
        String equipmentName,
        String sectorCode
) {}
