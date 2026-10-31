package br.com.pocketmaint.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.pocketmaint.model.ServiceOrder;
import br.com.pocketmaint.model.enums.OrderStatus;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    Optional<ServiceOrder> findByOrderNumber(String orderNumber);

    Page<ServiceOrder> findByAssignedTechnicianIdAndStatusIn(
            Long technicianId,
            List<OrderStatus> statuses,
            Pageable pageable
    );

    List<ServiceOrder> findByStatus(OrderStatus status);

    List<ServiceOrder> findByAssignedTechnicianId(Long technicianId);
}
