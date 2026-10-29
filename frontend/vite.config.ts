import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// In development the dashboard proxies /api to the Spring Boot app, so the browser sees one origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: { environment: 'node' },
});
