package br.com.pocketmaint.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.pocketmaint.model.AuditEvent;
import br.com.pocketmaint.model.enums.AuditEventType;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByServiceOrderIdOrderByOccurredAtAsc(Long serviceOrderId);

    boolean existsByServiceOrderIdAndEventTypeAndIdempotencyKey(
            Long serviceOrderId,
            AuditEventType eventType,
            String idempotencyKey
    );
}
