/* =============================================================================
   路由实例

   只负责装配：历史模式、路由表、滚动行为。
   具体路由定义在 routes.js，拦截逻辑在 guards.js。
   ============================================================================= */

import { createRouter, createWebHistory } from 'vue-router'
import { constantRoutes, asyncRoutes, notFoundRoute } from './routes.js'

const router = createRouter({
  /* ---------------- 历史模式 ----------------
     用真实的 URL（无 #），因此服务端必须做 SPA 回退。 */
  // HTML5 历史模式：nginx 那边需要有 SPA 回退（try_files $uri $uri/ /index.html）
  history: createWebHistory(import.meta.env.BASE_URL),

  /* ---------------- 路由表 ----------------
     顺序即匹配优先级：公开路由 → 需要权限的业务路由 → 404 兜底。 */
  routes: [
    ...constantRoutes,
    ...asyncRoutes,
    notFoundRoute // 必须最后
  ],

  /**
   * 滚动行为：
   * 后退/前进还原原位置；笔记详情之间切换保持不动；其余情况回到页首。
   */
  scrollBehavior(to, from, savedPosition) {
    /* 浏览器前进后退：优先还原历史滚动位置 */
    if (savedPosition) return savedPosition
    // 笔记列表换页时不希望整页滚回顶部，交给页面自己处理
    if (to.name === from.name && to.name === 'note-detail') return false
    /* 普通跳转：回到顶部 */
    return { top: 0 }
  }
})

export default router
