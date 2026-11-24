package com.hospital.backendHospital.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A8: los conflictos de concurrencia e integridad responden 409 sin exponer detalles de la base. */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class ThrowingController {

        @GetMapping("/optimistic-lock")
        void optimisticLock() {
            // La subclase que lanza Spring Data al detectar el conflicto de @Version
            throw new ObjectOptimisticLockingFailureException("MedicalSupply", 1L);
        }

        @GetMapping("/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("could not execute statement",
                    new SQLIntegrityConstraintViolationException(
                            "Duplicate entry '1-2026-12-01' for key 'appointments.uk_appointments_doctor_slot'"));
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void optimisticLockingFailureReturns409() throws Exception {
        mockMvc.perform(get("/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto de concurrencia"));
    }

    @Test
    void dataIntegrityViolationReturns409WithoutDatabaseDetails() throws Exception {
        mockMvc.perform(get("/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto con datos existentes"))
                .andExpect(content().string(not(containsString("uk_appointments_doctor_slot"))))
                .andExpect(content().string(not(containsString("Duplicate entry"))));
    }
}
