package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.dto.medicalRecord.MedicalRecordResponseDto;
import com.hospital.backendHospital.models.entity.MedicalRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = NameMapper.class)
public interface MedicalRecordMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.user", qualifiedByName = "fullName")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.user", qualifiedByName = "fullName")
    @Mapping(target = "specialty", source = "doctor.specialty.name")
    @Mapping(target = "appointmentId", source = "appointment.id")
    MedicalRecordResponseDto toResponseDto(MedicalRecord medicalRecord);
}
