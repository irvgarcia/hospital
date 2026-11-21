package com.hospital.backendHospital.models.dto.doctor;

import com.hospital.backendHospital.models.dto.doctorSchedule.ScheduleSlotDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Todos los campos son opcionales. Si "schedules" viene, reemplaza los horarios del doctor:
 * los dias que no aparezcan se eliminan (salvo que tengan citas pendientes).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateDoctorDto {
    @Email(message = "El email no es válido")
    @Size(max = 100, message = "El email no puede tener más de 100 caracteres")
    private String email;

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String lastName;

    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String newPassword;

    @Positive(message = "La especialidad no es válida")
    private Long specialtyId;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    private String phoneNumber;

    private List<@Valid @NotNull ScheduleSlotDto> schedules;
}
