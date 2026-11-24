package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.dto.appointment.AppointmentResponseDto;
import com.hospital.backendHospital.models.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = NameMapper.class)
public interface AppointmentMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.user", qualifiedByName = "fullName")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.user", qualifiedByName = "fullName")
    @Mapping(target = "specialty", source = "doctor.specialty.name")
    AppointmentResponseDto toResponseDto(Appointment appointment);
}
