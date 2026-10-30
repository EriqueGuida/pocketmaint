package br.com.aberturaordensservico.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aberturaordensservico.dto.SectorRequest;
import br.com.aberturaordensservico.model.Sector;
import br.com.aberturaordensservico.repository.SectorRepository;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;

    public SectorService(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    @Transactional
    public Sector createSector(SectorRequest request) {
        Sector sector = Sector.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(true)
                .build();
        return sectorRepository.save(sector);
    }

    public List<Sector> listAll() {
        return sectorRepository.findAll();
    }

    public List<Sector> listActive() {
        return sectorRepository.findByActiveTrue();
    }

    public Optional<Sector> findById(Long id) {
        return sectorRepository.findById(id);
    }

    public Optional<Sector> findByCode(String code) {
        return sectorRepository.findByCode(code);
    }

    @Transactional
    public Optional<Sector> updateSector(Long id, SectorRequest request) {
        return sectorRepository.findById(id).map(sector -> {
            sector.setName(request.getName().trim());
            sector.setDescription(request.getDescription());
            if (request.getCode() != null && !request.getCode().isBlank()) {
                sector.setCode(request.getCode().trim().toUpperCase());
            }
            return sectorRepository.save(sector);
        });
    }

    @Transactional
    public boolean inactivateSector(Long id) {
        return sectorRepository.findById(id).map(sector -> {
            sector.setActive(false);
            sectorRepository.save(sector);
            return true;
        }).orElse(false);
    }
}
