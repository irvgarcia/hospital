package com.hospital.backendHospital.auth.repository;

import com.hospital.backendHospital.models.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token emitido a un usuario (access o refresh).
 * No se guarda el JWT en claro sino su hash SHA-256: cabe en un indice unico de largo fijo
 * (un JWT con roles y un email largo excedia VARCHAR(255)) y una fuga de la tabla no expone tokens usables.
 * La tabla es nueva ("auth_tokens") para que ddl-auto=update la cree con el esquema correcto;
 * la tabla anterior "token" (y "token_seq") queda sin uso y se puede borrar a mano.
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum TokenType {
        ACCESS,
        REFRESH
    }
}
