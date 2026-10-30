package br.com.aberturaordensservico.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aberturaordensservico.dto.EquipmentRequest;
import br.com.aberturaordensservico.dto.EquipmentResponse;
import br.com.aberturaordensservico.model.Equipment;
import br.com.aberturaordensservico.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/equipments")
@Tag(name = "Equipments Management", description = "Gestão de ativos industriais e equipamentos (EP-010)")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    private EquipmentResponse toResponse(Equipment eq) {
        return new EquipmentResponse(
                eq.getId(),
                eq.getAssetTag(),
                eq.getName(),
                eq.getModel(),
                eq.getSerialNumber(),
                eq.getSector() != null ? eq.getSector().getId() : null,
                eq.getSector() != null ? eq.getSector().getCode() : null,
                eq.getSector() != null ? eq.getSector().getName() : null,
                eq.isActive(),
                eq.getCreatedAt()
        );
    }

    /**
     * Cadastro de Equipamentos (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar equipamento", description = "Cadastra um novo ativo fabril vinculado a um setor")
    public ResponseEntity<EquipmentResponse> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        return equipmentService.createEquipment(request)
                .map(this::toResponse)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res))
                .orElse(ResponseEntity.badRequest().build());
    }

    /**
     * Listagem de Equipamentos Ativos (SUPERVISOR, ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Listar equipamentos ativos", description = "Retorna todos os equipamentos ativos")
    public ResponseEntity<List<EquipmentResponse>> listEquipments() {
        List<EquipmentResponse> list = equipmentService.listActive().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Listagem de Todos os Equipamentos incluindo inativos (ADMIN)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos os equipamentos", description = "Retorna todos os equipamentos cadastrados, incluindo inativos")
    public ResponseEntity<List<EquipmentResponse>> listAllEquipments() {
        List<EquipmentResponse> list = equipmentService.listAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Consulta de Equipamento por ID (SUPERVISOR, ADMIN)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Consultar equipamento por ID", description = "Busca detalhes de um equipamento específico")
    public ResponseEntity<EquipmentResponse> getEquipmentById(@PathVariable Long id) {
        return equipmentService.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Consulta de Equipamentos por Setor (SUPERVISOR, ADMIN)
     */
    @GetMapping("/sector/{sectorId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Listar equipamentos por setor", description = "Busca ativos pertencentes a um setor fabril")
    public ResponseEntity<List<EquipmentResponse>> getEquipmentsBySector(@PathVariable Long sectorId) {
        List<EquipmentResponse> list = equipmentService.findBySectorId(sectorId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Atualização de Equipamento (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar equipamento", description = "Atualiza atributos de um ativo industrial existente")
    public ResponseEntity<EquipmentResponse> updateEquipment(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.updateEquipment(id, request)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Inativação Lógica de Equipamento (BR-009) - via DELETE
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inativar equipamento (soft delete)", description = "Inativação lógica do ativo (active = false) preservando integridade com ordens anteriores (BR-009)")
    public ResponseEntity<Void> inactivateEquipment(@PathVariable Long id) {
        boolean inactivated = equipmentService.inactivateEquipment(id);
        return inactivated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Inativação Lógica de Equipamento (BR-009) - via PATCH
     */
    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inativar equipamento explicitamente", description = "Marca o equipamento como inativo (active = false)")
    public ResponseEntity<Void> inactivateEquipmentExplicit(@PathVariable Long id) {
        boolean inactivated = equipmentService.inactivateEquipment(id);
        return inactivated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
