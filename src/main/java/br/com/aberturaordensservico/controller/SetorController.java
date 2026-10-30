package br.com.aberturaordensservico.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.aberturaordensservico.model.Setor;
import br.com.aberturaordensservico.service.SetorService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;


@RestController
public class SetorController {
    
    SetorService setorService;
    
    // Constructor
    public SetorController(SetorService setorService) {
        this.setorService = setorService;
    }

    @PostMapping ("/setores")
    public Setor cadastrarSetor(@RequestBody Setor setor) {
        System.out.println("Cadastrando setor: " + setor.getNome());
        return setorService.cadastrarSetor(setor);
    }

    @GetMapping("/setores")
    public List<Setor> listarSetores() {
        return setorService.listarSetores();
    }

    @GetMapping ("/setores/{id}")
    public Setor buscarSetorPorId(@PathVariable Long id) {
        return setorService.buscarSetorPorId(id);
    }

    @PutMapping ("/setores/{id}")
    public Setor atualizarSetor(@PathVariable Long id, @Valid @RequestBody Setor setorAtualizado) {
        return setorService.atualizarSetor(id, setorAtualizado);
    }

    @DeleteMapping ("/setores/{id}")
    public boolean deletarSetor(@PathVariable Long id) {
        return setorService.deletarSetor(id);
    }
}
