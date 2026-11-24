package com.hospital.backendHospital.services;

public interface IPDFGeneratorService {

    /** Expediente del paciente en PDF (M7): datos, historial clinico, citas y hospitalizaciones. */
    byte[] generateMedicalRecord(Long patientId);
}
