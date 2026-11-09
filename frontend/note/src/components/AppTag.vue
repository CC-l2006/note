<template>
  <!-- 标签：底色与描边随 tone 变化，closable 时右侧带移除按钮 -->
  <span class="tag" :class="[`tag--${tone}`, { 'is-static': !closable }]">
    <!-- 默认插槽：标签文字内容 -->
    <slot />
    <!-- 移除按钮：closable 为真时出现，阻止冒泡以免触发外层的点击行为 -->
    <button v-if="closable" class="tag__close" type="button" :aria-label="`移除 ${label}`" @click.stop="$emit('close')">
      ×
    </button>
  </span>
</template>

<script setup>
/**
 * AppTag —— 标签 / 徽标组件
 * 只用颜色表达语义（中性的流程标签、成功、警告、危险），
 * 可选的关闭按钮仅抛出 close 事件，移除时机与副作用由父级决定。
 */

/** 组件 props：色调、是否可移除、以及给关闭按钮用的无障碍名称 */
defineProps({
  /** neutral / accent / ok / warn / danger */
  tone: { type: String, default: 'neutral' },
  /** 是否显示移除按钮 */
  closable: { type: Boolean, default: false },
  /** 标签名称，用于拼接关闭按钮的 aria-label */
  label: { type: String, default: '' }
})

/** 组件事件：点击移除按钮时触发 */
defineEmits(['close'])
</script>

<style scoped>
/* ---------------- 基础布局 ---------------- */
.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 8px;
  border: 1px solid var(--rule-strong);
  border-radius: var(--r-sm);
  font-size: var(--fs-xs);
  line-height: 1.6;
  color: var(--ink-2);
  background: transparent;
  white-space: nowrap;
}

/* ---------------- 语义色调 ---------------- */
.tag--accent {
  border-color: transparent;
  background: var(--accent-soft);
  color: var(--accent-ink);
}

.tag--ok {
  border-color: transparent;
  background: var(--ok-soft);
  color: var(--ok);
}

.tag--warn {
  border-color: transparent;
  background: var(--warn-soft);
  color: var(--warn);
}

.tag--danger {
  border-color: transparent;
  background: var(--danger-soft);
  color: var(--danger);
}

/* ---------------- 移除按钮 ---------------- */
.tag__close {
  border: 0;
  background: none;
  padding: 0 0 0 2px;
  margin: 0;
  font-size: 14px;
  line-height: 1;
  color: currentColor;
  opacity: 0.5;
  cursor: pointer;
}

/* 悬停时加深到全不透明，提示可点击 */
.tag__close:hover {
  opacity: 1;
}
</style>
