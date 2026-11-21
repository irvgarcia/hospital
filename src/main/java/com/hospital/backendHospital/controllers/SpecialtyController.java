package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.specialty.CreateSpecialtyDto;
import com.hospital.backendHospital.models.dto.specialty.SpecialtyResponseDto;
import com.hospital.backendHospital.models.dto.specialty.UpdateSpecialtyDto;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.services.ISpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/specialties")
public class SpecialtyController {

    private final ISpecialtyService specialtyService;
    private final AuthService authService;

    /** Un PACIENTE solo recibe las especialidades activas. */
    @GetMapping
    public List<SpecialtyResponseDto> listSpecialties(@RequestParam(required = false) Boolean active) {
        boolean patient = authService.getAuthenticatedUser().hasRole(RoleEnum.PATIENT);
        return specialtyService.listSpecialties(patient ? Boolean.TRUE : active);
    }

    @GetMapping("/{id}")
    public SpecialtyResponseDto getSpecialty(@PathVariable Long id) {
        return specialtyService.getSpecialty(id);
    }

    @PostMapping
    public ResponseEntity<SpecialtyResponseDto> createSpecialty(@Valid @RequestBody CreateSpecialtyDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specialtyService.createSpecialty(dto));
    }

    @PatchMapping("/{id}")
    public SpecialtyResponseDto updateSpecialty(@PathVariable Long id, @Valid @RequestBody UpdateSpecialtyDto dto) {
        return specialtyService.updateSpecialty(id, dto);
    }
}
