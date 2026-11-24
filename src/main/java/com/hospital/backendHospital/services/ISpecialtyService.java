package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.specialty.CreateSpecialtyDto;
import com.hospital.backendHospital.models.dto.specialty.SpecialtyResponseDto;
import com.hospital.backendHospital.models.dto.specialty.UpdateSpecialtyDto;

import java.util.List;

public interface ISpecialtyService {

    List<SpecialtyResponseDto> listSpecialties(Boolean active);

    SpecialtyResponseDto getSpecialty(Long id);

    SpecialtyResponseDto createSpecialty(CreateSpecialtyDto createSpecialtyDto);

    SpecialtyResponseDto updateSpecialty(Long id, UpdateSpecialtyDto updateSpecialtyDto);
}
