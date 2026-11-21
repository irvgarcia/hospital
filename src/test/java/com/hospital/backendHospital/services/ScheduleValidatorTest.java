package com.hospital.backendHospital.services;

import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.services.validation.ScheduleValidator;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** M4: reglas unicas de horario. */
class ScheduleValidatorTest {

    @Test
    void validScheduleIsAccepted() {
        assertThatCode(() -> ScheduleValidator.validate(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(14, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void startMustBeBeforeEnd() {
        assertThatThrownBy(() -> ScheduleValidator.validate(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 0)))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("anterior");
        assertThatThrownBy(() -> ScheduleValidator.validate(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(10, 0)))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void scheduleMustBeInsideHospitalHours() {
        assertThatThrownBy(() -> ScheduleValidator.validate(DayOfWeek.MONDAY, LocalTime.of(5, 0), LocalTime.of(10, 0)))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("06:00");
        assertThatThrownBy(() -> ScheduleValidator.validate(DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(23, 0)))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void scheduleMustLastAtLeastOneHour() {
        assertThatThrownBy(() -> ScheduleValidator.validate(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 30)))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("60 minutos");
    }

    @Test
    void repeatedDaysAreRejected() {
        assertThatThrownBy(() -> ScheduleValidator.validateNoDuplicateDays(List.of(DayOfWeek.MONDAY, DayOfWeek.MONDAY)))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("MONDAY");
        assertThatCode(() -> ScheduleValidator.validateNoDuplicateDays(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)))
                .doesNotThrowAnyException();
    }
}
