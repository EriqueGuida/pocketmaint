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
public class SectorRequest {

    @NotBlank(message = "O código do setor não pode ser vazio")
    @Size(max = 20, message = "O código do setor deve ter no máximo 20 caracteres")
    private String code;

    @NotBlank(message = "O nome do setor não pode ser vazio")
    @Size(max = 100, message = "O nome do setor deve ter no máximo 100 caracteres")
    private String name;

    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
    private String description;
}
