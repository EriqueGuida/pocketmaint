package br.com.pocketmaint.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.pocketmaint.dto.EquipmentRequest;
import br.com.pocketmaint.model.Equipment;
import br.com.pocketmaint.model.Sector;
import br.com.pocketmaint.repository.EquipmentRepository;
import br.com.pocketmaint.repository.SectorRepository;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final SectorRepository sectorRepository;

    public EquipmentService(EquipmentRepository equipmentRepository, SectorRepository sectorRepository) {
        this.equipmentRepository = equipmentRepository;
        this.sectorRepository = sectorRepository;
    }

    @Transactional
    public Optional<Equipment> createEquipment(EquipmentRequest request) {
        Optional<Sector> sectorOptional = sectorRepository.findById(request.sectorId());
        if (sectorOptional.isEmpty()) {
            return Optional.empty();
        }

        Equipment equipment = new Equipment();
        equipment.setAssetTag(request.assetTag().trim().toUpperCase());
        equipment.setName(request.name().trim());
        equipment.setModel(request.model());
        equipment.setSerialNumber(request.serialNumber());
        equipment.setSector(sectorOptional.get());
        equipment.setActive(true);
        equipment.setCreatedAt(Instant.now());

        return Optional.of(equipmentRepository.save(equipment));
    }

    public List<Equipment> listAll() {
        return equipmentRepository.findAll();
    }

    public List<Equipment> listActive() {
        return equipmentRepository.findByActiveTrue();
    }

    public Optional<Equipment> findById(Long id) {
        return equipmentRepository.findById(id);
    }

    public Optional<Equipment> findByAssetTag(String assetTag) {
        return equipmentRepository.findByAssetTag(assetTag);
    }

    public List<Equipment> findBySectorId(Long sectorId) {
        return equipmentRepository.findBySectorIdAndActiveTrue(sectorId);
    }

    @Transactional
    public Optional<Equipment> updateEquipment(Long id, EquipmentRequest request) {
        return equipmentRepository.findById(id).flatMap(equipment -> {
            Optional<Sector> sectorOptional = sectorRepository.findById(request.sectorId());
            if (sectorOptional.isEmpty()) {
                return Optional.empty();
            }

            equipment.setName(request.name().trim());
            equipment.setAssetTag(request.assetTag().trim().toUpperCase());
            equipment.setModel(request.model());
            equipment.setSerialNumber(request.serialNumber());
            equipment.setSector(sectorOptional.get());

            return Optional.of(equipmentRepository.save(equipment));
        });
    }

    @Transactional
    public boolean inactivateEquipment(Long id) {
        return equipmentRepository.findById(id).map(equipment -> {
            equipment.setActive(false);
            equipmentRepository.save(equipment);
            return true;
        }).orElse(false);
    }
}
