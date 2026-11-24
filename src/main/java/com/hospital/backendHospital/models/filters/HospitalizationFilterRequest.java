package com.hospital.backendHospital.models.filters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HospitalizationFilterRequest {
    /** Fecha de ingreso. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private Long patientId;
    private Long doctorId;
    private Long roomId;
    /** true: hospitalizaciones activas; false: con alta. */
    private Boolean status;
}
