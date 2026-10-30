package br.com.aberturaordensservico.dto;

import java.time.Instant;

public record EquipmentResponse(
        Long id,
        String assetTag,
        String name,
        String model,
        String serialNumber,
        Long sectorId,
        String sectorCode,
        String sectorName,
        boolean active,
        Instant createdAt
) {}
