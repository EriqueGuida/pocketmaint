package br.com.pocketmaint.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderCancellationRequest(
        @NotBlank(message = "O motivo do cancelamento é obrigatório")
        @Size(min = 10, max = 300, message = "O motivo do cancelamento deve ter entre 10 e 300 caracteres")
        @JsonAlias({"cancellationReason", "reason", "motivo"})
        String reason
) {
    public String cancellationReason() {
        return reason;
    }
}
