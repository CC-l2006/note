<template>
  <!-- 应用主框架：左侧固定导航栏 + 右侧路由内容区 -->
  <div class="shell">
    <!-- ============ 左侧导航栏 ============ -->
    <aside class="rail">
      <!-- 品牌区：图标 + 站点名 -->
      <div class="rail__brand">
        <span class="rail__mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="18" height="18">
            <path
              d="M6 3h9a2 2 0 0 1 2 2v16l-6.5-3.6L4 21V5a2 2 0 0 1 2-2z"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linejoin="round"
            />
          </svg>
        </span>
        <span class="rail__name">池禾笔记</span>
      </div>

      <!-- 主导航：条目由 navItems 计算属性提供，含图标、文字与未读角标 -->
      <nav class="rail__nav" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="rail__link"
          :class="{ 'is-active': isActive(item) }"
        >
          <!-- 图标以 v-html 注入，内容为内置的静态 SVG 字符串 -->
          <span class="rail__icon" aria-hidden="true" v-html="item.icon" />
          <span>{{ item.label }}</span>
          <!-- 未读角标：只有 item.badge 有值时才显示 -->
          <span v-if="item.badge" class="rail__badge">{{ item.badge }}</span>
        </RouterLink>
      </nav>

      <!-- 脚部：AI 额度、个人信息、设置入口与退出登录 -->
      <div class="rail__foot">
        <!-- AI 额度：剩余不足 3 次时数值转为警示色 -->
        <div class="rail__quota" :class="{ 'is-low': userStore.aiQuota <= 2 }">
          <span class="rail__quota-label">AI 额度</span>
          <span class="rail__quota-value">{{ userStore.aiQuota }}</span>
        </div>

        <!-- 用户信息：点击进入个人资料页，有头像用图、否则用昵称首字 -->
        <RouterLink to="/profile" class="rail__user">
          <span class="avatar" aria-hidden="true">
            <img v-if="userStore.userInfo?.avatar" class="avatar__img" :src="userStore.userInfo.avatar" alt="" />
            <template v-else>{{ initial }}</template>
          </span>
          <span class="rail__user-name">{{ userStore.displayName }}</span>
        </RouterLink>

        <!-- 设置入口：左下角、退出登录上方。账号与安全相关操作都在那一页 -->
        <RouterLink
          to="/settings"
          class="rail__link rail__settings"
          :class="{ 'is-active': route.path === '/settings' }"
        >
          <span class="rail__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9c.14.36.4.66.74.85H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
          </span>
          <span>设置</span>
        </RouterLink>

        <!-- 退出登录：清空登录态后跳回登录页 -->
        <button class="rail__logout" type="button" @click="onLogout">退出登录</button>
      </div>
    </aside>

    <!-- ============ 内容区 ============ -->
    <!-- id 供「跳到正文」等无障碍链接定位，子路由渲染在 RouterView 中 -->
    <main id="main-content" class="main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
/**
 * MainLayout —— 登录后的主框架布局
 * 负责侧栏导航的展示与高亮判断、AI 额度与用户信息的呈现、退出登录流程，
 * 以及进入应用后拉取公告数量作为「系统公告」的未读角标。
 */

import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.js'
import { useToastStore } from '@/stores/toast.js'
import { announcementApi } from '@/api/index.js'

/** 当前路由，用于导航高亮判断 */
const route = useRoute()
/** 路由器实例，退出登录后用它做页面跳转 */
const router = useRouter()
/** 用户 store：登录态、昵称、头像与 AI 额度都从这里取 */
const userStore = useUserStore()
/** 提示 store：退出登录后给一条反馈 */
const toast = useToastStore()

/** 未读公告数量，进入页面后由接口结果填充，作为导航角标 */
const unreadAnnouncements = ref(0)

/** 头像回落用的首字：取昵称第一个字符，无昵称时用问号 */
const initial = computed(() => (userStore.displayName || '？').slice(0, 1))

/** 导航条目：基础三项对所有用户可见，管理员额外追加「管理后台」 */
const navItems = computed(() => {
  const items = [
    {
      to: '/notes',
      label: '我的笔记',
      icon: '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M5 4h11l3 3v13H5z"/><path d="M8 10h8M8 14h6"/></svg>'
    },
    {
      to: '/tags',
      label: '标签',
      icon: '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h8l8 8-8 8-8-8z"/><circle cx="9" cy="9" r="1.4"/></svg>'
    },
    {
      to: '/announcements',
      label: '系统公告',
      badge: unreadAnnouncements.value || '',
      icon: '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 10v4h3l7 4V6l-7 4H4z"/><path d="M18 9a4 4 0 0 1 0 6"/></svg>'
    }
  ]

  // 仅管理员可见的入口，权限来源是用户 store 的 isAdmin
  if (userStore.isAdmin) {
    items.push({
      to: '/admin',
      label: '管理后台',
      icon: '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l7 3v6c0 4-3 7-7 9-4-2-7-5-7-9V6z"/></svg>'
    })
  }
  return items
})

/** 判断导航项是否处于选中态：父级路径下的详情页也保持高亮 */
function isActive(item) {
  if (item.to === '/notes') {
    return route.path === '/notes' || route.path.startsWith('/notes/')
  }
  return route.path === item.to || route.path.startsWith(`${item.to}/`)
}

/** 退出登录：先等 store 清理完成，再提示并跳转登录页 */
async function onLogout() {
  await userStore.logout()
  toast.info('已退出登录')
  router.replace('/login')
}

/** 挂载后拉取公告列表以统计未读数量；失败时保持 0，不影响主流程 */
onMounted(async () => {
  try {
    const { data } = await announcementApi.list()
    unreadAnnouncements.value = data.length
  } catch {
    /* 公告拿不到不影响主流程 */
  }
})
</script>

<style scoped>
/* ---------------- 整体框架 ---------------- */
.shell {
  display: flex;
  min-height: 100vh;
  background: var(--paper);
}

/* ---------------- 侧栏 ---------------- */
.rail {
  position: sticky;
  top: 0;
  flex: none;
  width: var(--rail-w);
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: var(--sp-5) var(--sp-3) var(--sp-4);
  border-right: 1px solid var(--rule);
  background: var(--paper);
}

/* 品牌区 */
.rail__brand {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: 0 var(--sp-2) var(--sp-5);
}

.rail__mark {
  color: var(--accent);
}

.rail__name {
  font-family: var(--font-serif);
  font-size: var(--fs-md);
  font-weight: 600;
  letter-spacing: 0.02em;
}

/* 导航列表 */
.rail__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rail__link {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: 8px var(--sp-2) 8px var(--sp-3);
  border-radius: var(--r-sm);
  color: var(--ink-2);
  font-size: var(--fs-sm);
  text-decoration: none;
}

.rail__link:hover {
  background: var(--paper-deep);
  color: var(--ink);
  text-decoration: none;
}

/* 选中态用一道墨线，而不是填充整块 —— 与纸的隐喻一致 */
.rail__link.is-active {
  color: var(--accent-ink);
  background: var(--accent-soft);
  font-weight: 500;
}

.rail__link.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 2px;
  border-radius: 2px;
  background: var(--accent);
}

.rail__icon {
  display: inline-flex;
  opacity: 0.8;
}

.rail__badge {
  margin-left: auto;
  min-width: 18px;
  padding: 0 5px;
  border-radius: var(--r-pill);
  background: var(--paper-sink);
  color: var(--ink-3);
  font-size: var(--fs-xs);
  text-align: center;
}

/* 脚部：贴在侧栏底部 */
.rail__foot {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
  padding-top: var(--sp-4);
  border-top: 1px solid var(--rule);
}

/* AI 额度 */
.rail__quota {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 6px var(--sp-2);
  font-size: var(--fs-xs);
  color: var(--ink-3);
}

.rail__quota-value {
  font-family: var(--font-mono);
  font-size: var(--fs-base);
  color: var(--accent-ink);
}

/* 额度告急：数值转为危险色 */
.rail__quota.is-low .rail__quota-value {
  color: var(--danger);
}

/* 用户信息 */
.rail__user {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: 6px var(--sp-2);
  border-radius: var(--r-sm);
  color: var(--ink);
  font-size: var(--fs-sm);
  text-decoration: none;
}

.rail__user:hover {
  background: var(--paper-deep);
  text-decoration: none;
}

.avatar {
  flex: none;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--accent-soft);
  color: var(--accent-ink);
  font-family: var(--font-serif);
  font-size: var(--fs-sm);
}

/* 上传过头像时铺满圆形，否则回落到昵称首字 */
.avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.rail__user-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rail__logout {
  border: 0;
  background: none;
  padding: 4px var(--sp-2);
  text-align: left;
  font-size: var(--fs-xs);
  color: var(--ink-3);
  cursor: pointer;
}

/* 设置入口在脚部，比主导航低一级：字号小一点、默认灰一些 */
.rail__settings {
  padding-top: 4px;
  padding-bottom: 4px;
  font-size: var(--fs-xs);
  color: var(--ink-2);
}

.rail__settings.is-active {
  color: var(--accent-ink);
  background: var(--accent-soft);
}

/* 设置项不需要左侧墨线，避免与主导航的选中语汇混淆 */
.rail__settings.is-active::before {
  display: none;
}

/* 悬停变红：退出登录属于破坏性操作 */
.rail__logout:hover {
  color: var(--danger);
}

/* ---------------- 内容区 ---------------- */
.main {
  flex: 1;
  min-width: 0;
  outline: none;
}

/* ---------------- 响应式 ---------------- */
/* 窄屏：侧栏转为顶部横向条，隐藏次要信息保证导航可点 */
@media (max-width: 820px) {
  .shell {
    flex-direction: column;
  }

  .rail {
    position: static;
    width: 100%;
    height: auto;
    flex-direction: row;
    align-items: center;
    gap: var(--sp-3);
    padding: var(--sp-2) var(--sp-3);
    border-right: 0;
    border-bottom: 1px solid var(--rule);
    overflow-x: auto;
  }

  .rail__brand {
    padding: 0;
  }

  .rail__nav {
    flex-direction: row;
    gap: var(--sp-1);
  }

  .rail__link {
    padding: 6px 10px;
  }

  .rail__link.is-active::before {
    display: none;
  }

  .rail__icon,
  .rail__badge,
  .rail__quota-label {
    display: none;
  }

  .rail__foot {
    margin-top: 0;
    margin-left: auto;
    flex-direction: row;
    align-items: center;
    padding-top: 0;
    border-top: 0;
  }

  .rail__quota {
    padding: 0;
    gap: 4px;
  }

  .rail__user-name {
    display: none;
  }
}
</style>
