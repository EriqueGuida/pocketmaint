package br.com.aberturaordensservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancellationRequest {

    @NotBlank(message = "A justificativa de cancelamento não pode ser vazia")
    @Size(min = 10, max = 300, message = "A justificativa de cancelamento deve ter entre 10 e 300 caracteres")
    private String cancellationReason;
}
