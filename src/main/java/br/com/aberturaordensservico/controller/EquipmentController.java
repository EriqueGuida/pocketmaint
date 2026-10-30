package br.com.aberturaordensservico.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aberturaordensservico.dto.EquipmentRequest;
import br.com.aberturaordensservico.model.Equipment;
import br.com.aberturaordensservico.service.EquipmentService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    public ResponseEntity<Equipment> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        return equipmentService.createEquipment(request)
                .map(eq -> ResponseEntity.status(HttpStatus.CREATED).body(eq))
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping
    public List<Equipment> listEquipments() {
        return equipmentService.listActive();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Equipment> getEquipmentById(@PathVariable Long id) {
        return equipmentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sector/{sectorId}")
    public List<Equipment> getEquipmentsBySector(@PathVariable Long sectorId) {
        return equipmentService.findBySectorId(sectorId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Equipment> updateEquipment(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.updateEquipment(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inactivateEquipment(@PathVariable Long id) {
        boolean removed = equipmentService.inactivateEquipment(id);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
