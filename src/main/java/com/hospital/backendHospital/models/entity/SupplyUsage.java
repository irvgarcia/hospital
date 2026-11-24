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
@Table(name = "supply_usages")
public class SupplyUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalization_id", nullable = false)
    private Hospitalization hospitalization;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id", nullable = false)
    private MedicalSupply supply;

    @Column(name = "quantity_used", nullable = false)
    private int quantityUsed;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    /**
     * Igualdad por id: dos instancias representan la misma fila si tienen el mismo id.
     * Una entidad nueva (sin id) solo es igual a si misma. Se usa other.getId() porque el otro objeto puede ser un proxy de Hibernate.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplyUsage other)) return false;
        return id != null && id.equals(other.getId());
    }

    /** Constante por clase: no cambia cuando la entidad recibe su id al guardarse, asi sigue encontrandose en un Set. */
    @Override
    public int hashCode() {
        return SupplyUsage.class.hashCode();
    }
}
