package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.medicalRecord.CreateMedicalRecordDto;
import com.hospital.backendHospital.models.dto.medicalRecord.MedicalRecordResponseDto;
import com.hospital.backendHospital.models.dto.medicalRecord.UpdateMedicalRecordDto;
import com.hospital.backendHospital.services.IMedicalRecordService;
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
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

    private final IMedicalRecordService medicalRecordService;
    private final AuthService authService;

    @GetMapping("/patients/{patientId}")
    public Page<MedicalRecordResponseDto> listByPatient(@PathVariable Long patientId,
                                                        @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return medicalRecordService.listByPatient(patientId, pageable);
    }

    @GetMapping("/me")
    public Page<MedicalRecordResponseDto> listMine(@PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return medicalRecordService.listMine(authService.getAuthenticatedUser(), pageable);
    }

    @PostMapping
    public ResponseEntity<MedicalRecordResponseDto> createMedicalRecord(@Valid @RequestBody CreateMedicalRecordDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalRecordService.createMedicalRecord(authService.getAuthenticatedUser(), dto));
    }

    @PatchMapping("/{id}")
    public MedicalRecordResponseDto updateMedicalRecord(@PathVariable Long id, @Valid @RequestBody UpdateMedicalRecordDto dto) {
        return medicalRecordService.updateMedicalRecord(authService.getAuthenticatedUser(), id, dto);
    }
}
