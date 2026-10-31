package br.com.pocketmaint.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.pocketmaint.dto.SectorRequest;
import br.com.pocketmaint.model.Sector;
import br.com.pocketmaint.repository.SectorRepository;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;

    public SectorService(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    @Transactional
    public Sector createSector(SectorRequest request) {
        Sector sector = new Sector();
        sector.setCode(request.code().trim().toUpperCase());
        sector.setName(request.name().trim());
        sector.setDescription(request.description());
        sector.setActive(true);
        sector.setCreatedAt(Instant.now());
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
            sector.setName(request.name().trim());
            sector.setDescription(request.description());
            if (request.code() != null && !request.code().isBlank()) {
                sector.setCode(request.code().trim().toUpperCase());
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
