package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.hospitalization.CreateHospitalizationDto;
import com.hospital.backendHospital.models.dto.hospitalization.HospitalizationResponseDto;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.HospitalizationFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IHospitalzationService {

    Page<HospitalizationResponseDto> filterHospitalization(HospitalizationFilterRequest filter, Pageable pageable);

    HospitalizationResponseDto createHospitalization(User user, CreateHospitalizationDto createHospitalizationDto);

    void dischargeDateById(Long id);
}
