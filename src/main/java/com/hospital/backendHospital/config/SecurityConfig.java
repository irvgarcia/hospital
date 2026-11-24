package com.hospital.backendHospital.config;

import com.hospital.backendHospital.auth.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpMethod.*;

/**
 * Reglas de acceso por rol (resumen en docs/arquitectura.md, seccion 4.3).
 * Toda ruta que no aparezca aqui queda bloqueada por el denyAll() final: al agregar un endpoint
 * hay que darle su regla explicitamente (y agregarlo a RoleAccessIT).
 * Las reglas especificas ("me", "low-stock", "register") van antes que las que usan {id}.
 * La propiedad de los recursos (un paciente solo ve lo suyo, un doctor solo sus citas) se valida en los servicios.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String RECEPTIONIST = "RECEPTIONIST";
    private static final String DOCTOR = "DOCTOR";
    private static final String PATIENT = "PATIENT";
    private static final String[] ALL = {ADMIN, RECEPTIONIST, DOCTOR, PATIENT};
    private static final String[] STAFF = {ADMIN, RECEPTIONIST, DOCTOR};

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final TokenService tokenService;
    private final SecurityErrorHandler securityErrorHandler;

    @Value("${app.cors.allowed-origins:http://localhost:*,https://localhost:*}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .authorizeHttpRequests(http -> {
                    // PUBLICO
                    http.requestMatchers("/auth/**", "/error").permitAll();
                    http.requestMatchers(OPTIONS, "/**").permitAll();
                    http.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    http.requestMatchers(GET, "/actuator/health").permitAll();

                    // CUENTA Y PANEL
                    http.requestMatchers("/api/v1/account", "/api/v1/account/**").authenticated();
                    http.requestMatchers(GET, "/api/v1/dashboard").hasAnyRole(ADMIN, RECEPTIONIST);

                    // RECEPCIONISTAS
                    http.requestMatchers("/api/v1/receptionists", "/api/v1/receptionists/**").hasRole(ADMIN);

                    // PACIENTES
                    http.requestMatchers(GET, "/api/v1/patients/me", "/api/v1/patients/me/medical-record/pdf").hasRole(PATIENT);
                    http.requestMatchers(PATCH, "/api/v1/patients/me").hasRole(PATIENT);
                    http.requestMatchers(POST, "/api/v1/patients/register").hasAnyRole(ADMIN, RECEPTIONIST);
                    http.requestMatchers(POST, "/api/v1/patients").hasRole(PATIENT);
                    http.requestMatchers(GET, "/api/v1/patients", "/api/v1/patients/{id}").hasAnyRole(STAFF);
                    http.requestMatchers(GET, "/api/v1/patients/{id}/medical-record/pdf").hasAnyRole(ADMIN, DOCTOR);
                    http.requestMatchers(PATCH, "/api/v1/patients/{id}/deactivate", "/api/v1/patients/{id}/activate").hasRole(ADMIN);
                    http.requestMatchers(PATCH, "/api/v1/patients/{id}").hasAnyRole(ADMIN, RECEPTIONIST);

                    // DOCTORES (catalogo para todos: el paciente necesita saber a quien agendar)
                    http.requestMatchers(GET, "/api/v1/doctors/me").hasRole(DOCTOR);
                    http.requestMatchers(GET, "/api/v1/doctors", "/api/v1/doctors/{id}", "/api/v1/doctors/{id}/availability").hasAnyRole(ALL);
                    http.requestMatchers(POST, "/api/v1/doctors").hasRole(ADMIN);
                    http.requestMatchers(PATCH, "/api/v1/doctors/{id}", "/api/v1/doctors/{id}/deactivate", "/api/v1/doctors/{id}/activate").hasRole(ADMIN);

                    // ESPECIALIDADES
                    http.requestMatchers(GET, "/api/v1/specialties", "/api/v1/specialties/{id}").hasAnyRole(ALL);
                    http.requestMatchers(POST, "/api/v1/specialties").hasRole(ADMIN);
                    http.requestMatchers(PATCH, "/api/v1/specialties/{id}").hasRole(ADMIN);

                    // HORARIOS DE DOCTORES
                    http.requestMatchers(GET, "/api/v1/doctor-schedules").hasAnyRole(ALL);
                    http.requestMatchers(POST, "/api/v1/doctor-schedules").hasRole(ADMIN);
                    http.requestMatchers(PATCH, "/api/v1/doctor-schedules/{id}").hasRole(ADMIN);
                    http.requestMatchers(DELETE, "/api/v1/doctor-schedules/{id}").hasRole(ADMIN);

                    // CITAS
                    http.requestMatchers(GET, "/api/v1/appointments/me/next").hasRole(PATIENT);
                    http.requestMatchers(GET, "/api/v1/appointments/me").hasAnyRole(PATIENT, DOCTOR);
                    http.requestMatchers(GET, "/api/v1/appointments").hasAnyRole(STAFF);
                    http.requestMatchers(GET, "/api/v1/appointments/{id}").hasAnyRole(ALL);
                    http.requestMatchers(POST, "/api/v1/appointments").hasAnyRole(ADMIN, RECEPTIONIST, PATIENT);
                    http.requestMatchers(PATCH, "/api/v1/appointments/{id}/complete").hasRole(DOCTOR);
                    http.requestMatchers(PATCH, "/api/v1/appointments/{id}/cancel", "/api/v1/appointments/{id}/reschedule").hasAnyRole(ADMIN, RECEPTIONIST, PATIENT);

                    // HISTORIAL CLINICO
                    http.requestMatchers(GET, "/api/v1/medical-records/me").hasRole(PATIENT);
                    http.requestMatchers(GET, "/api/v1/medical-records/patients/{patientId}").hasAnyRole(ADMIN, DOCTOR);
                    http.requestMatchers(POST, "/api/v1/medical-records").hasRole(DOCTOR);
                    http.requestMatchers(PATCH, "/api/v1/medical-records/{id}").hasRole(DOCTOR);

                    // HOSPITALIZACIONES
                    http.requestMatchers(GET, "/api/v1/hospitalizations/me").hasRole(PATIENT);
                    http.requestMatchers(GET, "/api/v1/hospitalizations", "/api/v1/hospitalizations/{id}", "/api/v1/hospitalizations/{id}/supply-usages").hasAnyRole(STAFF);
                    http.requestMatchers(POST, "/api/v1/hospitalizations").hasAnyRole(STAFF);
                    http.requestMatchers(PATCH, "/api/v1/hospitalizations/{id}/discharge").hasAnyRole(STAFF);
                    http.requestMatchers(POST, "/api/v1/supply-usages").hasAnyRole(ADMIN, DOCTOR);

                    // SALAS
                    http.requestMatchers(GET, "/api/v1/rooms", "/api/v1/rooms/{id}").hasAnyRole(STAFF);
                    http.requestMatchers(POST, "/api/v1/rooms").hasRole(ADMIN);
                    http.requestMatchers(PATCH, "/api/v1/rooms/{id}").hasRole(ADMIN);

                    // INVENTARIO (el doctor consulta insumos para registrar consumos)
                    http.requestMatchers(GET, "/api/v1/medical-supplies/low-stock").hasRole(ADMIN);
                    http.requestMatchers(GET, "/api/v1/medical-supplies", "/api/v1/medical-supplies/{id}").hasAnyRole(ADMIN, DOCTOR);
                    http.requestMatchers(POST, "/api/v1/medical-supplies").hasRole(ADMIN);
                    http.requestMatchers(PATCH, "/api/v1/medical-supplies/{id}").hasRole(ADMIN);
                    http.requestMatchers("/api/v1/supply-movements").hasRole(ADMIN);

                    // ARCHIVOS (la propiedad del archivo se valida en FileService)
                    http.requestMatchers(GET, "/api/v1/files", "/api/v1/files/{id}").hasAnyRole(ALL);
                    http.requestMatchers(POST, "/api/v1/files").hasAnyRole(ALL);
                    http.requestMatchers(DELETE, "/api/v1/files/{id}").hasAnyRole(ALL);

                    // Todo lo demas queda bloqueado por defecto
                    http.anyRequest().denyAll();
                })
                .logout(logout -> logout.logoutUrl("/auth/logout")
                        .addLogoutHandler(this::logout)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        }))
                .build();
    }

    /** Origenes permitidos: APP_CORS_ALLOWED_ORIGINS (separados por coma). Por defecto, localhost en cualquier puerto. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Revoca todos los tokens (access y refresh) del usuario duenio del token enviado. */
    private void logout(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        tokenService.revokeAllTokensOfOwner(authHeader.substring(7));
        SecurityContextHolder.clearContext();
    }
}
