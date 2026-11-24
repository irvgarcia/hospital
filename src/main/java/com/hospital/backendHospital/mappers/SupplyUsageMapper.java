package com.hospital.backendHospital.mappers;

import com.hospital.backendHospital.models.dto.supplyUsage.SupplyUsageResponseDto;
import com.hospital.backendHospital.models.entity.SupplyUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplyUsageMapper {

    @Mapping(target = "hospitalizationId", source = "hospitalization.id")
    @Mapping(target = "supplyId", source = "supply.id")
    @Mapping(target = "supplyName", source = "supply.name")
    @Mapping(target = "unit", source = "supply.unit")
    SupplyUsageResponseDto toResponseDto(SupplyUsage supplyUsage);

    List<SupplyUsageResponseDto> toListDto(List<SupplyUsage> supplyUsages);
}
