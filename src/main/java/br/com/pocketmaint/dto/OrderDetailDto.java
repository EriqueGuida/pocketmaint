package br.com.pocketmaint.dto;

import java.time.Instant;

import br.com.pocketmaint.model.enums.OrderStatus;
import br.com.pocketmaint.model.enums.Priority;

public record OrderDetailDto(
        Long id,
        String orderNumber,
        String title,
        String problemDescription,
        String solutionDescription,
        OrderStatus status,
        Priority priority,
        EquipmentDetailDto equipment,
        SectorDetailDto sector,
        TechnicianDetailDto assignedTechnician,
        Instant createdAt,
        Instant assignedAt,
        Instant startedAt,
        Instant completedAt,
        Integer attendanceDurationMinutes,
        Long version
) {}
