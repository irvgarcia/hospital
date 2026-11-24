package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.models.dto.medicalSupply.CreateMedicalSupplyDto;
import com.hospital.backendHospital.models.dto.medicalSupply.MedicalSupplyResponseDto;
import com.hospital.backendHospital.models.dto.medicalSupply.UpdateMedicalSupplyDto;
import com.hospital.backendHospital.models.filters.MedicalSupplyFilterRequest;
import com.hospital.backendHospital.services.IMedicalSupplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/medical-supplies")
public class MedicalSupplyController {

    private final IMedicalSupplyService medicalSupplyService;

    @GetMapping
    public Page<MedicalSupplyResponseDto> filterMedicalSupply(MedicalSupplyFilterRequest filter,
                                                              @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return medicalSupplyService.filterMedicalSupply(filter, pageable);
    }

    /** M9/M11: reporte de insumos activos en o por debajo de su stock minimo. */
    @GetMapping("/low-stock")
    public List<MedicalSupplyResponseDto> listLowStock() {
        return medicalSupplyService.listLowStock();
    }

    @GetMapping("/{id}")
    public MedicalSupplyResponseDto getSupply(@PathVariable Long id) {
        return medicalSupplyService.getSupply(id);
    }

    @PostMapping
    public ResponseEntity<MedicalSupplyResponseDto> createSupply(@Valid @RequestBody CreateMedicalSupplyDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalSupplyService.createSupply(dto));
    }

    @PatchMapping("/{id}")
    public MedicalSupplyResponseDto updateSupply(@PathVariable Long id, @Valid @RequestBody UpdateMedicalSupplyDto dto) {
        return medicalSupplyService.updateSupply(id, dto);
    }
}
