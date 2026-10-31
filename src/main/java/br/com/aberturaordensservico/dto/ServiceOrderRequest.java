package br.com.aberturaordensservico.dto;

import br.com.aberturaordensservico.model.enums.OriginChannel;
import br.com.aberturaordensservico.model.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceOrderRequest(
        @NotBlank(message = "Título é obrigatório")
        @Size(min = 5, max = 60, message = "Título deve ter entre 5 e 60 caracteres")
        String title,

        @NotBlank(message = "Descrição do problema é obrigatória")
        @Size(min = 10, max = 500, message = "Descrição do problema deve ter entre 10 e 500 caracteres")
        String problemDescription,

        @NotNull(message = "Prioridade é obrigatória")
        Priority priority,

        OriginChannel originChannel,

        @NotNull(message = "O ID do equipamento é obrigatório")
        Long equipmentId,

        Long assignedTechnicianId
) {}
