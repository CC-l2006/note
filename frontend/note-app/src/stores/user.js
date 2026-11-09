import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi, userApi } from '../api/index.js'
import { clearToken, getToken, TOKEN_KEY } from '../api/request.js'

const USER_KEY = 'note_user'

/**
 * 登录态：token 存本地存储，用户资料用 Pinia 缓存；
 * 刷新/重启后先读 token，再由页面调用 fetchProfile 恢复资料。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref(uni.getStorageSync(USER_KEY) || null)

  const isLogin = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')
  const displayName = computed(
    () => userInfo.value?.nickname || userInfo.value?.username || '未登录'
  )
  const aiQuota = computed(() => userInfo.value?.aiQuota ?? 0)

  function setToken(value) {
    token.value = value || ''
    try {
      if (value) uni.setStorageSync(TOKEN_KEY, value)
      else uni.removeStorageSync(TOKEN_KEY)
    } catch {
      // 忽略存储异常
    }
  }

  function setUserInfo(info) {
    userInfo.value = info || null
    try {
      if (info) uni.setStorageSync(USER_KEY, info)
      else uni.removeStorageSync(USER_KEY)
    } catch {
      // 忽略存储异常
    }
  }

  async function login({ email, password }) {
    const { data } = await authApi.login({ email, password })
    setToken(data.token)
    setUserInfo(data.user)
    return data.user
  }

  async function fetchProfile() {
    const { data } = await userApi.profile()
    setUserInfo(data)
    return data
  }

  function patchUserInfo(patch) {
    if (!userInfo.value) return
    setUserInfo({ ...userInfo.value, ...patch })
  }

  async function logout() {
    try {
      if (token.value) await authApi.logout()
    } catch {
      // 退出接口失败也要清本地状态
    }
    setToken('')
    setUserInfo(null)
    clearToken()
  }

  return {
    token,
    userInfo,
    isLogin,
    isAdmin,
    displayName,
    aiQuota,
    setToken,
    setUserInfo,
    login,
    fetchProfile,
    patchUserInfo,
    logout
  }
})
