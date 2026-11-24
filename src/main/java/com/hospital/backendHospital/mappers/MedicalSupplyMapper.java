package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.dto.medicalSupply.CreateMedicalSupplyDto;
import com.hospital.backendHospital.models.dto.medicalSupply.MedicalSupplyResponseDto;
import com.hospital.backendHospital.models.entity.MedicalSupply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MedicalSupplyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "version", ignore = true)
    MedicalSupply toEntity(CreateMedicalSupplyDto createMedicalSupplyDto);

    @Mapping(target = "lowStock", expression = "java(medicalSupply.getQuantity() <= medicalSupply.getMinimumStock())")
    MedicalSupplyResponseDto toResponseDto(MedicalSupply medicalSupply);
}
