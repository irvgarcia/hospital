package com.hospital.backendHospital.services.validation;

import com.hospital.backendHospital.exceptions.InvalidDataException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Reglas unicas para los horarios de los doctores (M4). Se usan al crear un doctor, al crear un horario
 * suelto y al actualizar los horarios de un doctor, asi las tres rutas validan exactamente lo mismo.
 */
public final class ScheduleValidator {

    /** Horario de atencion del hospital: ningun turno empieza antes ni termina despues. */
    public static final LocalTime OPENING_TIME = LocalTime.of(6, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);
    /** Un turno mas corto no deja espacio razonable para citas. */
    public static final Duration MIN_DURATION = Duration.ofHours(1);

    private ScheduleValidator() {
    }

    public static void validate(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (dayOfWeek == null || startTime == null || endTime == null) {
            throw new InvalidDataException("El día, la hora de inicio y la hora de fin son obligatorios");
        }
        if (!startTime.isBefore(endTime)) {
            throw new InvalidDataException("La hora de inicio debe ser anterior a la de fin (" + dayOfWeek + ")");
        }
        if (startTime.isBefore(OPENING_TIME) || endTime.isAfter(CLOSING_TIME)) {
            throw new InvalidDataException("El horario debe estar entre las " + OPENING_TIME + " y las " + CLOSING_TIME + " (" + dayOfWeek + ")");
        }
        if (Duration.between(startTime, endTime).compareTo(MIN_DURATION) < 0) {
            throw new InvalidDataException("El turno debe durar al menos " + MIN_DURATION.toMinutes() + " minutos (" + dayOfWeek + ")");
        }
    }

    /** Un doctor tiene a lo mucho un turno por dia. */
    public static void validateNoDuplicateDays(Collection<DayOfWeek> days) {
        Set<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek day : days) {
            if (!seen.add(day)) {
                throw new InvalidDataException("El día " + day + " está repetido en los horarios");
            }
        }
    }
}
