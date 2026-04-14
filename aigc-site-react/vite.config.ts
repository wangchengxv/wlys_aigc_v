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
