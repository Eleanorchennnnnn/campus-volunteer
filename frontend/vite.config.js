import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址可通过环境变量 BACKEND 覆盖，默认本机 8081
const backend = process.env.BACKEND || 'http://localhost:8081'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 开发期代理：前端请求 /api/** 转发到后端，规避跨域
    proxy: {
      '/api': {
        target: backend,
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, '')
      }
    }
  }
})
