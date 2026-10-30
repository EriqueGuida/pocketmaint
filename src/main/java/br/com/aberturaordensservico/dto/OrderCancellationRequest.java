package br.com.aberturaordensservico.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderCancellationRequest(
        @NotBlank(message = "O motivo do cancelamento é obrigatório")
        @Size(min = 5, max = 255, message = "O motivo do cancelamento deve ter entre 5 e 255 caracteres")
        @JsonAlias({"cancellationReason", "reason", "motivo"})
        String reason
) {
    public String cancellationReason() {
        return reason;
    }
}
