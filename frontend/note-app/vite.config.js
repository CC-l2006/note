import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

// 这版 uni 插件在 ESM 下 default 可能再包一层，兼容两种导出形态。
const uni = uniModule.default || uniModule

// H5 只是调试预览：页面请求 /api，由 Vite 代理到远程 frp 后端。
// 想改成本地后端时，启动前设置 VITE_PROXY_TARGET=http://127.0.0.1:8084。
const PROXY_TARGET = process.env.VITE_PROXY_TARGET || 'https://frp-box.com:64456'
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
        // frp 目前使用的证书不在系统信任链，H5 调试由 Node 代理忽略证书；
        // 手机 App 走 uni.request，证书问题见 README。
        secure: false,
        // 远程 Nginx 自己会去掉 /api；只有本地直连 8084 才需要 Vite 去前缀。
        rewrite: IS_LOCAL_TARGET ? (path) => path.replace(/^\/api/, '') : undefined
      }
    }
  }
})
