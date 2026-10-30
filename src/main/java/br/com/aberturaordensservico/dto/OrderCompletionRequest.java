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
public class OrderCompletionRequest {

    @NotBlank(message = "A descrição da solução não pode ser vazia")
    @Size(min = 10, max = 500, message = "A descrição da solução deve ter entre 10 e 500 caracteres")
    private String solutionDescription;
}
