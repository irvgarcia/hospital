package com.hospital.backendHospital.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Archivo guardado en la base (M6). Pertenece a un paciente (estudios, recetas, expedientes) y guarda quien lo subio.
 * El listado usa una proyeccion sin "data" para no cargar el contenido de todos los archivos.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String type;

    @Column(nullable = false)
    private long size;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileEntity other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return FileEntity.class.hashCode();
    }
}
