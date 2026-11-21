package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.supplyMovement.CreateSupplyMovementDto;
import com.hospital.backendHospital.models.dto.supplyMovement.SupplyMovementResponseDto;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.SupplyMovementFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISupplyMovementService {

    Page<SupplyMovementResponseDto> getMovements(SupplyMovementFilterRequest filter, Pageable pageable);

    SupplyMovementResponseDto registerMovement(User user, CreateSupplyMovementDto createSupplyMovementDto);
}
