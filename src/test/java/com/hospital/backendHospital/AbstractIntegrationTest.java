package com.hospital.backendHospital;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.backendHospital.models.entity.Role;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.repositories.RoleRepository;
import com.hospital.backendHospital.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Clase base de las pruebas de integracion (*IT). Requiere Docker en ejecucion.
 * Todas las subclases comparten el mismo contexto de Spring y, por lo tanto, el mismo contenedor MySQL:
 * cada prueba debe crear sus propios datos (por ejemplo con emails unicos) para no depender de otras.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Password-de-prueba-123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RoleRepository roleRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    /** Crea un usuario activo con el rol indicado y un email unico. */
    protected User createUser(RoleEnum roleEnum) {
        return createUser(roleEnum.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.com", roleEnum);
    }

    protected User createUser(String email, RoleEnum roleEnum) {
        Role role = roleRepository.findByRoleEnum(roleEnum).orElseThrow();
        return userRepository.save(User.builder()
                .email(email)
                .firstName("Test")
                .lastName(roleEnum.name())
                .password(passwordEncoder.encode(PASSWORD))
                .roles(Set.of(role))
                .build());
    }

    /** Hace login y devuelve la respuesta completa ({accesToken, refreshToken}). */
    protected JsonNode login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw new IllegalStateException("Login fallido para " + email + ": " + result.getResponse().getStatus());
        }
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String accessToken(String email) throws Exception {
        return login(email).get("accesToken").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
