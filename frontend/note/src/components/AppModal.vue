<template>
  <!-- 弹窗挂到 body 上，避免被父级容器的 overflow / transform 裁剪 -->
  <Teleport to="body">
    <!-- 仅当 modelValue 为真时渲染，关闭后 DOM 直接移除 -->
    <div v-if="modelValue" class="modal" role="dialog" aria-modal="true" :aria-label="title">
      <!-- 遮罩层：点击时按 closeOnScrim 决定是否关闭 -->
      <div class="modal__scrim" @click="onScrim" />
      <!-- 面板：宽度由 width 控制，ref 用于打开后转移焦点 -->
      <div ref="panelRef" class="modal__panel" :style="{ width: width }">
        <!-- 头部：标题与右上角关闭按钮 -->
        <header class="modal__head">
          <h2 class="modal__title">{{ title }}</h2>
          <button class="modal__close" type="button" aria-label="关闭" @click="close">×</button>
        </header>

        <!-- 主体：默认插槽承载任意弹窗内容 -->
        <div class="modal__body">
          <slot />
        </div>

        <!-- 底部操作区：调用方提供 footer 插槽时渲染 -->
        <footer v-if="$slots.footer" class="modal__foot">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
/**
 * AppModal —— 通用模态框组件
 * 负责挂载位置、遮罩关闭、Esc 关闭以及打开/关闭时的焦点管理（焦点移入面板、关闭后归还），
 * 内容与底部按钮完全由插槽决定，组件本身不关心业务。
 */

import { ref, watch, nextTick, onBeforeUnmount } from 'vue'

/** 组件 props：显隐控制、标题、宽度与遮罩行为 */
const props = defineProps({
  /** v-model 控制的显隐状态 */
  modelValue: { type: Boolean, default: false },
  /** 标题，同时作为弹窗的 aria-label */
  title: { type: String, default: '' },
  /** 面板宽度，直接写入内联样式 */
  width: { type: String, default: '480px' },
  /** 点击遮罩是否关闭，表单类弹窗可关掉以防误触丢失输入 */
  closeOnScrim: { type: Boolean, default: true }
})

/** 组件事件：同步显隐状态与关闭通知 */
const emit = defineEmits(['update:modelValue', 'close'])
/** 面板 DOM 引用，用于聚焦内部第一个可交互元素 */
const panelRef = ref(null)
/** 记录打开前的焦点元素，关闭时还原，避免键盘用户丢失位置 */
let lastFocused = null

/** 关闭弹窗：先同步 v-model，再抛出 close 供父级做收尾（如重置表单） */
function close() {
  emit('update:modelValue', false)
  emit('close')
}

/** 遮罩点击：仅在允许点击遮罩关闭时生效 */
function onScrim() {
  if (props.closeOnScrim) close()
}

/** 全局键盘监听：Esc 关闭弹窗 */
function onKeydown(e) {
  if (e.key === 'Escape') close()
}

/** 监听显隐变化：打开时绑定键盘事件并转移焦点，关闭时解绑并还原焦点 */
watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      lastFocused = document.activeElement
      document.addEventListener('keydown', onKeydown)
      // 打开后把焦点送进面板，键盘用户不会被留在弹窗背后
      await nextTick()
      const focusable = panelRef.value?.querySelector(
        'input, textarea, select, button:not(.modal__close)'
      )
      ;(focusable || panelRef.value)?.focus?.()
    } else {
      document.removeEventListener('keydown', onKeydown)
      lastFocused?.focus?.()
    }
  }
)

/** 卸载前兜底移除监听，防止组件被直接销毁时事件泄漏 */
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
/* ---------------- 整体定位：全屏居中 ---------------- */
.modal {
  position: fixed;
  inset: 0;
  z-index: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--sp-4);
}

/* ---------------- 遮罩 ---------------- */
.modal__scrim {
  position: absolute;
  inset: 0;
  background: rgba(34, 31, 27, 0.42);
}

/* ---------------- 面板容器 ---------------- */
.modal__panel {
  position: relative;
  max-width: 100%;
  max-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  background: var(--sheet);
  border: 1px solid var(--rule);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-pop);
  outline: none;
}

/* 键盘聚焦到面板本身时给出可见轮廓 */
.modal__panel:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* ---------------- 头部 ---------------- */
.modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-3);
  padding: var(--sp-4) var(--sp-5);
  border-bottom: 1px solid var(--rule);
}

.modal__title {
  font-size: var(--fs-lg);
}

.modal__close {
  border: 0;
  background: none;
  font-size: 22px;
  line-height: 1;
  padding: 0 4px;
  color: var(--ink-3);
  cursor: pointer;
}

.modal__close:hover {
  color: var(--ink);
}

/* ---------------- 主体：内容超长时内部滚动 ---------------- */
.modal__body {
  padding: var(--sp-5);
  overflow-y: auto;
}

/* ---------------- 底部操作区 ---------------- */
.modal__foot {
  display: flex;
  justify-content: flex-end;
  gap: var(--sp-2);
  padding: var(--sp-3) var(--sp-5);
  border-top: 1px solid var(--rule);
  background: var(--paper);
  border-radius: 0 0 var(--r-md) var(--r-md);
}
</style>
