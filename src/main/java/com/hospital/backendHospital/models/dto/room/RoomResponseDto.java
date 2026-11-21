package com.hospital.backendHospital.models.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomResponseDto {
    private Long id;
    private String name;
    private int capacity;
    private int occupiedBeds;
    private boolean available;
}
