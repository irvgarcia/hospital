import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// En desarrollo, /api y /auth se envian al backend (Spring Boot en el puerto 8080).
export default defineConfig(({ mode }) => {
  const backend = loadEnv(mode, '.', '').VITE_BACKEND_URL || 'http://localhost:8080'
  return {
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': backend,
      '/auth': backend,
    },
  },
  }
})
