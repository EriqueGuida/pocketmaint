package br.com.pocketmaint.dto;

import java.time.Instant;

public record SectorResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean active,
        Instant createdAt
) {}
