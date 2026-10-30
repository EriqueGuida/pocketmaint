package br.com.aberturaordensservico.dto;

import jakarta.validation.constraints.NotNull;

public record OrderAssignmentRequest(
        @NotNull(message = "O ID do técnico é obrigatório")
        Long technicianId,

        String notes
) {}
