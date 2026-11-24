package com.hospital.backendHospital.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    @Query("""
            select t from Token t
            where t.user.id = :userId and t.expired = false and t.revoked = false
            """)
    List<Token> findAllValidTokenByUser(@Param("userId") Long userId);

    Optional<Token> findByTokenHash(String tokenHash);

    /** B7: borra los tokens que ya no sirven (revocados o vencidos). */
    @Modifying
    @Query("delete from Token t where t.revoked = true or t.expired = true or t.expiresAt < :now")
    int deleteUnusable(@Param("now") LocalDateTime now);
}
