package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.MedicalSupplyMapper;
import com.hospital.backendHospital.models.dto.medicalSupply.CreateMedicalSupplyDto;
import com.hospital.backendHospital.models.dto.medicalSupply.MedicalSupplyResponseDto;
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
        Specification<MedicalSupply> spec= Specification.where(null);

        // Busqueda parcial sin distinguir mayusculas; spec se reasigna porque Specification es inmutable
        if (filter.getName() != null && !filter.getName().isBlank()){
            String pattern = "%" + filter.getName().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), pattern));
        }

        return medicalSupplyRepository.findAll(spec, pageable)
                .map(medicalSupplyMapper::toResponseDto);
    }

    @Override
    @Transactional
    public MedicalSupplyResponseDto createSupply(CreateMedicalSupplyDto createMedicalSupplyDto) {

        if (medicalSupplyRepository.existsByName(createMedicalSupplyDto.getName())){
            throw new InvalidDataException("Medical supply already exists");
        }

        MedicalSupply medicalSupply = medicalSupplyMapper.toEntity(createMedicalSupplyDto);

        medicalSupplyRepository.save(medicalSupply);

        return medicalSupplyMapper.toResponseDto(medicalSupply);
    }
}
