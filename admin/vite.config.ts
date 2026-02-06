import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: '/admin/',
  build: {
    outDir: '../backend/src/main/resources/admin',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/forms': 'http://localhost:8080',
      '/admin/forms': 'http://localhost:8080',
    },
  },
})
