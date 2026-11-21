package com.hospital.backendHospital.config;

import com.hospital.backendHospital.models.entity.Permission;
import com.hospital.backendHospital.models.entity.Role;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.repositories.PermissionRepository;
import com.hospital.backendHospital.repositories.RoleRepository;
import com.hospital.backendHospital.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * B6: datos minimos al arrancar (idempotente). Crea los permisos y roles si faltan y el administrador inicial
 * con ADMIN_EMAIL / ADMIN_PASSWORD (si no estan configurados, no se crea). Ahora es un componente
 * y @Transactional si tiene efecto (en un metodo @Bean no lo tenia).
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    /** Permisos por rol. Se conservan como informacion; la autorizacion se decide por rol en SecurityConfig. */
    private static final Map<RoleEnum, Set<String>> PERMISSIONS = Map.of(
            RoleEnum.ADMIN, Set.of("CREATE", "READ", "UPDATE", "DELETE"),
            RoleEnum.DOCTOR, Set.of("CREATE", "READ", "UPDATE"),
            RoleEnum.RECEPTIONIST, Set.of("CREATE", "READ"),
            RoleEnum.PATIENT, Set.of("READ"));

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;
    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        PERMISSIONS.forEach(this::ensureRole);

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL o ADMIN_PASSWORD no estan configurados: no se crea el usuario administrador");
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return;
        }
        if (adminPassword.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos " + MIN_ADMIN_PASSWORD_LENGTH + " caracteres");
        }
        Role adminRole = roleRepository.findByRoleEnum(RoleEnum.ADMIN).orElseThrow();
        userRepository.save(User.builder()
                .email(email)
                .firstName("Administrador")
                .lastName("Hospital")
                .password(passwordEncoder.encode(adminPassword))
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());
        log.info("Administrador inicial creado: {}", email);
    }

    private void ensureRole(RoleEnum roleEnum, Set<String> permissionNames) {
        if (roleRepository.findByRoleEnum(roleEnum).isPresent()) {
            return;
        }
        Set<Permission> permissions = new HashSet<>();
        for (String name : permissionNames) {
            permissions.add(permissionRepository.findByName(name)
                    .orElseGet(() -> permissionRepository.save(Permission.builder().name(name).build())));
        }
        roleRepository.save(Role.builder().roleEnum(roleEnum).permissions(permissions).build());
    }
}
