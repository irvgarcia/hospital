package com.hospital.backendHospital.models.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileResponseDto {
    private UUID id;
    private String name;
    private String type;
    private long size;
    private String url;
    private Long patientId;
    private String patientName;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
