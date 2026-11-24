package com.hospital.backendHospital.models.filters;

import com.hospital.backendHospital.models.entity.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplyMovementFilterRequest {
    private Long supplyId;
    private MovementType type;
}
