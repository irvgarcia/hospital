package com.hospital.backendHospital.flows;

import com.hospital.backendHospital.AbstractClinicalIntegrationTest;
import com.hospital.backendHospital.models.entity.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** C5: /medicalRecords/patients/me devuelve el historial del paciente autenticado. */
class MedicalRecordIT extends AbstractClinicalIntegrationTest {

    @Test
    void patientGetsTheirOwnMedicalRecord() throws Exception {
        Patient other = createPatient();
        createMedicalRecord(other, "Diagnostico de otro paciente");
        Patient me = createPatient();
        createMedicalRecord(me, "Mi diagnostico");
        // El bug usaba user.id como patient.id: aqui son distintos
        assertThat(me.getId()).isNotEqualTo(me.getUser().getId());

        mockMvc.perform(get("/api/v1/medical-records/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(me.getUser().getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].diagnostics").value("Mi diagnostico"));
    }

    @Test
    void patientWithoutMedicalRecordGetsAnEmptyPage() throws Exception {
        Patient me = createPatient();

        // M3: una lista vacia no es un error
        mockMvc.perform(get("/api/v1/medical-records/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(me.getUser().getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
