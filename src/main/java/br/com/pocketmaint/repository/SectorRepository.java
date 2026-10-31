package br.com.pocketmaint.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.pocketmaint.model.Sector;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {

    Optional<Sector> findByCode(String code);

    List<Sector> findByActiveTrue();
}
