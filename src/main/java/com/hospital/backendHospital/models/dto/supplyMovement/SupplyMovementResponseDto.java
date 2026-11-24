package com.hospital.backendHospital.models.dto.supplyMovement;

import com.hospital.backendHospital.models.entity.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplyMovementResponseDto {
    private Long id;
    private Long supplyId;
    private String supplyName;
    private MovementType type;
    private int quantityChanged;
    private LocalDateTime timestamp;
    private String performedBy;
}
