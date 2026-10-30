package br.com.aberturaordensservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@NoArgsConstructor 
public class EquipamentoRequest {
    
    @NotBlank (message = "O nome do equipamento não pode ser vazio")
    private String nome;

    @NotBlank (message = "O número de patrimônio do equipamento não pode ser vazio")
    private String numeroPatrimonio;

    @NotNull (message = "O ID do setor não pode ser nulo")
    private Long setorId;
}
