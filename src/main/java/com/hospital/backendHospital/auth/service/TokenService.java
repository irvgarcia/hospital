package com.hospital.backendHospital.auth.service;

import com.hospital.backendHospital.auth.repository.Token;
import com.hospital.backendHospital.auth.repository.Token.TokenType;
import com.hospital.backendHospital.auth.repository.TokenRepository;
import com.hospital.backendHospital.models.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Registro y revocacion de los tokens emitidos. Un JWT con firma valida solo se acepta si ademas
 * esta registrado aqui y no fue revocado (logout, nuevo login, refresh o desactivacion del usuario).
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;

    @Transactional
    public void save(User user, String jwt, TokenType type) {
        tokenRepository.save(Token.builder()
                .user(user)
                .tokenHash(hash(jwt))
                .tokenType(type)
                .expired(false)
                .revoked(false)
                .build());
    }

    @Transactional(readOnly = true)
    public boolean isActive(String jwt, TokenType type) {
        return tokenRepository.findByTokenHash(hash(jwt))
                .filter(token -> token.getTokenType() == type)
                .filter(token -> !token.getRevoked() && !token.getExpired())
                .isPresent();
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        revokeAllUserTokens(user.getId());
    }

    /** Logout: revoca todos los tokens del usuario duenio del token recibido. */
    @Transactional
    public void revokeAllTokensOfOwner(String jwt) {
        tokenRepository.findByTokenHash(hash(jwt))
                .ifPresent(token -> revokeAllUserTokens(token.getUser().getId()));
    }

    private void revokeAllUserTokens(Long userId) {
        List<Token> validTokens = tokenRepository.findAllValidTokenByUser(userId);
        validTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validTokens);
    }

    static String hash(String jwt) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(jwt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
