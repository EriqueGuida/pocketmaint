package br.com.aberturaordensservico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.aberturaordensservico.model.Sector;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {

    Optional<Sector> findByCode(String code);

    List<Sector> findByActiveTrue();
}
