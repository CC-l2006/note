import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

// uni-app Vue3 固定入口：必须导出 createApp，由 uni 运行时接管挂载。
export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  return { app }
}
