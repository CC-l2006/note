<template>
  <!-- 空状态占位：线稿 + 标题 + 可选描述 + 可选操作按钮 -->
  <div class="empty">
    <!-- 用一枚「空白页」的线稿代替插画：与纸的主题一致，也不引入图片资源 -->
    <svg class="empty__art" viewBox="0 0 64 64" aria-hidden="true">
      <rect x="10" y="6" width="44" height="52" rx="3" fill="none" stroke="currentColor" stroke-width="1.5" />
      <path d="M18 18h28M18 26h28M18 34h20" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" opacity="0.55" />
      <path d="M10 6 L22 6 L10 18 Z" fill="currentColor" opacity="0.12" />
    </svg>
    <!-- 主文案：说明当前为空 -->
    <p class="empty__title">{{ title }}</p>
    <!-- 补充说明：仅在传入 description 时出现 -->
    <p v-if="description" class="empty__desc">{{ description }}</p>
    <!-- 操作区：父级提供 action 插槽（如「新建笔记」按钮）时才渲染 -->
    <div v-if="$slots.action" class="empty__action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup>
/**
 * AppEmpty —— 通用空状态组件
 * 列表、表格、搜索结果没有数据时复用这一套占位样式，
 * 通过 action 插槽把「下一步该做什么」交给调用方决定。
 */

/** 组件 props：标题与描述文案，均为纯展示 */
defineProps({
  /** 主标题，默认使用一句友好的占位语 */
  title: { type: String, default: '这里还是空的' },
  /** 副描述，为空时不渲染该行 */
  description: { type: String, default: '' }
})
</script>

<style scoped>
/* ---------------- 整体布局 ---------------- */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--sp-3);
  padding: var(--sp-10) var(--sp-6);
  text-align: center;
  color: var(--ink-3);
}

/* ---------------- 图形与文案 ---------------- */
.empty__art {
  width: 56px;
  height: 56px;
  color: var(--rule-strong);
}

.empty__title {
  font-family: var(--font-serif);
  font-size: var(--fs-md);
  color: var(--ink-2);
}

.empty__desc {
  font-size: var(--fs-sm);
  max-width: 34ch;
  line-height: 1.7;
}

/* ---------------- 操作区 ---------------- */
.empty__action {
  margin-top: var(--sp-2);
}
</style>
