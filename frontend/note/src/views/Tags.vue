<template>
  <!-- 标签管理页：列出当前用户的私有标签，支持新建、重命名与删除 -->
  <div class="page">
    <div class="page-title">
      <h1>标签</h1>
      <AppButton variant="primary" size="sm" @click="openCreate">新建标签</AppButton>
    </div>

    <p class="page-sub">
      标签属于你的私有分类，不同用户的同名标签互不影响。删除标签不会删除笔记，只解除关联。
    </p>

    <!-- 内容区：加载占位 / 空状态 / 标签列表，三者互斥 -->
    <div class="stack">
      <div v-if="loading" class="loading-rule" />

      <!-- 空状态：还没有标签，顺带给一个新建入口 -->
      <AppEmpty
        v-else-if="!tags.length"
        title="还没有标签"
        description="建几个标签（比如「池禾笔记」「读书」），写笔记时就能一键归类。"
      >
        <template #action>
          <AppButton variant="primary" @click="openCreate">新建标签</AppButton>
        </template>
      </AppEmpty>

      <!-- 标签列表：每行显示名称、关联笔记数与行内操作 -->
      <ul v-else class="tag-list">
        <li v-for="t in tags" :key="t.id" class="tag-row">
          <span class="tag-row__name">{{ t.name }}</span>
          <span class="tag-row__count">{{ t.noteCount }} 篇笔记</span>
          <span class="tag-row__actions">
            <AppButton variant="quiet" size="sm" @click="openRename(t)">重命名</AppButton>
            <AppButton variant="quiet" size="sm" class="danger-text" @click="askRemove(t)">
              删除
            </AppButton>
          </span>
        </li>
      </ul>
    </div>

    <!-- 新建 / 重命名弹窗：两种模式共用同一套表单，靠 editing 是否有值区分 -->
    <AppModal v-model="showForm" :title="editing ? '重命名标签' : '新建标签'">
      <AppField
        v-model="form.name"
        label="标签名"
        placeholder="最多 12 个字符"
        :maxlength="12"
        :error="formError"
        required
        @submit="submit"
      />
      <template #footer>
        <AppButton @click="showForm = false">取消</AppButton>
        <AppButton variant="primary" :loading="working" @click="submit">
          {{ editing ? '保存' : '创建' }}
        </AppButton>
      </template>
    </AppModal>

    <!-- 删除确认弹窗：明确告知只解除关联，笔记本身不会被删 -->
    <AppModal v-model="showRemove" title="删除这个标签？">
      <p class="confirm-text">
        删除「{{ target?.name }}」后，它和 {{ target?.noteCount }} 篇笔记的关联会解除，
        但<strong>笔记本身不会被删除</strong>。
      </p>
      <template #footer>
        <AppButton @click="showRemove = false">取消</AppButton>
        <AppButton variant="primary" :loading="working" @click="confirmRemove">确认删除</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
/**
 * Tags.vue —— 标签管理页
 * 职责：展示当前用户的私有标签，并提供新建、重命名、删除三种操作。
 * 说明：删除标签只解除与笔记的关联，不会删除笔记本身。
 */
import { onMounted, reactive, ref } from 'vue'
import AppButton from '@/components/AppButton.vue'
import AppField from '@/components/AppField.vue'
import AppModal from '@/components/AppModal.vue'
import AppEmpty from '@/components/AppEmpty.vue'
import { tagApi } from '@/api/index.js'
import { useToastStore } from '@/stores/toast.js'

/* ---------- 依赖与列表状态 ---------- */
const toast = useToastStore()

// 标签列表、首屏加载标记、任意操作进行中的标记
const tags = ref([])
const loading = ref(false)
const working = ref(false)

/* ---------- 弹窗与表单状态 ---------- */
// 两个弹窗的开关、正在重命名的标签、待删除的标签、表单值与校验错误
const showForm = ref(false)
const showRemove = ref(false)
const editing = ref(null)
const target = ref(null)
const form = reactive({ name: '' })
const formError = ref('')

/* ---------- 方法：数据加载 ---------- */
// 拉取标签列表；失败只弹 toast，finally 保证加载态收回
async function load() {
  loading.value = true
  try {
    const { data } = await tagApi.list()
    tags.value = data
  } catch (err) {
    toast.error(err.message)
  } finally {
    loading.value = false
  }
}

/* ---------- 方法：弹窗控制 ---------- */
// 打开新建弹窗：清掉 editing 与表单残留，确保标题与提交行为都走「新建」分支
function openCreate() {
  editing.value = null
  form.name = ''
  formError.value = ''
  showForm.value = true
}

// 打开重命名弹窗：带上当前标签名，editing 非空即代表走「更新」分支
function openRename(tag) {
  editing.value = tag
  form.name = tag.name
  formError.value = ''
  showForm.value = true
}

/* ---------- 方法：提交与删除 ---------- */
// 提交表单：名称非空才发请求，按 editing 决定新建还是重命名，成功后关弹窗并刷新列表
async function submit() {
  const name = form.name.trim()
  if (!name) {
    formError.value = '标签名不能为空'
    return
  }
  working.value = true
  try {
    if (editing.value) {
      await tagApi.update(editing.value.id, name)
      toast.success('标签已重命名')
    } else {
      await tagApi.create(name)
      toast.success('标签已创建')
    }
    showForm.value = false
    await load()
  } catch (err) {
    formError.value = err.message
  } finally {
    working.value = false
  }
}

// 删除入口：先记下目标标签并弹确认框，避免误点直接删除
function askRemove(tag) {
  target.value = tag
  showRemove.value = true
}

// 确认删除：成功后关闭弹窗并重新拉取列表，保证笔记数等数据同步
async function confirmRemove() {
  working.value = true
  try {
    await tagApi.remove(target.value.id)
    toast.success('标签已删除')
    showRemove.value = false
    await load()
  } catch (err) {
    toast.error(err.message)
  } finally {
    working.value = false
  }
}

/* ---------- 生命周期：进入页面时加载标签列表 ---------- */
onMounted(load)
</script>

<style scoped>
/* ---------- 页面留白 ---------- */
.page {
  padding: var(--sp-5) var(--sp-6);
}

/* ---------- 列表容器：整体一张表，行间用分隔线 ---------- */
.tag-list {
  border: 1px solid var(--rule);
  border-radius: var(--r-md);
  background: var(--sheet);
  overflow: hidden;
}

/* ---------- 单行布局：名称 / 笔记数 / 操作按钮 ---------- */
.tag-row {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: 11px var(--sp-4);
  border-bottom: 1px solid var(--rule);
}

.tag-row:last-child {
  border-bottom: 0;
}

.tag-row:hover {
  background: var(--paper);
}

.tag-row__name {
  font-family: var(--font-serif);
  font-size: var(--fs-base);
  color: var(--ink);
}

.tag-row__count {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}

/* ---------- 行内操作：靠右对齐 ---------- */
.tag-row__actions {
  margin-left: auto;
  display: flex;
  gap: var(--sp-1);
}

.danger-text {
  color: var(--danger);
}

/* ---------- 删除确认弹窗文案 ---------- */
.confirm-text {
  font-size: var(--fs-sm);
  line-height: 1.8;
  color: var(--ink-2);
}

.confirm-text strong {
  color: var(--ink);
}
</style>
