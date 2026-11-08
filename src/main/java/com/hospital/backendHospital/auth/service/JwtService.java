package com.hospital.backendHospital.auth.service;

import com.hospital.backendHospital.auth.repository.Token.TokenType;
import com.hospital.backendHospital.models.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    /** Claim que distingue un access token de un refresh token. */
    public static final String TYPE_CLAIM = "type";

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    public String generateToken(final User user) {
        return buildToken(user, jwtExpiration, TokenType.ACCESS);
    }

    public String generateRefreshToken(final User user) {
        return buildToken(user, refreshExpiration, TokenType.REFRESH);
    }

    /**
     * Devuelve el email (subject) solo si el token tiene firma valida, no esta expirado
     * y es del tipo esperado. Nunca lanza excepcion: cualquier token invalido da Optional.empty().
     */
    public Optional<String> extractUsername(final String token, final TokenType expectedType) {
        return parseClaims(token)
                .filter(claims -> expectedType.name().equals(claims.get(TYPE_CLAIM, String.class)))
                .map(Claims::getSubject);
    }

    private Optional<Claims> parseClaims(final String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String buildToken(final User user, final long expiration, final TokenType type) {
        final long now = System.currentTimeMillis();
        return Jwts.builder()
                // jti unico: dos tokens emitidos en el mismo segundo serian identicos (y su hash chocaria)
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim(TYPE_CLAIM, type.name())
                .claim("roles", user.getRoles().stream()
                        .map(role -> role.getRoleEnum().name())
                        .toList())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    private SecretKey getSignInKey() {
        final byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
