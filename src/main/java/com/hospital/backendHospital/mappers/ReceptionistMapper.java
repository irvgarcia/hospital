package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.dto.receptionist.ReceptionistResponseDto;
import com.hospital.backendHospital.models.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReceptionistMapper {

    ReceptionistResponseDto toDto(User user);

    List<ReceptionistResponseDto> toListDto(List<User> users);
}
