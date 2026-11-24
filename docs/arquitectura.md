# Arquitectura del sistema Hospital

Diagramas para entender cómo está construido el sistema: la base de datos, el backend, la seguridad y el frontend.
Están escritos en [Mermaid](https://mermaid.js.org/), así que GitHub e IntelliJ los muestran como imágenes directamente desde este archivo.

**Fuente de verdad:** el esquema sale de las migraciones de Flyway (`src/main/resources/db/migration`) y las reglas de acceso de `SecurityConfig`. Si cambian, hay que actualizar este documento.

## Contenido

1. [Vista general](#1-vista-general)
2. [Base de datos](#2-base-de-datos)
   - [Mapa de relaciones](#21-mapa-de-relaciones)
   - [Usuarios y seguridad](#22-usuarios-y-seguridad)
   - [Doctores, pacientes, citas e historial](#23-doctores-pacientes-citas-e-historial)
   - [Hospitalización, inventario y archivos](#24-hospitalización-inventario-y-archivos)
   - [Valores de los enums](#25-valores-de-los-enums)
   - [Restricciones e integridad](#26-restricciones-e-integridad)
   - [Migraciones](#27-migraciones-de-flyway)
3. [Backend](#3-backend)
   - [Capas](#31-capas)
   - [Paquetes](#32-paquetes)
   - [Controller → Service → Repository](#33-controller--service--repository)
   - [Recorrido de una petición](#34-recorrido-de-una-petición)
4. [Seguridad](#4-seguridad)
   - [Login y uso del access token](#41-login-y-uso-del-access-token)
   - [Renovación y logout](#42-renovación-del-token-y-logout)
   - [Permisos por rol](#43-permisos-por-rol)
5. [Ciclos de vida](#5-ciclos-de-vida)
6. [Frontend](#6-frontend)

---

## 1. Vista general

```mermaid
flowchart LR
    U(["Usuario<br/>paciente · doctor · recepción · admin"])

    subgraph FE["Frontend · React 19 + Vite"]
        SPA["SPA por rol<br/>TanStack Query"]
    end

    subgraph BE["Backend · Spring Boot 3.4 · Java 21"]
        API["API REST<br/>/api/v1 · /auth"]
        SW["Swagger UI<br/>/swagger-ui.html"]
        FW["Flyway<br/>migraciones"]
    end

    DB[("MySQL 8<br/>hospitalbd")]

    U -->|navegador| SPA
    SPA -->|"JSON + JWT<br/>Authorization: Bearer"| API
    API -->|JPA / Hibernate| DB
    FW -->|crea y actualiza el esquema<br/>al arrancar| DB
    U -.->|documentación| SW
```

| Pieza | Tecnología | Dónde |
|---|---|---|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS 4, TanStack Query, React Router | `frontend/` |
| Backend | Spring Boot 3.4 (Web, Security, Data JPA, Validation), JWT (jjwt), MapStruct, Lombok, OpenPDF | `src/main/java` |
| Base de datos | MySQL 8, esquema manejado por Flyway (`ddl-auto=validate`) | `docker-compose.yml` (desarrollo) |
| Documentación de la API | springdoc / Swagger | `/swagger-ui.html` |
| Pruebas | JUnit 5, Mockito, Testcontainers (MySQL real), JaCoCo | `src/test` |

---

## 2. Base de datos

### 2.1 Mapa de relaciones

Todas las tablas y cómo se conectan, sin columnas. Las secciones siguientes muestran el detalle por área.

```mermaid
erDiagram
    users ||--o{ user_roles : ""
    roles ||--o{ user_roles : ""
    roles ||--o{ role_permissions : ""
    permissions ||--o{ role_permissions : ""
    users ||--o{ auth_tokens : "sesiones"

    users ||--o| patients : "perfil de paciente"
    users ||--o| doctors : "perfil de doctor"
    specialties ||--o{ doctors : "agrupa"
    doctors ||--o{ doctor_schedules : "horario semanal"

    patients ||--o{ appointments : "agenda"
    doctors ||--o{ appointments : "atiende"
    patients ||--o{ medical_records : "historial"
    doctors |o--o{ medical_records : "escribe"
    appointments |o--o| medical_records : "origina"

    patients ||--o{ hospitalizations : "ingresa"
    doctors ||--o{ hospitalizations : "responsable"
    rooms ||--o{ hospitalizations : "aloja"
    hospitalizations ||--o{ supply_usages : "consume"
    medical_supplies ||--o{ supply_usages : "se usa en"
    medical_supplies ||--o{ supply_movements : "entradas y salidas"
    users ||--o{ supply_movements : "registra"

    patients |o--o{ files : "archivos"
    users |o--o{ files : "sube"
```

**Cómo leer las relaciones:** `||` = exactamente uno · `|o` = cero o uno · `o{` = cero o muchos.
Por ejemplo, `users ||--o| patients` significa que un paciente pertenece a exactamente un usuario, y que un usuario tiene como máximo un perfil de paciente.

**Idea central del modelo:** `users` guarda los datos de acceso (email, contraseña, nombre, activo) y los roles. Paciente y doctor son **perfiles** en tablas propias ligados a un usuario. El recepcionista y el administrador no tienen perfil: son solo un usuario con ese rol.

### 2.2 Usuarios y seguridad

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR email UK "usuario de login y subject del JWT"
        VARCHAR password "hash BCrypt"
        VARCHAR first_name
        VARCHAR last_name
        BOOLEAN active "false = no puede iniciar sesión"
    }
    roles {
        BIGINT id PK
        ENUM role_enum "ADMIN | DOCTOR | PATIENT | RECEPTIONIST"
    }
    permissions {
        BIGINT id PK
        VARCHAR name UK "dato histórico: la autorización es por rol"
    }
    user_roles {
        BIGINT user_id PK, FK
        BIGINT role_id PK, FK
    }
    role_permissions {
        BIGINT role_id PK, FK
        BIGINT permission_id PK, FK
    }
    auth_tokens {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR token_hash UK "SHA-256 del JWT, nunca el token"
        ENUM token_type "ACCESS | REFRESH"
        BOOLEAN revoked
        BOOLEAN expired
        DATETIME expires_at "TokenCleanupTask borra los vencidos"
    }

    users ||--o{ user_roles : ""
    roles ||--o{ user_roles : ""
    roles ||--o{ role_permissions : ""
    permissions ||--o{ role_permissions : ""
    users ||--o{ auth_tokens : ""
```

- `user_roles` y `role_permissions` son tablas intermedias de relaciones muchos a muchos.
- `auth_tokens` permite revocar tokens: un JWT con firma válida solo se acepta si su hash está aquí y no está revocado (ver [Seguridad](#4-seguridad)).

### 2.3 Doctores, pacientes, citas e historial

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR email UK
    }
    specialties {
        BIGINT id PK
        VARCHAR name UK
        VARCHAR description
        DECIMAL cost "precio de la consulta"
        BOOLEAN active
    }
    doctors {
        BIGINT id PK
        BIGINT user_id FK, UK
        BIGINT specialty_id FK
        VARCHAR phone_number
        DATE register_date
        BOOLEAN active
    }
    doctor_schedules {
        BIGINT id PK
        BIGINT doctor_id FK
        ENUM day_of_week "MONDAY ... SUNDAY"
        TIME start_time
        TIME end_time
    }
    patients {
        BIGINT id PK
        BIGINT user_id FK, UK
        VARCHAR phone_number
        VARCHAR address
        DATE date_of_birth
        ENUM gender "FEMALE | MALE | OTHER"
        VARCHAR blood_type "O+, A-, ..."
        DOUBLE height
        DOUBLE weight
        BOOLEAN smoker
        VARCHAR allergies
        VARCHAR chronic_diseases
        VARCHAR current_medications
        VARCHAR emergency_contact_name
        VARCHAR emergency_contact_relation
        VARCHAR emergency_contact
        DATE register_date
        BOOLEAN active
    }
    appointments {
        BIGINT id PK
        BIGINT patient_id FK
        BIGINT doctor_id FK
        DATE date
        TIME start_time
        TIME end_time
        ENUM status "PENDING | COMPLETED | CANCELLED"
        DECIMAL total "costo de la especialidad al agendar"
        VARCHAR reason "motivo, opcional"
    }
    medical_records {
        BIGINT id PK
        BIGINT patient_id FK
        BIGINT doctor_id FK "nulo en entradas antiguas"
        BIGINT appointment_id FK, UK "una entrada por cita"
        DATE date
        VARCHAR diagnostics
        VARCHAR treatment
        VARCHAR notes
    }

    users ||--o| doctors : ""
    users ||--o| patients : ""
    specialties ||--o{ doctors : ""
    doctors ||--o{ doctor_schedules : ""
    patients ||--o{ appointments : ""
    doctors ||--o{ appointments : ""
    patients ||--o{ medical_records : ""
    doctors |o--o{ medical_records : ""
    appointments |o--o| medical_records : ""
```

- `user_id` es **único** en `doctors` y `patients`: un usuario es como máximo un doctor o un paciente.
- Una entrada de `medical_records` solo se crea a partir de una cita `COMPLETED` del doctor autenticado, y solo una por cita (`appointment_id` único).
- `specialties.doctorCount` (doctores activos) no es una columna: Hibernate lo calcula con `@Formula`.

### 2.4 Hospitalización, inventario y archivos

```mermaid
erDiagram
    users {
        BIGINT id PK
    }
    patients {
        BIGINT id PK
    }
    doctors {
        BIGINT id PK
    }
    rooms {
        BIGINT id PK
        VARCHAR name UK
        INT capacity "número de camas"
        BOOLEAN available "true = quedan camas libres"
    }
    hospitalizations {
        BIGINT id PK
        BIGINT patient_id FK
        BIGINT doctor_id FK "doctor responsable"
        BIGINT room_id FK
        DATE admission_date
        DATE discharge_date "nulo mientras sigue internado"
        VARCHAR reason
        BOOLEAN active "true = sigue internado"
        DECIMAL cost_per_day
        DECIMAL total_cost "se calcula al dar de alta"
    }
    medical_supplies {
        BIGINT id PK
        VARCHAR name UK
        INT quantity "solo cambia con movimientos o consumos"
        INT minimum_stock "umbral de alerta, no un límite"
        ENUM unit "BOX | MILLIGRAM | MILLILITER | UNIT"
        BOOLEAN active
        BIGINT version "bloqueo optimista (@Version)"
    }
    supply_movements {
        BIGINT id PK
        BIGINT supply_id FK
        BIGINT performed_by FK "usuario que lo registró"
        ENUM type "IN | OUT"
        INT quantity_changed
        DATETIME timestamp
    }
    supply_usages {
        BIGINT id PK
        BIGINT hospitalization_id FK
        BIGINT supply_id FK
        INT quantity_used
        DATETIME used_at
    }
    files {
        BINARY id PK "UUID"
        BIGINT patient_id FK
        BIGINT uploaded_by FK
        VARCHAR name
        VARCHAR type "PDF, PNG, JPG, WEBP o TXT"
        BIGINT size "bytes, máx. 5 MB"
        DATETIME uploaded_at
        LONGBLOB data "contenido del archivo"
    }

    patients ||--o{ hospitalizations : ""
    doctors ||--o{ hospitalizations : ""
    rooms ||--o{ hospitalizations : ""
    hospitalizations ||--o{ supply_usages : ""
    medical_supplies ||--o{ supply_usages : ""
    medical_supplies ||--o{ supply_movements : ""
    users ||--o{ supply_movements : ""
    patients |o--o{ files : ""
    users |o--o{ files : ""
```

- Una sala admite tantos pacientes activos como su `capacity`. `rooms.occupiedBeds` se calcula con `@Formula` (hospitalizaciones activas en la sala).
- `total_cost` = `cost_per_day` × días internado (mínimo 1).
- El stock puede bajar hasta 0, nunca por debajo. Por debajo de `minimum_stock` el insumo aparece en el reporte `GET /medical-supplies/low-stock`.
- Los archivos se guardan dentro de la base (`LONGBLOB`). El listado lee solo los metadatos, sin cargar `data`.

### 2.5 Valores de los enums

Todos se guardan como **texto** (`@Enumerated(EnumType.STRING)`), no como número.

| Columna | Valores |
|---|---|
| `roles.role_enum` | `ADMIN`, `DOCTOR`, `PATIENT`, `RECEPTIONIST` |
| `auth_tokens.token_type` | `ACCESS`, `REFRESH` |
| `appointments.status` | `PENDING`, `COMPLETED`, `CANCELLED` |
| `doctor_schedules.day_of_week` | `MONDAY` … `SUNDAY` |
| `patients.gender` | `FEMALE`, `MALE`, `OTHER` |
| `patients.blood_type` | `A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-` (texto, con `BloodTypeConverter`) |
| `medical_supplies.unit` | `BOX`, `MILLIGRAM`, `MILLILITER`, `UNIT` |
| `supply_movements.type` | `IN`, `OUT` |

### 2.6 Restricciones e integridad

| Regla | Cómo se garantiza |
|---|---|
| Email, nombre de especialidad, sala e insumo únicos | Índices `UNIQUE` |
| Un usuario tiene como máximo un perfil de paciente o de doctor | `UNIQUE (user_id)` en `patients` y `doctors` |
| Un doctor no tiene dos citas que empiecen a la misma hora | Índice único funcional `uk_appointments_doctor_slot (doctor_id, date, start_time)` que **ignora las canceladas**, así un horario cancelado se puede volver a reservar |
| Las citas no se solapan (para el doctor ni para el paciente) | Validación en `AppointmentService`, con bloqueo pesimista de la fila del doctor al agendar o reprogramar |
| Una entrada de historial por cita | `UNIQUE (appointment_id)` en `medical_records` |
| Dos usuarios no descuentan el mismo stock a la vez | `@Version` en `medical_supplies` → 409 si hubo un cambio concurrente |
| No se borran registros clínicos | Pacientes, doctores, especialidades e insumos se **desactivan** (`active = false`) en lugar de borrarse |

### 2.7 Migraciones de Flyway

Flyway aplica estos scripts en orden al arrancar la aplicación y registra cuáles ya corrió en la tabla `flyway_schema_history`. Hibernate solo **valida** que las entidades coincidan con el esquema (`ddl-auto=validate`), nunca lo modifica.

| Versión | Archivo | Qué hace |
|---|---|---|
| V1 | `V1__baseline.sql` | Esquema inicial completo |
| V2 | `V2__enums_como_texto.sql` | `appointments.status` y `doctor_schedules.day_of_week` pasan de número a texto |
| V3 | `V3__concurrencia_e_integridad.sql` | `version` en insumos e índice único de citas por doctor y horario |
| V4 | `V4__reglas_negocio_y_api.sql` | Motivo de cita, costos de hospitalización, `medical_records` con doctor y cita, `unit`, `gender` como enum, archivos ligados a paciente, `expires_at` en tokens |
| V3.1, V4.1 | `db/dev-data/*.sql` | Datos de prueba. **Solo en el perfil `local`** |

---

## 3. Backend

### 3.1 Capas

```mermaid
flowchart TB
    REQ(["Petición HTTP"]) --> SEC

    subgraph SEC["Seguridad · config/"]
        direction LR
        JF["JwtAuthenticationFilter<br/>valida el access token"] --> SC["SecurityConfig<br/>reglas por rol"]
    end

    SEC --> CTRL

    subgraph CTRL["Controladores · controllers/ y auth/controller/"]
        C["@RestController<br/>rutas, @Valid sobre los DTOs, códigos HTTP"]
    end

    CTRL -->|"DTO de entrada"| SVC

    subgraph SVC["Servicios · services/"]
        direction LR
        I["Interfaz<br/>IAppointmentService"] -.-> IMPL["Implementación services/impl/<br/>reglas de negocio · @Transactional<br/>propiedad de los recursos"]
        IMPL --> MAP["Mappers MapStruct<br/>entidad ↔ DTO"]
    end

    SVC --> REPO

    subgraph REPO["Repositorios · repositories/"]
        R["Spring Data JPA<br/>consultas y Specifications (filtros)"]
    end

    REPO --> ENT["Entidades JPA<br/>models/entity/"]
    ENT --> DB[("MySQL")]

    EX["GlobalExceptionHandler<br/>excepción → ProblemDetail"]
    SVC -. "lanza excepciones" .-> EX
    CTRL -. "errores de validación" .-> EX
    EX -.-> RES(["Respuesta de error<br/>application/problem+json"])
```

| Capa | Responsabilidad | No debe |
|---|---|---|
| **Controller** | Recibir la petición, validar el formato del DTO (`@Valid`), obtener el usuario autenticado y devolver el código HTTP correcto | Tener reglas de negocio ni acceder a repositorios |
| **Service** | Reglas de negocio, transacciones, verificar que el usuario sea dueño del recurso, convertir con los mappers | Saber de HTTP |
| **Repository** | Leer y escribir en la base (Spring Data JPA) | Tener lógica |
| **Entity** | Representar una tabla | Exponerse en la API: siempre se responde con DTOs |
| **DTO** | Forma de los datos que entran y salen de la API | — |

**Errores:** los servicios lanzan excepciones propias (`EntityNotFoundException` → 404, `InvalidDataException` → 400, `ConflictException` → 409, `AccessDeniedException` → 403, `@Version` en conflicto → 409, cualquier otro error → 500 sin detalles internos). `GlobalExceptionHandler` las convierte en un JSON `ProblemDetail`:

```json
{
  "title": "Datos inválidos",
  "status": 400,
  "detail": "La cita está fuera del horario del doctor",
  "instance": "/api/v1/appointments",
  "errors": [{ "field": "date", "message": "..." }]
}
```

### 3.2 Paquetes

```
com.hospital.backendHospital
├── auth/
│   ├── controller/      AuthController (/auth: register, login, refresh-token)
│   ├── service/         AuthService, JwtService, TokenService, TokenCleanupTask
│   └── repository/      Token (entidad de auth_tokens) y TokenRepository
├── config/              SecurityConfig, JwtAuthenticationFilter, SecurityErrorHandler,
│                        AppConfig, OpenApiConfig, DataSeeder (roles y admin inicial)
├── controllers/         Un controlador REST por recurso (/api/v1/...)
├── services/            Interfaces I*Service
│   ├── impl/            Implementaciones (reglas de negocio)
│   └── validation/      ScheduleValidator
├── repositories/        Repositorios Spring Data JPA
├── mappers/             Mappers MapStruct (entidad ↔ DTO)
├── models/
│   ├── entity/          Entidades JPA y enums
│   ├── dto/             DTOs de entrada y salida, agrupados por recurso
│   └── filters/         Parámetros de búsqueda de los listados
└── exceptions/          Excepciones propias y GlobalExceptionHandler
```

### 3.3 Controller → Service → Repository

| Controlador (ruta) | Servicio | Repositorios que usa el servicio |
|---|---|---|
| `AuthController` (`/auth`) | `AuthService` | `UserRepository`, `RoleRepository`, `TokenRepository` (vía `TokenService`) |
| `AccountController` (`/account`) | `AccountService` | `UserRepository`, `PatientRepository`, `DoctorRepository` |
| `DashboardController` (`/dashboard`) | `DashboardService` | `PatientRepository`, `DoctorRepository`, `AppointmentRepository`, `HospitalizationRepository`, `RoomRepository`, `MedicalSupplyRepository` |
| `PatientController` (`/patients`) | `PatientService`, `PDFGeneratorService` | `PatientRepository`, `UserRepository`, `RoleRepository`, `AppointmentRepository`, `HospitalizationRepository`, `MedicalRecordRepository` |
| `DoctorController` (`/doctors`) | `DoctorService` | `DoctorRepository`, `DoctorScheduleRepository`, `AppointmentRepository`, `HospitalizationRepository`, `UserRepository`, `SpecialtyRepository`, `RoleRepository` |
| `DoctorScheduleController` (`/doctor-schedules`) | `DoctorScheduleService` | `DoctorScheduleRepository`, `DoctorRepository`, `AppointmentRepository` |
| `ReceptionistController` (`/receptionists`) | `ReceptionistService` | `UserRepository`, `RoleRepository` |
| `SpecialtyController` (`/specialties`) | `SpecialtyService` | `SpecialtyRepository`, `DoctorRepository` |
| `AppointmentController` (`/appointments`) | `AppointmentService` | `AppointmentRepository`, `PatientRepository`, `DoctorRepository` |
| `MedicalRecordController` (`/medical-records`) | `MedicalRecordService` | `MedicalRecordRepository`, `PatientRepository`, `AppointmentRepository` |
| `HospitalizationController` (`/hospitalizations`) | `HospitalizationService`, `SupplyUsageService` | `HospitalizationRepository`, `PatientRepository`, `RoomRepository`, `DoctorRepository` |
| `RoomController` (`/rooms`) | `RoomService` | `RoomRepository`, `HospitalizationRepository` |
| `MedicalSupplyController` (`/medical-supplies`) | `MedicalSupplyService` | `MedicalSupplyRepository` |
| `SupplyMovementController` (`/supply-movements`) | `SupplyMovementService` | `SupplyMovementRepository`, `MedicalSupplyRepository` |
| `SupplyUsageController` (`/supply-usages`) | `SupplyUsageService` | `SupplyUsageRepository`, `HospitalizationRepository`, `MedicalSupplyRepository`, `SupplyMovementRepository` |
| `FileController` (`/files`) | `FileService` | `FileRepository`, `PatientRepository`, `DoctorRepository`, `AppointmentRepository`, `HospitalizationRepository` |

Casi todos los controladores usan también `AuthService.getAuthenticatedUser()` para saber quién hace la petición. La única excepción a la regla de capas es `PatientController`, que usa `PatientRepository` directamente para resolver el paciente autenticado antes de generar su PDF. Todas las rutas, salvo `/auth`, empiezan con `/api/v1`.

### 3.4 Recorrido de una petición

Ejemplo: un paciente agenda una cita.

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente
    participant FE as Frontend
    participant F as JwtAuthenticationFilter
    participant S as SecurityConfig
    participant C as AppointmentController
    participant AS as AppointmentService
    participant R as Repositorios
    participant DB as MySQL

    P->>FE: Elige doctor, fecha y horario
    FE->>F: POST /api/v1/appointments<br/>Authorization: Bearer access token
    F->>F: Valida firma, tipo ACCESS y que no esté revocado
    F->>S: Usuario autenticado con rol PATIENT
    S->>C: ¿PATIENT puede hacer POST /appointments? Sí
    C->>C: @Valid CreateAppointmentDto
    C->>AS: createAppointment(usuario, dto)
    AS->>R: Bloquea la fila del doctor (PESSIMISTIC_WRITE)
    AS->>R: Valida horario del doctor, solapamientos y una cita pendiente por doctor
    AS->>R: save(appointment) con status PENDING
    R->>DB: INSERT INTO appointments
    AS-->>C: AppointmentResponseDto (MapStruct)
    C-->>FE: 201 Created + JSON
    Note over AS,FE: Si una regla falla, el servicio lanza InvalidDataException<br/>y GlobalExceptionHandler responde 400 con ProblemDetail
```

---

## 4. Seguridad

- **Sin sesión en el servidor** (`STATELESS`): cada petición trae su token.
- **Dos tokens JWT:** el *access token* (vida corta) se manda en cada petición; el *refresh token* (vida larga) solo sirve para pedir un par nuevo en `/auth/refresh-token`.
- **Tokens revocables:** cada token emitido se guarda como hash SHA-256 en `auth_tokens`. Un token con firma válida se rechaza si fue revocado: por logout, por un login nuevo (una sesión activa por usuario), por rotación del refresh, por desactivación del usuario o por cambio de email.
- **Contraseñas** con BCrypt.
- **Autorización en dos niveles:** `SecurityConfig` decide qué **rol** puede usar cada ruta, y los servicios verifican la **propiedad** (un paciente solo ve sus citas, un doctor solo completa las suyas). Cualquier ruta sin regla queda bloqueada (`denyAll`).

### 4.1 Login y uso del access token

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant AC as AuthController
    participant AU as AuthService
    participant AM as AuthenticationManager
    participant J as JwtService
    participant T as TokenService
    participant DB as MySQL
    participant F as JwtAuthenticationFilter

    FE->>AC: POST /auth/login {email, password}
    AC->>AU: authenticate()
    AU->>AM: Verifica email y contraseña (BCrypt)
    AM-->>AU: OK · 401 si la contraseña no coincide · 403 si el usuario está desactivado
    AU->>J: Genera access token y refresh token
    AU->>T: Revoca los tokens anteriores del usuario
    AU->>T: Guarda el hash de los dos tokens nuevos
    T->>DB: INSERT INTO auth_tokens
    AC-->>FE: 200 {accessToken, refreshToken}
    Note over FE: Los guarda en localStorage

    FE->>F: GET /api/v1/... + Bearer access token
    F->>J: Firma válida, no vencido y de tipo ACCESS
    F->>T: ¿Su hash está en auth_tokens sin revocar?
    F->>F: Carga el usuario y comprueba que esté activo
    F-->>FE: Si algo falla, la petición sigue sin autenticar<br/>y la cadena de seguridad responde 401
```

### 4.2 Renovación del token y logout

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend (api/client.ts)
    participant API as Backend
    participant T as auth_tokens

    FE->>API: Petición con access token vencido
    API-->>FE: 401
    FE->>API: POST /auth/refresh-token<br/>Authorization: Bearer refresh token
    API->>T: ¿Refresh válido y sin revocar?
    API->>T: Revoca todos los tokens del usuario (rotación)
    API->>T: Guarda el par nuevo
    API-->>FE: 200 {accessToken, refreshToken}
    FE->>API: Repite la petición original con el token nuevo
    Note over FE,API: Si el refresh también falla, el frontend<br/>cierra la sesión y manda al login

    FE->>API: POST /auth/logout + Bearer access token
    API->>T: Revoca los tokens del usuario
    API-->>FE: 204 No Content
```

### 4.3 Permisos por rol

Resumen de `SecurityConfig`. ✅ = permitido · *propios* = el servicio además limita a los registros del usuario.

| Recurso | Admin | Recepción | Doctor | Paciente |
|---|:-:|:-:|:-:|:-:|
| Cuenta propia (`/account`) | ✅ | ✅ | ✅ | ✅ |
| Panel (`/dashboard`) | ✅ | ✅ | — | — |
| Recepcionistas: gestionar | ✅ | — | — | — |
| Pacientes: ver listado y detalle | ✅ | ✅ | ✅ | — |
| Pacientes: registrar en un paso / editar | ✅ | ✅ | — | — |
| Pacientes: completar y editar perfil propio | — | — | — | ✅ |
| Pacientes: activar / desactivar | ✅ | — | — | — |
| Expediente PDF | ✅ | — | ✅ | ✅ (propio) |
| Doctores, especialidades y horarios: consultar | ✅ | ✅ | ✅ | ✅ |
| Doctores, especialidades y horarios: crear y editar | ✅ | — | — | — |
| Citas: listado general | ✅ | ✅ | ✅ | — |
| Citas: "mis citas" | — | — | ✅ (propias) | ✅ (propias) |
| Citas: agendar, cancelar, reprogramar | ✅ | ✅ | — | ✅ (propias) |
| Citas: completar | — | — | ✅ (propias) | — |
| Historial: consultar de un paciente | ✅ | — | ✅ | ✅ (propio) |
| Historial: crear y editar entrada | — | — | ✅ (de su cita completada) | — |
| Hospitalizaciones: ver, crear, dar de alta | ✅ | ✅ | ✅ | ✅ (solo ver las propias) |
| Consumo de insumos | ✅ | — | ✅ | — |
| Salas: consultar | ✅ | ✅ | ✅ | — |
| Salas: crear y editar | ✅ | — | — | — |
| Inventario: consultar | ✅ | — | ✅ | — |
| Inventario: crear, editar, movimientos, stock bajo | ✅ | — | — | — |
| Archivos | ✅ | ✅ | ✅ (sus pacientes) | ✅ (propios) |

Públicas, sin token: `/auth/**`, `/swagger-ui.html`, `/v3/api-docs` y `/actuator/health`.

---

## 5. Ciclos de vida

**Cita**

```mermaid
stateDiagram-v2
    [*] --> PENDING: agendar<br/>(paciente, recepción o admin)
    PENDING --> PENDING: reprogramar
    PENDING --> COMPLETED: completar<br/>(su doctor, desde el día de la cita)
    PENDING --> CANCELLED: cancelar<br/>(hasta el día de la cita)
    COMPLETED --> [*]: el doctor puede registrar<br/>una entrada de historial
    CANCELLED --> [*]: el horario queda libre
```

**Hospitalización**

```mermaid
stateDiagram-v2
    [*] --> Activa: hospitalizar<br/>(sala con camas libres, paciente y doctor activos)
    Activa --> Activa: registrar consumo de insumos<br/>(descuenta stock)
    Activa --> Alta: dar de alta<br/>(discharge_date y total_cost)
    Alta --> [*]
```

**Paciente y doctor:** se desactivan en lugar de borrarse. La desactivación se rechaza si tienen citas `PENDING` futuras o una hospitalización activa (propia o a su cargo). Un usuario inactivo no puede iniciar sesión y sus tokens quedan revocados.

---

## 6. Frontend

```mermaid
flowchart TB
    subgraph APP["frontend/src"]
        MAIN["main.tsx<br/>QueryClient + Router + AuthProvider"]
        ROUTES["App.tsx<br/>rutas"]
        GUARDS["components/guards.tsx<br/>exige sesión y rol"]
        AUTH["auth/AuthContext.tsx<br/>usuario actual, login, logout"]

        subgraph PAGES["pages/"]
            PA["auth/<br/>login · registro · completar perfil"]
            PP["patient/<br/>agendar · mis citas · historial ·<br/>hospitalizaciones · archivos · perfil"]
            PD["doctor/<br/>agenda · perfil"]
            PS["staff/<br/>pacientes · citas · hospitalizaciones"]
            PAD["admin/<br/>doctores · recepcionistas · especialidades ·<br/>salas · inventario"]
        end

        COMP["components/<br/>formularios y piezas reutilizables"]

        subgraph APIL["api/"]
            EP["endpoints.ts<br/>una función por endpoint"]
            CL["client.ts<br/>fetch + token + refresh automático<br/>+ errores ProblemDetail"]
            TY["types.ts<br/>tipos de los DTOs"]
        end
    end

    BE[("Backend<br/>/api/v1 · /auth")]

    MAIN --> ROUTES --> GUARDS --> PAGES
    GUARDS --> AUTH
    PAGES --> COMP
    PAGES -->|"useQuery / useMutation"| EP
    AUTH --> EP
    EP --> CL
    CL -->|HTTP| BE
```

- **`api/client.ts`** es el único lugar que habla con el backend: agrega el access token, ante un 401 pide un par nuevo con el refresh token y repite la petición, y convierte los errores `ProblemDetail` en mensajes para la interfaz.
- **TanStack Query** guarda en caché las respuestas (`useQuery`) y las invalida después de crear o modificar (`useMutation`).
- **`guards.tsx`** protege las rutas: sin sesión manda al login; un paciente sin perfil completo, a completar perfil; con un rol sin permiso, a la página de inicio. Esto es solo comodidad para el usuario: la seguridad real la aplica el backend.
- En desarrollo, Vite reenvía `/api` y `/auth` al backend (`vite.config.ts`), así no hay problemas de CORS.
