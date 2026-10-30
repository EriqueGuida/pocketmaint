package br.com.aberturaordensservico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.aberturaordensservico.model.OrderAssignment;

@Repository
public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {

    List<OrderAssignment> findByServiceOrderIdOrderByAssignedAtDesc(Long serviceOrderId);

    Optional<OrderAssignment> findByServiceOrderIdAndActiveAssignmentTrue(Long serviceOrderId);
}
