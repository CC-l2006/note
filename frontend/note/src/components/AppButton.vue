<template>
  <!-- 按钮：外观由 variant / size 组合决定，loading 时自动禁用并显示呼吸点 -->
  <button
    class="btn"
    :class="[`btn--${variant}`, `btn--${size}`, { 'is-loading': loading, 'is-block': block }]"
    :type="type"
    :disabled="disabled || loading"
  >
    <!-- 加载指示：纯 CSS 小圆点，不依赖图标资源 -->
    <span v-if="loading" class="spinner" aria-hidden="true" />
    <!-- 默认插槽：按钮文案或图标 -->
    <slot />
  </button>
</template>

<script setup>
/**
 * AppButton —— 全局按钮组件
 * 统一站内按钮的尺寸、色彩语义、禁用与加载表现；
 * 只负责渲染与样式，不包含任何业务逻辑，点击行为由父级通过原生事件监听。
 */

/** 组件 props：视觉变体、尺寸与交互状态 */
defineProps({
  /** primary=主行动 / ghost=次要 / quiet=纯文字 / danger=危险 */
  variant: { type: String, default: 'ghost' },
  /** 尺寸档位：md 常规、sm 紧凑（表格行内）、lg 强调 */
  size: { type: String, default: 'md' },
  /** 原生 button 类型，默认 button 以免在表单中意外触发提交 */
  type: { type: String, default: 'button' },
  /** 加载中：显示呼吸点并禁止点击 */
  loading: { type: Boolean, default: false },
  /** 外部受控禁用状态，仅影响可点击性，不改变配色 */
  disabled: { type: Boolean, default: false },
  /** 块级按钮：占满父容器宽度 */
  block: { type: Boolean, default: false }
})
</script>

<style scoped>
/* ---------------- 基础布局 ---------------- */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--sp-2);
  border: 1px solid transparent;
  border-radius: var(--r-sm);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
  line-height: 1;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

/* ---------------- 禁用状态 ---------------- */
.btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

/* ---------------- 尺寸档位 ---------------- */
.btn--md {
  padding: 9px 16px;
}

.btn--sm {
  padding: 6px 11px;
  font-size: var(--fs-xs);
}

.btn--lg {
  padding: 12px 22px;
  font-size: var(--fs-base);
}

/* ---------------- 宽度 ---------------- */
.btn--block {
  width: 100%;
}

/* ---------------- 变体配色 ---------------- */
/* 主行动：墨蓝实心 */
.btn--primary {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--ink-inverse);
}

.btn--primary:hover:not(:disabled) {
  background: var(--accent-hover);
  border-color: var(--accent-hover);
}

.btn--primary:active:not(:disabled) {
  background: var(--accent-press);
}

/* 次要：纸上的描边，没有填充 */
.btn--ghost {
  background: transparent;
  border-color: var(--rule-strong);
  color: var(--ink-2);
}

.btn--ghost:hover:not(:disabled) {
  border-color: var(--ink-3);
  color: var(--ink);
  background: var(--paper-deep);
}

/* 纯文字：用于列表行内操作 */
.btn--quiet {
  background: transparent;
  color: var(--accent);
  padding-left: 4px;
  padding-right: 4px;
}

.btn--quiet:hover:not(:disabled) {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.btn--danger {
  background: transparent;
  border-color: var(--rule-strong);
  color: var(--danger);
}

.btn--danger:hover:not(:disabled) {
  border-color: var(--danger);
  background: var(--danger-soft);
}

/* ---------------- 加载态 ---------------- */
/* 加载态用一个小墨点呼吸，不用旋转圈 */
@keyframes pulse-dot {
  0%, 100% { opacity: 0.25; }
  50% { opacity: 1; }
}

.spinner {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse-dot 1s ease-in-out infinite;
}
</style>
