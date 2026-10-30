package br.com.aberturaordensservico.dto;

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
public class EquipmentRequest {

    @NotBlank(message = "A etiqueta de patrimônio (asset tag) não pode ser vazia")
    @Size(max = 50, message = "A etiqueta de patrimônio deve ter no máximo 50 caracteres")
    private String assetTag;

    @NotBlank(message = "O nome do equipamento não pode ser vazio")
    @Size(max = 100, message = "O nome do equipamento deve ter no máximo 100 caracteres")
    private String name;

    @Size(max = 100, message = "O modelo deve ter no máximo 100 caracteres")
    private String model;

    @Size(max = 100, message = "O número de série deve ter no máximo 100 caracteres")
    private String serialNumber;

    @NotNull(message = "O ID do setor não pode ser nulo")
    private Long sectorId;
}
