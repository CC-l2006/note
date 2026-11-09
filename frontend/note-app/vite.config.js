import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

// 这版 uni 插件在 ESM 下 default 可能再包一层，兼容两种导出形态。
const uni = uniModule.default || uniModule

// H5 开发时不跨域：页面请求 /api，Vite 代理到后端 8084。
// 小程序没有代理，部署时用 VITE_API_BASE_URL 填完整后端地址。
export default defineConfig({
  plugins: [uni()],
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8084',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
