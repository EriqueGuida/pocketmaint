package br.com.aberturaordensservico.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.aberturaordensservico.model.Equipamento;

public interface EquipamentoRepository  extends JpaRepository<Equipamento, Long> {
    
    public List<Equipamento> findBySetorId(Integer setorId);
}
