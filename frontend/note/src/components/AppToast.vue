<template>
  <!-- 提示条容器挂到 body，避免被页面容器裁切或层叠顺序影响 -->
  <Teleport to="body">
    <!-- aria-live 让读屏在提示出现时自动播报，不打断当前操作 -->
    <div class="toasts" aria-live="polite" aria-atomic="false">
      <!-- 进出场动画交给 TransitionGroup，列表按加入顺序堆叠 -->
      <TransitionGroup name="toast">
        <!-- 单条提示：类型决定左侧色条与圆点颜色 -->
        <div
          v-for="t in toast.items"
          :key="t.id"
          class="toast"
          :class="`toast--${t.type}`"
          role="status"
        >
          <!-- 类型色点：纯装饰，语义已由 role/aria-live 承担 -->
          <span class="toast__mark" aria-hidden="true" />
          <!-- 提示正文，长文本自动换行 -->
          <span class="toast__msg">{{ t.message }}</span>
          <!-- 手动关闭：带上提示 id 通知 store 移除 -->
          <button class="toast__close" type="button" aria-label="关闭提示" @click="toast.dismiss(t.id)">
            ×
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
/**
 * AppToast —— 全局提示条渲染组件
 * 自身不维护任何状态，只读取 toast store 的队列并渲染；
 * 自动消失、去重、最大条数等策略全部由 store 负责。
 */

import { useToastStore } from '@/stores/toast.js'

/** 全局提示 store：提供 items 列表与 dismiss 方法 */
const toast = useToastStore()
</script>

<style scoped>
/* ---------------- 定位：底部居中、纵向堆叠 ---------------- */
.toasts {
  position: fixed;
  /* 放底部居中：顶部会压住编辑区工具栏和页标题（实测会挡掉「AI 总结」「删除」按钮），
     底部一次性提示更不容易遮住正在操作的东西。 */
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  display: flex;
  flex-direction: column-reverse;
  gap: var(--sp-2);
  pointer-events: none;
}

/* ---------------- 单条提示外观 ---------------- */
.toast {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-2);
  min-width: 220px;
  max-width: 420px;
  padding: 10px 12px;
  background: var(--sheet);
  border: 1px solid var(--rule-strong);
  border-left-width: 3px;
  border-radius: var(--r-sm);
  box-shadow: var(--shadow-pop);
  font-size: var(--fs-sm);
  color: var(--ink);
  pointer-events: auto;
}

/* ---------------- 类型状态：左侧色条与色点 ---------------- */
.toast--success { border-left-color: var(--ok); }
.toast--error   { border-left-color: var(--danger); }
.toast--warn    { border-left-color: var(--warn); }
.toast--info    { border-left-color: var(--accent); }

.toast__mark {
  flex: none;
  width: 6px;
  height: 6px;
  margin-top: 7px;
  border-radius: 50%;
  background: var(--ink-3);
}

.toast--success .toast__mark { background: var(--ok); }
.toast--error .toast__mark   { background: var(--danger); }
.toast--warn .toast__mark    { background: var(--warn); }
.toast--info .toast__mark    { background: var(--accent); }

/* ---------------- 文本与关闭按钮 ---------------- */
.toast__msg {
  flex: 1;
  line-height: 1.55;
  word-break: break-word;
}

.toast__close {
  flex: none;
  border: 0;
  background: none;
  padding: 0 2px;
  font-size: 16px;
  line-height: 1;
  color: var(--ink-3);
  cursor: pointer;
}

.toast__close:hover { color: var(--ink); }

/* ---------------- 进出场过渡 ---------------- */
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
