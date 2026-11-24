import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The Java backend runs on :8090. In development, Vite forwards /api calls to it,
// so the browser sees a single origin and no CORS configuration is needed.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.LEASELENS_API_URL ?? 'http://localhost:8090', changeOrigin: true },
    },
  },
})
