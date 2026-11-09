<template>
  <view class="page">
    <view class="profile-head">
      <view class="avatar">{{ initial }}</view>
      <view class="who">
        <text class="name">{{ userStore.displayName }}</text>
        <text class="email">{{ userStore.userInfo?.email || '未绑定邮箱' }}</text>
        <text class="role">{{ roleText }}</text>
      </view>
    </view>

    <view class="quota-card">
      <view>
        <text class="quota-label">AI 额度</text>
        <text class="quota-value">{{ userStore.aiQuota }}</text>
      </view>
      <text class="quota-hint">每次总结消耗 1 次</text>
    </view>

    <view class="menu">
      <view class="menu-item" hover-class="menu-item--active" @click="goNotes">
        <text class="menu-title">我的笔记</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" hover-class="menu-item--active" @click="about">
        <text class="menu-title">关于 CipherWS Note</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item danger" hover-class="menu-item--active" @click="logout">
        <text class="menu-title">退出登录</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <text class="footer">uni-app 端 · v1.0.0</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../stores/user.js'
import { getToken } from '../../api/request.js'

const userStore = useUserStore()

const initial = computed(() => (userStore.displayName || 'N').slice(0, 1).toUpperCase())
const roleText = computed(() => (userStore.isAdmin ? '管理员' : '普通用户'))

onShow(async () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  try {
    await userStore.fetchProfile()
  } catch (error) {
    if (error.code === 401) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    uni.showToast({ title: error.message || '资料加载失败', icon: 'none' })
  }
})

function goNotes() {
  uni.switchTab({ url: '/pages/notes/notes' })
}

function about() {
  uni.showModal({
    title: 'CipherWS Note',
    content: '在线笔记 + AI 智能总结 + 后台管控\nuni-app 端 v1.0.0',
    showCancel: false
  })
}

function logout() {
  uni.showModal({
    title: '退出登录',
    content: '退出后需要重新登录才能查看笔记。',
    confirmText: '退出',
    confirmColor: '#C96A3D',
    success: async (res) => {
      if (!res.confirm) return
      await userStore.logout()
      uni.reLaunch({ url: '/pages/login/login' })
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 40rpx 32rpx 60rpx;
  background: #f6f1e6;
  max-width: 1080rpx;
  margin: 0 auto;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 18rpx 8rpx 36rpx;
}

.avatar {
  width: 116rpx;
  height: 116rpx;
  line-height: 116rpx;
  text-align: center;
  border-radius: 36rpx;
  background: #18212b;
  color: #f6f1e6;
  font-family: Georgia, serif;
  font-size: 48rpx;
  font-weight: 700;
}

.who {
  flex: 1;
}

.name {
  display: block;
  color: #18212b;
  font-size: 40rpx;
  font-weight: 700;
}

.email {
  display: block;
  color: #6b746f;
  font-size: 23rpx;
  margin-top: 6rpx;
}

.role {
  display: inline-block;
  color: #2e6b66;
  background: #e8efeb;
  border-radius: 8rpx;
  padding: 5rpx 12rpx;
  font-size: 20rpx;
  margin-top: 10rpx;
}

.quota-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 30rpx;
  border-radius: 18rpx;
  background: #18212b;
}

.quota-label {
  display: block;
  color: #a9b8b6;
  font-size: 22rpx;
}

.quota-value {
  display: block;
  color: #f6f1e6;
  font-family: Georgia, serif;
  font-size: 58rpx;
  font-weight: 700;
  line-height: 1.2;
}

.quota-hint {
  color: #8fa39f;
  font-size: 21rpx;
}

.menu {
  margin-top: 32rpx;
  border-radius: 18rpx;
  overflow: hidden;
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 26rpx;
  border-bottom: 1rpx solid #ede5d5;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item--active {
  background: #f1eadb;
}

.menu-title {
  color: #18212b;
  font-size: 27rpx;
}

.menu-arrow {
  color: #a9aea7;
  font-size: 34rpx;
}

.danger .menu-title {
  color: #c96a3d;
}

.footer {
  display: block;
  text-align: center;
  color: #a9aea7;
  font-size: 21rpx;
  margin-top: 36rpx;
}
</style>
