package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.SupplyUsageMapper;
import com.hospital.backendHospital.models.dto.supplyUsage.CreateSupplyUsageDto;
import com.hospital.backendHospital.models.dto.supplyUsage.SupplyUsageResponseDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.HospitalizationRepository;
import com.hospital.backendHospital.repositories.MedicalSupplyRepository;
import com.hospital.backendHospital.repositories.SupplyMovementRepository;
import com.hospital.backendHospital.repositories.SupplyUsageRepository;
import com.hospital.backendHospital.services.ISupplyUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplyUsageService implements ISupplyUsageService {

    private final SupplyUsageRepository supplyUsageRepository;
    private final SupplyUsageMapper supplyUsageMapper;
    private final HospitalizationRepository hospitalizationRepository;
    private final MedicalSupplyRepository medicalSupplyRepository;
    private final SupplyMovementRepository supplyMovementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SupplyUsageResponseDto> getUsageByHospitalization(Long hospitalizationId) {
        if (!hospitalizationRepository.existsById(hospitalizationId)) {
            throw new EntityNotFoundException("No existe la hospitalización con id " + hospitalizationId);
        }
        // A7: antes devolvia los consumos de todas las hospitalizaciones
        return supplyUsageMapper.toListDto(supplyUsageRepository.findByHospitalizationIdOrderByUsedAtDesc(hospitalizationId));
    }

    @Override
    @Transactional
    public SupplyUsageResponseDto registerUsage(User user, CreateSupplyUsageDto dto) {
        Hospitalization hospitalization = hospitalizationRepository.findById(dto.getHospitalizationId())
                .orElseThrow(() -> new EntityNotFoundException("No existe la hospitalización con id " + dto.getHospitalizationId()));
        MedicalSupply supply = medicalSupplyRepository.findById(dto.getSupplyId())
                .orElseThrow(() -> new EntityNotFoundException("No existe el insumo con id " + dto.getSupplyId()));

        if (!hospitalization.isActive()){
            throw new InvalidDataException("La hospitalización ya terminó");
        }
        if (!supply.isActive()) {
            throw new InvalidDataException("El insumo " + supply.getName() + " está desactivado");
        }
        // M11: el stock puede bajar hasta 0 (el minimo es solo una alerta), nunca por debajo
        if (dto.getQuantityUsed() > supply.getQuantity()) {
            throw new InvalidDataException("No hay suficiente " + supply.getName() + " en inventario (disponible: " + supply.getQuantity() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        SupplyUsage usage = SupplyUsage.builder()
                .hospitalization(hospitalization)
                .supply(supply)
                .quantityUsed(dto.getQuantityUsed())
                .usedAt(now)
                .build();
        supplyUsageRepository.save(usage);

        // @Version en MedicalSupply: si otro movimiento cambio el stock al mismo tiempo, responde 409
        supply.setQuantity(supply.getQuantity() - dto.getQuantityUsed());
        medicalSupplyRepository.save(supply);

        supplyMovementRepository.save(SupplyMovement.builder()
                .supply(supply)
                .type(MovementType.OUT)
                .quantityChanged(dto.getQuantityUsed())
                .timestamp(now)
                .performedBy(user)
                .build());
        return supplyUsageMapper.toResponseDto(usage);
    }
}
