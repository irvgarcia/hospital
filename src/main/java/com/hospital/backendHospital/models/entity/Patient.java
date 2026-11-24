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
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String address;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private String allergies;

    @Convert(converter = BloodTypeConverter.class)
    @Column(name = "blood_type", nullable = false)
    private BloodType bloodType;

    @Column(name = "chronic_diseases", nullable = false)
    private String chronicDiseases;

    @Column(name = "current_medications", nullable = false)
    private String currentMedications;

    @Column(name = "emergency_contact", nullable = false)
    private String emergencyContact;

    @Column(name = "emergency_contact_name", nullable = false)
    private String emergencyContactName;

    @Column(name = "emergency_contact_relation", nullable = false)
    private String emergencyContactRelation;

    @Column(nullable = false)
    private Double height;

    @Column(nullable = false)
    private Double weight;

    @Column(name = "register_date", nullable = false)
    private LocalDate registerDate;

    @Column(nullable = false)
    private boolean smoker;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "patient")
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    /**
     * Igualdad por id: dos instancias representan la misma fila si tienen el mismo id.
     * Una entidad nueva (sin id) solo es igual a si misma. Se usa other.getId() porque el otro objeto puede ser un proxy de Hibernate.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient other)) return false;
        return id != null && id.equals(other.getId());
    }

    /** Constante por clase: no cambia cuando la entidad recibe su id al guardarse, asi sigue encontrandose en un Set. */
    @Override
    public int hashCode() {
        return Patient.class.hashCode();
    }
}
