package com.hospital.backendHospital.models.dto.supplyUsage;

import com.hospital.backendHospital.models.entity.Unit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplyUsageResponseDto {
    private Long id;
    private Long hospitalizationId;
    private Long supplyId;
    private String supplyName;
    private Unit unit;
    private int quantityUsed;
    private LocalDateTime usedAt;
}
