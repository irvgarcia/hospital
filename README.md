# Hospital

Sistema de gestión hospitalaria: pacientes, doctores y horarios, citas, historial clínico, hospitalizaciones, inventario de insumos y archivos.

- **Backend:** Spring Boot 3.4 · Java 21 · MySQL 8 · Flyway · JWT (access + refresh con rotación) · springdoc.
- **Frontend:** React 19 · TypeScript · Vite · Tailwind CSS 4 · TanStack Query (carpeta [`frontend/`](frontend)).

Los diagramas de la base de datos y de la arquitectura (backend, seguridad, permisos por rol y frontend) están en [`docs/arquitectura.md`](docs/arquitectura.md).

## Puesta en marcha (desarrollo)

Requisitos: Java 21, Docker y Node 20+.

```bash
# 1. Variables locales (no se versionan)
cp .env.example .env            # completa JWT_SECRET (openssl rand -base64 64 | tr -d '\n')

# 2. Base de datos
docker compose up -d            # MySQL 8 en el puerto 3307

# 3. Backend (perfil local: aplica migraciones y carga los datos de prueba)
./mvnw spring-boot:run          # http://localhost:8080 · documentación en /swagger-ui.html

# 4. Frontend
cd frontend
npm install
npm run dev                     # http://localhost:5173 (envía /api y /auth al backend)
```

Si el backend corre en otro puerto: `SERVER_PORT=8081 ./mvnw spring-boot:run` y `VITE_BACKEND_URL=http://localhost:8081 npm run dev`.

Para recargar los datos de prueba desde cero: `docker compose down -v && docker compose up -d`.

### Usuarios de prueba

Contraseña de todos: `Hospital-dev-123`.

| Rol | Email |
|---|---|
| Administrador | `admin@demo.local` |
| Recepción | `recepcion@demo.local`, `recepcion2@demo.local` |
| Doctor | `dra.lopez@demo.local` (Cardiología), `dr.ramirez@demo.local` (Medicina General), `dra.torres@demo.local` (Pediatría), `dr.mendez@demo.local` (inactivo) |
| Paciente | `ana.garcia@demo.local`, `luis.hernandez@demo.local`, `sofia.martinez@demo.local`, `jorge.ruiz@demo.local` |

## Qué puede hacer cada rol

| Rol | En el frontend |
|---|---|
| Paciente | Registrarse y completar su perfil, agendar con horarios libres reales, reprogramar o cancelar sus citas, ver su historial y descargar su expediente en PDF, ver sus hospitalizaciones, subir y descargar sus archivos. |
| Doctor | Agenda con filtros, completar consultas y registrar/editar la entrada del historial, ver pacientes (datos, historial, archivos, PDF), hospitalizar, registrar consumo de insumos y dar de alta. |
| Recepción | Panel del día, registrar pacientes en un paso, agendar/reprogramar/cancelar citas de cualquier paciente, hospitalizaciones, ocupación de salas, catálogo de doctores. |
| Administrador | Todo lo anterior más doctores con horarios, recepcionistas, especialidades, salas, inventario (stock bajo, entradas y salidas) y activar/desactivar cuentas. |

## Pruebas

```bash
./mvnw test       # unitarias (sin Docker)
./mvnw verify     # unitarias + integración con Testcontainers (requiere Docker) + JaCoCo (mín. 75 %)

cd frontend && npm run build && npm run lint
```

## Producción

Variables del backend (perfil `prod`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `APP_CORS_ALLOWED_ORIGINS` (origen del frontend) y `SPRINGDOC_ENABLED=false` para ocultar la documentación.

Frontend: `VITE_API_URL=https://<backend> npm run build` y servir `frontend/dist` como sitio estático con *fallback* a `index.html`.
