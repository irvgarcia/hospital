package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.hospitalization.CreateHospitalizationDto;
import com.hospital.backendHospital.models.dto.hospitalization.HospitalizationResponseDto;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.HospitalizationFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IHospitalizationService {

    Page<HospitalizationResponseDto> filterHospitalizations(HospitalizationFilterRequest filter, Pageable pageable);

    List<HospitalizationResponseDto> listMine(User user);

    HospitalizationResponseDto getHospitalization(Long id);

    HospitalizationResponseDto createHospitalization(User user, CreateHospitalizationDto createHospitalizationDto);

    HospitalizationResponseDto discharge(Long id);
}
