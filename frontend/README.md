# Frontend del Hospital

React 19 + TypeScript + Vite + Tailwind CSS 4 + TanStack Query + React Router.

```bash
npm install
npm run dev      # http://localhost:5173; /api y /auth van al backend (VITE_BACKEND_URL, por defecto http://localhost:8080)
npm run build    # tsc + build de produccion en dist/ (VITE_API_URL = URL del backend si se sirve aparte)
npm run lint
```

Estructura:

- `src/api/`: cliente HTTP (token, refresh automatico, errores ProblemDetail), endpoints y tipos de la API.
- `src/auth/`: sesion y cuenta del usuario.
- `src/components/`: UI comun, layout con menu por rol, formularios compartidos (perfil, agendar, archivos, historial).
- `src/pages/`: `auth/`, `patient/`, `doctor/`, `staff/` (recepcion, doctor y admin) y `admin/`.

Usuarios de prueba y puesta en marcha completa: ver el README de la raiz.
