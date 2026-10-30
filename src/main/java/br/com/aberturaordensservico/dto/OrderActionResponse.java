package br.com.aberturaordensservico.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.aberturaordensservico.model.enums.OrderStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderActionResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        Instant startedAt,
        Instant completedAt,
        Integer attendanceDurationMinutes,
        String solutionDescription,
        Long version,
        String message
) {}
