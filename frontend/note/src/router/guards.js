/* =============================================================================
   全局路由守卫

   在这里集中处理四件事：页面标题、登录拦截、角色校验、切换后的焦点归位。
   业务页面不需要各自写权限判断。
   ============================================================================= */

import router from './index.js'
import { useUserStore } from '@/stores/user.js'
import { useToastStore } from '@/stores/toast.js'

/* 站点名：浏览器标签页统一显示它 */
const APP_NAME = '池禾笔记'

// 未登录也能访问的页面
const WHITE_LIST = ['/login', '/register', '/forgot-password']

/**
 * 前置守卫：每次跳转前决定「放行 / 重定向」。
 * 返回 true 表示放行，返回路由对象表示改道。
 */
router.beforeEach(async (to) => {
  // 浏览器标签页只显示站点名，不再拼页面标题前缀
  document.title = APP_NAME

  /* store 必须在守卫内部取：模块顶层执行时 pinia 还没安装 */
  const userStore = useUserStore()
  const toast = useToastStore()

  /* ---------------- 分支 1：已登录用户回访访客页 ----------------
     登录 / 注册 / 找回密码这类页面只有未登录时才有意义。 */
  // 已登录还去登录页 → 直接回首页，避免重复登录
  if (to.meta.guestOnly && userStore.isLogin) {
    return { path: '/' }
  }

  /* ---------------- 分支 2：白名单直放 ----------------
     免登录页面不做任何拦截，直接交给路由。 */
  if (WHITE_LIST.includes(to.path) || to.meta.guestOnly) {
    return true
  }

  /* ---------------- 分支 3：未登录 ----------------
     记住原本想去的地址（redirect），登录成功后可原路返回。 */
  if (!userStore.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  /* ---------------- 分支 4：已登录但缺用户信息 ----------------
     刷新页面后 store 是空的，只有 token，需要补拉一次资料。 */
  // 已登录但刷新页面后还没有用户信息 → 补拉一次
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      /* 补拉失败说明令牌已失效：清本地状态、给提示、回登录页 */
      await userStore.logout()
      toast.warn('登录已失效，请重新登录')
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  /* ---------------- 分支 5：角色校验 ----------------
     只有路由 meta 里声明了 roles 且非空时才检查。 */
  // 角色校验（需求 4.1.4：普通用户无后台入口）
  const roles = to.meta.roles
  if (Array.isArray(roles) && roles.length) {
    const mine = userStore.userInfo?.role
    /* 当前角色不在允许列表里 → 提示并退回首页，而不是停在空白页 */
    if (!roles.includes(mine)) {
      toast.error('该页面仅管理员可访问')
      return { path: '/' }
    }
  }

  /* 以上分支都未命中 → 正常放行 */
  return true
})

/**
 * 后置守卫：跳转完成、DOM 尚未更新时执行。
 * 借用 requestAnimationFrame 等主区域渲染完再移焦点。
 */
router.afterEach((to) => {
  // 页面切换后把焦点交给主区域，键盘与读屏用户不用重新 Tab 一遍导航
  requestAnimationFrame(() => {
    const main = document.getElementById('main-content')
    /* 主区域不一定存在（如登录页采用别的骨架），存在才聚焦 */
    if (main) {
      main.setAttribute('tabindex', '-1')
      main.focus({ preventScroll: true })
    }
  })
})

/**
 * 路由级错误：懒加载 chunk 失败、组件渲染异常等。
 * 这里只记录日志，具体提示交给全局 errorHandler。
 */
router.onError((error) => {
  console.error('[router]', error)
})
