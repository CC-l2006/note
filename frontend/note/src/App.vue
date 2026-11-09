<template>
  <!-- 无障碍：键盘首次 Tab 时出现，直接把焦点送到正文主区域 -->
  <a class="skip-link" href="#main-content">跳到正文</a>

  <!-- 路由出口：所有业务页面（含 404）都在这里渲染 -->
  <RouterView />

  <!-- 全局提示层：固定在视口上方，不参与页面布局 -->
  <AppToast />
</template>

<script setup>
/* ---------------- 依赖 ----------------
   RouterView 用于渲染路由匹配到的页面组件，
   AppToast 是全局轻提示容器，挂在最外层保证任何页面都能弹出。 */
import { RouterView } from 'vue-router'
import AppToast from '@/components/AppToast.vue'
</script>

<style scoped>
/* ---------------- 跳转正文链接 ----------------
   默认移出视口（left: -9999px）彻底隐藏，仅在获得焦点时滑回左上角。 */
.skip-link {
  position: absolute;
  left: -9999px;
  top: 0;
  z-index: 1001;
  padding: 8px 14px;
  background: var(--ink);
  color: var(--ink-inverse);
  border-radius: 0 0 var(--r-sm) 0;
}

/* 聚焦态：回到可视区，保证键盘用户看得见 */
.skip-link:focus {
  left: 0;
}
</style>
