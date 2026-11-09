<template>
  <!-- 找回密码页：用注册邮箱收验证码，验证后直接设置新密码，成功回登录页 -->
  <AuthLayout title="找回密码" subtitle="输入注册邮箱获取验证码，验证后可设置新密码。">
    <!-- 重置密码表单：邮箱、验证码、新密码、确认新密码 -->
    <form class="form" @submit.prevent="onSubmit">
      <AppField
        v-model="form.email"
        label="注册邮箱"
        type="email"
        placeholder="请输入注册时填写的邮箱"
        :error="errors.email"
        required
        @submit="onSubmit"
      />

      <!-- 验证码输入 + 获取按钮并排，倒计时期间按钮禁用 -->
      <div class="code-row">
        <AppField
          v-model="form.code"
          label="验证码"
          placeholder="6 位数字"
          :error="errors.code"
          required
          @submit="onSubmit"
        />
        <AppButton
          variant="ghost"
          size="md"
          class="code-btn"
          :loading="sending"
          :disabled="countdown > 0"
          @click="onSendCode"
        >
          {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
        </AppButton>
      </div>

      <AppField
        v-model="form.password"
        label="新密码"
        type="password"
        placeholder="至少 6 位"
        :error="errors.password"
        required
        @submit="onSubmit"
      />
      <AppField
        v-model="form.confirm"
        label="确认新密码"
        type="password"
        placeholder="再输入一次新密码"
        :error="errors.confirm"
        required
        @submit="onSubmit"
      />

      <!-- 提示与错误：hint 是验证码发送成功提示，formError 是发送或重置失败的原因 -->
      <p v-if="hint" class="hint">{{ hint }}</p>
      <p v-if="formError" class="form__error" role="alert">{{ formError }}</p>

      <AppButton type="submit" variant="primary" size="lg" block :loading="loading">
        重置密码
      </AppButton>
    </form>

    <!-- 底部链接：返回登录页 -->
    <template #footer>
      想起来了？<RouterLink to="/login">返回登录</RouterLink>
    </template>
  </AuthLayout>
</template>

<script setup>
/**
 * ForgotPassword.vue —— 找回密码页
 * 职责：向注册邮箱发送验证码，验证后提交新密码完成重置，成功则跳回登录页。
 * 错误处理：字段级错误就近显示，验证码发送失败与重置失败共用表单级错误位。
 */
import { reactive, ref, onBeforeUnmount } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import AppField from '@/components/AppField.vue'
import AppButton from '@/components/AppButton.vue'
import { authApi } from '@/api/index.js'
import { useToastStore } from '@/stores/toast.js'

/* ---------- 依赖与全局状态 ---------- */
const router = useRouter()
const toast = useToastStore()

/* ---------- 表单状态 ---------- */
// 表单值、字段级校验错误、表单级错误、发送成功提示
const form = reactive({ email: '', code: '', password: '', confirm: '' })
const errors = reactive({ email: '', code: '', password: '', confirm: '' })
const formError = ref('')
const hint = ref('')
// 提交中、发送中标记
const loading = ref(false)
const sending = ref(false)

/* ---------- 倒计时状态 ---------- */
const countdown = ref(0)
let timer = null

// 统一校验：邮箱格式、6 位验证码、新密码长度、两次输入一致
function validate() {
  errors.email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()) ? '' : '邮箱格式不正确'
  errors.code = /^\d{6}$/.test(form.code.trim()) ? '' : '请输入 6 位数字验证码'
  errors.password = form.password.length >= 6 ? '' : '新密码至少 6 位'
  // 密码输两次：避免手滑打错，重置完就再也登不进去了
  errors.confirm = form.confirm === form.password ? '' : '两次输入的新密码不一致'
  return Object.values(errors).every((v) => !v)
}

// 请求验证码：先确认邮箱格式，再启动 60 秒重发倒计时
async function onSendCode() {
  if (countdown.value > 0 || sending.value) return
  formError.value = ''
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    errors.email = '请先填写正确的邮箱'
    return
  }
  sending.value = true
  try {
    const { data } = await authApi.sendCode(form.email.trim())
    hint.value = data?.hint || '验证码已发送，请查收邮件'
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (err) {
    formError.value = err.message
  } finally {
    sending.value = false
  }
}

// 提交重置：校验通过后调用重置接口，成功提示并回登录页；失败显示在表单顶部
async function onSubmit() {
  if (loading.value) return
  formError.value = ''
  if (!validate()) return

  loading.value = true
  try {
    await authApi.resetPassword({
      email: form.email.trim(),
      code: form.code.trim(),
      password: form.password
    })
    toast.success('密码已重置，请用新密码登录')
    router.replace('/login')
  } catch (err) {
    formError.value = err.message
  } finally {
    loading.value = false
  }
}

/* ---------- 生命周期：离开页面时停掉重发倒计时 ---------- */
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
/* ---------- 表单与错误提示 ---------- */
.form {
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
}

/* ---------- 验证码行：输入框占满剩余宽度，按钮保持原宽 ---------- */
.code-row {
  display: flex;
  align-items: flex-end;
  gap: var(--sp-2);
}

.code-row :deep(.field) {
  flex: 1;
}

.code-btn {
  flex: none;
  white-space: nowrap;
}

/* ---------- 验证码发送成功提示 ---------- */
.hint {
  font-size: var(--fs-xs);
  color: var(--ok);
}

.form__error {
  font-size: var(--fs-sm);
  color: var(--danger);
  padding: 8px 10px;
  border-radius: var(--r-sm);
  background: var(--danger-soft);
}
</style>
