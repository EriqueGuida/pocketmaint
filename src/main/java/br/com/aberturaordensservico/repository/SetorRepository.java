package br.com.aberturaordensservico.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.aberturaordensservico.model.Setor;

public interface SetorRepository  extends JpaRepository<Setor, Long> {
    
}
