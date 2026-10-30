package br.com.aberturaordensservico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.aberturaordensservico.model.Equipment;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByAssetTag(String assetTag);

    List<Equipment> findBySectorIdAndActiveTrue(Long sectorId);

    List<Equipment> findByActiveTrue();
}
