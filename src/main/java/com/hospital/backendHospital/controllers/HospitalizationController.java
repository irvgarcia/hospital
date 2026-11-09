package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.hospitalization.CreateHospitalizationDto;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.dto.hospitalization.HospitalizationResponseDto;
import com.hospital.backendHospital.models.filters.HospitalizationFilterRequest;
import com.hospital.backendHospital.services.IHospitalzationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hospitalizations")
public class HospitalizationController {

    private final IHospitalzationService hospitalizationService;
    private final AuthService authService;

    @GetMapping("/{id}")
    public ResponseEntity<Page<HospitalizationResponseDto>> getHospitalizationById
            (HospitalizationFilterRequest filter, @PageableDefault(page = 0, size = 5, sort = "admissionDate") Pageable pageable) {
        Page<HospitalizationResponseDto> hospitalizations = hospitalizationService.filterHospitalization(filter, pageable);

        return ResponseEntity.ok(hospitalizations);
    }

    @PostMapping
    public ResponseEntity<HospitalizationResponseDto> createHospitalization(@Valid @RequestBody CreateHospitalizationDto createHospitalizationDto) {
        User user = authService.getAuthenticatedUser();
        HospitalizationResponseDto response = hospitalizationService.createHospitalization(user, createHospitalizationDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/discharge")
    public ResponseEntity<Void> dischargePatient(@PathVariable Long id) {
        hospitalizationService.dischargeDateById(id);

        return ResponseEntity.noContent().build();
    }
}
