/* =============================================================================
   全局提示 store（组合式写法）

   维护一个消息队列，由 components/AppToast.vue 负责渲染。
   设计上只支持「一条一条地弹」，不做确认框 —— 需要用户决策的场景走页面内弹窗。
   ============================================================================= */

import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 轻量全局提示。
 * 需求 6.3.1 要求「额度不足、登录失效、保存失败给出明确提示」，
 * 所以所有失败路径都走这里，而不是各页面自己 alert。
 */
export const useToastStore = defineStore('toast', () => {
  /* ---------------- 状态 ---------------- */
  /* items 是当前在屏上的消息数组，结构为 { id, message, type } */
  const items = ref([])
  /* 自增序号：保证同一条消息重复推送时 key 也不冲突 */
  let seq = 0

  /* ---------------- 入队 ---------------- */

  /**
   * 推入一条提示。
   * ttl 为自动消失的毫秒数，传 0 表示常驻（需手动 dismiss）。
   */
  function push(message, type = 'info', ttl = 3200) {
    const id = ++seq
    items.value.push({ id, message, type })
    /* ttl > 0 才排定时器；常驻消息用于需要用户手动关闭的强提醒 */
    if (ttl > 0) {
      setTimeout(() => dismiss(id), ttl)
    }
    return id
  }

  /* ---------------- 出队 ---------------- */

  /* 按 id 移除；即使已被定时器移除过，过滤也是幂等的 */
  function dismiss(id) {
    items.value = items.value.filter((t) => t.id !== id)
  }

  /* ---------------- 常用快捷方法 ----------------
     不同语义给不同停留时长：越严重留得越久。 */
  const success = (m) => push(m, 'success')
  const error = (m) => push(m, 'error', 4200)
  const info = (m) => push(m, 'info')
  const warn = (m) => push(m, 'warn', 3800)

  return { items, push, dismiss, success, error, info, warn }
})
