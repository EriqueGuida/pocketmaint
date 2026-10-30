package br.com.aberturaordensservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderCompletionRequest(
        @NotBlank(message = "A descrição da solução não pode ser vazia")
        @Size(min = 10, max = 500, message = "A descrição da solução deve ter entre 10 e 500 caracteres")
        String solutionDescription
) {}
