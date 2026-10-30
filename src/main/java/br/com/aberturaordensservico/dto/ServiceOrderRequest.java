package br.com.aberturaordensservico.dto;

import br.com.aberturaordensservico.model.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ServiceOrderRequest {

    @NotNull(message = "O ID do equipamento não pode ser nulo")
    private Long equipmentId;

    @NotBlank(message = "O título da ordem de serviço não pode ser vazio")
    @Size(min = 5, max = 60, message = "O título deve ter entre 5 e 60 caracteres")
    private String title;

    @NotBlank(message = "A descrição do problema não pode ser vazia")
    @Size(min = 10, max = 500, message = "A descrição do problema deve ter entre 10 e 500 caracteres")
    private String problemDescription;

    @NotNull(message = "A prioridade da ordem não pode ser nula")
    private Priority priority;
}
