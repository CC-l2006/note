<!--
  Logs.vue —— 后台「操作日志」页
  职责：
  1. 按「用户操作 / 管理员操作」两类标签分页展示日志；
  2. 支持关键字（操作类型 / 详情 / 操作人）与起止日期筛选；
  3. 日志只读，页面不提供删除或编辑入口。
-->
<template>
  <div class="page">
    <!-- 页面标题与刷新入口 -->
    <div class="page-title">
      <h1>操作日志</h1>
      <div class="row">
        <AppButton variant="ghost" size="sm" :loading="loading" @click="load">刷新</AppButton>
      </div>
    </div>

    <p class="page-sub">日志只读，不可删除（需求 4.7.4）。</p>

    <!-- 日志类型切换：用户操作 / 管理员操作，右侧是当前类型的说明 -->
    <div class="tabs">
      <AppButton
        v-for="tab in TABS"
        :key="tab.value"
        :variant="type === tab.value ? 'primary' : 'ghost'"
        size="sm"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </AppButton>
      <span class="small muted tabs__hint">{{ currentTab.hint }}</span>
    </div>

    <!-- 筛选区：关键字 + 起止日期，日期互相约束上下限 -->
    <form class="filters" @submit.prevent="applyFilters">
      <div class="filters__search">
        <AppField v-model="keyword" placeholder="搜索操作类型 / 详情 / 操作人" />
      </div>

      <label class="date-field">
        <span class="date-field__label">起始</span>
        <input
          v-model="from"
          class="date-input"
          type="date"
          :max="to || undefined"
          @change="applyFilters"
        />
      </label>

      <span class="muted">至</span>

      <label class="date-field">
        <span class="date-field__label">结束</span>
        <input
          v-model="to"
          class="date-input"
          type="date"
          :min="from || undefined"
          @change="applyFilters"
        />
      </label>

      <AppButton variant="primary" size="sm" type="submit">查询</AppButton>
      <AppButton variant="ghost" size="sm" @click="resetFilters">重置</AppButton>
    </form>

    <!-- 日志表格：时间、操作人、类型、详情、IP，只读 -->
    <div class="table-scroll">
      <AppTable :columns="columns" :rows="rows" :loading="loading">
        <template #cell-type="{ row }">
          <AppTag :tone="row.type === 'admin' ? 'accent' : 'neutral'">
            {{ row.type === 'admin' ? '管理员操作' : '用户操作' }}
          </AppTag>
        </template>

        <template #cell-ip="{ row }">
          <span class="mono">{{ row.ip || '—' }}</span>
        </template>
      </AppTable>
    </div>

    <!-- 分页：每页固定 10 条 -->
    <AppPager v-model:page="page" :total="totalCount" :size="10" />
  </div>
</template>

<script setup>
/*
 * 本文件的分块顺序：常量 → 状态 → computed → 方法 → 生命周期。
 * 错误处理约定：接口调用统一 try / catch，失败只提示 toast，附表保持上次结果。
 */
import { computed, onMounted, ref, watch } from 'vue'
import AppButton from '@/components/AppButton.vue'
import AppField from '@/components/AppField.vue'
import AppPager from '@/components/AppPager.vue'
import AppTable from '@/components/AppTable.vue'
import AppTag from '@/components/AppTag.vue'
import { adminApi } from '@/api/index.js'
import { useToastStore } from '@/stores/toast.js'

const toast = useToastStore()

/* 常量：两类日志的标签与说明 */
const TABS = [
  { value: 'user', label: '用户操作日志', hint: '登录、新建 / 删除笔记、调用 AI 总结等用户行为' },
  { value: 'admin', label: '管理员操作日志', hint: '禁用用户、修改配置、巡检笔记等管理员操作' }
]

/* 表格列定义：时间、操作人、类型、详情、IP */
const columns = [
  { key: 'createdAt', label: '时间', width: '170px' },
  { key: 'actorName', label: '操作人', width: '120px' },
  { key: 'type', label: '操作类型', width: '130px' },
  { key: 'detail', label: '详情' },
  { key: 'ip', label: 'IP', width: '130px' }
]

/* 筛选状态：当前日志类型、关键字、起止日期 */
const type = ref('user')
const keyword = ref('')
const from = ref('')
const to = ref('')
/* 分页与列表状态：页码、当前页数据、总条数、加载中标记 */
const page = ref(1)
const rows = ref([])
const totalCount = ref(0)
const loading = ref(false)

/* computed：当前选中的标签对象，用于展示说明文字 */
const currentTab = computed(() => TABS.find((t) => t.value === type.value) || TABS[0])

/* 方法：按类型 / 关键字 / 日期与页码拉取日志 */
async function load() {
  loading.value = true
  try {
    const { data, total } = await adminApi.logs({
      type: type.value,
      keyword: keyword.value,
      from: from.value,
      to: to.value,
      page: page.value,
      size: 10
    })
    rows.value = data
    totalCount.value = total
  } catch (err) {
    toast.error(err.message)
  } finally {
    loading.value = false
  }
}

/** 筛选条件变了必须回第 1 页，否则可能停在一个已不存在的页码上 */
function applyFilters() {
  if (page.value === 1) load()
  else page.value = 1
}

/* 方法：清空全部筛选条件并重新查询 */
function resetFilters() {
  keyword.value = ''
  from.value = ''
  to.value = ''
  applyFilters()
}

/* 方法：切换日志类型，重复点击当前类型直接忽略 */
function switchTab(value) {
  if (type.value === value) return
  type.value = value
  applyFilters()
}

/* 生命周期：翻页重新拉取，进入页面先读一次 */
watch(page, load)

onMounted(load)
</script>

<style scoped>
/* 页面容器：标题、说明、标签、筛选、表格、分页自上而下排列 */
.page {
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
}

/* 类型切换行：按钮组 + 当前类型说明 */
.tabs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.tabs__hint {
  margin-left: var(--sp-2);
}

/* 筛选区：关键字与两个日期字段同一行，窄屏自动换行 */
.filters {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.filters__search {
  width: 260px;
  max-width: 100%;
}

.date-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.date-field__label {
  font-size: var(--fs-sm);
  color: var(--ink-2);
}

/* 原生 date 控件沿用 AppField 的纸墨样式，避免为它单独包一层组件 */
.date-input {
  padding: 8px 10px;
  background: var(--sheet);
  border: 1px solid var(--rule-strong);
  border-radius: var(--r-sm);
  font-family: var(--font-sans);
  font-size: var(--fs-base);
  color: var(--ink);
  transition: border-color 0.15s ease;
}

.date-input:hover {
  border-color: var(--ink-3);
}

.date-input:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

/* 表格横向滚动容器：窄屏保持列宽可读 */
.table-scroll {
  overflow-x: auto;
}

@media (max-width: 820px) {
  .filters__search {
    width: 100%;
  }

  .table-scroll :deep(table) {
    min-width: 860px;
  }
}
</style>
