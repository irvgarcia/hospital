package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.supplyUsage.CreateSupplyUsageDto;
import com.hospital.backendHospital.models.dto.supplyUsage.SupplyUsageResponseDto;
import com.hospital.backendHospital.services.ISupplyUsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consumo de insumos en una hospitalizacion. El listado esta en GET /hospitalizations/{id}/supply-usages. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply-usages")
public class SupplyUsageController {

    private final ISupplyUsageService supplyUsageService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<SupplyUsageResponseDto> registerUsage(@Valid @RequestBody CreateSupplyUsageDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplyUsageService.registerUsage(authService.getAuthenticatedUser(), dto));
    }
}
