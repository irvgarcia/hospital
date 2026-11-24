package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.hospitalization.CreateHospitalizationDto;
import com.hospital.backendHospital.models.dto.hospitalization.HospitalizationResponseDto;
import com.hospital.backendHospital.models.dto.supplyUsage.SupplyUsageResponseDto;
import com.hospital.backendHospital.models.filters.HospitalizationFilterRequest;
import com.hospital.backendHospital.services.IHospitalizationService;
import com.hospital.backendHospital.services.ISupplyUsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hospitalizations")
public class HospitalizationController {

    private final IHospitalizationService hospitalizationService;
    private final ISupplyUsageService supplyUsageService;
    private final AuthService authService;

    @GetMapping
    public Page<HospitalizationResponseDto> filterHospitalizations(
            HospitalizationFilterRequest filter,
            @PageableDefault(size = 10, sort = "admissionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return hospitalizationService.filterHospitalizations(filter, pageable);
    }

    /** Hospitalizaciones del paciente autenticado. */
    @GetMapping("/me")
    public List<HospitalizationResponseDto> getMyHospitalizations() {
        return hospitalizationService.listMine(authService.getAuthenticatedUser());
    }

    @GetMapping("/{id}")
    public HospitalizationResponseDto getHospitalization(@PathVariable Long id) {
        return hospitalizationService.getHospitalization(id);
    }

    @GetMapping("/{id}/supply-usages")
    public List<SupplyUsageResponseDto> getSupplyUsages(@PathVariable Long id) {
        return supplyUsageService.getUsageByHospitalization(id);
    }

    @PostMapping
    public ResponseEntity<HospitalizationResponseDto> createHospitalization(@Valid @RequestBody CreateHospitalizationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hospitalizationService.createHospitalization(authService.getAuthenticatedUser(), dto));
    }

    @PatchMapping("/{id}/discharge")
    public HospitalizationResponseDto discharge(@PathVariable Long id) {
        return hospitalizationService.discharge(id);
    }
}
