package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.medicalSupply.CreateMedicalSupplyDto;
import com.hospital.backendHospital.models.dto.medicalSupply.MedicalSupplyResponseDto;
import com.hospital.backendHospital.models.dto.medicalSupply.UpdateMedicalSupplyDto;
import com.hospital.backendHospital.models.filters.MedicalSupplyFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IMedicalSupplyService {

    Page<MedicalSupplyResponseDto> filterMedicalSupply(MedicalSupplyFilterRequest filter, Pageable pageable);

    List<MedicalSupplyResponseDto> listLowStock();

    MedicalSupplyResponseDto getSupply(Long id);

    MedicalSupplyResponseDto createSupply(CreateMedicalSupplyDto createMedicalSupplyDto);

    MedicalSupplyResponseDto updateSupply(Long id, UpdateMedicalSupplyDto updateMedicalSupplyDto);
}
