package br.com.pocketmaint.dto;

import java.time.Instant;

import br.com.pocketmaint.model.enums.AuditEventType;
import br.com.pocketmaint.model.enums.OriginChannel;

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
