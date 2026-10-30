package br.com.aberturaordensservico.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String service,
        String version,
        Instant serverTime
) {}
