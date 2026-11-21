package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.dto.hospitalization.HospitalizationResponseDto;
import com.hospital.backendHospital.models.entity.Hospitalization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = NameMapper.class)
public interface HospitalizationMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.user", qualifiedByName = "fullName")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.user", qualifiedByName = "fullName")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomName", source = "room.name")
    HospitalizationResponseDto toDto(Hospitalization hospitalization);
}
