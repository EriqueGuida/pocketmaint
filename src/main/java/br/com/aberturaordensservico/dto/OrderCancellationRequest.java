package br.com.aberturaordensservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderCancellationRequest(
        @NotBlank(message = "O motivo do cancelamento é obrigatório")
        @Size(min = 5, max = 255, message = "O motivo do cancelamento deve ter entre 5 e 255 caracteres")
        String reason
) {}
