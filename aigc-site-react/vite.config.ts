import path from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const comfyTarget = env.VITE_COMFY_PROXY_TARGET

  return {
    plugins: [react()],
    resolve: {
      alias: { '@': path.resolve(__dirname, 'src') },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) return
            if (id.includes('react-router')) return 'router-vendor'
            if (id.includes('/react-dom/') || id.includes('/react/')) return 'react-vendor'
            if (id.includes('@comfyorg/litegraph')) return 'graph-canvas'
            if (id.includes('/axios/')) return 'network-vendor'
            return 'vendor'
          },
        },
      },
    },
    server: {
      port: 5174,
      proxy: comfyTarget
        ? {
            '/api/comfy': {
              target: comfyTarget,
              changeOrigin: true,
              rewrite: (p) => p.replace(/^\/api\/comfy/, ''),
            },
          }
        : undefined,
    },
  }
})
