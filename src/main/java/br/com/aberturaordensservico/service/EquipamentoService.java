package br.com.aberturaordensservico.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.aberturaordensservico.dto.EquipamentoRequest;
import br.com.aberturaordensservico.model.Equipamento;
import br.com.aberturaordensservico.model.Setor;
import br.com.aberturaordensservico.repository.EquipamentoRepository;
import br.com.aberturaordensservico.repository.SetorRepository;

@Service 
public class EquipamentoService {
    
    private final EquipamentoRepository equipamentoRepository;

    private final SetorRepository setorRepository;

    // Constructor
    public EquipamentoService(EquipamentoRepository equipamentoRepository, SetorRepository setorRepository) {
        this.equipamentoRepository = equipamentoRepository;
        this.setorRepository = setorRepository;
    }

    // Métodos do CRUD
    public Optional<Equipamento> cadastrarEquipamento(EquipamentoRequest equipamentoRequest) {
        Optional<Setor> setorEncontrado = setorRepository.findById(equipamentoRequest.getSetorId());

        if (setorEncontrado.isEmpty()){
            return Optional.empty();
        }

        Equipamento equipamento = new Equipamento();

        equipamento.setNome(equipamentoRequest.getNome());
        equipamento.setNumeroPatrimonio(equipamentoRequest.getNumeroPatrimonio());  
        equipamento.setSetor(setorEncontrado.get());

        return Optional.of(equipamentoRepository.save(equipamento));
    }

    public List<Equipamento> listarEquipamentos() {
        return equipamentoRepository.findAll();
    }

    public Equipamento buscarEquipamentoPorId(Long id) {
        return equipamentoRepository.findById(id).orElse(null);
    }

    
    public Equipamento atualizarEquipamento(Long equipamentoId, EquipamentoRequest equipamentoAtualizado) {
        Equipamento equipamentoExistente = equipamentoRepository.findById(equipamentoId).orElse(null);
        if (equipamentoExistente != null) {
            equipamentoExistente.setNome(equipamentoAtualizado.getNome());
            return equipamentoRepository.save(equipamentoExistente);
        }
        return null;
    }

    public boolean deletarEquipamento(Long id) {
        if (equipamentoRepository.existsById(id)) {
            equipamentoRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public List<Equipamento> buscarEquipamentosPorSetorId(Integer setorId) {
        return equipamentoRepository.findBySetorId(setorId);
    }
}
