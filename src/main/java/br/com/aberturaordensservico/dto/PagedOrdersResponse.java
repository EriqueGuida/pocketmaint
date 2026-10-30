package br.com.aberturaordensservico.dto;

import java.util.List;

public record PagedOrdersResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<OrderSummaryItemDto> items
) {}
