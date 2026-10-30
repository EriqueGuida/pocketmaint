package br.com.aberturaordensservico.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aberturaordensservico.dto.EquipmentRequest;
import br.com.aberturaordensservico.model.Equipment;
import br.com.aberturaordensservico.model.Sector;
import br.com.aberturaordensservico.repository.EquipmentRepository;
import br.com.aberturaordensservico.repository.SectorRepository;

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
        Optional<Sector> sectorOptional = sectorRepository.findById(request.getSectorId());
        if (sectorOptional.isEmpty()) {
            return Optional.empty();
        }

        Equipment equipment = Equipment.builder()
                .assetTag(request.getAssetTag().trim().toUpperCase())
                .name(request.getName().trim())
                .model(request.getModel())
                .serialNumber(request.getSerialNumber())
                .sector(sectorOptional.get())
                .active(true)
                .build();

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
            Optional<Sector> sectorOptional = sectorRepository.findById(request.getSectorId());
            if (sectorOptional.isEmpty()) {
                return Optional.empty();
            }

            equipment.setName(request.getName().trim());
            equipment.setAssetTag(request.getAssetTag().trim().toUpperCase());
            equipment.setModel(request.getModel());
            equipment.setSerialNumber(request.getSerialNumber());
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
