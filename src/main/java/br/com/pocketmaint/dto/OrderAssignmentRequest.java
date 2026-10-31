package br.com.pocketmaint.dto;

import jakarta.validation.constraints.NotNull;

public record OrderAssignmentRequest(
        @NotNull(message = "O ID do técnico é obrigatório")
        Long technicianId,

        String notes
) {}
