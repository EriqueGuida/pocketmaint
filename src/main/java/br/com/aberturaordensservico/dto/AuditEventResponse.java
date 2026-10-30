package br.com.aberturaordensservico.dto;

import java.time.Instant;

import br.com.aberturaordensservico.model.enums.AuditEventType;
import br.com.aberturaordensservico.model.enums.OriginChannel;

public record AuditEventResponse(
        Long id,
        Long serviceOrderId,
        Long actorUserId,
        String actorName,
        AuditEventType eventType,
        String previousStatus,
        String newStatus,
        OriginChannel originChannel,
        String idempotencyKey,
        String details,
        Instant timestamp
) {}
