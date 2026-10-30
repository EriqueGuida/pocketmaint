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

import br.com.aberturaordensservico.dto.SectorRequest;
import br.com.aberturaordensservico.dto.SectorResponse;
import br.com.aberturaordensservico.model.Sector;
import br.com.aberturaordensservico.service.SectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sectors")
@Tag(name = "Sectors Management", description = "Gestão de setores fabris e utilidades (EP-010)")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    private SectorResponse toResponse(Sector s) {
        return new SectorResponse(
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getDescription(),
                s.isActive(),
                s.getCreatedAt()
        );
    }

    /**
     * Cadastro de Setores (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar setor", description = "Cadastra um novo setor fabril ativo")
    public ResponseEntity<SectorResponse> createSector(@Valid @RequestBody SectorRequest request) {
        Sector created = sectorService.createSector(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    /**
     * Listagem de Setores Ativos (SUPERVISOR, ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Listar setores ativos", description = "Retorna todos os setores fabris ativos")
    public ResponseEntity<List<SectorResponse>> listSectors() {
        List<SectorResponse> list = sectorService.listActive().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Listagem de Todos os Setores incluindo inativos (ADMIN)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos os setores", description = "Retorna todos os setores cadastrados, incluindo inativos")
    public ResponseEntity<List<SectorResponse>> listAllSectors() {
        List<SectorResponse> list = sectorService.listAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Consulta de Setor por ID (SUPERVISOR, ADMIN)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Consultar setor por ID", description = "Busca detalhes de um setor específico")
    public ResponseEntity<SectorResponse> getSectorById(@PathVariable Long id) {
        return sectorService.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Atualização de Setor (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar setor", description = "Atualiza dados de um setor existente")
    public ResponseEntity<SectorResponse> updateSector(@PathVariable Long id, @Valid @RequestBody SectorRequest request) {
        return sectorService.updateSector(id, request)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Inativação Lógica de Setor (BR-009) - via DELETE
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inativar setor (soft delete)", description = "Executa a inativação lógica do setor (active = false) preservando integridade relacional (BR-009)")
    public ResponseEntity<Void> inactivateSector(@PathVariable Long id) {
        boolean inactivated = sectorService.inactivateSector(id);
        return inactivated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Inativação Lógica de Setor (BR-009) - via PATCH
     */
    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inativar setor explicitamente", description = "Marca o setor como inativo (active = false)")
    public ResponseEntity<Void> inactivateSectorExplicit(@PathVariable Long id) {
        boolean inactivated = sectorService.inactivateSector(id);
        return inactivated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
