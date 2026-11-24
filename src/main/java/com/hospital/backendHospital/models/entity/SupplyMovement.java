package com.hospital.backendHospital.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "supply_movements")
public class SupplyMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id", nullable = false)
    private MedicalSupply supply;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    @Column(nullable = false)
    private int quantityChanged;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    /**
     * Igualdad por id: dos instancias representan la misma fila si tienen el mismo id.
     * Una entidad nueva (sin id) solo es igual a si misma. Se usa other.getId() porque el otro objeto puede ser un proxy de Hibernate.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplyMovement other)) return false;
        return id != null && id.equals(other.getId());
    }

    /** Constante por clase: no cambia cuando la entidad recibe su id al guardarse, asi sigue encontrandose en un Set. */
    @Override
    public int hashCode() {
        return SupplyMovement.class.hashCode();
    }
}
