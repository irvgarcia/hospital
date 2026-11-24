package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.patient.CreatePatientDto;
import com.hospital.backendHospital.models.dto.patient.PatientResponseDto;
import com.hospital.backendHospital.models.dto.patient.RegisterPatientDto;
import com.hospital.backendHospital.models.dto.patient.UpdatePatientDto;
import com.hospital.backendHospital.models.entity.Patient;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.models.filters.PatientFilterRequest;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.IPDFGeneratorService;
import com.hospital.backendHospital.services.IPatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final IPatientService patientService;
    private final IPDFGeneratorService pdfGeneratorService;
    private final PatientRepository patientRepository;
    private final AuthService authService;

    @GetMapping
    public Page<PatientResponseDto> filterPatients(PatientFilterRequest filter, @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return patientService.filterPatients(filter, pageable);
    }

    /** Segundo paso del registro publico: el PACIENTE completa su perfil. */
    @PostMapping
    public ResponseEntity<PatientResponseDto> createPatient(@Valid @RequestBody CreatePatientDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.createPatient(authService.getAuthenticatedUser(), dto));
    }

    /** Registro en un paso por recepcion o un admin (usuario + perfil). */
    @PostMapping("/register")
    public ResponseEntity<PatientResponseDto> registerPatient(@Valid @RequestBody RegisterPatientDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.registerPatient(dto));
    }

    @GetMapping("/me")
    public PatientResponseDto getMyProfile() {
        return patientService.getMyProfile(authService.getAuthenticatedUser());
    }

    @PatchMapping("/me")
    public PatientResponseDto updateMyProfile(@Valid @RequestBody UpdatePatientDto dto) {
        return patientService.updateMyProfile(authService.getAuthenticatedUser(), dto);
    }

    @GetMapping("/me/medical-record/pdf")
    public ResponseEntity<byte[]> downloadMyMedicalRecord() {
        Patient patient = patientRepository.findByUser(authService.getAuthenticatedUser())
                .orElseThrow(() -> new EntityNotFoundException("Completa tu perfil de paciente antes de continuar"));
        return pdf(patient.getId());
    }

    @GetMapping("/{id}")
    public PatientResponseDto getPatient(@PathVariable Long id) {
        return patientService.getPatient(id);
    }

    @PatchMapping("/{id}")
    public PatientResponseDto updatePatient(@PathVariable Long id, @Valid @RequestBody UpdatePatientDto dto) {
        return patientService.updatePatient(id, dto);
    }

    @GetMapping("/{id}/medical-record/pdf")
    public ResponseEntity<byte[]> downloadMedicalRecord(@PathVariable Long id) {
        return pdf(id);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePatient(@PathVariable Long id) {
        patientService.deactivatePatient(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activatePatient(@PathVariable Long id) {
        patientService.activatePatient(id);
        return ResponseEntity.noContent().build();
    }

    /** M7: el PDF se genera completo antes de escribir la respuesta, asi un 404 sigue saliendo como JSON. */
    private ResponseEntity<byte[]> pdf(Long patientId) {
        byte[] content = pdfGeneratorService.generateMedicalRecord(patientId);
        String fileName = "expediente_" + patientId + "_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(content);
    }
}
