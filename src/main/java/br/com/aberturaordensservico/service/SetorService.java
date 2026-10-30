package br.com.aberturaordensservico.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.aberturaordensservico.model.Setor;
import br.com.aberturaordensservico.repository.SetorRepository;

@Service 
public class SetorService {

    private final SetorRepository setorRepository;

    // Constructor
    public SetorService(SetorRepository setorRepository) {
        this.setorRepository = setorRepository;
    }

    // Operações CRUD
    public Setor cadastrarSetor(Setor setor) {
        return setorRepository.save(setor);
    }

    public List<Setor> listarSetores() {
        return setorRepository.findAll();
    }

    public Setor buscarSetorPorId(Long id) {
        return setorRepository.findById(id).orElse(null);
    }

    public Setor atualizarSetor(Long id, Setor setorAtualizado) {
        Setor setorExistente = setorRepository.findById(id).orElse(null);
        if (setorExistente != null) {
            setorExistente.setNome(setorAtualizado.getNome());
            return setorRepository.save(setorExistente);
        }
        return null;
    }

    public boolean deletarSetor(Long id) {
        if (setorRepository.existsById(id)) {
            setorRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
