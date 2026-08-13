import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  base: './',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    outDir: '../resources/mcp-ui',
    emptyOutDir: true,
    // MCP admin dashboard bundles @rjsf and @tanstack/react-query into a single
    // chunk (622 kB raw / ~188 kB gzip). The UI is an internal admin surface and
    // ships as one page, so silence the chunk-size warning with a limit that
    // matches the actual bundle rather than forcing code-splitting.
    chunkSizeWarningLimit: 700,
  },
  server: {
    proxy: {
      '/api/mcp-admin': 'http://localhost:8080',
    },
  },
})
