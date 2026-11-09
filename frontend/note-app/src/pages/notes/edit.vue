<template>
  <view class="page">
    <view class="meta">
      <text class="kicker">{{ id ? 'EDIT NOTE' : 'NEW NOTE' }}</text>
      <text class="status">{{ saving ? '保存中…' : '仅自己可见' }}</text>
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
      v-model="form.content"
      class="content-input"
      placeholder="写下此刻的想法…"
      placeholder-class="placeholder"
      :maxlength="-1"
      :auto-height="true"
      :show-confirm-bar="false"
    />

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
      <text v-else class="empty-tags">还没有标签，可在 Web 端创建。</text>
    </view>

    <!-- AI 总结：仅编辑已有笔记时可用 -->
    <view v-if="id" class="ai-box">
      <view>
        <text class="ai-title">AI 总结</text>
        <text class="ai-desc">把长笔记压缩成一段摘要</text>
      </view>
      <button class="ai-btn" :loading="aiLoading" :disabled="aiLoading" @click="summarize">生成</button>
    </view>

    <button class="save-btn" :loading="saving" :disabled="saving" @click="save">保存笔记</button>
    <text class="footer">CipherWS Note · {{ id ? '编辑' : '新建' }}</text>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { aiApi, noteApi, tagApi } from '../../api/index.js'

const id = ref(null)
const tags = ref([])
const selectedTags = ref([])
const saving = ref(false)
const aiLoading = ref(false)
const form = reactive({ title: '', content: '' })

onLoad(async (options) => {
  id.value = options?.id ? Number(options.id) : null
  uni.setNavigationBarTitle({ title: id.value ? '编辑笔记' : '新建笔记' })

  await loadTags()
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

async function loadDetail() {
  try {
    const { data } = await noteApi.detail(id.value)
    form.title = data.title || ''
    form.content = data.content || ''
    selectedTags.value = (data.tags || []).map((tag) => tag.id)
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
    if (id.value) await noteApi.update(id.value, body)
    else await noteApi.create(body)

    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (error) {
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

async function summarize() {
  aiLoading.value = true
  try {
    const { data } = await aiApi.summarize(id.value)
    uni.showModal({
      title: 'AI 摘要',
      content: data.summary || '没有返回摘要',
      showCancel: false,
      confirmText: '知道了'
    })
  } catch (error) {
    uni.showToast({ title: error.message || 'AI 总结失败', icon: 'none' })
  } finally {
    aiLoading.value = false
  }
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
</style>
