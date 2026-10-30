package br.com.aberturaordensservico.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@NoArgsConstructor 
public class EncerramentoOrdemServicoRequest {
    
    @NotBlank (message = "A solução da ordem de serviço não pode ser vazia")
    private String solucao;
}
