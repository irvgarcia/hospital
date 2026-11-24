package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.supplyMovement.CreateSupplyMovementDto;
import com.hospital.backendHospital.models.dto.supplyMovement.SupplyMovementResponseDto;
import com.hospital.backendHospital.models.filters.SupplyMovementFilterRequest;
import com.hospital.backendHospital.services.ISupplyMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply-movements")
public class SupplyMovementController {

    private final ISupplyMovementService supplyMovementService;
    private final AuthService authService;

    @GetMapping
    public Page<SupplyMovementResponseDto> getMovements(SupplyMovementFilterRequest filter,
                                                        @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return supplyMovementService.getMovements(filter, pageable);
    }

    /** Entrada (type=IN) o salida manual (type=OUT) de inventario. */
    @PostMapping
    public ResponseEntity<SupplyMovementResponseDto> registerMovement(@Valid @RequestBody CreateSupplyMovementDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplyMovementService.registerMovement(authService.getAuthenticatedUser(), dto));
    }
}
