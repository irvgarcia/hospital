package com.hospital.backendHospital.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospital.backendHospital.AbstractIntegrationTest;
import com.hospital.backendHospital.auth.repository.Token.TokenType;
import com.hospital.backendHospital.auth.service.JwtService;
import com.hospital.backendHospital.auth.service.TokenService;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.models.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login, tokens y refresh contra MySQL real (C7, C8, C10, A14). */
class AuthenticationIT extends AbstractIntegrationTest {

    private static final String PROTECTED_URL = "/api/v1/specialties";

    @Autowired private TokenService tokenService;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        User user = createUser(RoleEnum.PATIENT);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", "incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    void loginWithUnknownEmailReturns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nadie@test.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginOfDeactivatedUserReturns403() throws Exception {
        User user = createUser(RoleEnum.DOCTOR);
        user.setActive(false);
        userRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Usuario desactivado"));
    }

    @Test
    void validAccessTokenGrantsAccess() throws Exception {
        User user = createUser(RoleEnum.PATIENT);

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user.getEmail()))))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithoutTokenReturns401WithJsonBody() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer("esto.no.es-un-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        User user = createUser(RoleEnum.PATIENT);
        String expired = Jwts.builder()
                .subject(user.getEmail())
                .claim(JwtService.TYPE_CLAIM, TokenType.ACCESS.name())
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                .compact();
        // Registrado y sin revocar: lo unico que lo invalida es la expiracion
        tokenService.save(user, expired, TokenType.ACCESS);

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfUserDeactivatedAfterLoginIsRejected() throws Exception {
        User user = createUser(RoleEnum.RECEPTIONIST);
        String token = accessToken(user.getEmail());
        user.setActive(false);
        userRepository.save(user);

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedTokensAreRejected() throws Exception {
        User user = createUser(RoleEnum.DOCTOR);
        String token = accessToken(user.getEmail());
        tokenService.revokeAllUserTokens(user);

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        User user = createUser(RoleEnum.PATIENT);
        String refresh = login(user.getEmail()).get("refreshToken").asText();

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() throws Exception {
        User user = createUser(RoleEnum.PATIENT);

        mockMvc.perform(post("/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, bearer(accessToken(user.getEmail()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutHeaderReturns401() throws Exception {
        mockMvc.perform(post("/auth/refresh-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshIssuesNewPairAndRevokesThePreviousOne() throws Exception {
        User user = createUser(RoleEnum.PATIENT);
        JsonNode first = login(user.getEmail());
        String oldAccess = first.get("accesToken").asText();
        String oldRefresh = first.get("refreshToken").asText();

        String body = mockMvc.perform(post("/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, bearer(oldRefresh)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode second = objectMapper.readTree(body);
        String newAccess = second.get("accesToken").asText();
        String newRefresh = second.get("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(oldRefresh);

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(newAccess)))
                .andExpect(status().isOk());
        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(oldAccess)))
                .andExpect(status().isUnauthorized());
        // El refresh ya usado no se puede reutilizar
        mockMvc.perform(post("/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, bearer(oldRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTokens() throws Exception {
        User user = createUser(RoleEnum.PATIENT);
        JsonNode tokens = login(user.getEmail());
        String access = tokens.get("accesToken").asText();

        mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(access)))
                .andExpect(status().isOk());

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(access)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/refresh-token").header(HttpHeaders.AUTHORIZATION, bearer(tokens.get("refreshToken").asText())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userWithLongEmailCanLogin() throws Exception {
        String longEmail = "a".repeat(180) + "@hospital-con-un-dominio-bastante-largo.com";
        createUser(longEmail, RoleEnum.PATIENT);

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, bearer(accessToken(longEmail))))
                .andExpect(status().isOk());
    }

    @Test
    void unknownRouteIsDeniedByDefault() throws Exception {
        User admin = createUser(RoleEnum.ADMIN);

        mockMvc.perform(get("/api/v1/ruta-que-no-existe").header(HttpHeaders.AUTHORIZATION, bearer(accessToken(admin.getEmail()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ruta-que-no-existe"))
                .andExpect(status().isUnauthorized());
    }
}
