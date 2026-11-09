<template>
  <view class="page">
    <view class="header">
      <text class="kicker">ACCOUNT SETTINGS</text>
      <text class="title">账号设置</text>
      <text class="count">资料、密码与邮箱，与 Web 端同一套规则</text>
    </view>

    <!-- 资料 -->
    <view class="panel">
      <text class="panel-title">个人资料</text>
      <view class="avatar-row">
        <image v-if="profile.avatar" class="avatar" :src="resolveAssetUrl(profile.avatar)" mode="aspectFill" />
        <view v-else class="avatar avatar--text">{{ initial }}</view>
        <button class="upload-btn" :loading="uploading" @click="chooseAvatar">更换头像</button>
      </view>
      <view class="field">
        <text class="label">昵称</text>
        <input v-model="profile.nickname" class="input" type="text" placeholder="昵称" placeholder-class="placeholder" />
      </view>
      <view class="field">
        <text class="label">简介</text>
        <textarea v-model="profile.bio" class="textarea" placeholder="一句话介绍自己" placeholder-class="placeholder" :maxlength="200" />
      </view>
      <button class="primary" :loading="savingProfile" @click="saveProfile">保存资料</button>
    </view>

    <!-- 密码 -->
    <view class="panel">
      <text class="panel-title">修改密码</text>
      <view class="field">
        <text class="label">当前邮箱</text>
        <text class="readonly">{{ userStore.userInfo?.email || '-' }}</text>
      </view>
      <view class="field">
        <text class="label">邮箱验证码</text>
        <view class="code-row">
          <input v-model="passwordForm.code" class="input code" type="number" placeholder="6 位验证码" placeholder-class="placeholder" />
          <button class="code-btn" :disabled="pwdCountdown > 0 || pwdSending" @click="sendPasswordCode">
            {{ pwdCountdown > 0 ? `${pwdCountdown}s` : '获取验证码' }}
          </button>
        </view>
      </view>
      <view class="field">
        <text class="label">旧密码</text>
        <input v-model="passwordForm.oldPassword" class="input" :password="true" placeholder="当前密码" placeholder-class="placeholder" />
      </view>
      <view class="field">
        <text class="label">新密码</text>
        <input v-model="passwordForm.newPassword" class="input" :password="true" placeholder="至少 6 位" placeholder-class="placeholder" />
      </view>
      <button class="primary" :loading="savingPassword" @click="changePassword">修改密码</button>
    </view>

    <!-- 邮箱 -->
    <view class="panel">
      <text class="panel-title">更换绑定邮箱</text>
      <text class="hint">先验证当前邮箱，再验证新邮箱；两步都通过才会生效。</text>

      <view class="step">
        <text class="step-title">第 1 步 · 验证当前邮箱</text>
        <view class="code-row">
          <input v-model="emailForm.oldCode" class="input code" type="number" placeholder="当前邮箱验证码" placeholder-class="placeholder" :disabled="emailForm.oldVerified" />
          <button class="code-btn" :disabled="oldCountdown > 0 || oldSending || emailForm.oldVerified" @click="sendOldCode">
            {{ oldCountdown > 0 ? `${oldCountdown}s` : '发送' }}
          </button>
        </view>
        <button v-if="!emailForm.oldVerified" class="ghost" :loading="verifyingOld" @click="verifyOld">验证当前邮箱</button>
        <text v-else class="success">当前邮箱已验证，10 分钟内有效</text>
      </view>

      <view v-if="emailForm.oldVerified" class="step">
        <text class="step-title">第 2 步 · 绑定新邮箱</text>
        <view class="field">
          <text class="label">新邮箱</text>
          <input v-model="emailForm.newEmail" class="input" type="text" placeholder="new@example.com" placeholder-class="placeholder" />
        </view>
        <view class="code-row">
          <input v-model="emailForm.newCode" class="input code" type="number" placeholder="新邮箱验证码" placeholder-class="placeholder" />
          <button class="code-btn" :disabled="newCountdown > 0 || newSending" @click="sendNewCode">
            {{ newCountdown > 0 ? `${newCountdown}s` : '发送' }}
          </button>
        </view>
        <button class="primary" :loading="bindingEmail" @click="bindEmail">确认更换</button>
      </view>
    </view>

    <text class="footer">CipherWS Note · 账号设置</text>
  </view>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../stores/user.js'
import { getToken } from '../../api/request.js'
import { resolveAssetUrl } from '../../config.js'
import { userApi } from '../../api/index.js'

const userStore = useUserStore()
const profile = reactive({ nickname: '', bio: '', avatar: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', code: '' })
const emailForm = reactive({ oldCode: '', oldVerified: false, newEmail: '', newCode: '' })

const uploading = ref(false)
const savingProfile = ref(false)
const pwdSending = ref(false)
const pwdCountdown = ref(0)
const savingPassword = ref(false)
const oldSending = ref(false)
const oldCountdown = ref(0)
const verifyingOld = ref(false)
const newSending = ref(false)
const newCountdown = ref(0)
const bindingEmail = ref(false)

let timers = []

const initial = computed(() => (userStore.displayName || 'N').slice(0, 1).toUpperCase())

function countdown(refValue) {
  refValue.value = 60
  const timer = setInterval(() => {
    refValue.value -= 1
    if (refValue.value <= 0) clearInterval(timer)
  }, 1000)
  timers.push(timer)
}

async function loadProfile() {
  try {
    const { data } = await userApi.profile()
    profile.nickname = data.nickname || ''
    profile.bio = data.bio || ''
    profile.avatar = data.avatar || ''
    userStore.setUserInfo(data)
  } catch (error) {
    uni.showToast({ title: error.message || '资料加载失败', icon: 'none' })
  }
}

async function saveProfile() {
  savingProfile.value = true
  try {
    const { data } = await userApi.updateProfile({
      nickname: profile.nickname.trim(),
      bio: profile.bio.trim(),
      avatar: profile.avatar
    })
    userStore.setUserInfo(data)
    uni.showToast({ title: '资料已保存', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  } finally {
    savingProfile.value = false
  }
}

function chooseAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const path = res.tempFilePaths?.[0]
      if (!path) return
      uploading.value = true
      try {
        const { data } = await userApi.uploadAvatar(path)
        profile.avatar = typeof data === 'string' ? data : data?.url || ''
        await saveProfile()
      } catch (error) {
        uni.showToast({ title: error.message || '头像上传失败', icon: 'none' })
      } finally {
        uploading.value = false
      }
    }
  })
}

async function sendPasswordCode() {
  pwdSending.value = true
  try {
    await userApi.sendPasswordCode()
    uni.showToast({ title: '验证码已发送', icon: 'none' })
    countdown(pwdCountdown)
  } catch (error) {
    uni.showToast({ title: error.message || '发送失败', icon: 'none' })
  } finally {
    pwdSending.value = false
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.code) {
    uni.showToast({ title: '请填写旧密码、新密码和验证码', icon: 'none' })
    return
  }
  savingPassword.value = true
  try {
    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      code: passwordForm.code
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.code = ''
    uni.showToast({ title: '密码已修改', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '修改失败', icon: 'none' })
  } finally {
    savingPassword.value = false
  }
}

async function sendOldCode() {
  oldSending.value = true
  try {
    await userApi.sendOldEmailCode()
    uni.showToast({ title: '验证码已发送到当前邮箱', icon: 'none' })
    countdown(oldCountdown)
  } catch (error) {
    uni.showToast({ title: error.message || '发送失败', icon: 'none' })
  } finally {
    oldSending.value = false
  }
}

async function verifyOld() {
  if (!emailForm.oldCode) {
    uni.showToast({ title: '请填写当前邮箱验证码', icon: 'none' })
    return
  }
  verifyingOld.value = true
  try {
    await userApi.verifyOldEmail(emailForm.oldCode)
    emailForm.oldVerified = true
    uni.showToast({ title: '当前邮箱已验证', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '验证失败', icon: 'none' })
  } finally {
    verifyingOld.value = false
  }
}

async function sendNewCode() {
  if (!emailForm.newEmail.trim()) {
    uni.showToast({ title: '请先填写新邮箱', icon: 'none' })
    return
  }
  newSending.value = true
  try {
    await userApi.sendNewEmailCode(emailForm.newEmail.trim())
    uni.showToast({ title: '验证码已发送到新邮箱', icon: 'none' })
    countdown(newCountdown)
  } catch (error) {
    uni.showToast({ title: error.message || '发送失败', icon: 'none' })
  } finally {
    newSending.value = false
  }
}

async function bindEmail() {
  if (!emailForm.newEmail.trim() || !emailForm.newCode) {
    uni.showToast({ title: '请填写新邮箱和验证码', icon: 'none' })
    return
  }
  bindingEmail.value = true
  try {
    await userApi.bindEmail({
      email: emailForm.newEmail.trim(),
      code: emailForm.newCode
    })
    uni.showToast({ title: '邮箱已更换', icon: 'success' })
    emailForm.oldVerified = false
    emailForm.oldCode = ''
    emailForm.newEmail = ''
    emailForm.newCode = ''
    await loadProfile()
  } catch (error) {
    uni.showToast({ title: error.message || '更换失败', icon: 'none' })
  } finally {
    bindingEmail.value = false
  }
}

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  loadProfile()
})

onUnmounted(() => {
  timers.forEach((timer) => clearInterval(timer))
  timers = []
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 36rpx 32rpx 60rpx;
  background: #f6f1e6;
  max-width: 1080rpx;
  margin: 0 auto;
}

.header {
  margin-bottom: 26rpx;
}

.kicker {
  display: block;
  color: #c96a3d;
  font-size: 21rpx;
  letter-spacing: 2rpx;
  margin-bottom: 8rpx;
}

.title {
  display: block;
  color: #18212b;
  font-size: 50rpx;
  font-weight: 700;
}

.count {
  display: block;
  color: #8a918b;
  font-size: 23rpx;
  margin-top: 8rpx;
}

.panel {
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  border-radius: 18rpx;
  padding: 28rpx 26rpx;
  margin-bottom: 22rpx;
}

.panel-title {
  display: block;
  color: #18212b;
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 20rpx;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 30rpx;
  background: #18212b;
}

.avatar--text {
  line-height: 108rpx;
  text-align: center;
  color: #f6f1e6;
  font-family: Georgia, serif;
  font-size: 44rpx;
  font-weight: 700;
}

.upload-btn {
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 10rpx;
  background: #ede5d5;
  color: #18212b;
  font-size: 23rpx;
  margin: 0;
}

.field {
  margin-bottom: 20rpx;
}

.label {
  display: block;
  color: #6b746f;
  font-size: 22rpx;
  margin-bottom: 10rpx;
}

.input,
.textarea {
  width: 100%;
  padding: 0 20rpx;
  border-radius: 10rpx;
  background: #f6f1e6;
  border: 1rpx solid #e4dccb;
  color: #18212b;
  font-size: 27rpx;
}

.input {
  height: 80rpx;
}

.textarea {
  min-height: 130rpx;
  padding-top: 18rpx;
  line-height: 1.6;
}

.placeholder {
  color: #a9aea7;
}

.readonly {
  display: block;
  color: #18212b;
  font-size: 25rpx;
  padding: 18rpx 0;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.code {
  flex: 1;
}

.code-btn {
  width: 190rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 10rpx;
  background: #ede5d5;
  color: #18212b;
  font-size: 22rpx;
  margin: 0;
}

.primary,
.ghost {
  width: 100%;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 12rpx;
  font-size: 26rpx;
  margin-top: 8rpx;
}

.primary {
  background: #18212b;
  color: #f6f1e6;
}

.ghost {
  background: #ede5d5;
  color: #18212b;
}

.hint {
  display: block;
  color: #8a918b;
  font-size: 22rpx;
  line-height: 1.6;
  margin-bottom: 18rpx;
}

.step {
  border-top: 1rpx solid #ede5d5;
  padding-top: 20rpx;
  margin-top: 20rpx;
}

.step-title {
  display: block;
  color: #c96a3d;
  font-size: 23rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.success {
  display: block;
  color: #2e6b66;
  font-size: 22rpx;
  margin-top: 12rpx;
}

.footer {
  display: block;
  text-align: center;
  color: #a9aea7;
  font-size: 21rpx;
  margin-top: 30rpx;
}
</style>
