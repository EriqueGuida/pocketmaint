package br.com.aberturaordensservico.dto;

public record EquipmentDetailDto(
        Long id,
        String assetTag,
        String name,
        String model
) {}
