package br.com.aberturaordensservico.dto;

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
public class OrderAssignmentRequest {

    @NotNull(message = "O ID do técnico não pode ser nulo")
    private Long technicianId;

    @Size(max = 200, message = "As observações devem ter no máximo 200 caracteres")
    private String notes;
}
