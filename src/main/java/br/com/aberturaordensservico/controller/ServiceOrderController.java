package br.com.aberturaordensservico.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aberturaordensservico.dto.OrderCompletionRequest;
import br.com.aberturaordensservico.dto.ServiceOrderRequest;
import br.com.aberturaordensservico.model.ServiceOrder;
import br.com.aberturaordensservico.model.enums.OrderStatus;
import br.com.aberturaordensservico.service.ServiceOrderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    public ServiceOrderController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @PostMapping
    public ResponseEntity<ServiceOrder> createOrder(@Valid @RequestBody ServiceOrderRequest request) {
        return serviceOrderService.createOrder(request, null)
                .map(order -> ResponseEntity.status(HttpStatus.CREATED).body(order))
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrder> getOrderById(@PathVariable Long id) {
        return serviceOrderService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<ServiceOrder> listOrders() {
        return serviceOrderService.listAll();
    }

    @GetMapping("/open")
    public List<ServiceOrder> listOpenOrders() {
        return serviceOrderService.listByStatus(OrderStatus.OPEN);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ServiceOrder> completeOrder(@PathVariable Long id, @Valid @RequestBody OrderCompletionRequest request) {
        return serviceOrderService.completeOrder(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
