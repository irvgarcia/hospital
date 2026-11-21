package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.models.dto.receptionist.CreateReceptionistDto;
import com.hospital.backendHospital.models.dto.receptionist.ReceptionistResponseDto;
import com.hospital.backendHospital.models.dto.receptionist.UpdateReceptionistDto;
import com.hospital.backendHospital.services.IReceptionistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/receptionists")
public class ReceptionistController {

    private final IReceptionistService receptionistService;

    @GetMapping
    public List<ReceptionistResponseDto> getReceptionists() {
        return receptionistService.getReceptionists();
    }

    @GetMapping("/{id}")
    public ReceptionistResponseDto getReceptionist(@PathVariable Long id) {
        return receptionistService.getReceptionist(id);
    }

    @PostMapping
    public ResponseEntity<ReceptionistResponseDto> createReceptionist(@Valid @RequestBody CreateReceptionistDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receptionistService.createReceptionist(dto));
    }

    @PatchMapping("/{id}")
    public ReceptionistResponseDto updateReceptionist(@PathVariable Long id, @Valid @RequestBody UpdateReceptionistDto dto) {
        return receptionistService.updateReceptionist(id, dto);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateReceptionist(@PathVariable Long id) {
        receptionistService.deactivateReceptionist(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateReceptionist(@PathVariable Long id) {
        receptionistService.activateReceptionist(id);
        return ResponseEntity.noContent().build();
    }
}
