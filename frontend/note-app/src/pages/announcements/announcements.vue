<template>
  <view class="page">
    <view class="header">
      <text class="kicker">ANNOUNCEMENTS</text>
      <text class="title">公告</text>
      <text class="count">来自管理员的系统消息</text>
    </view>

    <view v-if="list.length" class="list">
      <view v-for="item in list" :key="item.id" class="card">
        <text class="card-title">{{ item.title }}</text>
        <text class="card-time">{{ formatTime(item.createdAt) }}</text>
        <text class="card-content">{{ item.content }}</text>
      </view>
    </view>

    <view v-else-if="!loading" class="empty">
      <text class="empty-title">暂无公告</text>
      <text class="empty-text">管理员发布公告后会显示在这里。</text>
    </view>

    <view v-if="loading" class="loading">加载中…</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { announcementApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'

const list = ref([])
const loading = ref(false)

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value).slice(0, 10)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

async function load() {
  loading.value = true
  try {
    const { data } = await announcementApi.list()
    list.value = Array.isArray(data) ? data : []
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  load()
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
  margin-bottom: 28rpx;
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
  font-size: 52rpx;
  font-weight: 700;
}

.count {
  display: block;
  color: #8a918b;
  font-size: 23rpx;
  margin-top: 8rpx;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.card {
  padding: 28rpx 26rpx;
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  border-radius: 18rpx;
}

.card-title {
  display: block;
  color: #18212b;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1.4;
}

.card-time {
  display: block;
  color: #a9aea7;
  font-size: 21rpx;
  margin-top: 8rpx;
}

.card-content {
  display: block;
  color: #6b746f;
  font-size: 25rpx;
  line-height: 1.75;
  margin-top: 16rpx;
  white-space: pre-wrap;
}

.empty {
  padding: 140rpx 30rpx;
  text-align: center;
}

.empty-title {
  display: block;
  color: #18212b;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.empty-text {
  color: #8a918b;
  font-size: 24rpx;
}

.loading {
  padding: 32rpx 0;
  text-align: center;
  color: #a9aea7;
  font-size: 23rpx;
}
</style>
