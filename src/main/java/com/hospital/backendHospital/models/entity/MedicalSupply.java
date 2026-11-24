package com.hospital.backendHospital.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "medical_supplies")
public class MedicalSupply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "minimum_stock", nullable = false)
    private int minimumStock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Unit unit;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Bloqueo optimista (A8): Hibernate incrementa este numero en cada UPDATE y lo exige en el WHERE.
     * Si otra transaccion modifico el insumo despues de leerlo, el UPDATE no afecta filas y se lanza
     * OptimisticLockingFailureException (409) en vez de perder el cambio de stock de la otra.
     */
    @Version
    private Long version;

    /**
     * Igualdad por id: dos instancias representan la misma fila si tienen el mismo id.
     * Una entidad nueva (sin id) solo es igual a si misma. Se usa other.getId() porque el otro objeto puede ser un proxy de Hibernate.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalSupply other)) return false;
        return id != null && id.equals(other.getId());
    }

    /** Constante por clase: no cambia cuando la entidad recibe su id al guardarse, asi sigue encontrandose en un Set. */
    @Override
    public int hashCode() {
        return MedicalSupply.class.hashCode();
    }
}
