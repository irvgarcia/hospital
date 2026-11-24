package com.hospital.backendHospital.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @Column(name = "register_date", nullable = false)
    private LocalDate registerDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "doctor")
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "doctor")
    @Builder.Default
    private List<DoctorSchedule> schedules = new ArrayList<>();

    /**
     * Igualdad por id: dos instancias representan la misma fila si tienen el mismo id.
     * Una entidad nueva (sin id) solo es igual a si misma. Se usa other.getId() porque el otro objeto puede ser un proxy de Hibernate.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Doctor other)) return false;
        return id != null && id.equals(other.getId());
    }

    /** Constante por clase: no cambia cuando la entidad recibe su id al guardarse, asi sigue encontrandose en un Set. */
    @Override
    public int hashCode() {
        return Doctor.class.hashCode();
    }
}
