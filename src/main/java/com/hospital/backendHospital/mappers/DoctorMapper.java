package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.dto.doctor.DoctorResponseDto;
import com.hospital.backendHospital.models.entity.Doctor;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Comparator;

@Mapper(componentModel = "spring", uses = {NameMapper.class, DoctorScheduleMapper.class})
public interface DoctorMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "fullName", source = "user", qualifiedByName = "fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "specialtyId", source = "specialty.id")
    @Mapping(target = "specialty", source = "specialty.name")
    @Mapping(target = "consultationCost", source = "specialty.cost")
    DoctorResponseDto toResponseDto(Doctor doctor);

    /** Horarios en orden de lunes a domingo. */
    @AfterMapping
    default void sortSchedules(@MappingTarget DoctorResponseDto dto) {
        if (dto.getSchedules() != null) {
            dto.getSchedules().sort(Comparator.comparing(schedule -> schedule.getDayOfWeek()));
        }
    }
}
