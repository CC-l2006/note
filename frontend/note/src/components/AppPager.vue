<template>
  <!-- 分页条：只有总条数超过单页容量时才出现，避免单页列表下方出现空控件 -->
  <nav v-if="total > size" class="pager" aria-label="分页">
    <!-- 上一页：已在首页时禁用 -->
    <button class="pager__btn" type="button" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
    <!-- 页码信息：当前页 / 总页数 / 总条数 -->
    <span class="pager__info">
      第 <strong>{{ page }}</strong> / {{ pages }} 页，共 {{ total }} 条
    </span>
    <!-- 下一页：已在末页时禁用 -->
    <button class="pager__btn" type="button" :disabled="page >= pages" @click="go(page + 1)">下一页</button>
  </nav>
</template>

<script setup>
/**
 * AppPager —— 轻量分页组件
 * 只做「上一页 / 下一页 + 页码展示」，不涉及数据请求；
 * 通过 v-model:page 把目标页码交回父组件，由父级决定如何加载数据。
 */

import { computed } from 'vue'

/** 组件 props：当前页、每页条数与总条数 */
const props = defineProps({
  /** 当前页码，从 1 开始 */
  page: { type: Number, default: 1 },
  /** 每页条数，用于计算总页数 */
  size: { type: Number, default: 10 },
  /** 数据总条数 */
  total: { type: Number, default: 0 }
})

/** 组件事件：请求切换到某一页 */
const emit = defineEmits(['update:page'])
/** 总页数：至少 1 页，避免出现「第 1 / 0 页」 */
const pages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

/** 翻页：越界或点回当前页时直接忽略，不做无效通知 */
function go(next) {
  if (next < 1 || next > pages.value || next === props.page) return
  emit('update:page', next)
}
</script>

<style scoped>
/* ---------------- 布局：右对齐的一行 ---------------- */
.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--sp-3);
  padding-top: var(--sp-4);
  font-size: var(--fs-sm);
  color: var(--ink-3);
}

/* ---------------- 翻页按钮 ---------------- */
.pager__btn {
  padding: 5px 12px;
  border: 1px solid var(--rule-strong);
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--ink-2);
  cursor: pointer;
  font-size: var(--fs-sm);
}

/* ---------------- 交互与禁用状态 ---------------- */
.pager__btn:hover:not(:disabled) {
  border-color: var(--ink-3);
  color: var(--ink);
  background: var(--paper-deep);
}

.pager__btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ---------------- 页码信息 ---------------- */
.pager__info strong {
  color: var(--ink);
  font-weight: 600;
}
</style>
