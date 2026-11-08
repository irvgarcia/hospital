package com.hospital.backendHospital.config;

import com.hospital.backendHospital.auth.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import java.util.List;

/**
 * Reglas de acceso por rol (matriz en docs/analisis-backend-y-plan-de-correccion.md, seccion 5).
 * Toda ruta que no aparezca aqui queda bloqueada por el denyAll() final: al agregar un endpoint
 * hay que darle su regla explicitamente.
 * Las reglas especificas van antes que las que usan {id}, porque "{id}" tambien coincide con "me".
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String RECEPTIONIST = "RECEPTIONIST";
    private static final String DOCTOR = "DOCTOR";
    private static final String PATIENT = "PATIENT";

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final TokenService tokenService;
    private final SecurityErrorHandler securityErrorHandler;

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
                    http.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // RECEPCIONISTAS
                    http.requestMatchers("/api/v1/receptionists/**").hasRole(ADMIN);

                    // PACIENTES
                    http.requestMatchers(HttpMethod.GET, "/api/v1/patients").hasAnyRole(ADMIN, RECEPTIONIST, DOCTOR);
                    http.requestMatchers(HttpMethod.POST, "/api/v1/patients/create").hasRole(PATIENT);
                    http.requestMatchers(HttpMethod.PATCH, "/api/v1/patients/update/me").hasRole(PATIENT);
                    http.requestMatchers(HttpMethod.PATCH, "/api/v1/patients/{id}/deactivate").hasRole(ADMIN);

                    // DOCTORES (lectura para todos los roles: el paciente necesita saber a quien agendar)
                    http.requestMatchers(HttpMethod.GET, "/api/v1/doctors").hasAnyRole(ADMIN, RECEPTIONIST, DOCTOR, PATIENT);
                    http.requestMatchers(HttpMethod.POST, "/api/v1/doctors").hasRole(ADMIN);
                    http.requestMatchers(HttpMethod.PATCH, "/api/v1/doctors/{id}/update", "/api/v1/doctors/{id}/deactivate").hasRole(ADMIN);

                    // ESPECIALIDADES
                    http.requestMatchers(HttpMethod.GET, "/api/v1/specialties", "/api/v1/specialties/search").hasAnyRole(ADMIN, RECEPTIONIST, DOCTOR, PATIENT);
                    http.requestMatchers(HttpMethod.POST, "/api/v1/specialties").hasRole(ADMIN);

                    // HORARIOS DE DOCTORES
                    http.requestMatchers(HttpMethod.GET, "/api/v1/doctorSchedules").hasAnyRole(ADMIN, RECEPTIONIST, DOCTOR, PATIENT);
                    http.requestMatchers(HttpMethod.POST, "/api/v1/doctorSchedules").hasRole(ADMIN);

                    // CITAS (el DOCTOR solo ve las suyas y el PACIENTE solo cancela las suyas: se valida en AppointmentService)
                    http.requestMatchers(HttpMethod.GET, "/api/v1/appointments/patients/me", "/api/v1/appointments/patients/active/me").hasRole(PATIENT);
                    http.requestMatchers(HttpMethod.GET, "/api/v1/appointments").hasAnyRole(ADMIN, RECEPTIONIST, DOCTOR);
                    http.requestMatchers(HttpMethod.POST, "/api/v1/appointments").hasAnyRole(ADMIN, RECEPTIONIST, PATIENT);
                    http.requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/{id}/complete").hasRole(DOCTOR);
                    http.requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/{id}/cancel").hasAnyRole(ADMIN, RECEPTIONIST, PATIENT);

                    // HISTORIAL CLINICO
                    http.requestMatchers(HttpMethod.GET, "/api/v1/medicalRecords/patients/me").hasRole(PATIENT);
                    http.requestMatchers(HttpMethod.GET, "/api/v1/medicalRecords/patients/{id}").hasAnyRole(ADMIN, DOCTOR);
                    http.requestMatchers(HttpMethod.POST, "/api/v1/medicalRecords").hasRole(DOCTOR);

                    // HOSPITALIZACIONES
                    http.requestMatchers("/api/v1/hospitalizations", "/api/v1/hospitalizations/**").hasAnyRole(ADMIN, RECEPTIONIST, DOCTOR);

                    // SALAS
                    http.requestMatchers(HttpMethod.GET, "/api/v1/rooms", "/api/v1/rooms/{id}").hasAnyRole(ADMIN, RECEPTIONIST, DOCTOR);
                    http.requestMatchers(HttpMethod.POST, "/api/v1/rooms").hasRole(ADMIN);

                    // INVENTARIO
                    http.requestMatchers("/api/v1/medicalSupplies", "/api/v1/medicalSupplies/**").hasRole(ADMIN);
                    http.requestMatchers("/api/v1/supplyMovements", "/api/v1/supplyMovements/**").hasRole(ADMIN);
                    http.requestMatchers("/api/supply-usage", "/api/supply-usage/**").hasAnyRole(ADMIN, DOCTOR);

                    // ARCHIVOS Y PDF (los archivos aun no tienen duenio: el acceso del paciente llega en la Fase 6)
                    http.requestMatchers(HttpMethod.GET, "/api/v1/fileManager/pdf/{patientId}/generate").hasAnyRole(ADMIN, DOCTOR);
                    http.requestMatchers(HttpMethod.POST, "/api/v1/fileManager/upload").hasAnyRole(ADMIN, RECEPTIONIST, DOCTOR);
                    http.requestMatchers(HttpMethod.GET, "/api/v1/fileManager/files", "/api/v1/fileManager/files/{id}").hasAnyRole(ADMIN, RECEPTIONIST, DOCTOR);

                    // Todo lo demas queda bloqueado por defecto
                    http.anyRequest().denyAll();
                })

                .logout(logout -> logout.logoutUrl("/auth/logout")
                        .addLogoutHandler(this::logout)
                        .logoutSuccessHandler(((request, response, authentication) -> SecurityContextHolder.clearContext())))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "https://enchanting-growth-production.up.railway.app",
                "http://enchanting-growth-production.up.railway.app",
                "http://localhost:*",
                "https://localhost:*"
        ));

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
    private void logout(
            final HttpServletRequest request, final HttpServletResponse response,
            final Authentication authentication
    ) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        tokenService.revokeAllTokensOfOwner(authHeader.substring(7));
        SecurityContextHolder.clearContext();
    }
}
