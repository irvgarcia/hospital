package com.hospital.backendHospital.auth;

import com.hospital.backendHospital.auth.repository.Token.TokenType;
import com.hospital.backendHospital.auth.service.JwtService;
import com.hospital.backendHospital.models.entity.Role;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.models.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);
    private static final String OTHER_SECRET = Base64.getEncoder().encodeToString("x".repeat(64).getBytes());

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = jwtService(SECRET, 60_000);
        user = User.builder()
                .email("ana@hospital.com")
                .roles(Set.of(Role.builder().roleEnum(RoleEnum.PATIENT).build()))
                .build();
    }

    @Test
    void accessTokenIsOnlyValidAsAccess() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token, TokenType.ACCESS)).contains("ana@hospital.com");
        assertThat(jwtService.extractUsername(token, TokenType.REFRESH)).isEmpty();
    }

    @Test
    void refreshTokenIsOnlyValidAsRefresh() {
        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractUsername(token, TokenType.REFRESH)).contains("ana@hospital.com");
        assertThat(jwtService.extractUsername(token, TokenType.ACCESS)).isEmpty();
    }

    @Test
    void tokensIssuedInTheSameSecondAreDifferent() {
        assertThat(jwtService.generateToken(user)).isNotEqualTo(jwtService.generateToken(user));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService(SECRET, -1_000).generateToken(user);

        assertThat(jwtService.extractUsername(token, TokenType.ACCESS)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = jwtService(OTHER_SECRET, 60_000).generateToken(user);

        assertThat(jwtService.extractUsername(token, TokenType.ACCESS)).isEmpty();
    }

    @Test
    void malformedTokensAreRejectedWithoutThrowing() {
        assertThat(jwtService.extractUsername("abc.def.ghi", TokenType.ACCESS)).isEmpty();
        assertThat(jwtService.extractUsername("", TokenType.ACCESS)).isEmpty();
    }

    private static JwtService jwtService(String secret, long expiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "jwtExpiration", expiration);
        ReflectionTestUtils.setField(service, "refreshExpiration", expiration);
        return service;
    }
}
