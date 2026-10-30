package br.com.aberturaordensservico.dto;

import br.com.aberturaordensservico.model.enums.OrderStatus;
import br.com.aberturaordensservico.model.enums.Priority;

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
