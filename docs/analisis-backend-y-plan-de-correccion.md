# Análisis del backend Hospital y plan de corrección

**Fecha:** 02/11/2026 · **Rama analizada:** `develop` (commit `b0fac92`)
**Alcance:** todo `src/main`, `src/test`, `pom.xml` y los `application*.properties`.

> Todas las rutas de código son relativas a `src/main/java/com/hospital/backendHospital/`, salvo que se indique otra cosa.
> Los números de línea corresponden al commit `b0fac92`.

## Estado actual verificado

- El proyecto **compila**.
- Las **53 pruebas unitarias** (`*ServiceTest`) **pasan**.
- `BackendHospitalApplicationTests.contextLoads` **falla**: necesita un MySQL real (`Public Key Retrieval is not allowed`).
- No hay pruebas para `AppointmentService`, `DoctorScheduleService`, `SupplyMovementService`, `SupplyUsageService`, `RoomService`, `FileService`, `PDFGeneratorService`, `AuthService`, `JwtService` ni para la seguridad. `MedicalRecordServiceTest` está comentado por completo.
- La regla de JaCoCo (95 % de líneas por paquete) hará fallar `mvn verify`.

## Resumen

| Prioridad | Cantidad | Qué significa |
|---|---|---|
| Baja | 10 | Limpieza, nombres, consistencia. No rompe nada. |
| Media | 14 | Diseño incompleto, validaciones flojas, deuda técnica. |
| Alta | 16 | Errores de lógica o de modelo de datos que corrompen datos o bloquean flujos. |
| Crítica | 11 | El endpoint no funciona o expone datos médicos o privilegios. |

Como pediste, los hallazgos van **de menor a mayor prioridad**. El **plan de corrección** (al final) va en el **orden en que conviene ejecutarlo**, que empieza por lo crítico.

---

## 1. Prioridad BAJA

### B1 — Inconsistencias en `pom.xml`
- **Dónde:** `pom.xml`
- **Problema:**
  - `java.version=17`, pero el `maven-compiler-plugin` usa `source/target 21`.
  - Versiones de jjwt mezcladas: api `0.12.3`, impl `0.12.6`, jackson `0.12.5`.
  - `junit-jupiter` y `mockito-junit-jupiter` tienen versión fija y pisan la del BOM de Spring Boot.
  - El procesador de Lombok (`1.18.30`) no coincide con la versión que gestiona Boot.
  - `description` dice "Demo project for Spring Boot".
- **Solución:**
  - Definir una sola versión de Java: `<release>${java.version}</release>`.
  - Crear una propiedad `jjwt.version` y usarla en los tres artefactos.
  - Quitar las versiones de junit y mockito.
  - Usar `${lombok.version}` en `annotationProcessorPaths`.

### B2 — Errores de escritura y nombres
- **Dónde:**
  - `services/IHospitalzationService.java`
  - `SupplyUsageMapper.toListoDto`
  - `SupplyUsageResponseDto.hospitalizaciontId`
  - `TokenResponse.accesToken` (`models/dto/token/TokenResponse.java:12`)
  - Paquete `dto/recepcionist`
  - `UnityEnum`, que debería llamarse `Unit`
  - Mensajes `"Star time"` y `"Doctor X" + "is inactive"` (falta el espacio)
  - `completeAppointment` lanza `"Error cancelling appointment"`
  - Mensaje `"because he has scheduled appointments"`
- **Solución:** renombrar y corregir los mensajes. ⚠️ Renombrar `accesToken` → `accessToken` cambia el JSON de login, así que hay que coordinarlo con el frontend.

### B3 — Código muerto
- **Dónde:**
  - `exceptions/IOException.java`, `exceptions/FileNotFoundException.java` y `exceptions/MaxSizeException.java` no se usan. Además, sus nombres chocan con `java.io.*`, que es lo que realmente importan `GlobalExceptionHandler`, `FileService` y `FileController`.
  - `PatientSummaryDto` no se usa.
  - `AuthService.patientService` no se usa (`auth/service/AuthService.java:37`).
  - `authService` no se usa en `DoctorController` ni en `DoctorScheduleController`.
  - `AuthController` importa `CreatePatientDto` sin usarlo.
- **Solución:** eliminar todo lo anterior, o usar las excepciones propias de forma consistente con nombres que no choquen (por ejemplo `FileStorageException`).

### B4 — Rutas REST inconsistentes
- **Dónde:** controladores.
- **Problema:**
  - Hay verbos en la URL: `/create`, `/{id}/update`.
  - `/api/supply-usage` no lleva `/v1` (`controllers/SupplyUsageController.java:16`).
  - Se mezcla camelCase (`/medicalSupplies`) con kebab-case (`/supply-usage`).
  - `POST /patients/create` devuelve `200` en vez de `201`, y lo mismo pasa en `/supplyMovements/entry|exit` y `/api/supply-usage`.
- **Solución:** definir una convención única, por ejemplo `POST /api/v1/patients`, `PATCH /api/v1/doctors/{id}` y todo en kebab-case, y aplicarla junto con la fase de seguridad para no tener que reescribir las reglas dos veces.

### B5 — Tipo de id incorrecto en `SpecialtyResponseDto`
- **Dónde:** `models/dto/specialty/SpecialtyResponseDto.java:16`
- **Problema:** el id es `Integer`, pero la entidad usa `Long`. MapStruct hace una conversión con pérdida.
- **Solución:** cambiarlo a `Long`.

### B6 — Inicializador de datos (`CommandLineRunner`)
- **Dónde:** `BackendHospitalApplication.java`
- **Problema:**
  - `@Transactional` sobre un método `@Bean` no tiene efecto.
  - Hay un `roleRepository.save(rolePatient)` redundante.
  - Existe un permiso `REFACTOR` sin significado.
  - El admin se crea con el email `"chucholo"`, que no es un email válido.
- **Solución:** moverlo a un componente `DataSeeder` propio con un `@Transactional` que sí funcione, y tomar los datos del admin de variables de entorno (ver C11).

### B7 — Rendimiento menor
- **Problema:**
  - `SpecialtyMapper.countDoctors` carga la colección de doctores de cada especialidad (N+1).
  - `JwtAuthenticationFilter` consulta al usuario dos veces por petición (`loadUserByUsername` y `findByEmail`) y además busca el token.
  - La query de `TokenRepository.findAllValidTokenByUser` usa `expired = false OR revoked = false` cuando debería ser `AND`.
  - Los tokens revocados nunca se borran.
- **Solución:**
  - Usar `@Query` con `COUNT` o `@Formula` para contar doctores.
  - Reutilizar el `UserDetails` en el filtro.
  - Corregir el `AND`.
  - Agregar una tarea `@Scheduled` que limpie los tokens expirados.

### B8 — DTOs de respuesta pobres
- **Problema:**
  - `patientName` y `doctorName` solo traen el nombre de pila.
  - `HospitalizationResponseDto` no incluye el doctor.
  - `AppointmentResponseDto` no incluye la especialidad.
- **Solución:** mapear el nombre completo con `expression` o `@AfterMapping` y agregar los campos que el frontend necesita.

### B9 — Configuración local de MySQL
- **Dónde:** `src/main/resources/application-local.properties`
- **Problema:** con MySQL 8 la conexión falla con `Public Key Retrieval is not allowed`. Así fue como falló `contextLoads`.
- **Solución:**
  - Agregar `&allowPublicKeyRetrieval=true` a la URL local.
  - Mejor aún, levantar el MySQL de desarrollo con un `docker-compose.yml` (ver Fase 0). Así nadie depende de un MySQL instalado a mano ni de `root/root`.

### B10 — Formato de error inconsistente
- **Dónde:** `exceptions/GlobalExceptionHandler.java`
- **Problema:** unos errores devuelven `{error, detalle}` y otros un mapa `{campo: mensaje}`.
- **Solución:** usar `ProblemDetail` (RFC 7807), que ya viene en Spring 6, con un formato único: `status`, `title`, `detail`, `errors[]`.

---

## 2. Prioridad MEDIA

### M1 — Validaciones de DTO incoherentes o insuficientes
- **Dónde y problema:**
  - `models/dto/user/CreateUserDto.java:21,25`: nombre y apellido con mínimo de 5 caracteres, así que "Ana" o "Gil" no pueden registrarse.
  - `models/dto/patient/UpdatePatientDto.java:15,18`: mínimo de 10 caracteres en nombre y apellido.
  - `models/dto/recepcionist/UpdateReceptionistDto.java:19,22`: mínimo 10 con el mensaje "between 5 and 20".
  - `CreatePatientDto`:
    - `height` y `weight` no llevan `@Positive`.
    - `dateOfBirth` no lleva `@Past`.
    - `bloodType` y `gender` son texto libre, cuando deberían ser enums.
    - `@NotNull` sobre `boolean smoker` no tiene efecto.
  - `CreateRoomDto.capacity`: `@NotNull` sobre un `int` no tiene efecto y falta `@Positive`.
  - `UpdatePatientDto.newEmergencyContact` valida la longitud pero no que sean dígitos.
- **Solución:** unificar las reglas de nombres (2–50), usar wrappers (`Integer`, `Boolean`) cuando se quiera `@NotNull`, y crear enums `BloodType` y `Gender`.

### M2 — Reglas de fecha y hora en citas
- **Dónde:** `models/dto/appointment/CreateAppointmentDto.java:23` y `AppointmentService.createAppointment`.
- **Problema:**
  - `@Future` impide agendar citas para el mismo día.
  - No se valida explícitamente `startTime < endTime`. Hoy, un rango invertido devuelve el mensaje engañoso "should last at least 15 minutes".
- **Solución:**
  - Cambiar a `@FutureOrPresent`.
  - Si la fecha es hoy, exigir que `startTime` sea posterior a la hora actual.
  - Validar `start < end` con su propio mensaje.

### M3 — Listas vacías devuelven 404
- **Dónde:**
  - `services/impl/AppointmentService.java:71`
  - `services/impl/ReceptionistService.java:38`
- **Problema:** una lista vacía no es un error.
- **Solución:** devolver una página o lista vacía con `200`.

### M4 — Validación de horarios duplicada y floja
- **Dónde:** `DoctorScheduleService.createDoctorSchedule`, `DoctorService.createDoctorAndSchedule` y `DoctorService.updateDoctorAndSchedule`.
- **Problema:**
  - La misma lógica está copiada en tres lugares.
  - Se permite `endTime == startTime`.
  - Los horarios existentes que se modifican en el update no pasan por ninguna validación.
- **Solución:** crear un `ScheduleValidator` único que exija `start < end`, una duración mínima y horas dentro del rango del hospital, y usarlo en los tres puntos.

### M5 — Filtro de horarios
- **Dónde:** `services/impl/DoctorScheduleService.java`
- **Problema:**
  - `DoctorScheduleFilterRequest.doctorId` (que además es `Integer`) nunca se aplica.
  - `firstName` no comprueba si viene en blanco.
  - Cuando se filtra por nombre y apellido se hacen dos `join` iguales.
- **Solución:** aplicar el filtro por `doctorId` (como `Long`), comprobar `isBlank()` y reutilizar un solo join.

### M6 — Gestión de archivos
- **Dónde:** `services/impl/FileService.java`, `controllers/FileController.java`, `models/entity/FileEntity.java:27`.
- **Problema:**
  - La URL de descarga se arma como `api/fileManager/files/`, sin `/v1` (`FileService.java:51`), así que los enlaces generados están rotos.
  - `getAllFiles()` carga en memoria el contenido completo de todos los archivos solo para calcular su tamaño.
  - La subida no valida el tipo ni la extensión.
  - El mensaje dice "2MB", pero no hay ningún `spring.servlet.multipart.max-file-size` configurado (el valor por defecto es 1MB).
  - `MaxUploadSizeExceededException` responde `417` cuando debería ser `413`.
  - `@Lob byte[]` deja el tipo de columna al dialecto. Hay que verificar en la BD que no quedó `tinyblob`/`blob` demasiado pequeño y declarar `columnDefinition = "LONGBLOB"`.
  - `FileEntity` no está ligado a ningún paciente ni dueño.
- **Solución:**
  - Corregir la URL.
  - Guardar el tamaño en su propia columna y usar una proyección sin `data`.
  - Validar el content-type contra una lista blanca y configurar el límite de tamaño.
  - Agregar `patient_id` y `uploaded_by` a `FileEntity`.

### M7 — El PDF del expediente es un placeholder
- **Dónde:** `services/impl/PDFGeneratorService.java:48` y `controllers/FileController.java`.
- **Problema:**
  - El cuerpo del PDF es `"This is a paragraph."`.
  - `Content-Disposition` se escribe dos veces con nombres distintos, y uno de ellos contiene `:`, que no es válido en Windows.
  - Cada llamada guarda un `FileEntity` nuevo, así que se acumulan duplicados.
- **Solución:**
  - Generar el contenido real: datos del paciente, alergias, historial, citas y hospitalizaciones.
  - Poner el header en un solo lugar.
  - Decidir si se guarda en BD (con versión) o solo se descarga.

### M8 — Modelo del historial clínico
- **Dónde:** `models/entity/MedicalRecord.java`, `services/impl/MedicalRecordService.java`.
- **Problema:**
  - La relación es `@OneToOne` con el paciente, así que cada paciente puede tener **un solo registro** en toda su vida.
  - No hay forma de actualizarlo.
  - No guarda qué doctor lo creó ni de qué cita viene (el propio código lo anota: *"Considerar crear historial medico solo para citas completadas"*).
- **Solución:** convertirlo en `MedicalRecordEntry` con relación `@ManyToOne` a `Patient`, `Doctor` y `Appointment`. Crearlo solo a partir de citas `COMPLETED` del doctor autenticado, y listarlo paginado.

### M9 — Funcionalidad CRUD faltante
- **Problema:**
  - Especialidades: no se pueden actualizar ni desactivar.
  - Salas: no se pueden actualizar ni listar solo las disponibles.
  - Insumos: no se pueden actualizar ni desactivar, y no hay alerta de stock bajo.
  - Horarios: no se pueden eliminar ni listar por doctor.
  - Hospitalizaciones: no hay un `GET` real. `@GetMapping("/{id}")` ignora el `id` y en su lugar ejecuta el filtro (`controllers/HospitalizationController.java:23`).
  - Citas: no se pueden reprogramar ni listar por doctor.
  - Doctor: no tiene endpoints "me" (mis citas, mi agenda).
- **Solución:** implementarlo en la Fase 6 del plan.

### M10 — Dos `UserDetailsService` y permisos sin usar
- **Dónde:** `config/AppConfig.java:25` y `services/impl/UserDetailService.java:22`.
- **Problema:**
  - Hay dos beans. `JwtAuthenticationFilter` recibe el de `AppConfig` solo porque coincide el nombre del parámetro, lo cual es frágil.
  - El de `AppConfig` no carga los permisos.
  - Los permisos (`CREATE`, `READ`…) se siembran, pero nada los usa.
  - `@EnableMethodSecurity` está activado sin ningún `@PreAuthorize`.
- **Solución:** dejar un solo `UserDetailsService` (el de `services/impl`) y decidir entre dos opciones: eliminar los permisos, o usarlos con `@PreAuthorize("hasAuthority('...')")`.

### M11 — Stock mínimo aplicado de forma incoherente
- **Dónde:** `services/impl/SupplyMovementService.java:73` frente a `SupplyUsageService.registerUsage`.
- **Problema:** una salida manual no puede dejar el stock por debajo del mínimo, pero el consumo en una hospitalización sí puede. Normalmente el stock mínimo es un **umbral de alerta**, no un piso.
- **Solución:** definir la regla (sugerido: permitir bajar hasta 0 y marcar `lowStock=true` o generar una alerta) y aplicarla igual en ambos flujos.

### M12 — `ddl-auto=update` en producción
- **Dónde:** `application-prod.properties`
- **Problema:** Hibernate modifica el esquema solo, sin control de versiones. Nunca elimina columnas y puede fallar con cambios de tipo, como el de ORDINAL a STRING (A1).
- **Solución:** usar Flyway con un `V1__baseline.sql` y `ddl-auto=validate`.

### M13 — Pruebas e infraestructura de testing
- **Problema:**
  - `contextLoads` necesita un MySQL real.
  - Faltan pruebas de los servicios con más lógica (citas, inventario, autenticación) y de la seguridad por rol.
  - `MedicalRecordServiceTest` está comentado.
  - JaCoCo exige 95 % y el build no lo cumple.
- **Solución:**
  - Hacer las pruebas de integración con **Testcontainers**, que levanta un MySQL 8 real en Docker durante las pruebas (detalle en la Fase 0). No se recomienda H2: no reproduce el comportamiento de MySQL en tipos LOB, enums, largo de columnas, restricciones únicas ni bloqueos, que es justo donde están varios de los bugs de este documento (A1, A8, A14, M6).
  - Separar las pruebas en dos grupos:
    - **Unitarias** (`*Test`, Mockito): corren con `surefire`, no necesitan Docker.
    - **De integración** (`*IT`, Testcontainers): corren con `failsafe` en `mvn verify` y sí necesitan Docker.
  - Agregar pruebas `@SpringBootTest` + MockMvc por rol sobre la base del contenedor.
  - Bajar el umbral temporalmente (por ejemplo a 70 %) y subirlo a medida que crezca la cobertura.

### M14 — Detalles de la cadena de seguridad
- **Dónde:** `config/SecurityConfig.java`, `config/JwtAuthenticationFilter.java`.
- **Problema:**
  - `.httpBasic(...)` está activo (`SecurityConfig.java:44`). Habilita un segundo mecanismo de login y hace que un 401 muestre el popup de usuario y contraseña del navegador.
  - No hay un `AuthenticationEntryPoint` ni un `AccessDeniedHandler` en JSON.
  - `/error` no tiene `permitAll`, así que los errores de la cadena terminan en un 403 sin cuerpo.
  - El filtro deja pasar cualquier ruta que *contenga* `/auth`.
  - `spring.security.user.*` y `springdoc.*` están en los properties, pero no tienen efecto porque hay un `UserDetailsService` propio y springdoc no es una dependencia.
- **Solución:**
  - Quitar `httpBasic`.
  - Agregar un entry point que devuelva `401` en JSON y un handler que devuelva `403` en JSON.
  - Poner `permitAll` en `/error`.
  - Cambiar la comprobación del filtro a `startsWith("/auth/")`.
  - Agregar springdoc o borrar esas propiedades.

---

## 3. Prioridad ALTA

### A1 — Enums guardados como ORDINAL
- **Dónde:** `models/entity/Appointment.java:43` (`status`) y `models/entity/DoctorSchedule.java:25` (`dayOfWeek`).
- **Problema:**
  - Ninguno de los dos lleva `@Enumerated(EnumType.STRING)`, así que se guardan como números.
  - Reordenar el enum `AppointmentStatus` cambia el significado de los datos que ya existen.
  - El filtro de citas hace `cb.upper(root.get("status"))` sobre un entero (`AppointmentService.java:51`), lo que falla en cuanto se filtra por estado.
- **Solución:**
  - Agregar `@Enumerated(EnumType.STRING)` y migrar los datos con Flyway.
  - En el filtro, convertir con `AppointmentStatus.valueOf(...)` y comparar con `cb.equal`.

### A2 — "Cita activa" del paciente
- **Dónde:** `repositories/AppointmentRepository.java:18` y `AppointmentService.java:82`.
- **Problema:** `findByPatientIdAndStatus` devuelve una sola entidad. Si el paciente tiene dos citas `PENDING`, lanza `IncorrectResultSizeDataAccessException` y responde 500. Además, nada impide que un paciente tenga varias citas pendientes.
- **Solución:**
  - Definir la regla: una sola cita pendiente por paciente, o varias.
  - Si son varias, cambiar a `findFirstByPatientIdAndStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc` o devolver una lista.
  - Si es una sola, validarlo en `createAppointment`.

### A3 — Registro de paciente en dos pasos sin control
- **Dónde:** `auth/service/AuthService.register` y `services/impl/PatientService.java:68`.
- **Problema:**
  - `/auth/register` crea un `User` con rol PATIENT pero sin perfil `Patient`.
  - Hasta que el usuario llama a `/patients/create`, todos los endpoints de paciente responden 404.
  - `createPatient` no comprueba si ese usuario ya tiene un perfil: o se crean duplicados, o se produce un 500 por la restricción única de `@OneToOne`.
- **Solución:**
  - Validar `patientRepository.existsByUser(user)` antes de crear el perfil.
  - Considerar un registro en un solo paso (usuario + perfil en una transacción) o un flag `profileCompleted`.

### A4 — `updateDoctorAndSchedule` frágil
- **Dónde:** `services/impl/DoctorService.java:139-206`.
- **Problema:**
  - Si `schedules` no viene en la petición, el `.stream()` sobre `null` lanza NPE y responde 500 (línea 164).
  - Si vienen días repetidos, `Collectors.toMap` lanza `IllegalStateException` y responde 500 (línea 165).
  - Los horarios existentes que se modifican no se validan (ver M4).
  - Se pueden eliminar horarios de días en los que el doctor tiene citas futuras.
  - El campo `email` del DTO se ignora sin avisar.
- **Solución:**
  - Procesar los horarios solo si `schedules != null`.
  - Detectar días repetidos y responder 400.
  - Validar todos los horarios.
  - Bloquear la eliminación de días con citas `PENDING` futuras.
  - Implementar el cambio de email (con chequeo de unicidad) o quitarlo del DTO.

### A5 — Desactivación de doctor o paciente
- **Dónde:** `DoctorService.java:214` y `PatientService.java:122`.
- **Problema:**
  - Cuenta las citas **canceladas o completadas** con fecha futura como si bloquearan la desactivación.
  - Ignora las citas de **hoy**, porque usa `isAfter`.
  - `Doctor.appointments`, `Doctor.schedules`, `Patient.appointments` y `Specialty.doctor` no llevan `@Builder.Default`, así que valen `null` cuando la entidad se crea con el builder (Lombok ya lo advierte al compilar). Eso provoca NPE en los tests y en cualquier objeto recién construido.
- **Solución:**
  - Usar una query `existsByDoctorIdAndStatusAndDateGreaterThanEqual(id, PENDING, today)`.
  - Agregar `@Builder.Default` en las colecciones.

### A6 — Reglas de hospitalización
- **Dónde:** `services/impl/HospitalizationService.java`.
- **Problema:**
  - La sala se marca como no disponible con el primer paciente (línea 93) y `capacity` se ignora.
  - Se puede dar de alta dos veces la misma hospitalización, y la segunda sobreescribe la fecha (línea 107).
  - No se valida que el paciente esté activo.
  - `costPerDay` se pide en el DTO pero no existe en la entidad: siempre se responde `null`.
- **Solución:**
  - Contar las hospitalizaciones activas por sala, compararlas con `capacity` y marcar `available` solo cuando se llene.
  - Rechazar el alta si `!active`.
  - Validar los estados del paciente y del doctor.
  - Agregar `costPerDay` a la entidad y calcular el total al dar de alta, o quitarlo de los DTOs.

### A7 — Consumo de insumos
- **Dónde:** `services/impl/SupplyUsageService.java`, `models/dto/supplyUsage/CreateSupplyUsageDto.java:22`.
- **Problema:**
  - `quantityUsed` no tiene `@Positive`. Un valor negativo **aumenta** el stock y registra una salida negativa.
  - `getUsageByHospitalization(id)` ignora el `id` y devuelve **todos** los consumos (línea 34).
  - No se valida que el insumo esté `active`.
- **Solución:**
  - Agregar `@Positive`.
  - Crear `findByHospitalizationId` en el repositorio.
  - Validar que el insumo esté activo.

### A8 — Concurrencia
- **Problema:**
  - Stock: dos movimientos simultáneos leen la misma cantidad y uno de los dos se pierde.
  - Citas: dos pacientes pueden reservar el mismo horario a la vez, porque entre el chequeo y el `save` no hay ningún bloqueo.
- **Solución:**
  - Agregar `@Version` a `MedicalSupply` (bloqueo optimista) y responder `409` ante `OptimisticLockingFailureException`.
  - Para citas, combinar una restricción única `(doctor_id, date, start_time)` con un bloqueo pesimista del doctor al reservar.

### A9 — Endpoints de recepcionista
- **Dónde:** `services/impl/ReceptionistService.java`, `controllers/ReceptionistController.java:45`.
- **Problema:**
  - `updateReceptionist` y `deactivateReceptionist` buscan **cualquier** `User` por id, así que permiten editar o desactivar a doctores, pacientes o al propio admin.
  - Al cambiar el email no se comprueba que sea único (línea 73), lo que da un 500 por la restricción.
  - `deactivateReceptionist(Long id)` no tiene `@PathVariable`: el `id` siempre llega `null` y el endpoint **siempre falla**.
- **Solución:**
  - Comprobar que el usuario tenga el rol RECEPTIONIST.
  - Validar el email con `existsByEmailAndIdNot`.
  - Agregar `@PathVariable`.

### A10 — Excepciones que terminan en 500
- **Dónde:**
  - `DoctorService.java:86` y `SpecialtyService.java:44` usan `IllegalArgumentException` para duplicados.
  - `AuthService.java:99`.
  - `GlobalExceptionHandler`.
- **Problema:** varias excepciones caen en el handler genérico y responden 500:
  - `IllegalArgumentException`
  - `DataIntegrityViolationException`
  - `HttpMessageNotReadableException` (JSON mal formado o enum inválido)
  - `MethodArgumentTypeMismatchException`
  - `MissingServletRequestParameterException`
  - `AccessDeniedException`

  Además, el handler genérico devuelve `ex.getMessage()` al cliente, lo que puede filtrar detalles internos de SQL.
- **Solución:**
  - Usar `InvalidDataException` (o una nueva `ConflictException` que responda `409`) para duplicados.
  - Agregar handlers específicos para las excepciones anteriores.
  - En el handler genérico, registrar el error en el log y responder con un mensaje neutro.

### A11 — Los DTOs de respuesta no devuelven el `id`
- **Dónde:** `MedicalSupplyResponseDto`, `RoomResponseDto`, `HospitalizationResponseDto`, `MedicalRecordResponseDto`, `DoctorScheduleResponseDto` (no trae el id del horario).
- **Problema:** el cliente no puede dar de alta una hospitalización, registrar un movimiento o un consumo de insumo, ni elegir una sala, porque nunca recibe el id.
- **Solución:** agregar el `id` a todos los DTOs de respuesta.

### A12 — Los roles no cubren los flujos reales
- **Dónde:** `config/SecurityConfig.java`.
- **Problema:**
  - El PACIENTE no puede listar doctores (`GET /api/v1/doctors` es solo ADMIN, línea 63) ni especialidades, así que **no tiene forma de saber a quién agendar**.
  - El DOCTOR no tiene "mis citas" ni "mi agenda".
  - El rol **RECEPTIONIST no tiene acceso a nada útil** (no puede registrar pacientes, agendar citas ni hospitalizar).
- **Solución:**
  - Definir una matriz de permisos por rol (abajo hay una propuesta).
  - Agregar un endpoint público o de paciente con doctores activos, filtrables por especialidad, y sus horarios.
  - Agregar endpoints `/doctors/me/...`.

### A13 — Secretos y credenciales en el repositorio
- **Dónde:**
  - `application-local.properties`: secreto JWT y `root/root` de la base de datos.
  - `BackendHospitalApplication.java:59`: admin `"chucholo"` / `"1234"`.
- **Problema:** el secreto JWT está en el historial de git. Si alguna vez se usó en algún entorno compartido, cualquiera puede **firmar tokens de ADMIN**. La contraseña del admin es trivial.
- **Solución:**
  - Rotar el secreto.
  - Leer el secreto y los datos del admin de variables de entorno, o de un `application-local.properties` en `.gitignore` con un `.example` versionado.
  - Exigir una contraseña fuerte para el admin o forzar su cambio en el primer login.

### A14 — Entidad `Token`
- **Dónde:** `auth/repository/Token.java:15,22`.
- **Problema:**
  - La columna `token` es `VARCHAR(255) UNIQUE`. Un JWT HS384 con el claim de roles ronda los 200 caracteres más el largo del email, así que **los emails largos rompen el login** con "Data too long".
  - La clase es `final`, lo que impide que Hibernate cree proxies.
  - `@GeneratedValue` sin estrategia crea una tabla de secuencia en MySQL.
- **Solución:** declarar `@Column(length = 1024)` (o guardar un hash SHA-256 del token con índice único), quitar `final` y usar `GenerationType.IDENTITY`.

### A15 — Cascadas peligrosas
- **Dónde:** `models/entity/Specialty.java:38`.
- **Problema:** `cascade = CascadeType.ALL` hacia los doctores significa que **borrar una especialidad borra a sus doctores**, y con ellos sus usuarios por la cascada de `Doctor.user`.
- **Solución:** quitar la cascada, y en general preferir la desactivación lógica a los borrados.

### A16 — `@Data` de Lombok en entidades JPA bidireccionales
- **Dónde:** todas las entidades excepto `User`.
- **Problema:** `@Data` genera `equals`, `hashCode` y `toString` que recorren las relaciones (Doctor ↔ Appointment, Specialty ↔ Doctor, Patient ↔ Appointment). Esto provoca `StackOverflowError` y cargas lazy inesperadas al loguear, al meter entidades en un `Set` o al depurar.
- **Solución:** usar `@Getter @Setter`, implementar `equals`/`hashCode` basados en el id y, si hace falta `toString`, escribirlo sin las relaciones.

---

## 4. Prioridad CRÍTICA

### C1 — No se puede crear ninguna hospitalización
- **Dónde:** `services/impl/HospitalizationService.java:85`, `models/dto/hospitalization/CreateHospitalizationDto.java`.
- **Problema:** `Hospitalization.doctor` es `optional=false` y `nullable=false`, pero el builder nunca asigna un doctor y el DTO ni siquiera tiene `doctorId`. Cada `POST /hospitalizations` falla con un 500 (not-null property). Las pruebas no lo detectan porque el repositorio está mockeado.
- **Solución:** agregar `doctorId` al DTO, buscar el doctor y validar que esté activo.

### C2 — Filtros que nunca se aplican
- **Dónde:**
  - `services/impl/HospitalizationService.java:40-60`
  - `services/impl/MedicalSupplyService.java:34`
- **Problema:** se llama `spec.and(...)` sin reasignar el resultado (`Specification` es inmutable), así que los filtros se descartan y siempre se devuelven **todos** los registros. Además, el filtro de insumos usa `like` sin `%`.
- **Solución:** reasignar con `spec = spec.and(...)` y buscar con `cb.like(cb.lower(...), "%" + x.toLowerCase() + "%")`.

### C3 — Filtros sobre atributos que no existen (500)
- **Dónde:**
  - `services/impl/DoctorService.java:70`: `"isActive"`
  - `services/impl/PatientService.java:49`: `"isActive"`
  - `services/impl/PatientService.java:59`: `"hospitalized"`
- **Problema:** las entidades tienen el campo `active` y no existe ningún `hospitalized`. Filtrar por estado u hospitalización lanza `IllegalArgumentException` y responde 500.
- **Solución:**
  - Cambiar a `"active"`.
  - Para `hospitalized`, usar una subquery `EXISTS` sobre `Hospitalization` con `active = true`.

### C4 — Los doctores no pueden completar citas
- **Dónde:** `services/impl/AppointmentService.java:157`.
- **Problema:** compara `appointment.getDoctor().getId()` (id de la tabla `doctors`) con `user.getId()` (id de la tabla `users`). Solo funciona cuando los dos ids coinciden por casualidad, y en ese caso **otro doctor** podría completar la cita.
- **Solución:** comparar `appointment.getDoctor().getUser().getId()` con `user.getId()`.

### C5 — "Mi historial" devuelve el historial de otro paciente
- **Dónde:** `controllers/MedicalRecordController.java:33`.
- **Problema:** pasa `user.getId()` como si fuera un `patientId`. El paciente recibe un 404 o, peor, **el expediente de otro paciente** cuyo id coincida.
- **Solución:** resolver el `Patient` con `findByUser(user)` y usar su id.

### C6 — El solapamiento de citas no se filtra por doctor
- **Dónde:** `repositories/AppointmentRepository.java:20-29`.
- **Problema:** la query no filtra por `doctor`. Si **cualquier** doctor tiene una cita en ese horario, **ningún** otro doctor puede atender a esa hora, así que el hospital solo puede dar una cita a la vez. Tampoco se impide que un mismo paciente tenga dos citas a la misma hora.
- **Solución:** agregar `AND a.doctor.id = :doctorId` y una segunda verificación por `patient.id`.

### C7 — Errores de autenticación que responden 500 o 403
- **Dónde:** `exceptions/GlobalExceptionHandler.java:36`, `config/JwtAuthenticationFilter.java:53`.
- **Problema:**
  - Un login con contraseña incorrecta lanza `BadCredentialsException` dentro del controlador, la atrapa el handler `Exception.class` y responde **500 "Bad credentials"**.
  - Un token expirado, mal firmado o mal formado hace que `extractUsername` lance `JwtException` fuera del `DispatcherServlet` y la respuesta termina en 500 o 403 sin cuerpo.
  - El handler genérico también convierte en 500 los `NoResourceFoundException`, que deberían ser 404 (rutas inexistentes).
- **Solución:**
  - Agregar un handler para `AuthenticationException` que responda `401`, uno para `DisabledException` que responda `403` y uno para `NoResourceFoundException` que responda `404`.
  - En el filtro, envolver el parseo en un `try/catch (JwtException)` y continuar sin autenticar, para que el entry point devuelva 401.

### C8 — Flujo de refresh token roto
- **Dónde:** `auth/service/AuthService.java:104,110,113` y los properties.
- **Problema:**
  - El "nuevo access token" se genera con `generateRefreshToken`, es decir, con la duración de un refresh.
  - Cuando el token no es válido se hace `return null`, que responde `200` con cuerpo vacío.
  - `findByEmail(...).orElseThrow()` lanza `NoSuchElementException`, que responde 500.
  - `refresh-token.expiration=60480000` son unas **16.8 h**, menos que las 24 h del access token. Probablemente se quiso escribir `604800000` (7 días).
  - Como no hay un claim que distinga el tipo de token, **un access token sirve como refresh**.
  - Los refresh tokens no se guardan, así que no se pueden revocar.
- **Solución:**
  - Generar el nuevo token con `generateToken`.
  - Responder 401 en los casos inválidos.
  - Corregir la expiración.
  - Agregar un claim `type: access|refresh` y validarlo.
  - Guardar y revocar los refresh tokens.

### C9 — La autorización no coincide con las rutas reales (fuga de datos médicos)
- **Dónde:** `config/SecurityConfig.java:48-101`.
- **Problema:**
  - Hay reglas para rutas **que no existen**: `/api/v1/pdf/{id}/generate`, `/api/v1/fileManager/files` solo para listar, `/appointments/patients/{id}`, `/appointments/patients/{id}/active`, `/patients/search`, `/doctorSchedules/doctors/{id}` y `/medicalRecords/appointments/{id}`.
  - Las rutas reales quedan bajo `anyRequest().authenticated()`, es decir, abiertas a **cualquier usuario logueado, incluido un PACIENTE**:
    - `GET /api/v1/fileManager/pdf/{patientId}/generate`: cualquiera genera el expediente de cualquier paciente.
    - `GET /api/v1/fileManager/files/{id}` y `POST /upload`: cualquiera descarga o sube archivos.
    - `GET /api/v1/appointments`: un paciente puede listar **todas** las citas de todos los pacientes.
    - `/api/v1/hospitalizations/**`, `/api/v1/rooms/**` y `/api/supply-usage/**`: un paciente puede hospitalizar, crear salas y consumir inventario.
  - `GET /medicalRecords/patients/{id}` permite que un PACIENTE lea el historial de **cualquier** otro paciente (IDOR).
- **Solución:**
  - Reescribir las reglas a partir de la matriz de roles.
  - Cerrar la regla final con `anyRequest().denyAll()` para que una ruta nueva quede bloqueada por defecto.
  - Para los recursos de paciente, validar la propiedad del recurso en el servicio (si el rol es PATIENT, solo puede ver lo suyo).
  - Agregar pruebas MockMvc por rol y endpoint.

### C10 — Los usuarios desactivados pueden seguir entrando
- **Dónde:** `config/AppConfig.java:25`, `services/impl/UserDetailService.java`, `deactivate*`.
- **Problema:**
  - `User.active` nunca se pasa a `UserDetails.disabled(...)`, así que un doctor, paciente o recepcionista desactivado **puede seguir haciendo login**.
  - Desactivar a un usuario no revoca sus tokens vigentes.
- **Solución:** mapear `.disabled(!user.isActive())` y, en cada `deactivate*`, revocar los tokens del usuario.

### C11 — El contexto de Spring no arranca en el entorno de pruebas
- **Dónde:** `src/test/.../BackendHospitalApplicationTests.java`, configuración.
- **Problema:** sin un MySQL local configurado exactamente igual, el contexto no levanta. Esto oculta todos los errores que solo aparecen al arrancar o al ejecutar SQL real, como C1, C3 o A1. Es la razón principal de que los bugs críticos hayan pasado las 53 pruebas.
- **Solución:** pruebas de integración con **Testcontainers + Docker** (MySQL 8 real, con el mismo esquema que crea Flyway) y repositorios reales. Ver Fase 0.

---

## 5. Matriz de roles propuesta (para C9 y A12)

| Recurso | ADMIN | RECEPTIONIST | DOCTOR | PATIENT |
|---|---|---|---|---|
| Recepcionistas (CRUD) | ✅ | — | — | — |
| Doctores: crear/editar/desactivar | ✅ | — | — | — |
| Doctores activos + horarios (lectura) | ✅ | ✅ | ✅ | ✅ |
| Especialidades: lectura | ✅ | ✅ | ✅ | ✅ |
| Especialidades: escritura | ✅ | — | — | — |
| Pacientes: listar/filtrar | ✅ | ✅ | ✅ | — |
| Paciente: mi perfil | — | — | — | ✅ |
| Citas: crear | ✅ | ✅ (para un paciente) | — | ✅ (propias) |
| Citas: listar todas/filtrar | ✅ | ✅ | solo las suyas | solo las suyas |
| Citas: completar | — | — | ✅ (propias) | — |
| Citas: cancelar | ✅ | ✅ | — | ✅ (propias) |
| Historial clínico: crear | — | — | ✅ (de cita completada propia) | — |
| Historial clínico: leer | ✅ | — | ✅ | ✅ (propio) |
| Hospitalizaciones | ✅ | ✅ | ✅ | lectura propia |
| Salas | ✅ | lectura | lectura | — |
| Insumos / movimientos | ✅ | — | — | — |
| Consumo de insumos | ✅ | — | ✅ | — |
| PDF del expediente | ✅ | — | ✅ | ✅ (propio) |
| Archivos | ✅ | ✅ | ✅ (de sus pacientes) | ✅ (propios) |

*(Es una propuesta: ajústala a las reglas reales del hospital antes de implementarla.)*

---

## 6. Plan de corrección, parte por parte

El plan está en **orden de ejecución**. Primero se arma una base de pruebas confiable, después se cierran los agujeros de seguridad y los bugs críticos, y al final se completa la funcionalidad y se limpia. Conviene que cada fase sea una rama y un PR hacia `develop`, y que no se avance a la siguiente sin tener los tests en verde.

### Fase 0 — Base para poder verificar, con Docker y Testcontainers (1–1½ días) — ✅ COMPLETADA (02/11/2026)
**Hallazgos:** C11, B1, B9, M13 (parte).

> **Lo que se ajustó al implementarla (respecto a lo descrito abajo):**
> - Se fijó `testcontainers.version=1.21.4`. La 1.20.6 que trae Spring Boot 3.4.5 falla con Docker Engine 29 ("Could not find a valid Docker environment", Status 400).
> - Se usa Java 21 (era lo que el compilador ya usaba) y `jjwt-impl`/`jjwt-jackson` quedaron con scope `runtime`.
> - El MySQL de `docker-compose` se publica en el puerto **3307**, para no chocar con un MySQL instalado en el 3306.
> - No se usa `withReuse(true)`: Spring reutiliza el contexto y, con él, el contenedor entre clases `*IT`.
> - JaCoCo: regla `BUNDLE` con un mínimo de 30 % de líneas (cobertura real medida: 36.7 %). Los reportes y el check corren en `verify`, después de las pruebas de integración.
> - No se agregaron el `Dockerfile` ni `spring-boot-docker-compose` (opcionales).
> - **Pendiente:** en el perfil `test` el admin se sigue creando con los datos fijos del `CommandLineRunner`. Se resuelve en la Fase 1 (paso 7, datos del admin desde variables de entorno) y en la Fase 7 (`DataSeeder`, B6); al hacerlo, desactivar la creación del admin en `application-test.properties` o darle datos de prueba.

**Requisito:** Docker Desktop instalado y corriendo (en Windows, con el backend WSL 2).

1. **Alinear `pom.xml`:** versión de Java, versiones de jjwt, quitar las versiones de junit y mockito, y la versión de Lombok.

2. **Dependencias de Testcontainers.** Spring Boot 3.4 ya gestiona sus versiones, así que no hace falta indicarlas:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-testcontainers</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>junit-jupiter</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>mysql</artifactId>
       <scope>test</scope>
   </dependency>
   ```

3. **Configuración compartida del contenedor.** Con `@ServiceConnection`, Spring Boot configura `spring.datasource.*` a partir del contenedor, sin URLs ni contraseñas escritas a mano:
   ```java
   // src/test/java/com/hospital/backendHospital/TestcontainersConfig.java
   @TestConfiguration(proxyBeanMethods = false)
   public class TestcontainersConfig {
       @Bean
       @ServiceConnection
       MySQLContainer<?> mysqlContainer() {
           return new MySQLContainer<>("mysql:8.0").withReuse(true);
       }
   }
   ```
   ```java
   // Clase base para las pruebas de integración
   @SpringBootTest
   @AutoConfigureMockMvc
   @ActiveProfiles("test")
   @Import(TestcontainersConfig.class)
   public abstract class AbstractIntegrationTest { }
   ```
   - Crear `src/test/resources/application-test.properties` con un secreto JWT de prueba y la creación del admin desactivada o con datos de prueba.
   - Al cambiar a `ddl-auto=validate` con Flyway (Fase 3), las pruebas validarán también las migraciones reales.
   - `withReuse(true)` funciona solo si se activa `testcontainers.reuse.enable=true` en `~/.testcontainers.properties`. Sin eso, se crea un contenedor nuevo por ejecución, lo cual también funciona.

4. **Separar unitarias e integración:**
   - `contextLoads` pasa a llamarse `BackendHospitalApplicationIT` y extiende `AbstractIntegrationTest`.
   - Agregar `maven-failsafe-plugin` para ejecutar `*IT` en `mvn verify`.
   - Resultado: `./mvnw test` corre solo las unitarias (sin Docker) y `./mvnw verify` corre todo.

5. **`docker-compose.yml` para desarrollo local** (en la raíz del repo):
   ```yaml
   services:
     mysql:
       image: mysql:8.0
       environment:
         MYSQL_DATABASE: hospitalbd
         MYSQL_USER: ${DB_USER:-hospital}
         MYSQL_PASSWORD: ${DB_PASSWORD:-hospital}
         MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-change-me}
       ports:
         - "3306:3306"
       volumes:
         - mysql-data:/var/lib/mysql
   volumes:
     mysql-data:
   ```
   - Ajustar `application-local.properties` para leer `DB_USER`/`DB_PASSWORD` de variables de entorno (con un `.env.example` versionado y `.env` en `.gitignore`). Esto también avanza A13.
   - Agregar `allowPublicKeyRetrieval=true` a la URL local (B9).
   - Opcional: Spring Boot Docker Compose support (`spring-boot-docker-compose`) para que `./mvnw spring-boot:run` levante el contenedor automáticamente.

6. **Opcional:** un `Dockerfile` multi-stage (build con Maven y runtime con JRE 21) para desplegar en Railway con la misma imagen que se prueba localmente.

7. **JaCoCo:** bajar temporalmente el umbral para que `mvn verify` pase, y dejar un TODO para subirlo. Configurar la agregación para que cuente la cobertura de unitarias e integración.

**Criterio de salida:**
- `./mvnw test` en verde sin Docker.
- `./mvnw verify` en verde con Docker corriendo, sin ningún MySQL instalado localmente.
- `docker compose up -d` + `./mvnw spring-boot:run` levanta la aplicación en local.

### Fase 1 — Autenticación y seguridad (2–3 días) — ✅ COMPLETADA (07/11/2026)
**Hallazgos:** C7, C8, C9, C10, A13, A14, M10, M14.

> **Decisiones y ajustes al implementarla:**
> - Matriz de roles aplicada solo a los endpoints que existen hoy. El PACIENTE puede leer doctores, especialidades y horarios. El RECEPCIONISTA agenda y cancela citas (con `patientId` en el body) y gestiona hospitalizaciones. Los endpoints nuevos ("me" del doctor, catálogo, archivos del paciente) quedan para la Fase 6, igual que unificar las rutas (B4).
> - El DOCTOR puede usar `GET /appointments`, pero el servicio fuerza su propio `doctorId`.
> - `Token` guarda el **hash SHA-256** del JWT (no el JWT) en una tabla nueva `auth_tokens`. Así evita el límite de `VARCHAR(255)` y una fuga de la tabla no expone tokens. Las tablas viejas `token` y `token_seq` quedan sin uso y se pueden borrar a mano.
> - Cada JWT lleva un `jti` aleatorio: dos tokens emitidos en el mismo segundo eran idénticos y su hash chocaba (lo detectó `AuthenticationIT`).
> - Refresh con rotación: cada refresh revoca el par anterior y entrega uno nuevo. Login, refresh y logout dejan una sola sesión activa por usuario.
> - `application-local.properties` lee el `.env` (`spring.config.import`). `JWT_SECRET` es obligatorio; `ADMIN_EMAIL`/`ADMIN_PASSWORD` son opcionales (si faltan, no se crea el admin; la contraseña exige 12 caracteres). En el perfil `test` no se crea el admin (resuelve el pendiente de la Fase 0).
> - M10: queda un solo `UserDetailsService`. Los permisos (`CREATE`, `READ`…) se siguen cargando pero nada los usa; la decisión de borrarlos va con el `DataSeeder` (Fase 7).
>
> **Pendientes detectados:**
> - **Admin legado `chucholo`/`1234`:** el seeder nuevo no lo toca, así que sigue existiendo y entrando en cualquier base donde ya se creó. Hay que desactivarlo o borrarlo a mano en cada entorno (incluido producción).
> - **Producción (Railway):** configurar un `JWT_SECRET` nuevo (rotación; cierra todas las sesiones), `JWT_REFRESH_EXPIRATION=604800000`, `ADMIN_EMAIL` y `ADMIN_PASSWORD`. `SWAGGER_USER`/`SWAGGER_PASSWORD` ya no se usan.
> - `GET /fileManager/pdf/{id}/generate` con un paciente inexistente responde 500: el controlador fija `Content-Type: application/pdf` antes de buscar al paciente y el 404 no se puede escribir como JSON. Se corrige con M7 (Fase 6).
> - C5 (`/medicalRecords/patients/me` usa el id del usuario) sigue abierto: es de la Fase 2.
1. Dejar un solo `UserDetailsService` y mapear `disabled(!active)` (C10, M10).
2. En `JwtAuthenticationFilter`: `try/catch JwtException` y `startsWith("/auth/")` (C7, M14).
3. En `SecurityConfig`:
   - Quitar `httpBasic`.
   - Agregar entry point 401 y handler 403 en JSON.
   - Poner `permitAll` en `/error`.
   - Reescribir las reglas según la matriz y cerrar con `denyAll()` (C9, M14).
4. Corregir el refresh: usar `generateToken`, agregar el claim `type`, responder 401 en vez de `null`, corregir la expiración y guardar los refresh tokens (C8).
5. Entidad `Token`: aumentar el largo de la columna, quitar `final` e `IDENTITY`. Corregir el `AND` del repositorio (A14, B7).
6. Revocar los tokens en cada `deactivate*` (C10).
7. Sacar los secretos del repo, rotar el secreto JWT y tomar los datos del admin de variables de entorno (A13, B6). En `application-test.properties`, desactivar la creación del admin o darle datos de prueba (pendiente de la Fase 0).
8. Agregar handlers de `AuthenticationException`, `AccessDeniedException` y `NoResourceFoundException` (C7).

**Pruebas** (integración con Testcontainers + MockMvc):
- Por rol: cada endpoint con el rol permitido debe responder 2xx, con un rol no permitido 403 y sin token 401.
- Login con contraseña incorrecta debe responder 401.
- Usuario desactivado debe recibir 401/403.
- Token expirado debe responder 401.
- Un access token usado como refresh debe responder 401.

### Fase 2 — Bugs funcionales críticos (1–2 días)
**Hallazgos:** C1, C2, C3, C4, C5, C6.
1. Hospitalización: agregar `doctorId` al DTO, buscar el doctor y validarlo (C1).
2. Reasignar `spec = spec.and(...)` en Hospitalization y MedicalSupply, y corregir el `like` (C2).
3. Filtros: cambiar `"isActive"` → `"active"` e implementar `hospitalized` con una subquery `EXISTS` (C3).
4. `completeAppointment`: comparar contra `doctor.getUser().getId()` (C4).
5. `/medicalRecords/patients/me`: resolver el `Patient` desde el usuario (C5).
6. Solapamiento: filtrar por `doctorId` y agregar una verificación por paciente (C6).

**Pruebas:** una prueba de integración (`*IT`, extendiendo `AbstractIntegrationTest`, contra MySQL en Testcontainers) por cada caso. Los mocks no detectaron ninguno de estos bugs.

### Fase 3 — Modelo de datos e integridad (2–3 días)
**Hallazgos:** A1, A5 (`@Builder.Default`), A8, A15, A16, M12.
1. Introducir Flyway: generar `V1__baseline.sql` desde el esquema actual y poner `ddl-auto=validate` (M12).
2. `@Enumerated(STRING)` en `Appointment.status` y `DoctorSchedule.dayOfWeek`, con la migración `V2` que convierte los ordinales (A1).
3. Cambiar `@Data` por `@Getter/@Setter` con `equals/hashCode` basados en el id en todas las entidades (A16).
4. `@Builder.Default` en todas las colecciones (A5).
5. Quitar `CascadeType.ALL` de `Specialty.doctor` (A15).
6. `@Version` en `MedicalSupply`, restricción única `(doctor_id, date, start_time)` en `appointments`, y handler de `OptimisticLockingFailureException` y `DataIntegrityViolationException` que responda 409 (A8).
7. **Datos de prueba para desarrollo:** una migración de Flyway en una carpeta aparte (por ejemplo `src/main/resources/db/dev-data`) que solo se agrega a `spring.flyway.locations` en el perfil `local`, nunca en `prod` ni en `test`.
   - Contenido: un usuario por rol (ADMIN, RECEPTIONIST, DOCTOR, PATIENT) más algunos extra, especialidades, doctores con horarios, pacientes con perfil completo, salas, insumos y citas en distintos estados.
   - Las contraseñas van como hashes BCrypt precalculados. Documentar en este plan (o en el README) el email y la contraseña de prueba de cada rol.
   - Flyway la ejecuta una sola vez por base. Para recargarla desde cero: `docker compose down -v` y `docker compose up -d`.
   - Se hace en esta fase y no antes porque el esquema cambia aquí (A1) y en la Fase 6 (M8); un script escrito antes habría que reescribirlo. Si el historial clínico cambia en la Fase 6, se actualiza con una migración nueva en la misma carpeta.

### Fase 4 — Reglas de negocio (3–4 días)
**Hallazgos:** A2, A3, A4, A5, A6, A7, A9, M2, M4, M11.
1. Crear `ScheduleValidator` y usarlo en los tres puntos (M4).
2. `updateDoctorAndSchedule`: manejar `schedules` nulos, días repetidos, días con citas futuras y el email (A4).
3. Desactivación: usar queries `exists...StatusAndDateGreaterThanEqual` (A5).
4. Citas: decidir la regla de citas pendientes por paciente, cambiar a `@FutureOrPresent`, validar `start < end` y que la hora no haya pasado si la fecha es hoy, y validar que el paciente esté activo (A2, M2).
5. Paciente: impedir perfiles duplicados y decidir entre registro en un paso o en dos (A3).
6. Hospitalización: tener en cuenta la capacidad de la sala, impedir el doble alta, validar los estados y decidir qué hacer con `costPerDay` (A6).
7. Insumos: `@Positive` en `quantityUsed`, filtro por hospitalización, validar `active` y unificar la regla de stock mínimo (A7, M11).
8. Recepcionistas: verificar el rol, la unicidad del email y agregar `@PathVariable` (A9).

**Pruebas:** un test unitario por regla, incluyendo los casos negativos.

### Fase 5 — Errores y validación (1–2 días)
**Hallazgos:** A10, M1, M3, B10.
1. `GlobalExceptionHandler` con `ProblemDetail`:
   - 400: validación, JSON ilegible, tipo incorrecto.
   - 401/403: autenticación y acceso.
   - 404: recurso no encontrado.
   - 409: conflicto e integridad.
   - 413: tamaño de archivo.
   - 500: mensaje genérico y log del error.
2. Reemplazar las `IllegalArgumentException` de negocio por excepciones propias (A10).
3. Corregir las validaciones de los DTOs y crear los enums `BloodType` y `Gender` (M1).
4. Devolver listas y páginas vacías en vez de 404 (M3).

### Fase 6 — Completar la API (4–6 días)
**Hallazgos:** A11, A12, M5, M6, M7, M8, M9, B4, B8.
1. Agregar `id` a todos los DTOs de respuesta, nombres completos y los campos faltantes (A11, B8).
2. Unificar las rutas REST (B4). Hacerlo en el mismo PR que actualiza `SecurityConfig` y avisar al frontend.
3. Endpoints de catálogo para pacientes: doctores activos por especialidad y sus horarios (A12).
4. Endpoints "me" del doctor y funciones de recepcionista según la matriz (A12).
5. CRUD faltante: especialidades, salas, insumos (más el reporte de stock bajo), horarios, `GET` de hospitalización por id y listado, y reprogramación de citas (M9).
6. Filtro de horarios: `doctorId`, chequeo de blancos y un solo join (M5).
7. Archivos: corregir la URL, guardar el tamaño, usar `LONGBLOB`, validar tipo y tamaño, y agregar `patient_id`/`uploaded_by` (M6).
8. Rediseñar el historial clínico como entradas ligadas a una cita y un doctor (M8), y generar el PDF con contenido real (M7).

### Fase 7 — Limpieza y calidad (1–2 días)
**Hallazgos:** B2, B3, B5, B6, B7, M13 (resto).
1. Renombrados y mensajes (B2). El cambio `accesToken` → `accessToken` se coordina con el frontend.
2. Eliminar el código muerto (B3) y cambiar el id de especialidad a `Long` (B5).
3. Crear el `DataSeeder` (B6), arreglar el N+1 de `doctorCount` y limpiar los tokens expirados (B7).
4. Agregar springdoc-openapi para documentar la API, o borrar sus propiedades.
5. Subir la cobertura y restaurar el umbral de JaCoCo (M13).

---

## 7. Checklist de verificación final

- [ ] `./mvnw test` (unitarias) en verde sin Docker.
- [ ] `./mvnw verify` (unitarias + integración con Testcontainers) en verde con Docker, sin MySQL instalado localmente.
- [ ] Las migraciones de Flyway se ejecutan y validan contra el MySQL del contenedor en cada `verify`.
- [ ] `docker compose up -d` levanta la base de datos de desarrollo y la aplicación arranca contra ella.
- [ ] Pruebas de seguridad por rol cubren el 100 % de los endpoints.
- [ ] Flujo completo probado de punta a punta:
  - [ ] Registrar paciente → completar perfil.
  - [ ] Listar doctores → agendar cita.
  - [ ] El doctor completa la cita → crea una entrada del historial.
  - [ ] Hospitalizar → consumir insumos → dar de alta.
  - [ ] Generar el PDF del expediente.
- [ ] Ningún endpoint responde 500 ante entradas inválidas.
- [ ] Sin secretos en el repo y con el secreto JWT rotado.
- [ ] El esquema lo maneja Flyway y `ddl-auto=validate`.
