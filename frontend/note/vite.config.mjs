import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 单体后端只有一个端口 8084。
 * 前端请求 /api/**，代理去掉 /api 前缀；/uploads 原样转发。
 */
const API_TARGET = 'http://127.0.0.1:8084'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    // 端口分配约定：main=8000 / train=8001 / note=8002
    port: 8002,
    proxy: {
      '/api': {
        target: API_TARGET,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 头像等上传文件：公开路径，不去 /api 前缀
      '/uploads': {
        target: API_TARGET,
        changeOrigin: true
      }
    }
  }
})
