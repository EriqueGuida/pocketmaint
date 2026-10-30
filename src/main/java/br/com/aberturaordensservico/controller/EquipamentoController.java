package br.com.aberturaordensservico.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.aberturaordensservico.dto.EquipamentoRequest;
import br.com.aberturaordensservico.model.Equipamento;
import br.com.aberturaordensservico.service.EquipamentoService;
import jakarta.validation.Valid;

@RestController
public class EquipamentoController {
    
    private final EquipamentoService equipamentoService;
    
    // Constructor
    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }
    
    // Métodos do CRUD
    @PostMapping("/equipamentos")
    public Optional<Equipamento> cadastrarEquipamento(@Valid @RequestBody EquipamentoRequest equipamentoRequest) {
        return equipamentoService.cadastrarEquipamento(equipamentoRequest);
        
    }

    @GetMapping("/equipamentos")
    public List<Equipamento> listarEquipamentos() {
        return equipamentoService.listarEquipamentos();
    }

    @GetMapping ("/equipamentos/{id}")
    public Equipamento buscarEquipamentoPorId(@PathVariable Long id) {
        return equipamentoService.buscarEquipamentoPorId(id);
    }

    @PutMapping ("/equipamentos/{id}")
    public ResponseEntity<?> atualizarEquipamento(@PathVariable Long id, @Valid  @RequestBody EquipamentoRequest equipamentoAtualizado) {
        
        if (equipamentoService.buscarEquipamentoPorId(id) == null) {
            return ResponseEntity.status(404).body("Equipamento não encontrado");
        }
        return ResponseEntity.ok(equipamentoService.atualizarEquipamento(id, equipamentoAtualizado));
    }
    
    @DeleteMapping ("/equipamentos/{id}")
    public boolean deletarEquipamento(@PathVariable Long id) {
        return equipamentoService.deletarEquipamento(id);
    }

    @GetMapping("/equipamentos/setor/{setorId}")
    public List<Equipamento> buscarEquipamentosPorSetorId(@PathVariable Integer setorId) {
        return equipamentoService.buscarEquipamentosPorSetorId(setorId);
    }
}