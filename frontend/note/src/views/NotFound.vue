<template>
  <!-- 404 兜底页：路由没有匹配到任何页面时展示，解释原因并给出返回入口 -->
  <div class="notfound">
    <p class="notfound__code" aria-hidden="true">404</p>
    <h1 class="notfound__title">这一页是空白的</h1>
    <p class="notfound__desc">
      地址可能敲错了，或者这篇笔记已经被删除。回到笔记列表重新找找。
    </p>
    <!-- 唯一出口：回到笔记列表 -->
    <div class="notfound__actions">
      <RouterLink to="/notes">
        <AppButton variant="primary">回到我的笔记</AppButton>
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
/**
 * NotFound.vue —— 404 兜底页
 * 职责：承接未匹配的路由，给出「页面不存在」的解释与返回笔记列表的入口。
 * 纯静态页面：没有状态、没有请求，也不做错误处理，只依赖两个子组件。
 */
import { RouterLink } from 'vue-router'
import AppButton from '@/components/AppButton.vue'
</script>

<style scoped>
/* ---------- 整体：整页垂直居中 ---------- */
.notfound {
  min-height: 70vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--sp-3);
  padding: var(--sp-8) var(--sp-4);
  text-align: center;
}

/* ---------- 大号 404 数字 ---------- */
.notfound__code {
  font-family: var(--font-serif);
  font-size: 72px;
  line-height: 1;
  color: var(--rule-strong);
  letter-spacing: 0.04em;
}

/* ---------- 文案与操作区 ---------- */
.notfound__title {
  font-size: var(--fs-xl);
}

.notfound__desc {
  max-width: 36ch;
  font-size: var(--fs-sm);
  line-height: 1.8;
  color: var(--ink-3);
}

.notfound__actions {
  margin-top: var(--sp-2);
}
</style>
