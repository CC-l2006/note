<!--
  Trash.vue —— 笔记回收站
  职责：
  1. 列出已删除的笔记（删除时间、标题），支持单篇恢复；
  2. 支持单篇永久删除与清空回收站，两者都不可逆，必须弹窗二次确认；
  3. 数据只来自后端回收站接口，页面不做本地缓存。
-->
<template>
  <div class="page">
    <!-- 页面标题与返回 / 清空入口 -->
    <div class="page-title">
      <h1>回收站</h1>
      <div class="row">
        <RouterLink to="/notes" class="back">← 返回笔记</RouterLink>
        <AppButton v-if="rows.length" variant="danger" size="sm" @click="showClear = true">
          清空回收站
        </AppButton>
      </div>
    </div>

    <p class="page-sub">
      删除的笔记会先进回收站，可以恢复。清空回收站后无法找回。
    </p>

    <!-- 回收站列表：表格自带加载态与「回收站是空的」空状态 -->
    <div class="stack">
      <AppTable
        :columns="columns"
        :rows="rows"
        :loading="loading"
        empty-title="回收站是空的"
        empty-description="被删除的笔记会出现在这里，可以随时恢复。"
      >
        <template #cell-title="{ row }">
          <span class="cell-title">{{ row.title }}</span>
        </template>
        <template #cell-updatedAt="{ row }">
          <span class="mono">{{ formatDate(row.updatedAt) }}</span>
        </template>
        <template #cell-actions="{ row }">
          <div class="row">
            <AppButton variant="quiet" size="sm" @click="restore(row)">恢复</AppButton>
            <AppButton variant="quiet" size="sm" class="danger-text" @click="askPurge(row)">
              永久删除
            </AppButton>
          </div>
        </template>
      </AppTable>
    </div>

    <!-- 永久删除单篇 -->
    <AppModal v-model="showPurge" title="永久删除这篇笔记？">
      <p class="confirm-text">
        《{{ target?.title }}》将被彻底删除，<strong>无法恢复</strong>。
      </p>
      <template #footer>
        <AppButton @click="showPurge = false">取消</AppButton>
        <AppButton variant="primary" :loading="working" @click="confirmPurge">确认删除</AppButton>
      </template>
    </AppModal>

    <!-- 清空回收站 -->
    <AppModal v-model="showClear" title="清空回收站？">
      <p class="confirm-text">
        回收站里的 <strong>{{ rows.length }}</strong> 篇笔记会被彻底删除，<strong>无法恢复</strong>。
      </p>
      <template #footer>
        <AppButton @click="showClear = false">取消</AppButton>
        <AppButton variant="primary" :loading="working" @click="confirmClear">确认清空</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
/*
 * 本文件的分块顺序：状态 → 表格列定义 → 方法 → 生命周期。
 * 错误处理约定：所有接口调用都 try / catch，失败只提示 toast，并保持当前列表不变。
 */
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppTable from '@/components/AppTable.vue'
import AppButton from '@/components/AppButton.vue'
import AppModal from '@/components/AppModal.vue'
import { noteApi } from '@/api/index.js'
import { useToastStore } from '@/stores/toast.js'

const toast = useToastStore()

/* 状态：列表数据、加载中、写操作进行中，以及两个确认弹窗的开关与目标 */
const rows = ref([])
const loading = ref(false)
const working = ref(false)
const showPurge = ref(false)
const showClear = ref(false)
const target = ref(null)

/* 表格列定义：标题、删除时间、操作三列 */
const columns = [
  { key: 'title', label: '标题' },
  { key: 'updatedAt', label: '删除时间', width: '160px' },
  { key: 'actions', label: '操作', width: '180px' }
]

/* 方法：拉取回收站列表 */
async function load() {
  loading.value = true
  try {
    const { data } = await noteApi.trash({ size: 100 })
    rows.value = data
  } catch (err) {
    toast.error(err.message)
  } finally {
    loading.value = false
  }
}

/* 方法：恢复单篇笔记，成功后刷新列表 */
async function restore(row) {
  try {
    await noteApi.restore(row.id)
    toast.success(`已恢复《${row.title}》`)
    await load()
  } catch (err) {
    toast.error(err.message)
  }
}

/* 方法：打开「永久删除单篇」确认弹窗，仅记录目标不直接删 */
function askPurge(row) {
  target.value = row
  showPurge.value = true
}

/* 方法：确认永久删除单篇 */
async function confirmPurge() {
  working.value = true
  try {
    await noteApi.remove(target.value.id, true)
    toast.success('已永久删除')
    showPurge.value = false
    await load()
  } catch (err) {
    toast.error(err.message)
  } finally {
    working.value = false
  }
}

/* 方法：确认清空整个回收站 */
async function confirmClear() {
  working.value = true
  try {
    await noteApi.clearTrash()
    toast.success('回收站已清空')
    showClear.value = false
    await load()
  } catch (err) {
    toast.error(err.message)
  } finally {
    working.value = false
  }
}

/* 小工具：把后端的时间字符串格式化成中文本地时间 */
function formatDate(iso) {
  return iso ? new Date(iso).toLocaleString('zh-CN', { hour12: false }) : '—'
}

/* 生命周期：进入页面即拉取回收站列表 */
onMounted(load)
</script>

<style scoped>
/* 页面容器与表头链接 */
.page {
  padding: var(--sp-5) var(--sp-6);
}

/* 表格单元格：标题用宋体，永久删除按钮用危险色 */
.cell-title {
  font-family: var(--font-serif);
  color: var(--ink);
}

.danger-text {
  color: var(--danger);
}

.back {
  font-size: var(--fs-sm);
  color: var(--ink-3);
}

/* 两个确认弹窗的正文排版 */
.confirm-text {
  font-size: var(--fs-sm);
  line-height: 1.8;
  color: var(--ink-2);
}

.confirm-text strong {
  color: var(--danger);
  font-weight: 600;
}
</style>
