<template>
  <view class="page">
    <view class="header">
      <view>
        <text class="kicker">ADMIN CONSOLE</text>
        <text class="title">管理后台</text>
        <text class="count">仅管理员可见 · 与 Web 端同一套接口</text>
      </view>
    </view>

    <scroll-view class="tabs" scroll-x>
      <text
        v-for="item in tabs"
        :key="item.key"
        class="tab"
        :class="{ active: tab === item.key }"
        @click="switchTab(item.key)"
      >
        {{ item.label }}
      </text>
    </scroll-view>

    <!-- 用户 -->
    <view v-if="tab === 'users'" class="section">
      <view class="section-head">
        <text class="section-title">用户</text>
        <text class="section-meta">{{ users.length }} 条</text>
      </view>
      <view v-for="user in users" :key="user.id" class="card">
        <view class="row">
          <view>
            <text class="card-title">{{ user.nickname || user.username }}</text>
            <text class="card-sub">{{ user.email }} · {{ user.role }} · {{ user.noteCount || 0 }} 篇</text>
          </view>
          <text class="badge" :class="{ off: !user.enabled }">{{ user.enabled ? '启用' : '禁用' }}</text>
        </view>
        <view class="actions">
          <button class="mini" @click="toggleUser(user)">{{ user.enabled ? '禁用' : '启用' }}</button>
          <button class="mini" @click="editQuota(user)">额度 {{ user.aiQuota ?? 0 }}</button>
        </view>
      </view>
    </view>

    <!-- 笔记巡检 -->
    <view v-if="tab === 'notes'" class="section">
      <view class="section-head">
        <text class="section-title">笔记巡检</text>
        <text class="section-meta">只标记，不改正文</text>
      </view>
      <view v-for="note in inspectedNotes" :key="note.id" class="card">
        <view class="row">
          <text class="card-title">{{ note.title || '无标题笔记' }}</text>
          <text class="badge" :class="{ off: !note.flagged }">{{ note.flagged ? '已标记' : '正常' }}</text>
        </view>
        <text class="card-sub">{{ note.authorName }} · {{ note.updatedAt }}</text>
        <text class="card-body">{{ snippet(note.content) }}</text>
        <button class="mini" @click="toggleFlag(note)">{{ note.flagged ? '取消标记' : '标记违规' }}</button>
      </view>
    </view>

    <!-- 公告 -->
    <view v-if="tab === 'announcements'" class="section">
      <view class="section-head">
        <text class="section-title">公告</text>
        <button class="mini primary-mini" @click="openAnnouncement()">新建</button>
      </view>
      <view v-for="item in announcements" :key="item.id" class="card">
        <view class="row">
          <text class="card-title">{{ item.title }}</text>
          <text class="badge" :class="{ off: !item.enabled }">{{ item.enabled ? '已发布' : '草稿' }}</text>
        </view>
        <text class="card-body">{{ item.content }}</text>
        <view class="actions">
          <button class="mini" @click="openAnnouncement(item)">编辑</button>
          <button class="mini" @click="removeAnnouncement(item)">删除</button>
        </view>
      </view>
    </view>

    <!-- AI 配置 -->
    <view v-if="tab === 'ai'" class="section">
      <view class="section-head">
        <text class="section-title">AI 参数</text>
        <text class="section-meta">密钥只显示掩码</text>
      </view>
      <view class="form-card">
        <view class="field">
          <text class="label">接口地址</text>
          <input v-model="aiForm.endpoint" class="input" type="text" placeholder="https://api.openai.com/v1" placeholder-class="placeholder" />
        </view>
        <view class="field">
          <text class="label">模型</text>
          <input v-model="aiForm.model" class="input" type="text" placeholder="gpt-4o-mini" placeholder-class="placeholder" />
        </view>
        <view class="field">
          <text class="label">单次最大字数</text>
          <input v-model="aiForm.maxChars" class="input" type="number" placeholder="1200" placeholder-class="placeholder" />
        </view>
        <view class="field">
          <text class="label">API Key（留空表示沿用旧密钥）</text>
          <input v-model="aiForm.apiKey" class="input" type="text" :placeholder="aiForm.apiKeyMasked || '未配置'" placeholder-class="placeholder" />
        </view>
        <button class="primary" :loading="savingAi" @click="saveAiConfig">保存配置</button>
      </view>
    </view>

    <!-- 日志 -->
    <view v-if="tab === 'logs'" class="section">
      <view class="section-head">
        <text class="section-title">操作日志</text>
        <text class="section-meta" @click="pickLogType">{{ logType || '全部类型' }} ›</text>
      </view>
      <view v-for="log in logs" :key="log.id" class="card">
        <view class="row">
          <text class="card-title">{{ log.type }}</text>
          <text class="card-sub">{{ log.createdAt }}</text>
        </view>
        <text class="card-body">{{ log.detail }}</text>
        <text class="card-sub">{{ log.actorName }} · {{ log.ip }}</text>
      </view>
    </view>

    <!-- 统计 -->
    <view v-if="tab === 'stats'" class="section">
      <view class="stat-grid">
        <view class="stat"><text class="stat-value">{{ stats.totals?.users || 0 }}</text><text class="stat-label">用户</text></view>
        <view class="stat"><text class="stat-value">{{ stats.totals?.notes || 0 }}</text><text class="stat-label">笔记</text></view>
        <view class="stat"><text class="stat-value">{{ stats.totals?.aiCalls || 0 }}</text><text class="stat-label">AI 调用</text></view>
        <view class="stat"><text class="stat-value">{{ stats.totals?.flaggedNotes || 0 }}</text><text class="stat-label">违规笔记</text></view>
      </view>
      <text class="section-title">近 7 天 AI 调用</text>
      <view v-for="item in stats.daily || []" :key="item.date" class="bar-row">
        <text class="bar-label">{{ item.date }}</text>
        <view class="bar-track"><view class="bar-fill" :style="{ width: barWidth(item.count) }" /></view>
        <text class="bar-value">{{ item.count }}</text>
      </view>
    </view>

    <view v-if="loading" class="loading">加载中…</view>

    <!-- 公告编辑 -->
    <view v-if="showAnnouncementForm" class="mask" @click="showAnnouncementForm = false">
      <view class="sheet" @click.stop>
        <text class="section-title">{{ announcementForm.id ? '编辑公告' : '新建公告' }}</text>
        <input v-model="announcementForm.title" class="input" type="text" placeholder="标题" placeholder-class="placeholder" />
        <textarea v-model="announcementForm.content" class="textarea" placeholder="内容" placeholder-class="placeholder" />
        <view class="switch-row" @click="announcementForm.enabled = !announcementForm.enabled">
          <text class="label">立即发布</text>
          <text class="badge" :class="{ off: !announcementForm.enabled }">{{ announcementForm.enabled ? '是' : '否' }}</text>
        </view>
        <view class="actions">
          <button class="mini" @click="showAnnouncementForm = false">取消</button>
          <button class="mini primary-mini" :loading="savingAnnouncement" @click="saveAnnouncement">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../stores/user.js'
import { getToken } from '../../api/request.js'
import { adminApi } from '../../api/index.js'

const userStore = useUserStore()
const tabs = [
  { key: 'users', label: '用户' },
  { key: 'notes', label: '巡检' },
  { key: 'announcements', label: '公告' },
  { key: 'ai', label: 'AI' },
  { key: 'logs', label: '日志' },
  { key: 'stats', label: '统计' }
]

const tab = ref('users')
const loading = ref(false)
const users = ref([])
const inspectedNotes = ref([])
const announcements = ref([])
const logs = ref([])
const stats = ref({ totals: {}, daily: [], topUsers: [] })
const logType = ref('')

const aiForm = reactive({ endpoint: '', model: '', maxChars: 1200, apiKey: '', apiKeyMasked: '' })
const savingAi = ref(false)

const showAnnouncementForm = ref(false)
const savingAnnouncement = ref(false)
const announcementForm = reactive({ id: null, title: '', content: '', enabled: true })

function snippet(value) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  return text.length > 120 ? `${text.slice(0, 120)}…` : text
}

function barWidth(count) {
  const max = Math.max(1, ...(stats.value.daily || []).map((item) => item.count))
  return `${Math.max(4, Math.round((count / max) * 100))}%`
}

async function loadTab() {
  loading.value = true
  try {
    if (tab.value === 'users') {
      const { data } = await adminApi.users({ page: 1, size: 50 })
      users.value = Array.isArray(data) ? data : []
    } else if (tab.value === 'notes') {
      const { data } = await adminApi.notes({ page: 1, size: 50 })
      inspectedNotes.value = Array.isArray(data) ? data : []
    } else if (tab.value === 'announcements') {
      const { data } = await adminApi.announcements({ page: 1, size: 50 })
      announcements.value = Array.isArray(data) ? data : []
    } else if (tab.value === 'ai') {
      const { data } = await adminApi.aiConfig()
      aiForm.endpoint = data.endpoint || ''
      aiForm.model = data.model || ''
      aiForm.maxChars = data.maxChars || 1200
      aiForm.apiKeyMasked = data.apiKeyMasked || ''
      aiForm.apiKey = ''
    } else if (tab.value === 'logs') {
      const { data } = await adminApi.logs({ page: 1, size: 50, type: logType.value || undefined })
      logs.value = Array.isArray(data) ? data : []
    } else if (tab.value === 'stats') {
      const { data } = await adminApi.stats()
      stats.value = data || { totals: {}, daily: [] }
    }
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  tab.value = key
  loadTab()
}

async function toggleUser(user) {
  try {
    await adminApi.setUserStatus(user.id, !user.enabled)
    user.enabled = !user.enabled
    uni.showToast({ title: '已更新', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  }
}

function editQuota(user) {
  uni.showModal({
    title: '设置 AI 额度',
    editable: true,
    placeholderText: String(user.aiQuota ?? 0),
    success: async (res) => {
      if (!res.confirm) return
      const quota = Number(res.content)
      if (!Number.isInteger(quota) || quota < 0 || quota > 9999) {
        uni.showToast({ title: '额度需在 0-9999', icon: 'none' })
        return
      }
      try {
        await adminApi.setUserQuota(user.id, quota)
        user.aiQuota = quota
        uni.showToast({ title: '额度已更新', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error.message || '更新失败', icon: 'none' })
      }
    }
  })
}

async function toggleFlag(note) {
  try {
    await adminApi.flagNote(note.id, !note.flagged)
    note.flagged = !note.flagged
    uni.showToast({ title: '已更新', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  }
}

function openAnnouncement(item) {
  announcementForm.id = item?.id || null
  announcementForm.title = item?.title || ''
  announcementForm.content = item?.content || ''
  announcementForm.enabled = item?.enabled ?? true
  showAnnouncementForm.value = true
}

async function saveAnnouncement() {
  if (!announcementForm.title.trim() || !announcementForm.content.trim()) {
    uni.showToast({ title: '请填写标题和内容', icon: 'none' })
    return
  }
  savingAnnouncement.value = true
  try {
    const body = {
      title: announcementForm.title.trim(),
      content: announcementForm.content.trim(),
      enabled: announcementForm.enabled
    }
    if (announcementForm.id) await adminApi.updateAnnouncement(announcementForm.id, body)
    else await adminApi.createAnnouncement(body)
    showAnnouncementForm.value = false
    uni.showToast({ title: '已保存', icon: 'success' })
    await loadTab()
  } catch (error) {
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  } finally {
    savingAnnouncement.value = false
  }
}

function removeAnnouncement(item) {
  uni.showModal({
    title: '删除公告',
    content: `确定删除《${item.title}》？`,
    confirmText: '删除',
    confirmColor: '#C96A3D',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await adminApi.removeAnnouncement(item.id)
        uni.showToast({ title: '已删除', icon: 'none' })
        await loadTab()
      } catch (error) {
        uni.showToast({ title: error.message || '删除失败', icon: 'none' })
      }
    }
  })
}

async function saveAiConfig() {
  savingAi.value = true
  try {
    await adminApi.saveAiConfig({
      endpoint: aiForm.endpoint.trim(),
      model: aiForm.model.trim(),
      maxChars: Number(aiForm.maxChars) || 1200,
      apiKey: aiForm.apiKey.trim() || null
    })
    uni.showToast({ title: '配置已保存', icon: 'success' })
    await loadTab()
  } catch (error) {
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  } finally {
    savingAi.value = false
  }
}

function pickLogType() {
  const types = ['全部类型', '登录', 'AI 总结', '禁用用户', '启用用户', '重置额度', '标记违规', '公告']
  uni.showActionSheet({
    itemList: types,
    success: (res) => {
      logType.value = res.tapIndex === 0 ? '' : types[res.tapIndex]
      loadTab()
    }
  })
}

onShow(async () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  if (!userStore.userInfo) {
    try {
      await userStore.fetchProfile()
    } catch {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
  }
  if (!userStore.isAdmin) {
    uni.showToast({ title: '仅管理员可访问', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 600)
    return
  }
  loadTab()
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
  margin-bottom: 20rpx;
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

.tabs {
  display: flex;
  white-space: nowrap;
  margin-bottom: 24rpx;
}

.tab {
  display: inline-block;
  padding: 14rpx 24rpx;
  margin-right: 12rpx;
  border-radius: 10rpx;
  background: #ede5d5;
  color: #6b746f;
  font-size: 23rpx;
}

.tab.active {
  background: #18212b;
  color: #f6f1e6;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  color: #18212b;
  font-size: 28rpx;
  font-weight: 700;
}

.section-meta {
  color: #8a918b;
  font-size: 22rpx;
}

.card,
.form-card {
  padding: 24rpx;
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  border-radius: 16rpx;
}

.row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.card-title {
  color: #18212b;
  font-size: 27rpx;
  font-weight: 700;
}

.card-sub {
  display: block;
  color: #8a918b;
  font-size: 21rpx;
  margin-top: 6rpx;
}

.card-body {
  display: block;
  color: #6b746f;
  font-size: 23rpx;
  line-height: 1.6;
  margin-top: 12rpx;
}

.badge {
  color: #2e6b66;
  background: #e8efeb;
  border-radius: 8rpx;
  padding: 4rpx 10rpx;
  font-size: 20rpx;
}

.badge.off {
  color: #c96a3d;
  background: #f3e3db;
}

.actions {
  display: flex;
  gap: 12rpx;
  margin-top: 16rpx;
}

.mini {
  height: 60rpx;
  line-height: 60rpx;
  padding: 0 20rpx;
  border-radius: 9rpx;
  background: #ede5d5;
  color: #18212b;
  font-size: 22rpx;
  margin: 0;
}

.primary-mini {
  background: #18212b;
  color: #f6f1e6;
}

.field {
  margin-bottom: 18rpx;
}

.label {
  display: block;
  color: #6b746f;
  font-size: 22rpx;
  margin-bottom: 8rpx;
}

.input,
.textarea {
  width: 100%;
  padding: 0 18rpx;
  border-radius: 10rpx;
  background: #f6f1e6;
  border: 1rpx solid #e4dccb;
  color: #18212b;
  font-size: 26rpx;
}

.input {
  height: 78rpx;
}

.textarea {
  min-height: 150rpx;
  padding-top: 16rpx;
}

.placeholder {
  color: #a9aea7;
}

.primary {
  width: 100%;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 12rpx;
  background: #18212b;
  color: #f6f1e6;
  font-size: 26rpx;
  margin-top: 8rpx;
}

.stat-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.stat {
  width: calc(50% - 7rpx);
  padding: 22rpx;
  border-radius: 14rpx;
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
}

.stat-value {
  display: block;
  color: #c96a3d;
  font-family: Georgia, serif;
  font-size: 46rpx;
  font-weight: 700;
}

.stat-label {
  display: block;
  color: #8a918b;
  font-size: 21rpx;
  margin-top: 4rpx;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.bar-label {
  width: 150rpx;
  color: #8a918b;
  font-size: 20rpx;
}

.bar-track {
  flex: 1;
  height: 12rpx;
  border-radius: 6rpx;
  background: #ede5d5;
  overflow: hidden;
}

.bar-fill {
  height: 12rpx;
  border-radius: 6rpx;
  background: #2e6b66;
}

.bar-value {
  width: 50rpx;
  text-align: right;
  color: #18212b;
  font-size: 20rpx;
}

.loading {
  padding: 30rpx 0;
  text-align: center;
  color: #a9aea7;
  font-size: 23rpx;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(16, 24, 32, 0.55);
  display: flex;
  align-items: flex-end;
}

.sheet {
  width: 100%;
  background: #fffdf8;
  border-radius: 26rpx 26rpx 0 0;
  padding: 36rpx 30rpx 50rpx;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}
</style>
