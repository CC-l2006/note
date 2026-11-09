/* =============================================================================
   应用入口

   负责三件事：创建 Vue 应用、注册全局插件（Pinia / Router）、挂载根组件。
   真正的业务逻辑都在 views / stores / api 里，这里只做装配。
   ============================================================================= */

/* ---------------- 框架与根组件 ---------------- */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

/* ---------------- 路由与全局状态 ---------------- */
import router from './router/index.js'
import { useToastStore } from '@/stores/toast.js'

/* ---------------- 全局设计令牌（必须在组件样式之前生效） ---------------- */
import './styles/tokens.css'

// guards 必须在 router 之后引入：它内部会 import router 并注册 beforeEach
import './router/guards.js'

/* ---------------- 创建应用实例 ---------------- */
const app = createApp(App)

/* ---------------- 注册插件 ----------------
   顺序有讲究：先 Pinia 再 Router，因为路由守卫里要用 store。 */
// pinia 必须在 router 之前注册：
// guards.js 里的路由守卫会调用 useUserStore()
app.use(createPinia())
app.use(router)

/* ---------------- 全局错误兜底 ----------------
   覆盖组件渲染、生命周期、事件回调里抛出的未捕获异常。 */
// 统一兜底：任何未捕获的异常都给用户一个明确提示，而不是白屏
app.config.errorHandler = (err) => {
  /* 先打到控制台，方便开发期定位堆栈 */
  console.error('[app]', err)
  /* 再给用户一个可见的提示；Toast 本身异常时静默，避免错误处理再抛错 */
  try {
    useToastStore().error(err?.message || '页面出错了，请刷新重试')
  } catch {
    /* pinia 还没就绪时忽略 */
  }
}

/* ---------------- 挂载到 index.html 的 #app 节点 ---------------- */
app.mount('#app')
