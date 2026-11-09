<template>
  <view class="page">
    <!-- 顶部：标题 + 新建 -->
    <view class="header">
      <view>
        <text class="kicker">MY NOTES</text>
        <text class="title">我的笔记</text>
        <text class="count">{{ total }} 篇 · 只属于你</text>
      </view>
      <button class="new-btn" @click="createNote">＋</button>
    </view>

    <!-- 搜索 -->
    <view class="search">
      <input
        v-model="keyword"
        class="search-input"
        type="text"
        placeholder="搜索标题或正文"
        placeholder-class="placeholder"
        confirm-type="search"
        @confirm="reload"
      />
      <text class="search-btn" @click="reload">搜索</text>
    </view>

    <!-- 笔记列表 -->
    <view v-if="list.length" class="list">
      <view
        v-for="note in list"
        :key="note.id"
        class="note-card"
        hover-class="note-card--active"
        @click="openNote(note)"
        @longpress="removeNote(note)"
      >
        <view class="note-top">
          <text class="note-title">{{ note.title || '无标题笔记' }}</text>
          <text class="note-time">{{ formatTime(note.updatedAt) }}</text>
        </view>
        <text class="note-snippet">{{ snippet(note.content) }}</text>
        <view class="note-bottom">
          <view class="tags">
            <text v-for="tag in note.tags || []" :key="tag.id" class="tag">#{{ tag.name }}</text>
          </view>
          <text class="delete" @click.stop="removeNote(note)">删除</text>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else-if="!loading" class="empty">
      <text class="empty-icon">✎</text>
      <text class="empty-title">还没有笔记</text>
      <text class="empty-text">点右上角“＋”，写下第一条想法。</text>
      <button class="empty-btn" @click="createNote">新建笔记</button>
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="loading">
      <text>{{ list.length ? '加载中…' : '正在读取笔记…' }}</text>
    </view>
    <view v-else-if="list.length && finished" class="loading">
      <text>已经到底了</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { noteApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const keyword = ref('')
const loading = ref(false)
const finished = ref(false)

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function snippet(content) {
  const text = String(content || '').replace(/\s+/g, ' ').trim()
  if (!text) return '空白笔记'
  return text.length > 90 ? `${text.slice(0, 90)}…` : text
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
    const { data, total: count } = await noteApi.list({
      page: page.value,
      size,
      keyword: keyword.value.trim() || undefined
    })
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
    uni.stopPullDownRefresh()
  }
}

function reload() {
  load(true)
}

function createNote() {
  uni.navigateTo({ url: '/pages/notes/edit' })
}

function openNote(note) {
  uni.navigateTo({ url: `/pages/notes/edit?id=${note.id}` })
}

function removeNote(note) {
  uni.showModal({
    title: '删除笔记',
    content: `把《${note.title || '无标题笔记'}》放入回收站？`,
    confirmText: '删除',
    confirmColor: '#C96A3D',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await noteApi.remove(note.id)
        uni.showToast({ title: '已移入回收站', icon: 'none' })
        reload()
      } catch (error) {
        uni.showToast({ title: error.message || '删除失败', icon: 'none' })
      }
    }
  })
}

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  reload()
})

onPullDownRefresh(() => reload())
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
  align-items: flex-start;
  justify-content: space-between;
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
  font-size: 54rpx;
  font-weight: 700;
  line-height: 1.2;
}

.count {
  display: block;
  color: #8a918b;
  font-size: 23rpx;
  margin-top: 8rpx;
}

.new-btn {
  width: 82rpx;
  height: 82rpx;
  line-height: 76rpx;
  border-radius: 22rpx;
  background: #18212b;
  color: #f6f1e6;
  font-size: 44rpx;
  font-weight: 300;
  margin: 0;
  padding: 0;
}

.search {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 30rpx;
}

.search-input {
  flex: 1;
  height: 82rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  color: #18212b;
  font-size: 26rpx;
}

.placeholder {
  color: #a9aea7;
}

.search-btn {
  color: #c96a3d;
  font-size: 25rpx;
  font-weight: 600;
  padding: 18rpx 8rpx;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.note-card {
  padding: 26rpx 26rpx 22rpx;
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  border-radius: 18rpx;
}

.note-card--active {
  background: #f1eadb;
}

.note-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 18rpx;
}

.note-title {
  flex: 1;
  color: #18212b;
  font-size: 31rpx;
  font-weight: 700;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-time {
  color: #a9aea7;
  font-size: 21rpx;
  flex-shrink: 0;
}

.note-snippet {
  display: block;
  color: #6b746f;
  font-size: 25rpx;
  line-height: 1.6;
  margin-top: 14rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.note-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18rpx;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.tag {
  color: #2e6b66;
  background: #e8efeb;
  border-radius: 8rpx;
  padding: 5rpx 12rpx;
  font-size: 20rpx;
}

.delete {
  color: #c96a3d;
  font-size: 21rpx;
  padding: 6rpx 0 6rpx 20rpx;
}

.empty {
  padding: 110rpx 24rpx 80rpx;
  text-align: center;
}

.empty-icon {
  display: block;
  color: #c96a3d;
  font-size: 64rpx;
  margin-bottom: 18rpx;
}

.empty-title {
  display: block;
  color: #18212b;
  font-size: 34rpx;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.empty-text {
  display: block;
  color: #8a918b;
  font-size: 25rpx;
  line-height: 1.7;
}

.empty-btn {
  width: 260rpx;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 14rpx;
  background: #18212b;
  color: #f6f1e6;
  font-size: 26rpx;
  margin: 34rpx auto 0;
}

.loading {
  padding: 32rpx 0 8rpx;
  text-align: center;
  color: #a9aea7;
  font-size: 23rpx;
}
</style>
