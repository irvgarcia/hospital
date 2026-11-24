package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.FileStorageException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.models.dto.file.FileResponseDto;
import com.hospital.backendHospital.models.entity.Doctor;
import com.hospital.backendHospital.models.entity.FileEntity;
import com.hospital.backendHospital.models.entity.Patient;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.repositories.AppointmentRepository;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.FileRepository;
import com.hospital.backendHospital.repositories.FileRepository.FileSummary;
import com.hospital.backendHospital.repositories.HospitalizationRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.IFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Archivos de pacientes (M6). Acceso: ADMIN y RECEPTIONIST a todos; DOCTOR a los de sus pacientes
 * (con alguna cita u hospitalizacion con el); PATIENT solo a los suyos.
 */
@Service
@RequiredArgsConstructor
public class FileService implements IFileService {

    /** Lista blanca: tipo MIME y las extensiones que le corresponden. */
    static final Map<String, List<String>> ALLOWED_TYPES = Map.of(
            "application/pdf", List.of("pdf"),
            "image/png", List.of("png"),
            "image/jpeg", List.of("jpg", "jpeg"),
            "image/webp", List.of("webp"),
            "text/plain", List.of("txt"));

    private final FileRepository fileRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final HospitalizationRepository hospitalizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FileResponseDto> listFiles(User user, Long patientId) {
        Long effectivePatientId = patientId;
        if (user.hasRole(RoleEnum.PATIENT)) {
            effectivePatientId = currentPatient(user).getId();
        } else if (isDoctorOnly(user)) {
            if (patientId == null) {
                throw new InvalidDataException("Indica el paciente (patientId)");
            }
            checkDoctorTreatsPatient(user, patientId);
        }
        return fileRepository.findSummaries(effectivePatientId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public FileResponseDto store(User user, MultipartFile file, Long patientId) {
        Patient patient = resolvePatientForUpload(user, patientId);
        if (file == null || file.isEmpty()) {
            throw new InvalidDataException("El archivo está vacío");
        }
        String name = StringUtils.cleanPath(file.getOriginalFilename() == null ? "archivo" : file.getOriginalFilename());
        if (name.contains("..") || name.isBlank()) {
            throw new InvalidDataException("El nombre del archivo no es válido");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = StringUtils.getFilenameExtension(name);
        List<String> extensions = ALLOWED_TYPES.get(type);
        if (extensions == null || extension == null || !extensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new InvalidDataException("Tipo de archivo no permitido; se aceptan PDF, PNG, JPG, WEBP y TXT");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new FileStorageException("No se pudo leer el archivo", e);
        }
        FileEntity entity = FileEntity.builder()
                .name(name)
                .type(type)
                .size(data.length)
                .data(data)
                .patient(patient)
                .uploadedBy(user)
                .uploadedAt(LocalDateTime.now())
                .build();
        fileRepository.save(entity);
        return FileResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .size(entity.getSize())
                .url(downloadUrl(entity.getId()))
                .patientId(patient.getId())
                .patientName(patient.getUser().getFirstName() + " " + patient.getUser().getLastName())
                .uploadedBy(user.getFirstName() + " " + user.getLastName())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FileEntity getFile(User user, UUID id) {
        FileEntity file = findFile(id);
        checkCanAccess(user, file);
        return file;
    }

    @Override
    @Transactional
    public void deleteFile(User user, UUID id) {
        FileEntity file = findFile(id);
        boolean isUploader = file.getUploadedBy() != null && file.getUploadedBy().getId().equals(user.getId());
        if (!user.hasRole(RoleEnum.ADMIN) && !isUploader) {
            throw new AccessDeniedException("Solo un administrador o quien subió el archivo puede borrarlo");
        }
        fileRepository.delete(file);
    }

    private void checkCanAccess(User user, FileEntity file) {
        if (user.hasRole(RoleEnum.ADMIN) || user.hasRole(RoleEnum.RECEPTIONIST)) {
            return;
        }
        Long patientId = file.getPatient() == null ? null : file.getPatient().getId();
        if (user.hasRole(RoleEnum.PATIENT)) {
            if (patientId == null || !patientId.equals(currentPatient(user).getId())) {
                throw new AccessDeniedException("No tienes acceso a este archivo");
            }
            return;
        }
        if (user.hasRole(RoleEnum.DOCTOR)) {
            if (patientId == null) {
                throw new AccessDeniedException("No tienes acceso a este archivo");
            }
            checkDoctorTreatsPatient(user, patientId);
            return;
        }
        throw new AccessDeniedException("No tienes acceso a este archivo");
    }

    private Patient resolvePatientForUpload(User user, Long patientId) {
        if (user.hasRole(RoleEnum.PATIENT)) {
            return currentPatient(user);
        }
        if (patientId == null) {
            throw new InvalidDataException("Indica el paciente (patientId)");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("No existe el paciente con id " + patientId));
        if (isDoctorOnly(user)) {
            checkDoctorTreatsPatient(user, patientId);
        }
        return patient;
    }

    private void checkDoctorTreatsPatient(User user, Long patientId) {
        Doctor doctor = doctorRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("No existe el perfil de doctor del usuario"));
        boolean treats = appointmentRepository.existsByDoctorIdAndPatientId(doctor.getId(), patientId)
                || hospitalizationRepository.existsByDoctorIdAndPatientId(doctor.getId(), patientId);
        if (!treats) {
            throw new AccessDeniedException("Solo puedes ver los archivos de tus pacientes");
        }
    }

    private static boolean isDoctorOnly(User user) {
        return user.hasRole(RoleEnum.DOCTOR) && !user.hasRole(RoleEnum.ADMIN) && !user.hasRole(RoleEnum.RECEPTIONIST);
    }

    private Patient currentPatient(User user) {
        return patientRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Completa tu perfil de paciente antes de continuar"));
    }

    private FileEntity findFile(UUID id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el archivo con id " + id));
    }

    private FileResponseDto toDto(FileSummary summary) {
        return FileResponseDto.builder()
                .id(summary.getId())
                .name(summary.getName())
                .type(summary.getType())
                .size(summary.getSize())
                .url(downloadUrl(summary.getId()))
                .patientId(summary.getPatientId())
                .patientName(join(summary.getPatientFirstName(), summary.getPatientLastName()))
                .uploadedBy(join(summary.getUploaderFirstName(), summary.getUploaderLastName()))
                .uploadedAt(summary.getUploadedAt())
                .build();
    }

    /** M6: ruta relativa a la API (antes faltaba /v1 y los enlaces estaban rotos). */
    static String downloadUrl(UUID id) {
        return "/api/v1/files/" + id;
    }

    private static String join(String first, String last) {
        if (first == null && last == null) {
            return null;
        }
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }
}
