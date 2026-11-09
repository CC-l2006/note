<template>
  <!-- 注册页：邮箱 + 验证码 + 昵称 + 密码，注册成功后跳到登录页 -->
  <AuthLayout
    title="注册"
    subtitle="用邮箱注册：先收一个验证码，验证通过才建账号。注册后即获得私有笔记本与 5 次 AI 总结额度。"
  >
    <!-- 注册表单：先发验证码再提交，所有字段一次校验 -->
    <form class="form" @submit.prevent="onSubmit">
      <!-- 邮箱：既是登录账号，也是验证码收件地址 -->
      <AppField
        v-model="form.email"
        label="邮箱"
        type="email"
        placeholder="用来登录，也会收到验证码"
        :error="errors.email"
        required
        @submit="onSubmit"
      />

      <!-- 验证码 + 发送按钮 -->
      <div class="code-row">
        <AppField
          v-model="form.code"
          label="验证码"
          :maxlength="6"
          placeholder="6 位数字"
          :error="errors.code"
          @submit="onSubmit"
        />
        <AppButton
          variant="ghost"
          :disabled="countdown > 0 || sending"
          :loading="sending"
          @click="sendCode"
        >
          {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
        </AppButton>
      </div>
      <!-- 验证码发送结果提示：文案由后端返回，前端不回显验证码本身 -->
      <p v-if="codeHint" class="hint">{{ codeHint }}</p>

      <!-- 昵称可选：留空时后端用邮箱前缀兜底；右侧实时显示字数 -->
      <AppField
        v-model="form.nickname"
        label="昵称（可选）"
        :maxlength="20"
        placeholder="不填就用邮箱前缀"
        :hint="`${nickLen}/20`"
        @submit="onSubmit"
      />
      <AppField
        v-model="form.password"
        label="密码"
        type="password"
        placeholder="至少 6 位"
        :error="errors.password"
        required
        @submit="onSubmit"
      />
      <AppField
        v-model="form.confirm"
        label="确认密码"
        type="password"
        placeholder="再输入一次"
        :error="errors.confirm"
        required
        @submit="onSubmit"
      />

      <!-- 表单级错误：注册失败（如验证码错误、邮箱已被占用）显示在这里 -->
      <p v-if="formError" class="form__error" role="alert">{{ formError }}</p>

      <AppButton type="submit" variant="primary" size="lg" block :loading="loading">
        注册
      </AppButton>
    </form>

    <!-- 底部链接：已有账号直接去登录 -->
    <template #footer>
      已有账号？<RouterLink to="/login">直接登录</RouterLink>
    </template>
  </AuthLayout>
</template>

<script setup>
/**
 * Register.vue —— 注册页
 * 职责：先发邮箱验证码，再提交邮箱、验证码、昵称与密码完成注册，成功后跳登录页。
 * 错误处理：字段级错误就近显示，验证码发送失败与注册失败分别落在对应提示位。
 */
import { computed, onUnmounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import AppField from '@/components/AppField.vue'
import AppButton from '@/components/AppButton.vue'
import { authApi } from '@/api/index.js'
import { useUserStore } from '@/stores/user.js'
import { useToastStore } from '@/stores/toast.js'

/* ---------- 依赖与全局状态 ---------- */
const router = useRouter()
const userStore = useUserStore()
const toast = useToastStore()

// 邮箱格式正则：格式明显不对时先拦住，避免白跑一次请求
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/* ---------- 表单状态 ---------- */
// 没有 username —— 账号名由后端用邮箱前缀生成，用户不用抢
const form = reactive({ email: '', code: '', nickname: '', password: '', confirm: '' })
// 字段级校验错误 + 表单级错误 + 提交中标记
const errors = reactive({ email: '', code: '', password: '', confirm: '' })
const formError = ref('')
const loading = ref(false)

/* ---------- computed：昵称字数（按字符而非字节） ---------- */
const nickLen = computed(() => [...String(form.nickname || '')].length)

/* ---------- 发送验证码 ---------- */
// 发送相关状态：发送中标记、后端提示文案、重发倒计时与定时器句柄
const sending = ref(false)
const codeHint = ref('')
const countdown = ref(0)
let timer = null

// 生命周期：离开页面时清掉倒计时，避免定时器泄漏
onUnmounted(() => clearInterval(timer))

// 启动倒计时：先把秒数写好，再每秒减一，归零即停止
function startCountdown(seconds) {
  countdown.value = seconds
  clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

// 请求验证码：校验邮箱格式后按 register 场景发送，成功后按后端冷却时间倒计时
async function sendCode() {
  const email = form.email.trim()
  errors.email = !email ? '请先填写邮箱' : EMAIL_RE.test(email) ? '' : '邮箱格式不正确'
  if (errors.email) return

  sending.value = true
  try {
    // 后端只返回提示，不会回传验证码
    const { data } = await authApi.sendCode(email, 'register')
    codeHint.value = data?.hint || '验证码已发送'
    startCountdown(data?.cooldown || 60)
  } catch (err) {
    errors.email = err.message
  } finally {
    sending.value = false
  }
}

/* ---------- 提交 ---------- */
// 统一校验：邮箱格式、6 位数字验证码、密码长度、两次密码一致
function validate() {
  const email = form.email.trim()
  errors.email = !email ? '请输入邮箱' : EMAIL_RE.test(email) ? '' : '邮箱格式不正确'
  errors.code = /^\d{6}$/.test(form.code.trim()) ? '' : '请输入 6 位数字验证码'
  errors.password = form.password.length >= 6 ? '' : '密码至少 6 位'
  errors.confirm = form.confirm === form.password ? '' : '两次输入的密码不一致'
  return Object.values(errors).every((v) => !v)
}

// 提交注册：校验通过后调用 store 注册，成功提示并跳登录页；失败把原因挂到表单顶部
async function onSubmit() {
  if (loading.value) return
  formError.value = ''
  if (!validate()) return

  loading.value = true
  try {
    await userStore.register({
      email: form.email.trim(),
      code: form.code.trim(),
      password: form.password,
      nickname: form.nickname.trim()
    })
    toast.success('注册成功，请用邮箱登录')
    router.replace('/login')
  } catch (err) {
    formError.value = err.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ---------- 表单与错误提示 ---------- */
.form {
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
}

/* 验证码输入框 + 发送按钮并排 */
.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--sp-2);
  align-items: end;
}

/* ---------- 验证码发送结果提示 ---------- */
.hint {
  margin-top: calc(-1 * var(--sp-2));
  font-size: var(--fs-xs);
  line-height: 1.7;
  color: var(--accent-ink);
}

.form__error {
  font-size: var(--fs-sm);
  color: var(--danger);
  padding: 8px 10px;
  border-radius: var(--r-sm);
  background: var(--danger-soft);
}

/* ---------- 窄屏适配：验证码行改为上下排列 ---------- */
@media (max-width: 420px) {
  .code-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
