<!--
  UserList.vue —— 后台「用户管理」页
  职责：
  1. 分页列出全部账号，支持按账号 / 昵称 / 邮箱搜索；
  2. 启用或禁用账号（二次确认），禁用后该用户无法登录、已登录会话失效；
  3. 重置单个用户的 AI 额度，额度决定还能调用多少次 AI 总结。
-->
<template>
  <div class="page">
    <!-- 页面标题与刷新入口 -->
    <div class="page-title">
      <h1>用户管理</h1>
      <div class="row">
        <AppButton variant="ghost" size="sm" :loading="loading" @click="load">刷新</AppButton>
      </div>
    </div>

    <!-- 搜索区：按账号 / 昵称 / 邮箱查询 -->
    <form class="toolbar" @submit.prevent="onSearch">
      <div class="toolbar__search">
        <AppField v-model="keyword" placeholder="搜索账号 / 昵称 / 邮箱" />
      </div>
      <AppButton variant="primary" size="sm" type="submit">搜索</AppButton>
    </form>

    <!-- 用户表格：账号、昵称、邮箱、角色、笔记数、AI 额度、状态与操作 -->
    <div class="table-scroll">
      <AppTable :columns="columns" :rows="rows" :loading="loading">
        <template #cell-role="{ row }">
          <AppTag :tone="row.role === 'admin' ? 'accent' : 'neutral'">
            {{ row.role === 'admin' ? '管理员' : '普通用户' }}
          </AppTag>
        </template>

        <template #cell-enabled="{ row }">
          <AppTag :tone="row.enabled ? 'ok' : 'danger'">{{ row.enabled ? '启用' : '已禁用' }}</AppTag>
        </template>

        <template #cell-actions="{ row }">
          <div class="actions">
            <span v-if="isSelf(row)" class="small muted">当前账号</span>
            <AppButton v-else variant="quiet" size="sm" @click="openStatus(row)">
              {{ row.enabled ? '禁用' : '启用' }}
            </AppButton>
            <AppButton variant="quiet" size="sm" @click="openQuota(row)">重置额度</AppButton>
          </div>
        </template>
      </AppTable>
    </div>

    <!-- 分页：每页固定 10 条 -->
    <AppPager v-model:page="page" :total="totalCount" :size="10" />

    <!-- 启用 / 禁用：状态变更影响登录，必须二次确认 -->
    <AppModal v-model="statusModal" title="确认变更账号状态">
      <div v-if="statusTarget" class="stack">
        <p>
          确定要{{ statusTarget.enabled ? '禁用' : '启用' }}账号
          <strong>{{ statusTarget.username }}</strong> 吗？
        </p>
        <p v-if="statusTarget.enabled" class="small muted">
          被禁用用户无法登录，已登录的会话也会失效，直到重新启用。
        </p>
      </div>

      <template #footer>
        <AppButton variant="ghost" size="sm" @click="statusModal = false">取消</AppButton>
        <AppButton
          :variant="statusTarget?.enabled ? 'danger' : 'primary'"
          size="sm"
          :loading="statusSaving"
          @click="confirmStatus"
        >
          {{ statusTarget?.enabled ? '确认禁用' : '确认启用' }}
        </AppButton>
      </template>
    </AppModal>

    <!-- 重置 AI 额度弹窗：预填当前额度，保存前做范围校验 -->
    <AppModal v-model="quotaModal" title="重置 AI 额度">
      <div v-if="quotaTarget" class="stack">
        <p class="small muted">
          用户：{{ quotaTarget.nickname || quotaTarget.username }}，当前剩余
          {{ quotaTarget.aiQuota }} 次
        </p>
        <AppField
          v-model="quotaInput"
          label="新的 AI 额度"
          type="number"
          :error="quotaError"
          hint="取 0 - 9999 的整数，0 表示不能再调用 AI 总结"
        />
      </div>

      <template #footer>
        <AppButton variant="ghost" size="sm" @click="quotaModal = false">取消</AppButton>
        <AppButton variant="primary" size="sm" :loading="quotaSaving" @click="confirmQuota">
          保存
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
/*
 * 本文件的分块顺序：状态 → 表格列定义 → 方法 → 生命周期。
 * 错误处理约定：接口调用统一 try / catch，失败只提示 toast，弹窗保持打开便于重试。
 */
import { onMounted, ref, watch } from 'vue'
import AppButton from '@/components/AppButton.vue'
import AppField from '@/components/AppField.vue'
import AppModal from '@/components/AppModal.vue'
import AppPager from '@/components/AppPager.vue'
import AppTable from '@/components/AppTable.vue'
import AppTag from '@/components/AppTag.vue'
import { adminApi } from '@/api/index.js'
import { useToastStore } from '@/stores/toast.js'
import { useUserStore } from '@/stores/user.js'

const toast = useToastStore()
const userStore = useUserStore()

/* 表格列定义：账号、昵称、邮箱、角色、笔记数、AI 额度、状态、操作 */
const columns = [
  { key: 'username', label: '账号', width: '120px' },
  { key: 'nickname', label: '昵称', width: '120px' },
  { key: 'email', label: '邮箱', width: '180px' },
  { key: 'role', label: '角色', width: '96px' },
  { key: 'noteCount', label: '笔记数', width: '80px' },
  { key: 'aiQuota', label: 'AI 额度', width: '88px' },
  { key: 'enabled', label: '状态', width: '88px' },
  { key: 'actions', label: '操作', width: '150px' }
]

/* 筛选与分页状态：关键字、页码、当前页数据、总条数、加载中 */
const keyword = ref('')
const page = ref(1)
const rows = ref([])
const totalCount = ref(0)
const loading = ref(false)

/* 启用 / 禁用弹窗状态：开关、目标用户、提交中标记 */
const statusModal = ref(false)
const statusTarget = ref(null)
const statusSaving = ref(false)

/* 重置额度弹窗状态：开关、目标用户、输入值、校验错误、提交中标记 */
const quotaModal = ref(false)
const quotaTarget = ref(null)
const quotaInput = ref('')
const quotaError = ref('')
const quotaSaving = ref(false)

// 后端也会拦「禁用自己」，但界面上直接不给按钮，避免点了才报错
function isSelf(row) {
  return row.id === userStore.userInfo?.id
}

/* 方法：按关键字与页码拉取用户列表 */
async function load() {
  loading.value = true
  try {
    const { data, total } = await adminApi.users({
      keyword: keyword.value,
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

function onSearch() {
  // 回到第一页由 watch 负责拉取；已经在第一页时 watch 不会触发，需要手动拉
  if (page.value === 1) load()
  else page.value = 1
}

/* 方法：打开账号状态变更的确认弹窗 */
function openStatus(row) {
  statusTarget.value = row
  statusModal.value = true
}

/* 方法：确认启用 / 禁用，成功后刷新列表 */
async function confirmStatus() {
  const row = statusTarget.value
  if (!row) return
  statusSaving.value = true
  try {
    await adminApi.setUserStatus(row.id, !row.enabled)
    toast.success(row.enabled ? '已禁用该账号' : '已启用该账号')
    statusModal.value = false
    await load()
  } catch (err) {
    toast.error(err.message)
  } finally {
    statusSaving.value = false
  }
}

/* 方法：打开重置额度弹窗，预填该用户当前额度 */
function openQuota(row) {
  quotaTarget.value = row
  quotaInput.value = String(row.aiQuota ?? 0)
  quotaError.value = ''
  quotaModal.value = true
}

/* 方法：校验 0 - 9999 的整数后提交新额度 */
async function confirmQuota() {
  const row = quotaTarget.value
  if (!row) return
  const aiQuota = Number(quotaInput.value)
  if (!Number.isInteger(aiQuota) || aiQuota < 0 || aiQuota > 9999) {
    quotaError.value = '请输入 0 - 9999 之间的整数'
    return
  }

  quotaSaving.value = true
  try {
    await adminApi.setUserQuota(row.id, aiQuota)
    toast.success(`已将 ${row.username} 的 AI 额度重置为 ${aiQuota}`)
    quotaModal.value = false
    await load()
  } catch (err) {
    toast.error(err.message)
  } finally {
    quotaSaving.value = false
  }
}

/* 生命周期：翻页重新拉取，进入页面先读一次 */
watch(page, load)

onMounted(load)
</script>

<style scoped>
/* 页面容器：标题、搜索、表格、分页的纵向排布 */
.page {
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
}

/* 搜索工具条：输入框 + 搜索按钮 */
.toolbar {
  display: flex;
  align-items: flex-end;
  gap: var(--sp-2);
}

.toolbar__search {
  width: 280px;
  max-width: 100%;
}

/* 表格横向滚动容器：窄屏保持列宽可读 */
.table-scroll {
  overflow-x: auto;
}

/* 行内操作按钮：禁用 / 启用、重置额度 */
.actions {
  display: flex;
  align-items: center;
  gap: var(--sp-1);
  white-space: nowrap;
}

@media (max-width: 820px) {
  .toolbar {
    flex-wrap: wrap;
  }

  .toolbar__search {
    width: 100%;
  }

  /* 窄屏把表格撑到可读宽度，让它横向滚动而不是压成一列一列的字 */
  .table-scroll :deep(table) {
    min-width: 780px;
  }
}
</style>
