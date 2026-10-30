package br.com.aberturaordensservico.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.aberturaordensservico.model.AuditEvent;
import br.com.aberturaordensservico.model.enums.AuditEventType;
import br.com.aberturaordensservico.model.enums.OriginChannel;
import br.com.aberturaordensservico.repository.AuditEventRepository;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Registra um evento de auditoria obrigatoriamente acoplado à transação ativa (BR-008).
     * Se uma Idempotency-Key já foi processada para a mesma ordem e tipo, evita duplicidade (BR-007).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<AuditEvent> recordEvent(
            Long serviceOrderId,
            Long actorUserId,
            AuditEventType eventType,
            String previousStatus,
            String newStatus,
            OriginChannel originChannel,
            String idempotencyKey,
            String details
    ) {
        // Verificação de idempotência (BR-007)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            boolean alreadyLogged = auditEventRepository.existsByServiceOrderIdAndEventTypeAndIdempotencyKey(
                    serviceOrderId, eventType, idempotencyKey
            );
            if (alreadyLogged) {
                return Optional.empty(); // Já registrado anteriormente com a mesma chave
            }
        }

        AuditEvent event = new AuditEvent();
        event.setServiceOrderId(serviceOrderId);
        event.setActorUserId(actorUserId);
        event.setEventType(eventType);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setOriginChannel(originChannel != null ? originChannel : OriginChannel.API_CLIENT);
        event.setIdempotencyKey(idempotencyKey);
        event.setDetails(details);
        event.setOccurredAt(Instant.now());

        return Optional.of(auditEventRepository.save(event));
    }

    public List<AuditEvent> getOrderAuditTrail(Long serviceOrderId) {
        return auditEventRepository.findByServiceOrderIdOrderByOccurredAtAsc(serviceOrderId);
    }
}
