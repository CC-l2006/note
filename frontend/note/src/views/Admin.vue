<template>
  <!-- 管理后台的整体外壳：只负责顶栏、侧边导航与子路由出口，具体管理功能由 /admin/* 子页面实现 -->
  <div class="admin">
    <!-- 后台自己的顶栏：与前台区分，但沿用同一套纸墨语言 -->
    <header class="admin__top">
      <div class="admin__brand">
        <RouterLink to="/notes" class="admin__back" title="返回笔记">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 6l-6 6 6 6" />
          </svg>
        </RouterLink>
        <span class="admin__title">管理后台</span>
      </div>

      <div class="admin__who">
        <span class="admin__user">{{ userStore.displayName }}</span>
        <AppButton variant="quiet" size="sm" @click="onLogout">退出</AppButton>
      </div>
    </header>

    <div class="admin__body">
      <!-- 后台功能导航：每一项对应一个 /admin/* 子路由 -->
      <nav class="admin__nav" aria-label="后台导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="admin__nav-link">
          {{ item.label }}
        </RouterLink>
      </nav>

      <!-- 子路由出口：用户管理、笔记巡检、AI 参数等页面都在这里渲染 -->
      <main id="main-content" class="admin__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
/**
 * Admin.vue —— 管理后台外壳页
 * 职责：搭出后台顶栏与侧边导航，渲染 /admin/* 子路由，并提供退出登录入口。
 * 本页不承载业务逻辑，各管理功能的数据请求都在对应子页面中完成。
 */
import { RouterLink, RouterView, useRouter } from 'vue-router'
import AppButton from '@/components/AppButton.vue'
import { useUserStore } from '@/stores/user.js'

/* ---------- 状态：路由实例与当前登录用户 ---------- */
const router = useRouter()
const userStore = useUserStore()

/* ---------- 静态配置：后台导航表 ---------- */

// 需求 4.6 / 4.7 对应的 6 个后台页面
const navItems = [
  { to: '/admin/users', label: '用户管理' },
  { to: '/admin/notes', label: '笔记巡检' },
  { to: '/admin/ai-config', label: 'AI 参数' },
  { to: '/admin/announcements', label: '公告管理' },
  { to: '/admin/logs', label: '操作日志' },
  { to: '/admin/stats', label: '调用统计' }
]

/* ---------- 方法 ---------- */
// 退出登录：先清空本地登录态，再回到登录页；失败提示由 user store 统一处理
async function onLogout() {
  await userStore.logout()
  router.replace('/login')
}
</script>

<style scoped>
/* ---------- 整体布局：整页纵向铺满 ---------- */
.admin {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--paper);
}

/* ---------- 顶栏：品牌区与用户区左右分布 ---------- */
.admin__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-4);
  padding: var(--sp-3) var(--sp-5);
  border-bottom: 1px solid var(--rule);
  background: var(--paper-deep);
}

.admin__brand {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.admin__back {
  display: inline-flex;
  align-items: center;
  color: var(--ink-3);
}

.admin__back:hover {
  color: var(--ink);
}

.admin__title {
  font-family: var(--font-serif);
  font-size: var(--fs-lg);
  font-weight: 600;
}

.admin__who {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  font-size: var(--fs-sm);
  color: var(--ink-3);
}

/* ---------- 主体：左侧固定宽导航 + 右侧自适应内容 ---------- */
.admin__body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* 侧边导航：纵向排列的链接列表 */
.admin__nav {
  flex: none;
  width: 176px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--sp-4) var(--sp-3);
  border-right: 1px solid var(--rule);
}

.admin__nav-link {
  padding: 8px var(--sp-3);
  border-radius: var(--r-sm);
  color: var(--ink-2);
  font-size: var(--fs-sm);
  text-decoration: none;
}

.admin__nav-link:hover {
  background: var(--paper-deep);
  color: var(--ink);
  text-decoration: none;
}

.admin__nav-link.router-link-active {
  background: var(--accent-soft);
  color: var(--accent-ink);
  font-weight: 500;
}

/* 内容区：子页面在这里渲染 */
.admin__content {
  flex: 1;
  min-width: 0;
  padding: var(--sp-5) var(--sp-6);
  outline: none;
}

/* ---------- 窄屏适配：导航从侧边切到顶部横向滚动 ---------- */
@media (max-width: 820px) {
  .admin__body {
    flex-direction: column;
  }

  .admin__nav {
    width: 100%;
    flex-direction: row;
    overflow-x: auto;
    padding: var(--sp-2) var(--sp-3);
    border-right: 0;
    border-bottom: 1px solid var(--rule);
  }

  .admin__nav-link {
    white-space: nowrap;
  }

  .admin__content {
    padding: var(--sp-4);
  }
}
</style>
