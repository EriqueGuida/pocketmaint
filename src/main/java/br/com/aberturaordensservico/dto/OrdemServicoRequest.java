package br.com.aberturaordensservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter
@NoArgsConstructor
public class OrdemServicoRequest {
    
    @NotBlank (message = "A descrição da ordem de serviço não pode ser vazia")
    private String descricao;

    @NotNull (message = "O ID do equipamento da ordem de serviço não pode ser nulo")
    private Long equipamentoId;
}
