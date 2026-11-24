package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    /** Datos de un archivo sin el contenido (M6: listar no carga los bytes). */
    interface FileSummary {
        UUID getId();
        String getName();
        String getType();
        long getSize();
        LocalDateTime getUploadedAt();
        Long getPatientId();
        String getPatientFirstName();
        String getPatientLastName();
        String getUploaderFirstName();
        String getUploaderLastName();
    }

    @Query("""
            select f.id as id, f.name as name, f.type as type, f.size as size, f.uploadedAt as uploadedAt,
                   p.id as patientId, pu.firstName as patientFirstName, pu.lastName as patientLastName,
                   u.firstName as uploaderFirstName, u.lastName as uploaderLastName
            from FileEntity f
            left join f.patient p
            left join p.user pu
            left join f.uploadedBy u
            where (:patientId is null or p.id = :patientId)
            order by f.uploadedAt desc
            """)
    List<FileSummary> findSummaries(@Param("patientId") Long patientId);
}
