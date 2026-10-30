package br.com.aberturaordensservico.service;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aberturaordensservico.dto.OrderCompletionRequest;
import br.com.aberturaordensservico.dto.ServiceOrderRequest;
import br.com.aberturaordensservico.model.Equipment;
import br.com.aberturaordensservico.model.ServiceOrder;
import br.com.aberturaordensservico.model.User;
import br.com.aberturaordensservico.model.enums.OrderStatus;
import br.com.aberturaordensservico.repository.EquipmentRepository;
import br.com.aberturaordensservico.repository.ServiceOrderRepository;

@Service
public class ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final EquipmentRepository equipmentRepository;

    public ServiceOrderService(ServiceOrderRepository serviceOrderRepository, EquipmentRepository equipmentRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional
    public Optional<ServiceOrder> createOrder(ServiceOrderRequest request, User creator) {
        Optional<Equipment> equipmentOptional = equipmentRepository.findById(request.getEquipmentId());
        if (equipmentOptional.isEmpty()) {
            return Optional.empty();
        }

        Equipment equipment = equipmentOptional.get();

        long count = serviceOrderRepository.count() + 1;
        String orderNumber = String.format("OS-%d-%04d", Year.now().getValue(), count);

        ServiceOrder order = ServiceOrder.builder()
                .orderNumber(orderNumber)
                .equipment(equipment)
                .sector(equipment.getSector())
                .createdBy(creator)
                .title(request.getTitle().trim())
                .problemDescription(request.getProblemDescription().trim())
                .priority(request.getPriority())
                .status(OrderStatus.OPEN)
                .version(0L)
                .build();

        return Optional.of(serviceOrderRepository.save(order));
    }

    public Optional<ServiceOrder> findById(Long id) {
        return serviceOrderRepository.findById(id);
    }

    public Optional<ServiceOrder> findByOrderNumber(String orderNumber) {
        return serviceOrderRepository.findByOrderNumber(orderNumber);
    }

    public List<ServiceOrder> listAll() {
        return serviceOrderRepository.findAll();
    }

    public List<ServiceOrder> listByStatus(OrderStatus status) {
        return serviceOrderRepository.findByStatus(status);
    }

    public Page<ServiceOrder> listTechnicianOrders(Long technicianId, List<OrderStatus> statuses, Pageable pageable) {
        return serviceOrderRepository.findByAssignedTechnicianIdAndStatusIn(technicianId, statuses, pageable);
    }

    @Transactional
    public Optional<ServiceOrder> completeOrder(Long id, OrderCompletionRequest request) {
        return serviceOrderRepository.findById(id).map(order -> {
            order.setStatus(OrderStatus.COMPLETED);
            order.setSolutionDescription(request.getSolutionDescription().trim());
            order.setCompletedAt(Instant.now());
            return serviceOrderRepository.save(order);
        });
    }
}
