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

import br.com.aberturaordensservico.dto.SectorRequest;
import br.com.aberturaordensservico.model.Sector;
import br.com.aberturaordensservico.service.SectorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sectors")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @PostMapping
    public ResponseEntity<Sector> createSector(@Valid @RequestBody SectorRequest request) {
        Sector created = sectorService.createSector(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Sector> listSectors() {
        return sectorService.listActive();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sector> getSectorById(@PathVariable Long id) {
        return sectorService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sector> updateSector(@PathVariable Long id, @Valid @RequestBody SectorRequest request) {
        return sectorService.updateSector(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inactivateSector(@PathVariable Long id) {
        boolean removed = sectorService.inactivateSector(id);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
