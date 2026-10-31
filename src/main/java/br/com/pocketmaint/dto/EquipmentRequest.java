package br.com.pocketmaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipmentRequest(
        @NotBlank(message = "A tag do ativo é obrigatória")
        @Size(max = 30, message = "A tag do ativo deve ter no máximo 30 caracteres")
        String assetTag,

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        String model,
        String serialNumber,

        @NotNull(message = "O ID do setor é obrigatório")
        Long sectorId
) {}
