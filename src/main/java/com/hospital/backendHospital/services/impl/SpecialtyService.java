package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.SpecialtyMapper;
import com.hospital.backendHospital.models.dto.specialty.CreateSpecialtyDto;
import com.hospital.backendHospital.models.dto.specialty.SpecialtyResponseDto;
import com.hospital.backendHospital.models.dto.specialty.UpdateSpecialtyDto;
import com.hospital.backendHospital.models.entity.Specialty;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.SpecialtyRepository;
import com.hospital.backendHospital.services.ISpecialtyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyService implements ISpecialtyService {

    private final SpecialtyRepository specialtyRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyMapper specialtyMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SpecialtyResponseDto> listSpecialties(Boolean active) {
        List<Specialty> specialties = active == null
                ? specialtyRepository.findAllByOrderByNameAsc()
                : specialtyRepository.findByActiveOrderByNameAsc(active);
        return specialtyMapper.toListDtos(specialties);
    }

    @Override
    @Transactional(readOnly = true)
    public SpecialtyResponseDto getSpecialty(Long id) {
        return specialtyMapper.toResponseDto(findSpecialty(id));
    }

    @Override
    @Transactional
    public SpecialtyResponseDto createSpecialty(CreateSpecialtyDto dto) {
        String name = dto.getName().trim();
        if (specialtyRepository.existsByName(name)){
            throw new ConflictException("Ya existe la especialidad " + name);
        }
        Specialty specialty = Specialty.builder()
                .name(name)
                .description(dto.getDescription().trim())
                .cost(dto.getCost())
                .build();
        specialtyRepository.save(specialty);
        return specialtyMapper.toResponseDto(specialty);
    }

    @Override
    @Transactional
    public SpecialtyResponseDto updateSpecialty(Long id, UpdateSpecialtyDto dto) {
        Specialty specialty = findSpecialty(id);
        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (specialtyRepository.existsByNameAndIdNot(name, id)) {
                throw new ConflictException("Ya existe la especialidad " + name);
            }
            specialty.setName(name);
        }
        if (dto.getDescription() != null) {
            specialty.setDescription(dto.getDescription().trim());
        }
        if (dto.getCost() != null) {
            // Solo afecta a las citas nuevas: cada cita guarda su total al agendarse
            specialty.setCost(dto.getCost());
        }
        if (dto.getActive() != null) {
            if (!dto.getActive() && specialty.isActive() && doctorRepository.countBySpecialtyIdAndActiveTrue(id) > 0) {
                throw new InvalidDataException("No se puede desactivar " + specialty.getName()
                        + " porque tiene doctores activos; cámbialos de especialidad o desactívalos primero");
            }
            specialty.setActive(dto.getActive());
        }
        specialtyRepository.save(specialty);
        return specialtyMapper.toResponseDto(specialty);
    }

    private Specialty findSpecialty(Long id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la especialidad con id " + id));
    }
}
