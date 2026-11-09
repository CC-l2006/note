<template>
  <!-- 表单控件：label + 输入区 + 错误/提示，整体包在 label 里以扩大点击聚焦范围 -->
  <label class="field" :class="{ 'is-invalid': !!error }">
    <!-- 字段标签：带 required 时追加红色星号（仅视觉提示，不参与校验） -->
    <span v-if="label" class="field__label">
      {{ label }}
      <span v-if="required" class="field__req" aria-hidden="true">*</span>
    </span>

    <!-- 多行分支：type 为 textarea 时渲染文本域，支持自定义行数 -->
    <textarea
      v-if="type === 'textarea'"
      class="field__control field__control--area"
      :value="modelValue"
      :placeholder="placeholder"
      :rows="rows"
      :maxlength="maxlength"
      :disabled="disabled"
      :aria-invalid="!!error"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <!-- 单行分支：其余 type（text/password/email 等）走原生 input，回车即触发 submit -->
    <input
      v-else
      class="field__control"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :maxlength="maxlength"
      :disabled="disabled"
      :aria-invalid="!!error"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="$emit('submit')"
    />

    <!-- 辅助信息：错误优先于普通提示，两者不会同时出现 -->
    <span v-if="error" class="field__error">{{ error }}</span>
    <span v-else-if="hint" class="field__hint">{{ hint }}</span>
  </label>
</template>

<script setup>
/**
 * AppField —— 通用表单输入组件
 * 同时覆盖单行输入与多行文本，支持 v-model、占位符、字数限制、禁用、
 * 必填标记以及错误/提示两种辅助文案，校验逻辑由使用方负责。
 */

/** 组件 props：值绑定、文案、校验状态与可用性 */
defineProps({
  /** v-model 绑定值，组件只通过 update:modelValue 向外同步 */
  modelValue: { type: String, default: '' },
  /** 字段名，为空时不渲染标签行 */
  label: { type: String, default: '' },
  /** 控件类型，textarea 走多行分支，其余交给原生 input */
  type: { type: String, default: 'text' },
  /** 占位提示文字 */
  placeholder: { type: String, default: '' },
  /** 常规辅助说明，没有错误时展示 */
  hint: { type: String, default: '' },
  /** 错误文案，非空即进入 is-invalid 状态并展示红色边框 */
  error: { type: String, default: '' },
  /** 多行模式的行数 */
  rows: { type: Number, default: 4 },
  /** 最大字符数，undefined 表示不限制 */
  maxlength: { type: [Number, String], default: undefined },
  /** 禁用状态 */
  disabled: { type: Boolean, default: false },
  /** 是否显示必填星号 */
  required: { type: Boolean, default: false }
})

/** 组件事件：双向绑定更新与回车提交 */
defineEmits(['update:modelValue', 'submit'])
</script>

<style scoped>
/* ---------------- 布局 ---------------- */
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field__label {
  font-size: var(--fs-sm);
  color: var(--ink-2);
}

.field__req {
  color: var(--danger);
  margin-left: 2px;
}

/* ---------------- 控件外观 ---------------- */
.field__control {
  width: 100%;
  padding: 9px 12px;
  background: var(--sheet);
  border: 1px solid var(--rule-strong);
  border-radius: var(--r-sm);
  font-family: var(--font-sans);
  font-size: var(--fs-base);
  color: var(--ink);
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.field__control::placeholder {
  color: var(--ink-3);
}

/* ---------------- 交互状态：悬停 / 聚焦 / 禁用 ---------------- */
.field__control:hover:not(:disabled) {
  border-color: var(--ink-3);
}

.field__control:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.field__control:disabled {
  background: var(--paper-deep);
  color: var(--ink-3);
  cursor: not-allowed;
}

/* ---------------- 多行文本 ---------------- */
.field__control--area {
  resize: vertical;
  min-height: 84px;
  line-height: 1.7;
}

/* ---------------- 辅助文案 ---------------- */
.field__error {
  font-size: var(--fs-xs);
  color: var(--danger);
}

.field__hint {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}

/* ---------------- 校验失败态 ---------------- */
.is-invalid .field__control {
  border-color: var(--danger);
}

.is-invalid .field__control:focus {
  box-shadow: 0 0 0 3px var(--danger-soft);
}
</style>
