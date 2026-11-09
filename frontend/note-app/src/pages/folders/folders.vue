<template>
  <view class="page">
    <view class="header">
      <view>
        <text class="kicker">FOLDERS</text>
        <text class="title">文件夹</text>
        <text class="count">{{ folders.length }} 个文件夹</text>
      </view>
      <button class="new-btn" @click="openCreate">＋</button>
    </view>

    <view class="actions">
      <button class="action" :loading="organizing" @click="autoOrganize">按同名标签自动整理</button>
    </view>

    <view v-if="folders.length" class="list">
      <view v-for="folder in folders" :key="folder.id" class="folder-row" @click="openEdit(folder)">
        <view>
          <text class="folder-name">{{ folder.name }}</text>
          <text class="folder-count">{{ folder.noteCount || 0 }} 篇笔记</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view v-else-if="!loading" class="empty">
      <text class="empty-title">还没有文件夹</text>
      <text class="empty-text">文件夹是单层结构；同名标签可以一键自动整理。</text>
    </view>

    <!-- 新建/重命名 -->
    <view v-if="showForm" class="mask" @click="closeForm">
      <view class="sheet" @click.stop>
        <text class="sheet-title">{{ editingId ? '重命名文件夹' : '新建文件夹' }}</text>
        <input v-model="name" class="input" type="text" placeholder="文件夹名，例如：毕业设计" placeholder-class="placeholder" />
        <view class="sheet-actions">
          <button v-if="editingId" class="danger-btn" @click="removeFolder">删除</button>
          <button class="ghost" @click="closeForm">取消</button>
          <button class="primary" :loading="saving" @click="save">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { folderApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'

const folders = ref([])
const loading = ref(false)
const organizing = ref(false)
const showForm = ref(false)
const editingId = ref(null)
const name = ref('')
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const { data } = await folderApi.list()
    folders.value = Array.isArray(data) ? data : []
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  name.value = ''
  showForm.value = true
}

function openEdit(folder) {
  editingId.value = folder.id
  name.value = folder.name
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

async function save() {
  const value = name.value.trim()
  if (!value) {
    uni.showToast({ title: '请输入文件夹名', icon: 'none' })
    return
  }
  saving.value = true
  try {
    if (editingId.value) await folderApi.update(editingId.value, { name: value })
    else await folderApi.create({ name: value })
    uni.showToast({ title: '已保存', icon: 'success' })
    closeForm()
    await load()
  } catch (error) {
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function removeFolder() {
  uni.showModal({
    title: '删除文件夹',
    content: '删除后，夹内笔记会回到未分类，确定继续？',
    confirmText: '删除',
    confirmColor: '#C96A3D',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await folderApi.remove(editingId.value)
        uni.showToast({ title: '已删除', icon: 'none' })
        closeForm()
        await load()
      } catch (error) {
        uni.showToast({ title: error.message || '删除失败', icon: 'none' })
      }
    }
  })
}

async function autoOrganize() {
  organizing.value = true
  try {
    const { data } = await folderApi.autoOrganize()
    uni.showToast({ title: `已整理 ${data || 0} 篇`, icon: 'none' })
    await load()
  } catch (error) {
    uni.showToast({ title: error.message || '自动整理失败', icon: 'none' })
  } finally {
    organizing.value = false
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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
  font-size: 52rpx;
  font-weight: 700;
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
  margin: 0;
  padding: 0;
}

.actions {
  margin-bottom: 24rpx;
}

.action {
  height: 68rpx;
  line-height: 68rpx;
  border-radius: 10rpx;
  background: #ede5d5;
  color: #18212b;
  font-size: 23rpx;
  margin: 0;
}

.list {
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  border-radius: 18rpx;
  overflow: hidden;
}

.folder-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 26rpx;
  border-bottom: 1rpx solid #ede5d5;
}

.folder-row:last-child {
  border-bottom: none;
}

.folder-name {
  display: block;
  color: #18212b;
  font-size: 29rpx;
  font-weight: 600;
}

.folder-count {
  display: block;
  color: #8a918b;
  font-size: 21rpx;
  margin-top: 6rpx;
}

.arrow {
  color: #a9aea7;
  font-size: 34rpx;
}

.empty {
  padding: 120rpx 30rpx;
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

.mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(16, 24, 32, 0.55);
  display: flex;
  align-items: flex-end;
}

.sheet {
  width: 100%;
  background: #fffdf8;
  border-radius: 26rpx 26rpx 0 0;
  padding: 40rpx 32rpx 52rpx;
}

.sheet-title {
  display: block;
  color: #18212b;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 26rpx;
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

.sheet-actions {
  display: flex;
  gap: 18rpx;
  margin-top: 26rpx;
}

.ghost,
.primary {
  flex: 1;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 12rpx;
  font-size: 27rpx;
  margin: 0;
}

.danger-btn {
  flex: 0 0 120rpx;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 12rpx;
  background: #f3e3db;
  color: #c96a3d;
  font-size: 25rpx;
  margin: 0;
}

.ghost {
  background: #ede5d5;
  color: #18212b;
}

.primary {
  background: #18212b;
  color: #f6f1e6;
}
</style>
