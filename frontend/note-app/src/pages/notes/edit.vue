<template>
  <view class="page">
    <view class="meta">
      <text class="kicker">{{ id ? 'EDIT NOTE' : 'NEW NOTE' }}</text>
      <view class="mode-switch">
        <text class="mode" :class="{ active: mode === 'edit' }" @click="mode = 'edit'">编辑</text>
        <text class="mode" :class="{ active: mode === 'preview' }" @click="mode = 'preview'">预览</text>
      </view>
    </view>

    <input
      v-model="form.title"
      class="title-input"
      type="text"
      placeholder="无标题笔记"
      placeholder-class="placeholder-title"
      maxlength="200"
    />

    <textarea
      v-if="mode === 'edit'"
      v-model="form.content"
      class="content-input"
      placeholder="写下此刻的想法，支持 Markdown…"
      placeholder-class="placeholder"
      :maxlength="-1"
      :auto-height="true"
      :show-confirm-bar="false"
    />

    <view v-else class="preview-shell">
      <MdPreview :content="form.content" />
    </view>

    <!-- 标签 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">标签</text>
        <text class="section-hint">{{ selectedTags.length ? `已选 ${selectedTags.length} 个` : '可选' }}</text>
      </view>
      <view v-if="tags.length" class="tag-list">
        <text
          v-for="tag in tags"
          :key="tag.id"
          class="tag"
          :class="{ active: selectedTags.includes(tag.id) }"
          @click="toggleTag(tag.id)"
        >
          #{{ tag.name }}
        </text>
      </view>
      <text v-else class="empty-tags">还没有标签，可在“我的 → 标签管理”创建。</text>
    </view>

    <!-- 文件夹 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">文件夹</text>
        <text class="section-hint" @click="pickFolder">{{ folderLabel }} ›</text>
      </view>
    </view>

    <!-- AI 总结：仅编辑已有笔记时可用 -->
    <view v-if="id" class="ai-box">
      <view>
        <text class="ai-title">AI 总结</text>
        <text class="ai-desc">{{ aiDraft ? '已生成摘要，可编辑或替换正文' : '把长笔记压缩成一段摘要' }}</text>
      </view>
      <view class="ai-actions">
        <button class="ai-btn ghost-ai" @click="showHistory">历史</button>
        <button class="ai-btn" :loading="aiLoading" :disabled="aiLoading" @click="summarize">生成</button>
      </view>
    </view>

    <button class="save-btn" :loading="saving" :disabled="saving" @click="save">保存笔记</button>
    <text class="footer">CipherWS Note · {{ id ? '编辑' : '新建' }}</text>

    <!-- AI 摘要面板 -->
    <view v-if="aiOpen" class="ai-mask" @click="closeAi">
      <view class="ai-sheet" @click.stop>
        <view class="ai-sheet-head">
          <view>
            <text class="ai-sheet-kicker">AI SUMMARY</text>
            <text class="ai-sheet-title">AI 摘要</text>
          </view>
          <text class="ai-close" @click="closeAi">×</text>
        </view>

        <view class="ai-tabs">
          <text class="ai-tab" :class="{ active: aiTab === 'preview' }" @click="aiTab = 'preview'">预览</text>
          <text class="ai-tab" :class="{ active: aiTab === 'edit' }" @click="aiTab = 'edit'">编辑</text>
        </view>

        <scroll-view class="ai-sheet-body" scroll-y>
          <MdPreview v-if="aiTab === 'preview'" :content="aiDraft" />
          <textarea
            v-else
            v-model="aiDraft"
            class="ai-textarea"
            placeholder="摘要支持 Markdown，可直接编辑…"
            placeholder-class="placeholder"
            :maxlength="-1"
          />

          <view v-if="history.length" class="ai-history">
            <text class="ai-history-title">历史摘要</text>
            <view v-for="item in history" :key="item.id" class="history-item" @click="useHistory(item)">
              <text class="history-time">{{ formatTime(item.createdAt) }}</text>
              <text class="history-text">{{ item.summary }}</text>
            </view>
          </view>
        </scroll-view>

        <view class="ai-sheet-actions">
          <button class="ghost" @click="closeAi">关闭</button>
          <button class="primary" :disabled="!aiDraft" @click="replaceContent">替换正文</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { aiApi, folderApi, noteApi, tagApi } from '../../api/index.js'
import MdPreview from '../../components/MdPreview.vue'

const id = ref(null)
const tags = ref([])
const folders = ref([])
const selectedTags = ref([])
const folderId = ref(null)
const saving = ref(false)
const aiLoading = ref(false)
const mode = ref('edit')
const aiOpen = ref(false)
const aiTab = ref('preview')
const aiDraft = ref('')
const history = ref([])
const form = reactive({ title: '', content: '' })

const folderLabel = computed(() => {
  if (!folderId.value) return '未分类'
  return folders.value.find((item) => item.id === folderId.value)?.name || '未分类'
})

onLoad(async (options) => {
  id.value = options?.id ? Number(options.id) : null
  uni.setNavigationBarTitle({ title: id.value ? '编辑笔记' : '新建笔记' })

  await Promise.all([loadTags(), loadFolders()])
  if (id.value) await loadDetail()
})

async function loadTags() {
  try {
    const { data } = await tagApi.list()
    tags.value = Array.isArray(data) ? data : []
  } catch (error) {
    // 标签加载失败不阻断写笔记
    console.warn('[note-app] load tags failed', error)
  }
}

async function loadFolders() {
  try {
    const { data } = await folderApi.list()
    folders.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.warn('[note-app] load folders failed', error)
  }
}

function pickFolder() {
  if (!folders.value.length) {
    uni.showToast({ title: '还没有文件夹', icon: 'none' })
    return
  }
  uni.showActionSheet({
    itemList: ['未分类', ...folders.value.map((item) => item.name)],
    success: (res) => {
      folderId.value = res.tapIndex === 0 ? null : folders.value[res.tapIndex - 1].id
    }
  })
}

async function loadDetail() {
  try {
    const { data } = await noteApi.detail(id.value)
    form.title = data.title || ''
    form.content = data.content || ''
    selectedTags.value = (data.tags || []).map((tag) => tag.id)
    folderId.value = data.folderId || null
  } catch (error) {
    uni.showToast({ title: error.message || '笔记加载失败', icon: 'none' })
  }
}

function toggleTag(tagId) {
  const index = selectedTags.value.indexOf(tagId)
  if (index >= 0) selectedTags.value.splice(index, 1)
  else selectedTags.value.push(tagId)
}

async function save() {
  if (!form.title.trim() && !form.content.trim()) {
    uni.showToast({ title: '写点什么再保存', icon: 'none' })
    return
  }

  saving.value = true
  try {
    const body = {
      title: form.title.trim() || '无标题笔记',
      content: form.content,
      tagIds: selectedTags.value
    }
    let noteId = id.value
    if (noteId) {
      await noteApi.update(noteId, body)
    } else {
      const { data } = await noteApi.create(body)
      noteId = data?.id
    }
    if (noteId) await noteApi.move(noteId, folderId.value)

    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (error) {
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value).slice(0, 16)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function summarize() {
  aiLoading.value = true
  try {
    const { data } = await aiApi.summarize(id.value)
    aiDraft.value = data.summary || ''
    aiTab.value = 'preview'
    aiOpen.value = true
    await loadHistory(true)
  } catch (error) {
    uni.showToast({ title: error.message || 'AI 总结失败', icon: 'none' })
  } finally {
    aiLoading.value = false
  }
}

async function loadHistory(silent = false) {
  try {
    const { data } = await aiApi.history(id.value)
    history.value = Array.isArray(data) ? data : []
    if (!aiDraft.value && history.value[0]) aiDraft.value = history.value[0].summary
    if (!silent && !history.value.length) {
      uni.showToast({ title: '还没有总结历史', icon: 'none' })
    }
  } catch (error) {
    if (!silent) uni.showToast({ title: error.message || '历史加载失败', icon: 'none' })
  }
}

async function showHistory() {
  await loadHistory()
  aiTab.value = 'preview'
  aiOpen.value = true
}

function useHistory(item) {
  aiDraft.value = item.summary || ''
  aiTab.value = 'preview'
}

function closeAi() {
  aiOpen.value = false
}

function replaceContent() {
  if (!aiDraft.value.trim()) {
    uni.showToast({ title: '摘要为空', icon: 'none' })
    return
  }
  uni.showModal({
    title: '替换正文',
    content: form.content.trim()
      ? '当前正文会被 AI 摘要覆盖，确定替换？'
      : '用这段 AI 摘要作为正文？',
    confirmText: '替换',
    confirmColor: '#C96A3D',
    success: (res) => {
      if (!res.confirm) return
      form.content = aiDraft.value
      mode.value = 'edit'
      closeAi()
      uni.showToast({ title: '已替换，记得保存', icon: 'none' })
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 32rpx 32rpx 60rpx;
  background: #f6f1e6;
  max-width: 1080rpx;
  margin: 0 auto;
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.kicker {
  color: #c96a3d;
  font-size: 21rpx;
  letter-spacing: 2rpx;
}

.status {
  color: #a9aea7;
  font-size: 21rpx;
}

.mode-switch {
  display: flex;
  border: 1rpx solid #d8cfbe;
  border-radius: 9rpx;
  overflow: hidden;
}

.mode {
  padding: 8rpx 16rpx;
  color: #6b746f;
  font-size: 21rpx;
  background: #fffdf8;
}

.mode.active {
  background: #18212b;
  color: #f6f1e6;
}

.preview-shell {
  min-height: 460rpx;
  padding: 26rpx 24rpx;
  border-radius: 16rpx;
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
}

.md-preview {
  font-size: 29rpx;
  line-height: 1.85;
  color: #3f4a45;
  word-break: break-word;
}

/* H5 v-html 下的兜底样式；App rich-text 已在 markdown.js 写内联样式 */
.md-preview h1,
.md-preview h2,
.md-preview h3,
.md-preview h4 {
  color: #18212b;
  font-weight: 700;
}

.md-preview p {
  margin: 10rpx 0;
}

.md-preview blockquote {
  border-left: 6rpx solid #c96a3d;
  background: #f1eadb;
  padding: 16rpx 20rpx;
  margin: 16rpx 0;
}

.md-preview ul,
.md-preview ol {
  padding-left: 36rpx;
  margin: 10rpx 0;
}

.md-preview code {
  background: #ede5d5;
  padding: 2rpx 8rpx;
  border-radius: 6rpx;
}

.md-preview pre {
  background: #18212b;
  color: #f6f1e6;
  padding: 20rpx;
  border-radius: 10rpx;
  overflow-x: auto;
}

.md-preview a {
  color: #2e6b66;
}

.title-input {
  width: 100%;
  min-height: 92rpx;
  color: #18212b;
  font-size: 48rpx;
  font-weight: 700;
  line-height: 1.4;
  padding: 8rpx 0 16rpx;
}

.placeholder-title {
  color: #b9b3a7;
}

.content-input {
  width: 100%;
  min-height: 460rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: #fffdf8;
  border: 1rpx solid #d8cfbe;
  color: #18212b;
  font-size: 29rpx;
  line-height: 1.8;
}

.placeholder {
  color: #a9aea7;
}

.section {
  margin-top: 30rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14rpx;
}

.section-title {
  color: #18212b;
  font-size: 25rpx;
  font-weight: 700;
}

.section-hint {
  color: #a9aea7;
  font-size: 21rpx;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.tag {
  padding: 10rpx 18rpx;
  border-radius: 10rpx;
  background: #ede5d5;
  color: #6b746f;
  font-size: 23rpx;
}

.tag.active {
  background: #2e6b66;
  color: #fffdf8;
}

.empty-tags {
  color: #a9aea7;
  font-size: 22rpx;
}

.ai-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: #18212b;
}

.ai-title {
  display: block;
  color: #f6f1e6;
  font-size: 26rpx;
  font-weight: 700;
}

.ai-desc {
  display: block;
  color: #a9b8b6;
  font-size: 21rpx;
  margin-top: 6rpx;
}

.ai-btn {
  width: 132rpx;
  height: 62rpx;
  line-height: 62rpx;
  border-radius: 10rpx;
  background: #c96a3d;
  color: #fffdf8;
  font-size: 23rpx;
  margin: 0;
}

.ai-actions {
  display: flex;
  gap: 12rpx;
}

.ghost-ai {
  background: #22313a;
  color: #a9b8b6;
}

.save-btn {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 14rpx;
  background: #18212b;
  color: #f6f1e6;
  font-size: 30rpx;
  font-weight: 600;
  margin-top: 42rpx;
}

.footer {
  display: block;
  text-align: center;
  color: #a9aea7;
  font-size: 21rpx;
  margin-top: 28rpx;
}

/* AI 摘要面板 */
.ai-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 20;
  background: rgba(16, 24, 32, 0.58);
  display: flex;
  align-items: flex-end;
}

.ai-sheet {
  width: 100%;
  max-height: 88vh;
  background: #fffdf8;
  border-radius: 28rpx 28rpx 0 0;
  padding: 32rpx 30rpx 42rpx;
  display: flex;
  flex-direction: column;
}

.ai-sheet-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.ai-sheet-kicker {
  display: block;
  color: #c96a3d;
  font-size: 20rpx;
  letter-spacing: 2rpx;
  margin-bottom: 6rpx;
}

.ai-sheet-title {
  display: block;
  color: #18212b;
  font-size: 36rpx;
  font-weight: 700;
}

.ai-close {
  color: #8a918b;
  font-size: 44rpx;
  line-height: 1;
  padding: 0 6rpx;
}

.ai-tabs {
  display: flex;
  gap: 10rpx;
  padding: 6rpx;
  border-radius: 10rpx;
  background: #ede5d5;
  margin-bottom: 18rpx;
}

.ai-tab {
  flex: 1;
  text-align: center;
  padding: 12rpx 0;
  border-radius: 8rpx;
  color: #6b746f;
  font-size: 23rpx;
}

.ai-tab.active {
  background: #18212b;
  color: #f6f1e6;
}

.ai-sheet-body {
  height: 560rpx;
  padding: 22rpx;
  border-radius: 14rpx;
  background: #f6f1e6;
  border: 1rpx solid #e4dccb;
  box-sizing: border-box;
}

.ai-textarea {
  width: 100%;
  height: 500rpx;
  color: #18212b;
  font-size: 27rpx;
  line-height: 1.75;
}

.ai-history {
  margin-top: 26rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #d8cfbe;
}

.ai-history-title {
  display: block;
  color: #c96a3d;
  font-size: 22rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.history-item {
  padding: 14rpx 0;
  border-bottom: 1rpx solid #e4dccb;
}

.history-time {
  display: block;
  color: #a9aea7;
  font-size: 20rpx;
  margin-bottom: 4rpx;
}

.history-text {
  display: block;
  color: #6b746f;
  font-size: 23rpx;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.ai-sheet-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}

.ai-sheet-actions .ghost,
.ai-sheet-actions .primary {
  flex: 1;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 12rpx;
  font-size: 27rpx;
  margin: 0;
}

.ai-sheet-actions .ghost {
  background: #ede5d5;
  color: #18212b;
}

.ai-sheet-actions .primary {
  background: #18212b;
  color: #f6f1e6;
}
</style>
