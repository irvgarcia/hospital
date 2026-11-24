package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.dto.doctorSchedule.DoctorScheduleResponseDto;
import com.hospital.backendHospital.models.entity.DoctorSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = NameMapper.class)
public interface DoctorScheduleMapper {

    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.user", qualifiedByName = "fullName")
    DoctorScheduleResponseDto toResponseDto(DoctorSchedule doctorSchedule);
}
