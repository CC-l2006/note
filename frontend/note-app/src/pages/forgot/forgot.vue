<template>
  <view class="page">
    <view class="topbar">
      <text class="back" @click="back">‹</text>
      <text class="kicker">RESET PASSWORD</text>
    </view>

    <view class="hero">
      <text class="title">找回密码</text>
      <text class="subtitle">验证邮箱后重设密码，和 Web 端同一套流程。</text>
    </view>

    <view class="card">
      <view class="field">
        <text class="label">邮箱</text>
        <input v-model="form.email" class="input" type="text" placeholder="注册邮箱" placeholder-class="placeholder" />
      </view>

      <view class="field">
        <text class="label">验证码</text>
        <view class="code-row">
          <input v-model="form.code" class="input code" type="number" placeholder="6 位验证码" placeholder-class="placeholder" />
          <button class="code-btn" :disabled="countdown > 0 || sending" @click="sendCode">
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </button>
        </view>
      </view>

      <view class="field">
        <text class="label">新密码</text>
        <input v-model="form.password" class="input" :password="true" placeholder="至少 6 位" placeholder-class="placeholder" />
      </view>

      <view class="field">
        <text class="label">确认新密码</text>
        <input v-model="form.confirm" class="input" :password="true" placeholder="再输入一次" placeholder-class="placeholder" />
      </view>

      <button class="primary" :loading="loading" :disabled="loading" @click="submit">重设密码</button>
    </view>
  </view>
</template>

<script setup>
import { onUnmounted, reactive, ref } from 'vue'
import { authApi } from '../../api/index.js'

const form = reactive({ email: '', code: '', password: '', confirm: '' })
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer = null

function back() {
  uni.navigateBack()
}

function startCountdown() {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function sendCode() {
  const mail = form.email.trim()
  if (!mail) {
    uni.showToast({ title: '请先填写邮箱', icon: 'none' })
    return
  }
  sending.value = true
  try {
    await authApi.sendCode(mail, 'reset')
    uni.showToast({ title: '验证码已发送', icon: 'none' })
    startCountdown()
  } catch (error) {
    uni.showToast({ title: error.message || '发送失败', icon: 'none' })
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!form.email.trim() || !form.code.trim() || !form.password) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  if (form.password !== form.confirm) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await authApi.resetPassword({
      email: form.email.trim(),
      code: form.code.trim(),
      password: form.password
    })
    uni.showToast({ title: '密码已重设', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (error) {
    uni.showToast({ title: error.message || '重设失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 40rpx 40rpx 56rpx;
  background: #f6f1e6;
  max-width: 960rpx;
  margin: 0 auto;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 48rpx;
}

.back {
  font-size: 52rpx;
  line-height: 1;
  color: #18212b;
}

.kicker {
  color: #c96a3d;
  font-size: 22rpx;
  letter-spacing: 2rpx;
}

.hero {
  margin-bottom: 36rpx;
}

.title {
  display: block;
  color: #18212b;
  font-size: 56rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}

.subtitle {
  display: block;
  color: #6b746f;
  font-size: 26rpx;
  line-height: 1.7;
}

.card {
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  border-radius: 22rpx;
  padding: 40rpx 32rpx 32rpx;
}

.field {
  margin-bottom: 26rpx;
}

.label {
  display: block;
  color: #18212b;
  font-size: 24rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.input {
  width: 100%;
  height: 84rpx;
  padding: 0 22rpx;
  border-radius: 12rpx;
  background: #f6f1e6;
  border: 1rpx solid #e4dccb;
  color: #18212b;
  font-size: 28rpx;
}

.placeholder {
  color: #a9aea7;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.code {
  flex: 1;
}

.code-btn {
  width: 210rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 12rpx;
  background: #ede5d5;
  color: #18212b;
  font-size: 23rpx;
  margin: 0;
}

.primary {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  border-radius: 14rpx;
  background: #18212b;
  color: #f6f1e6;
  font-size: 30rpx;
  font-weight: 600;
  margin-top: 10rpx;
}
</style>
