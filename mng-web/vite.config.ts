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
      // 以下代理配置仅本地开发(vite dev)生效，生产环境由 nginx 负责转发
      proxy: {
        // 旧版兼容：部分接口直接走 /api 前缀
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8811',
          changeOrigin: true,
        },
        // 主流路径：前端 baseURL 为 /skada/mng-service/api/v1，
        // 转发到 mng-service 时去掉 /skada/mng-service 前缀，
        // 因为后端 controller 路径是 /api/v1/...（无此前缀）。
        // 等价于 nginx: rewrite ^/skada/mng-service/(.*)$ /$1 break;
        '/skada/mng-service': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8811',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/skada\/mng-service/, ''),
        },
      },
    },
  }
})
