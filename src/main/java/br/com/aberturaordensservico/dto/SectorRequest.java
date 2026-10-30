package br.com.aberturaordensservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectorRequest(
        @NotBlank(message = "Código é obrigatório")
        @Size(max = 20, message = "Código deve ter no máximo 20 caracteres")
        String code,

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String name,

        String description
) {}
