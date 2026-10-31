package br.com.pocketmaint.dto;

public record EquipmentDetailDto(
        Long id,
        String assetTag,
        String name,
        String model
) {}
