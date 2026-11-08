package com.hospital.backendHospital;

import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.PermissionRepository;
import com.hospital.backendHospital.repositories.RoleRepository;
import com.hospital.backendHospital.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode =  EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class BackendHospitalApplication {

	private static final Logger log = LoggerFactory.getLogger(BackendHospitalApplication.class);
	private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

	public static void main(String[] args) {
		SpringApplication.run(BackendHospitalApplication.class, args);
	}

	@Bean
	@Transactional
	CommandLineRunner init(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PermissionRepository permissionRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.email:}") String adminEmail,
			@Value("${app.admin.password:}") String adminPassword) {

		return args -> {
			// 1. Crear permisos primero (con verificación de existencia)
			Permission createPermission = createPermissionIfNotExists(permissionRepository, "CREATE");
			Permission readPermission = createPermissionIfNotExists(permissionRepository, "READ");
			Permission updatePermission = createPermissionIfNotExists(permissionRepository, "UPDATE");
			Permission deletePermission = createPermissionIfNotExists(permissionRepository, "DELETE");
			Permission refactorPermission = createPermissionIfNotExists(permissionRepository, "REFACTOR");

			// 2. Crear roles (con verificación de existencia)
			Role roleAdmin = createRoleIfNotExists(roleRepository, RoleEnum.ADMIN,
					Set.of(createPermission, readPermission, updatePermission, deletePermission, refactorPermission));

			Role roleDoctor = createRoleIfNotExists(roleRepository, RoleEnum.DOCTOR,
					Set.of(createPermission, readPermission, updatePermission));

			Role roleRecepcionist = createRoleIfNotExists(roleRepository, RoleEnum.RECEPTIONIST,
					Set.of(createPermission, readPermission));

			Role rolePatient = createRoleIfNotExists(roleRepository, RoleEnum.PATIENT,
					Set.of(readPermission));

			roleRepository.save(rolePatient);

			// 3. Crear el admin inicial con los datos de ADMIN_EMAIL / ADMIN_PASSWORD
			if (adminEmail.isBlank() || adminPassword.isBlank()) {
				log.warn("ADMIN_EMAIL o ADMIN_PASSWORD no estan configurados: no se crea el usuario administrador");
				return;
			}
			createUserIfNotExists(userRepository, passwordEncoder,
					adminEmail, "Administrador", "Hospital", adminPassword, roleAdmin);
		};
	}

	private Permission createPermissionIfNotExists(PermissionRepository repository, String name) {
		return repository.findByName(name)
				.orElseGet(() -> repository.save(
						Permission.builder().name(name).build()
				));
	}

	private Role createRoleIfNotExists(RoleRepository repository, RoleEnum roleEnum, Set<Permission> permissions) {
		return repository.findByRoleEnum(roleEnum)
				.orElseGet(() -> {
					Role role = Role.builder()
							.roleEnum(roleEnum)
							.build();
					Role savedRole = repository.save(role);
					// Establecer permisos después de guardar el rol
					savedRole.setPermissions(permissions);
					return repository.save(savedRole); // Guardar nuevamente con permisos
				});
	}

	private void createUserIfNotExists(UserRepository repository,
									   PasswordEncoder encoder,
									   String username,
									   String firstName,
									   String lastName,
									   String password,
									   Role role) {
		if (!repository.existsByEmail(username)) {
			if (password.length() < MIN_ADMIN_PASSWORD_LENGTH) {
				throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos " + MIN_ADMIN_PASSWORD_LENGTH + " caracteres");
			}
			User user = User.builder()
					.email(username)
					.firstName(firstName)
					.lastName(lastName)
					.password(encoder.encode(password))
					.build();
			User savedUser = repository.save(user);
			// Establecer rol después de guardar el usuario
			savedUser.setRoles(Set.of(role));
			repository.save(savedUser);
		}
	}
}