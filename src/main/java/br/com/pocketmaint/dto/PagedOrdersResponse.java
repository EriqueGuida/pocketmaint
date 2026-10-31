package br.com.pocketmaint.dto;

import java.util.List;

public record PagedOrdersResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<OrderSummaryItemDto> items
) {}
