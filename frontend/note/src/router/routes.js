import MainLayout from '@/layouts/MainLayout.vue'

/**
 * 路由表按《需求文档》的 7 个功能模块组织：
 *   1 登录与权限  → /login /register /forgot-password
 *   2 用户信息    → /profile
 *   3 笔记核心    → /notes /notes/:id /notes/trash
 *   5 标签分类    → /tags
 *   4 AI 总结     → 不是独立页面，挂在笔记编辑区的右侧留白（页边批注）
 *   6 系统后台    → /admin/*
 *   7 日志与统计  → /admin/logs /admin/stats
 *
 * 分两组导出：
 *   constantRoutes 任何人可访问（含登录页），
 *   asyncRoutes    需要 admin 角色，守卫里按 meta.roles 拦截。
 *
 * 注意：`asyncRoutes` 必须排在 404 兜底之前注册，否则会被 catch-all 抢先匹配。
 */

export const constantRoutes = [
  /* ---- 认证：登录 / 注册 / 找回密码，guestOnly 表示已登录会被弹回首页 ---- */
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', guestOnly: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', guestOnly: true }
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('@/views/auth/ForgotPassword.vue'),
    meta: { title: '找回密码', guestOnly: true }
  },

  /* ---- 主框架：带侧栏 + 顶栏，子路由都渲染在 MainLayout 的 RouterView 里 ---- */
  {
    path: '/',
    component: MainLayout,
    redirect: '/notes',
    meta: { requiresAuth: true },
    children: [
      /* 笔记列表：左中右三栏里的「中栏」 */
      {
        path: 'notes',
        name: 'notes',
        component: () => import('@/views/notes/Notes.vue'),
        meta: { title: '我的笔记', requiresAuth: true }
      },
      /* 回收站：静态路径段放在 :id 之前更直观 */
      {
        // 放在 :id 之前更直观；vue-router 4 本身也会按静态段优先匹配
        path: 'notes/trash',
        name: 'trash',
        component: () => import('@/views/notes/Trash.vue'),
        meta: { title: '回收站', requiresAuth: true }
      },
      /* 笔记详情：复用 Notes.vue，靠 id 参数决定打开哪一篇 */
      {
        path: 'notes/:id',
        name: 'note-detail',
        component: () => import('@/views/notes/Notes.vue'),
        meta: { title: '笔记', requiresAuth: true }
      },
      /* 标签管理 */
      {
        path: 'tags',
        name: 'tags',
        component: () => import('@/views/Tags.vue'),
        meta: { title: '标签', requiresAuth: true }
      },
      /* 系统公告：普通用户只读 */
      {
        path: 'announcements',
        name: 'announcements',
        component: () => import('@/views/Announcements.vue'),
        meta: { title: '系统公告', requiresAuth: true }
      },
      /* 个人资料：昵称、头像等公开信息 */
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人资料', requiresAuth: true }
      },
      /* 账号安全：换绑邮箱、改密码等敏感操作单独一页 */
      {
        // 账号与安全：更换绑定邮箱（两步验证）、修改密码（邮箱验证码）
        // 单独一页，从侧栏左下角的「设置」进入
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '设置', requiresAuth: true }
      }
    ]
  }
]

/* =============================================================================
   需要管理员权限的路由
   守卫按 meta.roles 判断，普通用户访问会被弹回首页。
   ============================================================================= */
export const asyncRoutes = [
  /* 后台外壳：左侧后台菜单，子路由渲染在 Admin.vue 里，默认进用户管理 */
  {
    path: '/admin',
    component: () => import('@/views/Admin.vue'),
    redirect: '/admin/users',
    meta: { title: '管理后台', requiresAuth: true, roles: ['admin'] },
    children: [
      /* 用户管理：启停账号、调整 AI 额度 */
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/UserList.vue'),
        meta: { title: '用户管理', requiresAuth: true, roles: ['admin'] }
      },
      /* 笔记巡检：查看全站笔记并标记违规内容 */
      {
        path: 'notes',
        name: 'admin-notes',
        component: () => import('@/views/admin/NoteInspect.vue'),
        meta: { title: '笔记巡检', requiresAuth: true, roles: ['admin'] }
      },
      /* AI 参数配置：模型名、温度、调用上限等 */
      {
        path: 'ai-config',
        name: 'admin-ai-config',
        component: () => import('@/views/admin/AiConfig.vue'),
        meta: { title: 'AI 参数配置', requiresAuth: true, roles: ['admin'] }
      },
      /* 公告管理：新增、编辑、发布与删除 */
      {
        path: 'announcements',
        name: 'admin-announcements',
        component: () => import('@/views/admin/AnnouncementAdmin.vue'),
        meta: { title: '公告管理', requiresAuth: true, roles: ['admin'] }
      },
      /* 操作日志：按条件检索后台与用户的关键操作 */
      {
        path: 'logs',
        name: 'admin-logs',
        component: () => import('@/views/admin/Logs.vue'),
        meta: { title: '操作日志', requiresAuth: true, roles: ['admin'] }
      },
      /* AI 调用统计：按时间维度的用量图表 */
      {
        path: 'stats',
        name: 'admin-stats',
        component: () => import('@/views/admin/Stats.vue'),
        meta: { title: 'AI 调用统计', requiresAuth: true, roles: ['admin'] }
      }
    ]
  }
]

/* ---- 404 兜底：catch-all 会吞掉所有未匹配路径，必须排在最后 ---- */
/** ⚠️ 必须最后注册 */
export const notFoundRoute = {
  path: '/:pathMatch(.*)*',
  name: 'not-found',
  component: () => import('@/views/NotFound.vue'),
  meta: { title: '页面不存在' }
}
