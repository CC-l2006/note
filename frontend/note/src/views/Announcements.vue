<template>
  <!-- 系统公告页：只读列表，展示管理员发布的公告，停用的公告由后端过滤掉 -->
  <div class="page">
    <div class="page-title">
      <h1>系统公告</h1>
    </div>

    <p class="page-sub">管理员发布的公告。停用的公告不会出现在这里。</p>

    <!-- 加载态：用一条细线占位，避免内容出现前的跳动 -->
    <div v-if="loading" class="loading-rule" />

    <!-- 空状态：当前没有可展示的公告 -->
    <AppEmpty
      v-else-if="!items.length"
      title="暂时没有公告"
      description="有新的系统通知时会显示在这里。"
    />

    <!-- 公告列表：每条公告一张「纸」，包含标题、发布时间与正文 -->
    <ul v-else class="list">
      <li v-for="a in items" :key="a.id" class="notice sheet">
        <header class="notice__head">
          <h2 class="notice__title">{{ a.title }}</h2>
          <time class="notice__time">{{ formatDate(a.createdAt) }}</time>
        </header>
        <p class="notice__body">{{ a.content }}</p>
      </li>
    </ul>
  </div>
</template>

<script setup>
/**
 * Announcements.vue —— 系统公告页
 * 职责：进入页面时拉取管理员发布的公告，做只读展示。
 * 数据流：onMounted 请求一次列表 → 渲染列表 / 空状态 / 加载占位；失败只弹 toast。
 */
import { onMounted, ref } from 'vue'
import AppEmpty from '@/components/AppEmpty.vue'
import { announcementApi } from '@/api/index.js'
import { useToastStore } from '@/stores/toast.js'

/* ---------- 状态：公告列表与加载标记 ---------- */
const toast = useToastStore()
const items = ref([])
const loading = ref(false)

/* ---------- 工具方法 ---------- */
// 把后端返回的 ISO 时间串格式化成中文本地时间；空值返回空串，避免显示 Invalid Date
function formatDate(iso) {
  return iso ? new Date(iso).toLocaleString('zh-CN', { hour12: false }) : ''
}

/* ---------- 生命周期：进入页面时加载一次公告 ---------- */
// 错误处理：请求失败只在 toast 里提示，不影响页面其余部分；finally 保证加载态一定会收回
onMounted(async () => {
  loading.value = true
  try {
    const { data } = await announcementApi.list()
    items.value = data
  } catch (err) {
    toast.error(err.message)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* ---------- 页面留白 ---------- */
.page {
  padding: var(--sp-5) var(--sp-6);
}

/* ---------- 公告列表容器 ---------- */
.list {
  display: flex;
  flex-direction: column;
  gap: var(--sp-3);
  max-width: 760px;
}

/* ---------- 单条公告 ---------- */
.notice {
  padding: var(--sp-4) var(--sp-5);
}

/* 标题与时间一行排开 */
.notice__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--sp-3);
  padding-bottom: var(--sp-2);
  border-bottom: 1px solid var(--rule);
}

.notice__title {
  font-size: var(--fs-lg);
}

.notice__time {
  font-size: var(--fs-xs);
  color: var(--ink-3);
  white-space: nowrap;
}

/* ---------- 公告正文：保留换行，按段落原样展示 ---------- */
.notice__body {
  margin-top: var(--sp-3);
  font-family: var(--font-serif);
  font-size: var(--fs-base);
  line-height: 1.9;
  color: var(--ink-2);
  white-space: pre-wrap;
}
</style>
