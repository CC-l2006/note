<template>
  <!-- 表格容器：统一圆角、边框与背景，负责裁剪内部表格的直角 -->
  <div class="table-wrap">
    <table class="table">
      <!-- 表头：列宽与对齐方式来自 columns 配置 -->
      <thead>
        <tr>
          <th
            v-for="col in columns"
            :key="col.key"
            :style="{ width: col.width, textAlign: col.align || 'left' }"
          >
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <!-- 表体：按 rowKey 取主键，逐行逐列渲染 -->
      <tbody>
        <tr v-for="row in rows" :key="row[rowKey]">
          <td
            v-for="col in columns"
            :key="col.key"
            :style="{ textAlign: col.align || 'left' }"
          >
            <!-- 每个单元格优先用 cell-<key> 插槽，否则直接输出值 -->
            <slot :name="`cell-${col.key}`" :row="row" :value="row[col.key]">
              {{ row[col.key] ?? '—' }}
            </slot>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 加载态：表格下方一条流动的墨线，避免整表闪烁 -->
    <div v-if="loading" class="loading-rule" />

    <!-- 空态：无数据且不在加载中时，复用 AppEmpty 并透传 empty 插槽作为操作区 -->
    <AppEmpty v-else-if="!rows.length" :title="emptyTitle" :description="emptyDescription">
      <template v-if="$slots.empty" #action>
        <slot name="empty" />
      </template>
    </AppEmpty>
  </div>
</template>

<script setup>
/**
 * AppTable —— 通用数据表格组件
 * 用 columns 描述列结构，单元格内容可通过 `cell-<key>` 插槽自定义，
 * 内置加载条与空状态，排序、分页等能力由外层页面组合。
 */

import AppEmpty from './AppEmpty.vue'

/** 组件 props：列定义、数据源、主键字段与各种状态文案 */
defineProps({
  /** 列配置数组：{ key, label, width?, align? } */
  columns: { type: Array, required: true },
  /** 行数据，默认空数组便于直接渲染空态 */
  rows: { type: Array, default: () => [] },
  /** 行主键字段名，用于 v-for 的 key */
  rowKey: { type: String, default: 'id' },
  /** 是否正在加载，为真时显示加载条而非空态 */
  loading: { type: Boolean, default: false },
  /** 空态标题 */
  emptyTitle: { type: String, default: '没有数据' },
  /** 空态描述 */
  emptyDescription: { type: String, default: '' }
})
</script>

<style scoped>
/* ---------------- 容器 ---------------- */
.table-wrap {
  border: 1px solid var(--rule);
  border-radius: var(--r-md);
  background: var(--sheet);
  overflow: hidden;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--fs-sm);
}

/* ---------------- 表头 ---------------- */
.table th {
  padding: 10px 14px;
  text-align: left;
  font-weight: 500;
  color: var(--ink-3);
  background: var(--paper-deep);
  border-bottom: 1px solid var(--rule);
  white-space: nowrap;
}

/* ---------------- 单元格 ---------------- */
.table td {
  padding: 11px 14px;
  border-bottom: 1px solid var(--rule);
  color: var(--ink-2);
  vertical-align: middle;
}

/* 最后一行不要双线 */
.table tbody tr:last-child td {
  border-bottom: 0;
}

/* 行悬停：用浅纸色提示当前行 */
.table tbody tr:hover td {
  background: var(--paper);
}
</style>
