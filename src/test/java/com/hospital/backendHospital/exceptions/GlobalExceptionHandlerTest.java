package com.hospital.backendHospital.exceptions;

import com.hospital.backendHospital.models.entity.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** B10/A10: formato ProblemDetail unico y ninguna entrada invalida termina en 500. */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    record Body(@NotBlank(message = "El nombre es obligatorio") String name, Gender gender) {
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/optimistic-lock")
        void optimisticLock() {
            throw new ObjectOptimisticLockingFailureException("MedicalSupply", 1L);
        }

        @GetMapping("/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("could not execute statement",
                    new SQLIntegrityConstraintViolationException(
                            "Duplicate entry '1-2026-12-01' for key 'appointments.uk_appointments_doctor_slot'"));
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("Ya existe");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("detalle interno de SQL");
        }

        @GetMapping("/typed/{id}")
        String typed(@PathVariable Long id, @RequestParam int page) {
            return "ok";
        }

        @PostMapping("/body")
        String body(@Valid @RequestBody Body body) {
            return "ok";
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflicto de concurrencia"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void dataIntegrityViolationReturns409WithoutDatabaseDetails() throws Exception {
        mockMvc.perform(get("/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto con datos existentes"))
                .andExpect(content().string(not(containsString("uk_appointments_doctor_slot"))))
                .andExpect(content().string(not(containsString("Duplicate entry"))));
    }

    @Test
    void conflictExceptionReturns409() throws Exception {
        mockMvc.perform(get("/conflict")).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Ya existe"));
    }

    @Test
    void unexpectedErrorsHideTheirMessage() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("SQL"))));
    }

    @Test
    void wrongTypesAndMissingParametersReturn400() throws Exception {
        mockMvc.perform(get("/typed/abc").param("page", "1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("id")));
        mockMvc.perform(get("/typed/1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("page")));
    }

    @Test
    void validationErrorsListEveryField() throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("El nombre es obligatorio"));
    }

    @Test
    void malformedJsonAndInvalidEnumsReturn400() throws Exception {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"a\",\"gender\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("gender")));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(post("/conflict")).andExpect(status().isMethodNotAllowed());
    }
}
