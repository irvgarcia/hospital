package com.hospital.backendHospital.auth.repository;

import com.hospital.backendHospital.models.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Token emitido a un usuario (access o refresh).
 * No se guarda el JWT en claro sino su hash SHA-256: cabe en un indice unico de largo fijo
 * (un JWT con roles y un email largo excedia VARCHAR(255)) y una fuga de la tabla no expone tokens usables.
 * La tabla "auth_tokens" la crea Flyway (V1__baseline.sql). En bases antiguas puede quedar la tabla anterior
 * "token" (y "token_seq") sin uso; se puede borrar a mano.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auth_tokens")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 20)
    private TokenType tokenType;

    @Column(nullable = false)
    private Boolean revoked;

    @Column(nullable = false)
    private Boolean expired;

    /** Vencimiento del JWT: la limpieza programada (B7) borra los tokens vencidos o revocados. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum TokenType {
        ACCESS,
        REFRESH
    }

    /**
     * Igualdad por id: dos instancias representan la misma fila si tienen el mismo id.
     * Una entidad nueva (sin id) solo es igual a si misma. Se usa other.getId() porque el otro objeto puede ser un proxy de Hibernate.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token other)) return false;
        return id != null && id.equals(other.getId());
    }

    /** Constante por clase: no cambia cuando la entidad recibe su id al guardarse, asi sigue encontrandose en un Set. */
    @Override
    public int hashCode() {
        return Token.class.hashCode();
    }
}
