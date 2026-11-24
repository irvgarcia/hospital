package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.FileStorageException;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.AppointmentRepository;
import com.hospital.backendHospital.repositories.HospitalizationRepository;
import com.hospital.backendHospital.repositories.MedicalRecordRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.IPDFGeneratorService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera el expediente medico en PDF (M7). Solo se descarga: no se guarda en la tabla de archivos,
 * asi cada descarga refleja los datos actuales y no se acumulan copias.
 */
@Service
@RequiredArgsConstructor
public class PDFGeneratorService implements IPDFGeneratorService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color HEADER_BG = new Color(226, 236, 245);

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final HospitalizationRepository hospitalizationRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateMedicalRecord(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("No existe el paciente con id " + patientId));
        List<MedicalRecord> records = medicalRecordRepository.findByPatientIdOrderByDateDescIdDesc(patientId);
        List<Appointment> appointments = appointmentRepository.findTop20ByPatientIdOrderByDateDescStartTimeDesc(patientId);
        List<Hospitalization> hospitalizations = hospitalizationRepository.findByPatientIdOrderByAdmissionDateDesc(patientId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font section = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

            Paragraph heading = new Paragraph("Expediente médico", title);
            heading.setAlignment(Element.ALIGN_CENTER);
            document.add(heading);
            Paragraph generated = new Paragraph("Generado el " + LocalDateTime.now().format(DATE_TIME), small);
            generated.setAlignment(Element.ALIGN_CENTER);
            generated.setSpacingAfter(14);
            document.add(generated);

            User user = patient.getUser();
            document.add(sectionTitle("Datos del paciente", section));
            PdfPTable data = new PdfPTable(new float[]{1.3f, 2.7f});
            data.setWidthPercentage(100);
            row(data, "Nombre", user.getFirstName() + " " + user.getLastName());
            row(data, "Email", user.getEmail());
            row(data, "Fecha de nacimiento", patient.getDateOfBirth().format(DATE)
                    + " (" + Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears() + " años)");
            row(data, "Sexo", genderLabel(patient.getGender()));
            row(data, "Grupo sanguíneo", patient.getBloodType() == null ? "Sin registrar" : patient.getBloodType().getLabel());
            row(data, "Estatura / peso", patient.getHeight() + " m / " + patient.getWeight() + " kg");
            row(data, "Fumador", patient.isSmoker() ? "Sí" : "No");
            row(data, "Teléfono", patient.getPhoneNumber());
            row(data, "Dirección", patient.getAddress());
            row(data, "Alergias", patient.getAllergies());
            row(data, "Enfermedades crónicas", patient.getChronicDiseases());
            row(data, "Medicamentos actuales", patient.getCurrentMedications());
            row(data, "Contacto de emergencia", patient.getEmergencyContactName() + " (" + patient.getEmergencyContactRelation()
                    + ") " + patient.getEmergencyContact());
            document.add(data);

            document.add(sectionTitle("Historial clínico", section));
            if (records.isEmpty()) {
                document.add(new Paragraph("Sin entradas registradas."));
            } else {
                PdfPTable table = table(new float[]{1f, 1.5f, 2f, 2f}, "Fecha", "Doctor", "Diagnóstico", "Tratamiento");
                for (MedicalRecord record : records) {
                    table.addCell(record.getDate().format(DATE));
                    table.addCell(record.getDoctor() == null ? "-" : name(record.getDoctor().getUser()));
                    table.addCell(record.getDiagnostics());
                    table.addCell(record.getTreatment() + (record.getNotes() == null ? "" : "\nNotas: " + record.getNotes()));
                }
                document.add(table);
            }

            document.add(sectionTitle("Citas recientes", section));
            if (appointments.isEmpty()) {
                document.add(new Paragraph("Sin citas registradas."));
            } else {
                PdfPTable table = table(new float[]{1f, 1f, 2f, 1.5f, 1f}, "Fecha", "Hora", "Doctor", "Especialidad", "Estado");
                for (Appointment appointment : appointments) {
                    table.addCell(appointment.getDate().format(DATE));
                    table.addCell(appointment.getStartTime() + " - " + appointment.getEndTime());
                    table.addCell(name(appointment.getDoctor().getUser()));
                    table.addCell(appointment.getDoctor().getSpecialty().getName());
                    table.addCell(statusLabel(appointment.getStatus()));
                }
                document.add(table);
            }

            document.add(sectionTitle("Hospitalizaciones", section));
            if (hospitalizations.isEmpty()) {
                document.add(new Paragraph("Sin hospitalizaciones."));
            } else {
                PdfPTable table = table(new float[]{1f, 1f, 1.2f, 2.3f, 1.5f}, "Ingreso", "Alta", "Sala", "Motivo", "Doctor");
                for (Hospitalization hospitalization : hospitalizations) {
                    table.addCell(hospitalization.getAdmissionDate().format(DATE));
                    table.addCell(hospitalization.getDischargeDate() == null ? "En curso" : hospitalization.getDischargeDate().format(DATE));
                    table.addCell(hospitalization.getRoom().getName());
                    table.addCell(hospitalization.getReason());
                    table.addCell(name(hospitalization.getDoctor().getUser()));
                }
                document.add(table);
            }
            document.close();
        } catch (DocumentException e) {
            throw new FileStorageException("No se pudo generar el PDF", e);
        }
        return out.toByteArray();
    }

    private static Paragraph sectionTitle(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(14);
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private static void row(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        labelCell.setBackgroundColor(HEADER_BG);
        table.addCell(labelCell);
        table.addCell(new Phrase(value == null ? "-" : value, FontFactory.getFont(FontFactory.HELVETICA, 10)));
    }

    private static PdfPTable table(float[] widths, String... headers) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            cell.setBackgroundColor(HEADER_BG);
            table.addCell(cell);
        }
        return table;
    }

    private static String name(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private static String genderLabel(Gender gender) {
        if (gender == null) return "-";
        return switch (gender) {
            case FEMALE -> "Femenino";
            case MALE -> "Masculino";
            case OTHER -> "Otro";
        };
    }

    private static String statusLabel(AppointmentStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case COMPLETED -> "Completada";
            case CANCELLED -> "Cancelada";
        };
    }
}
