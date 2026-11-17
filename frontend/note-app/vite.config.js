import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

// 这版 uni 插件在 ESM 下 default 可能再包一层，兼容两种导出形态。
const uni = uniModule.default || uniModule

// H5 只是调试预览：页面请求 /api，由 Vite 代理到远程 note 后端。
// 想改成本地后端时，启动前设置 VITE_PROXY_TARGET=http://127.0.0.1:8084。
const PROXY_TARGET = process.env.VITE_PROXY_TARGET || 'https://note.cipherws.icu'
const IS_LOCAL_TARGET = /127\.0\.0\.1|localhost/i.test(PROXY_TARGET)

export default defineConfig({
  plugins: [uni()],
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy: {
      '/api': {
        target: PROXY_TARGET,
        changeOrigin: true,
        // note.cipherws.icu 使用受信任证书；保留 false 只兼容本地/自签名目标。
        secure: false,
        // 远程 Nginx 自己会去掉 /api；只有本地直连 8084 才需要 Vite 去前缀。
        rewrite: IS_LOCAL_TARGET ? (path) => path.replace(/^\/api/, '') : undefined
      }
    }
  }
})
