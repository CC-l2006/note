<template>
  <view class="page">
    <view class="header">
      <view>
        <text class="kicker">TRASH</text>
        <text class="title">回收站</text>
        <text class="count">{{ total }} 篇 · 可恢复</text>
      </view>
      <text v-if="list.length" class="clear" @click="clearAll">清空</text>
    </view>

    <view v-if="list.length" class="list">
      <view v-for="note in list" :key="note.id" class="note-card">
        <text class="note-title">{{ note.title || '无标题笔记' }}</text>
        <text class="note-time">{{ formatTime(note.updatedAt) }}</text>
        <text class="note-snippet">{{ snippet(note.content) }}</text>
        <view class="actions">
          <button class="action restore" @click="restore(note)">恢复</button>
          <button class="action danger" @click="remove(note)">永久删除</button>
        </view>
      </view>
    </view>

    <view v-else-if="!loading" class="empty">
      <text class="empty-title">回收站是空的</text>
      <text class="empty-text">删除笔记会先到这里，恢复或永久删除都由你决定。</text>
    </view>

    <view v-if="loading" class="loading">加载中…</view>
    <view v-else-if="list.length && finished" class="loading">已经到底了</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import { noteApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const finished = ref(false)

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value).slice(0, 10)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function snippet(content) {
  const text = String(content || '').replace(/\s+/g, ' ').trim()
  return text.length > 80 ? `${text.slice(0, 80)}…` : text || '空白笔记'
}

async function load(reset = false) {
  if (loading.value) return
  if (!reset && finished.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const { data, total: count } = await noteApi.trash({ page: page.value, size })
    const rows = Array.isArray(data) ? data : []
    list.value = reset ? rows : [...list.value, ...rows]
    total.value = Number(count) || list.value.length
    finished.value = rows.length < size || list.value.length >= total.value
    page.value += 1
  } catch (error) {
    if (error.code === 401) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function restore(note) {
  uni.showModal({
    title: '恢复笔记',
    content: `把《${note.title || '无标题笔记'}》恢复到笔记列表？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await noteApi.restore(note.id)
        uni.showToast({ title: '已恢复', icon: 'success' })
        await load(true)
      } catch (error) {
        uni.showToast({ title: error.message || '恢复失败', icon: 'none' })
      }
    }
  })
}

function remove(note) {
  uni.showModal({
    title: '永久删除',
    content: '永久删除后无法恢复，确定继续？',
    confirmText: '删除',
    confirmColor: '#C96A3D',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await noteApi.remove(note.id, true)
        uni.showToast({ title: '已永久删除', icon: 'none' })
        await load(true)
      } catch (error) {
        uni.showToast({ title: error.message || '删除失败', icon: 'none' })
      }
    }
  })
}

function clearAll() {
  uni.showModal({
    title: '清空回收站',
    content: '将永久删除回收站内全部笔记，确定继续？',
    confirmText: '清空',
    confirmColor: '#C96A3D',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await noteApi.clearTrash()
        uni.showToast({ title: '已清空', icon: 'success' })
        list.value = []
        total.value = 0
      } catch (error) {
        uni.showToast({ title: error.message || '清空失败', icon: 'none' })
      }
    }
  })
}

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  load(true)
})

onReachBottom(() => load(false))
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.clear {
  color: #c96a3d;
  font-size: 24rpx;
  padding: 12rpx 0;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.note-card {
  padding: 26rpx;
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  border-radius: 18rpx;
}

.note-title {
  color: #18212b;
  font-size: 30rpx;
  font-weight: 700;
}

.note-time {
  float: right;
  color: #a9aea7;
  font-size: 21rpx;
}

.note-snippet {
  display: block;
  color: #6b746f;
  font-size: 24rpx;
  line-height: 1.6;
  margin-top: 12rpx;
}

.actions {
  display: flex;
  gap: 14rpx;
  margin-top: 18rpx;
}

.action {
  flex: 1;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 10rpx;
  font-size: 23rpx;
  margin: 0;
}

.restore {
  background: #e8efeb;
  color: #2e6b66;
}

.danger {
  background: #f3e3db;
  color: #c96a3d;
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
  line-height: 1.7;
}

.loading {
  padding: 32rpx 0;
  text-align: center;
  color: #a9aea7;
  font-size: 23rpx;
}
</style>
