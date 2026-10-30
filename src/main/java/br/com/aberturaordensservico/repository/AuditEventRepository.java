package br.com.aberturaordensservico.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.aberturaordensservico.model.AuditEvent;
import br.com.aberturaordensservico.model.enums.AuditEventType;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByServiceOrderIdOrderByOccurredAtAsc(Long serviceOrderId);

    boolean existsByServiceOrderIdAndEventTypeAndIdempotencyKey(
            Long serviceOrderId,
            AuditEventType eventType,
            String idempotencyKey
    );
}
