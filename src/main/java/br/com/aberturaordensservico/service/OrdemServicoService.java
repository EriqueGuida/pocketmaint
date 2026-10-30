package br.com.aberturaordensservico.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.com.aberturaordensservico.dto.EncerramentoOrdemServicoRequest;
import br.com.aberturaordensservico.dto.OrdemServicoRequest;
import br.com.aberturaordensservico.model.Equipamento;
import br.com.aberturaordensservico.model.OrdemServico;
import br.com.aberturaordensservico.repository.EquipamentoRepository;
import br.com.aberturaordensservico.repository.OrdemServicoRepository;

@Service
public class OrdemServicoService {
    
    OrdemServicoRepository ordemServicoRepository;
    EquipamentoRepository equipamentoRepository;

    public OrdemServicoService(OrdemServicoRepository ordemServicoRepository, EquipamentoRepository equipamentoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.equipamentoRepository = equipamentoRepository;
    }
    
    public Optional<OrdemServico> cadastrarOrdemServico(OrdemServicoRequest ordemServicoRequest) {
        Optional<Equipamento> equipamentoEncontrado = equipamentoRepository.findById(ordemServicoRequest.getEquipamentoId());

        if (equipamentoEncontrado.isEmpty()){
            return Optional.empty();
        }

        OrdemServico ordemServico = new OrdemServico();

        ordemServico.setDescricao(ordemServicoRequest.getDescricao());
        ordemServico.setDataAbertura(LocalDateTime.now());
        ordemServico.setEquipamento(equipamentoEncontrado.get());
        ordemServico.setDataFechamento(null);
        ordemServico.setSolucao(null);

        return Optional.of(ordemServicoRepository.save(ordemServico));
    }

    public Optional<OrdemServico> buscarOrdemServicoPorId(Long id) {
        return ordemServicoRepository.findById(id);
    }

    public List<OrdemServico> listarOrdensServico() {
        return ordemServicoRepository.findAll();
    }

    public Optional<OrdemServico> encerrar(Long id, EncerramentoOrdemServicoRequest dados) {
        Optional<OrdemServico> ordemEncontrada = ordemServicoRepository.findById(id);

        if (ordemEncontrada.isPresent()) {
            if (ordemEncontrada.get().getDataFechamento() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A ordem de serviço já foi encerrada.");
            }
            OrdemServico ordemServico = ordemEncontrada.get();
            ordemServico.setDataFechamento(LocalDateTime.now());
            ordemServico.setSolucao(dados.getSolucao());
            return Optional.of(ordemServicoRepository.save(ordemServico));
        } else {
            return Optional.empty();
        }
    }

    public List<OrdemServico> listarOrdensServicoAbertas() {
        return ordemServicoRepository.findByDataFechamentoIsNull();
    }
}
