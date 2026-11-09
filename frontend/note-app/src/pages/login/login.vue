<template>
  <view class="page">
    <!-- 品牌区 -->
    <view class="hero">
      <image class="logo" src="/static/logo.png" mode="aspectFit" />
      <text class="kicker">CIPHERWS NOTE</text>
      <text class="title">把想法，留在纸上。</text>
      <text class="subtitle">登录后继续你的笔记、标签与 AI 总结。</text>
    </view>

    <!-- 登录表单 -->
    <view class="card">
      <view class="field">
        <text class="label">邮箱</text>
        <input
          v-model="email"
          class="input"
          type="text"
          placeholder="you@example.com"
          placeholder-class="placeholder"
          confirm-type="next"
        />
      </view>

      <view class="field">
        <text class="label">密码</text>
        <input
          v-model="password"
          class="input"
          :password="true"
          placeholder="请输入密码"
          placeholder-class="placeholder"
          confirm-type="done"
          @confirm="submit"
        />
      </view>

      <button class="primary" :loading="loading" :disabled="loading" @click="submit">
        登录
      </button>

      <view class="links">
        <text class="link" @click="goRegister">没有账号？去注册</text>
        <text class="link muted" @click="showDemo">演示账号</text>
      </view>
    </view>

    <text class="footer">CipherWS Note · uni-app</text>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '../../stores/user.js'
import { getToken } from '../../api/request.js'

const userStore = useUserStore()
const email = ref('')
const password = ref('')
const loading = ref(false)

onLoad(() => {
  // 已有 token 时不再停留在登录页
  if (getToken()) {
    uni.switchTab({ url: '/pages/notes/notes' })
  }
})

function goRegister() {
  uni.navigateTo({ url: '/pages/register/register' })
}

function showDemo() {
  uni.showModal({
    title: '演示账号',
    content: '管理员：admin@cipherws.icu / admin123\n普通用户：demo@cipherws.icu / demo123',
    showCancel: false
  })
}

async function submit() {
  const mail = email.value.trim()
  if (!mail || !password.value) {
    uni.showToast({ title: '请输入邮箱和密码', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.login({ email: mail, password: password.value })
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/notes/notes' })
    }, 300)
  } catch (error) {
    uni.showToast({ title: error.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 96rpx 40rpx 48rpx;
  background: #f6f1e6;
  max-width: 960rpx;
  margin: 0 auto;
}

.hero {
  padding: 24rpx 8rpx 56rpx;
}

.logo {
  display: block;
  width: 96rpx;
  height: 96rpx;
  border-radius: 18rpx;
  margin-bottom: 36rpx;
}

.kicker {
  display: block;
  color: #c96a3d;
  font-size: 22rpx;
  letter-spacing: 2rpx;
  margin-bottom: 20rpx;
}

.title {
  display: block;
  color: #18212b;
  font-size: 62rpx;
  line-height: 1.25;
  font-weight: 700;
  margin-bottom: 20rpx;
}

.subtitle {
  display: block;
  color: #6b746f;
  font-size: 27rpx;
  line-height: 1.7;
}

.card {
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  border-radius: 22rpx;
  padding: 44rpx 36rpx 36rpx;
}

.field {
  margin-bottom: 32rpx;
}

.label {
  display: block;
  color: #18212b;
  font-size: 24rpx;
  font-weight: 600;
  margin-bottom: 14rpx;
}

.input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  border-radius: 12rpx;
  background: #f6f1e6;
  border: 1rpx solid #e4dccb;
  color: #18212b;
  font-size: 28rpx;
}

.placeholder {
  color: #a9aea7;
}

.primary {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 14rpx;
  background: #18212b;
  color: #f6f1e6;
  font-size: 30rpx;
  font-weight: 600;
  margin-top: 8rpx;
}

.links {
  display: flex;
  justify-content: space-between;
  margin-top: 28rpx;
}

.link {
  color: #c96a3d;
  font-size: 24rpx;
}

.link.muted {
  color: #8a918b;
}

.footer {
  display: block;
  text-align: center;
  color: #a9aea7;
  font-size: 22rpx;
  margin-top: 48rpx;
}
</style>
