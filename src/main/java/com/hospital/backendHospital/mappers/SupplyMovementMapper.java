package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.dto.supplyMovement.SupplyMovementResponseDto;
import com.hospital.backendHospital.models.entity.SupplyMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = NameMapper.class)
public interface SupplyMovementMapper {

    @Mapping(target = "supplyId", source = "supply.id")
    @Mapping(target = "supplyName", source = "supply.name")
    @Mapping(target = "performedBy", source = "performedBy", qualifiedByName = "fullName")
    SupplyMovementResponseDto toResponseDto(SupplyMovement supplyMovement);
}
