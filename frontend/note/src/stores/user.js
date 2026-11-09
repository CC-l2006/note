/* =============================================================================
   用户状态 store（组合式写法）

   两块状态：token（持久化在 localStorage）与 userInfo（仅内存，刷新后由守卫补拉）。
   所有涉及登录态的写操作都收敛在这里，页面不直接碰 localStorage。
   ============================================================================= */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, userApi } from '@/api/index.js'
import { getToken, TOKEN_KEY } from '@/api/request.js'

/**
 * 用户状态。
 *
 * 上一版这里是写死的 mock（mock-token-xxx + 固定 roles），现在接到 api/index.js，
 * 真实后端就绪后**本文件一行都不用改** —— 只要后端按 api/index.js 里的契约实现。
 */
export const useUserStore = defineStore('user', () => {
  /* ---------------- 状态 ----------------
     token 初值从 localStorage 读，刷新页面后不会丢失登录态；
     userInfo 先留空，由路由守卫在需要时补拉。 */
  const token = ref(getToken())
  const userInfo = ref(null)

  /* ---------------- 派生状态 ---------------- */
  /* 有没有 token 就是「是否登录」的唯一判据 */
  const isLogin = computed(() => !!token.value)
  /* 管理后台入口的可见性依据 */
  const isAdmin = computed(() => userInfo.value?.role === 'admin')
  /* AI 剩余次数：后端没给时按 0 处理，避免界面显示 undefined */
  const aiQuota = computed(() => userInfo.value?.aiQuota ?? 0)
  /* 展示名优先级：昵称 > 账号 > 未登录，保证侧栏永远有字可显示 */
  const displayName = computed(
    () => userInfo.value?.nickname || userInfo.value?.username || '未登录'
  )

  /* ---------------- 状态写入 ---------------- */

  /**
   * 写入 token：同时同步到 localStorage。
   * 传空值表示清除（退出登录）。
   */
  function setToken(value) {
    token.value = value || ''
    try {
      if (value) localStorage.setItem(TOKEN_KEY, value)
      else localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* 存储不可用（隐私模式 / 配额满）时只保留内存态，不阻塞登录流程 */
    }
  }

  /* 整体替换用户信息；传空即清空 */
  function setUserInfo(info) {
    userInfo.value = info || null
  }

  /* ---------------- 登录 / 注册 ---------------- */

  /** 登录：邮箱 + 密码（2026-10-20 起账号标识是邮箱，不再是用户名） */
  async function login({ email, password }) {
    /* 成功后后端返回 token + user，两者一起落库，避免再发一次 profile */
    const { data } = await authApi.login({ email, password })
    setToken(data.token)
    setUserInfo(data.user)
    return data.user
  }

  /** 注册：邮箱 + 验证码 + 密码（+ 可选昵称），账号名由后端生成 */
  async function register({ email, code, password, nickname }) {
    /* 注册成功不自动登录，页面会引导用户回登录页 */
    const { data } = await authApi.register({ email, code, password, nickname })
    return data
  }

  /* ---------------- 登录态恢复 ---------------- */

  /** 路由守卫里调用：刷新页面后凭 token 恢复登录态 */
  async function fetchUserInfo() {
    /* 失败时异常向上抛，由守卫决定是跳登录页还是提示 */
    const { data } = await userApi.profile()
    setUserInfo(data)
    return data
  }

  /* ---------------- 局部更新 ---------------- */

  /** 局部更新（改资料、AI 额度扣减后同步） */
  function patchUserInfo(patch) {
    /* 未登录时没有可合并的对象，直接忽略 */
    if (!userInfo.value) return
    userInfo.value = { ...userInfo.value, ...patch }
  }

  /* ---------------- 退出 ---------------- */

  /**
   * 退出登录：先尽力通知后端作废令牌，再无条件清本地。
   * 即便接口失败也要清干净，否则用户会卡在「看似已登录」的状态。
   */
  async function logout() {
    try {
      if (token.value) await authApi.logout()
    } catch {
      /* 忽略：本地状态清理比服务端令牌失效更重要 */
    }
    setToken('')
    setUserInfo(null)
  }

  /* ---------------- 对外暴露 ---------------- */
  return {
    token,
    userInfo,
    isLogin,
    isAdmin,
    aiQuota,
    displayName,
    setToken,
    setUserInfo,
    patchUserInfo,
    login,
    register,
    fetchUserInfo,
    logout
  }
})
