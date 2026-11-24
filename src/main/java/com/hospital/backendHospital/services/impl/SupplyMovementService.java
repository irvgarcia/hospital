package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.SupplyMovementMapper;
import com.hospital.backendHospital.models.dto.supplyMovement.CreateSupplyMovementDto;
import com.hospital.backendHospital.models.dto.supplyMovement.SupplyMovementResponseDto;
import com.hospital.backendHospital.models.entity.MedicalSupply;
import com.hospital.backendHospital.models.entity.MovementType;
import com.hospital.backendHospital.models.entity.SupplyMovement;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.SupplyMovementFilterRequest;
import com.hospital.backendHospital.repositories.MedicalSupplyRepository;
import com.hospital.backendHospital.repositories.SupplyMovementRepository;
import com.hospital.backendHospital.services.ISupplyMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SupplyMovementService implements ISupplyMovementService {

    private final SupplyMovementRepository supplyMovementRepository;
    private final MedicalSupplyRepository medicalSupplyRepository;
    private final SupplyMovementMapper supplyMovementMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<SupplyMovementResponseDto> getMovements(SupplyMovementFilterRequest filter, Pageable pageable) {
        Specification<SupplyMovement> spec = Specification.where(null);
        if (filter.getSupplyId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("supply").get("id"), filter.getSupplyId()));
        }
        if (filter.getType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), filter.getType()));
        }
        return supplyMovementRepository.findAll(spec, pageable).map(supplyMovementMapper::toResponseDto);
    }

    /**
     * Entrada (IN) o salida manual (OUT). M11: una salida puede dejar el stock en 0, igual que un consumo;
     * el stock minimo solo marca el insumo como "stock bajo".
     */
    @Override
    @Transactional
    public SupplyMovementResponseDto registerMovement(User user, CreateSupplyMovementDto dto) {
        MedicalSupply supply = medicalSupplyRepository.findById(dto.getSupplyId())
                .orElseThrow(() -> new EntityNotFoundException("No existe el insumo con id " + dto.getSupplyId()));
        if (!supply.isActive()) {
            throw new InvalidDataException("El insumo " + supply.getName() + " está desactivado");
        }
        if (dto.getType() == MovementType.OUT && dto.getQuantity() > supply.getQuantity()) {
            throw new InvalidDataException("La salida excede el inventario (disponible: " + supply.getQuantity() + ")");
        }
        int delta = dto.getType() == MovementType.IN ? dto.getQuantity() : -dto.getQuantity();
        supply.setQuantity(supply.getQuantity() + delta);
        medicalSupplyRepository.save(supply);

        SupplyMovement movement = SupplyMovement.builder()
                .supply(supply)
                .type(dto.getType())
                .quantityChanged(dto.getQuantity())
                .timestamp(LocalDateTime.now())
                .performedBy(user)
                .build();
        supplyMovementRepository.save(movement);
        return supplyMovementMapper.toResponseDto(movement);
    }
}
