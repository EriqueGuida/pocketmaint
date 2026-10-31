package br.com.pocketmaint.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String service,
        String version,
        Instant serverTime
) {}
