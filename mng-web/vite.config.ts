import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    base: '/skada/mng-web/',
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8811',
          changeOrigin: true,
        },
        '/skada/mng-web/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8811',
          changeOrigin: true,
        },
      },
    },
  }
})
