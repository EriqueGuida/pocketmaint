package br.com.aberturaordensservico.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aberturaordensservico.dto.EncerramentoOrdemServicoRequest;
import br.com.aberturaordensservico.dto.OrdemServicoRequest;
import br.com.aberturaordensservico.model.OrdemServico;
import br.com.aberturaordensservico.service.OrdemServicoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping ("/ordens-servico")
public class OrdemServicoController {
    
    private OrdemServicoService ordemServicoService;

    public OrdemServicoController(OrdemServicoService ordemServicoService) {
        this.ordemServicoService = ordemServicoService;
    }

    @PostMapping 
    public Optional<OrdemServico> cadastrarOrdemServico(@Valid @RequestBody OrdemServicoRequest ordemServicoRequest) {
        return ordemServicoService.cadastrarOrdemServico(ordemServicoRequest);
    }
    
    @GetMapping ("/{id}")
    public Optional<OrdemServico> buscarOrdemServicoPorId(@PathVariable Long id) {
        return ordemServicoService.buscarOrdemServicoPorId(id);
    }

    @GetMapping
    public List<OrdemServico> listarOrdensServico() {
        return ordemServicoService.listarOrdensServico();
    }

    @PatchMapping ("/{id}/encerramento")
    public Optional<OrdemServico> encerrarOrdemServico(@PathVariable Long id, @Valid @RequestBody EncerramentoOrdemServicoRequest dados) {
        return ordemServicoService.encerrar(id, dados);
    }

    @GetMapping ("/abertas")
    public List<OrdemServico> listarOrdensServicoAbertas() {
        return ordemServicoService.listarOrdensServicoAbertas();
    }
}
