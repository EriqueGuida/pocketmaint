package br.com.aberturaordensservico.dto;

import br.com.aberturaordensservico.model.enums.OriginChannel;
import br.com.aberturaordensservico.model.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceOrderRequest(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 100, message = "Título deve ter no máximo 100 caracteres")
        String title,

        @NotBlank(message = "Descrição do problema é obrigatória")
        String problemDescription,

        @NotNull(message = "Prioridade é obrigatória")
        Priority priority,

        OriginChannel originChannel,

        @NotNull(message = "O ID do equipamento é obrigatório")
        Long equipmentId,

        Long assignedTechnicianId
) {}
