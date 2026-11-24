package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.mappers.MedicalSupplyMapper;
import com.hospital.backendHospital.models.dto.medicalSupply.CreateMedicalSupplyDto;
import com.hospital.backendHospital.models.dto.medicalSupply.MedicalSupplyResponseDto;
import com.hospital.backendHospital.models.dto.medicalSupply.UpdateMedicalSupplyDto;
import com.hospital.backendHospital.models.entity.MedicalSupply;
import com.hospital.backendHospital.models.filters.MedicalSupplyFilterRequest;
import com.hospital.backendHospital.repositories.MedicalSupplyRepository;
import com.hospital.backendHospital.services.IMedicalSupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalSupplyService implements IMedicalSupplyService {

    private final MedicalSupplyRepository medicalSupplyRepository;
    private final MedicalSupplyMapper medicalSupplyMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalSupplyResponseDto> filterMedicalSupply(MedicalSupplyFilterRequest filter, Pageable pageable) {
        Specification<MedicalSupply> spec = Specification.where(null);
        // Busqueda parcial sin distinguir mayusculas; spec se reasigna porque Specification es inmutable
        if (filter.getName() != null && !filter.getName().isBlank()){
            String pattern = "%" + filter.getName().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }
        if (filter.getActive() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), filter.getActive()));
        }
        if (filter.getLowStock() != null) {
            spec = spec.and((root, query, cb) -> filter.getLowStock()
                    ? cb.lessThanOrEqualTo(root.get("quantity"), root.get("minimumStock"))
                    : cb.greaterThan(root.get("quantity"), root.get("minimumStock")));
        }
        return medicalSupplyRepository.findAll(spec, pageable).map(medicalSupplyMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalSupplyResponseDto> listLowStock() {
        return medicalSupplyRepository.findLowStock().stream().map(medicalSupplyMapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalSupplyResponseDto getSupply(Long id) {
        return medicalSupplyMapper.toResponseDto(findSupply(id));
    }

    @Override
    @Transactional
    public MedicalSupplyResponseDto createSupply(CreateMedicalSupplyDto dto) {
        String name = dto.getName().trim();
        if (medicalSupplyRepository.existsByName(name)){
            throw new ConflictException("Ya existe el insumo " + name);
        }
        MedicalSupply supply = MedicalSupply.builder()
                .name(name)
                .quantity(dto.getQuantity())
                .minimumStock(dto.getMinimumStock())
                .unit(dto.getUnit())
                .build();
        medicalSupplyRepository.save(supply);
        return medicalSupplyMapper.toResponseDto(supply);
    }

    @Override
    @Transactional
    public MedicalSupplyResponseDto updateSupply(Long id, UpdateMedicalSupplyDto dto) {
        MedicalSupply supply = findSupply(id);
        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (medicalSupplyRepository.existsByNameAndIdNot(name, id)) {
                throw new ConflictException("Ya existe el insumo " + name);
            }
            supply.setName(name);
        }
        if (dto.getMinimumStock() != null) supply.setMinimumStock(dto.getMinimumStock());
        if (dto.getUnit() != null) supply.setUnit(dto.getUnit());
        if (dto.getActive() != null) supply.setActive(dto.getActive());
        medicalSupplyRepository.save(supply);
        return medicalSupplyMapper.toResponseDto(supply);
    }

    private MedicalSupply findSupply(Long id) {
        return medicalSupplyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el insumo con id " + id));
    }
}
