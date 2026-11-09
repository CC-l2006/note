<template>
  <!-- 登录页：用邮箱 + 密码进入系统，成功后按 redirect 参数或用户角色跳转 -->
  <AuthLayout
    title="登录"
    subtitle="用邮箱和密码进入你的私人笔记本。笔记默认私有，只有你自己能看到。"
  >
    <!-- 登录表单：邮箱需符合格式，密码必填 -->
    <form class="form" @submit.prevent="onSubmit">
      <AppField
        v-model="form.email"
        label="邮箱"
        type="email"
        placeholder="注册时使用的邮箱"
        :error="errors.email"
        required
        @submit="onSubmit"
      />
      <AppField
        v-model="form.password"
        label="密码"
        type="password"
        placeholder="请输入密码"
        :error="errors.password"
        required
        @submit="onSubmit"
      />

      <!-- 表单级错误：登录失败（如邮箱或密码不正确）统一显示在这里 -->
      <p v-if="formError" class="form__error" role="alert">{{ formError }}</p>

      <AppButton type="submit" variant="primary" size="lg" block :loading="loading">
        登录
      </AppButton>
    </form>

    <!-- 底部链接：没有账号去注册，忘记密码去重置 -->
    <template #footer>
      <span class="links">
        <RouterLink to="/register">注册新账号</RouterLink>
        <span class="links__sep" aria-hidden="true">·</span>
        <RouterLink to="/forgot-password">忘记密码</RouterLink>
      </span>
    </template>
  </AuthLayout>
</template>

<script setup>
/**
 * Login.vue —— 登录页
 * 职责：校验邮箱与密码，调用 user store 的 login，成功后跳回原页面或按角色进入默认页。
 * 错误处理：字段级错误就近显示在输入框下，登录失败等整体错误显示在表单顶部。
 */
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import AppField from '@/components/AppField.vue'
import AppButton from '@/components/AppButton.vue'
import { useUserStore } from '@/stores/user.js'
import { useToastStore } from '@/stores/toast.js'

/* ---------- 依赖与全局状态 ---------- */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToastStore()

/* ---------- 表单状态 ---------- */
// 登录标识是邮箱，不再是账号名 —— 这样注册时不用抢用户名
const form = reactive({ email: '', password: '' })
// 字段级校验错误 + 表单级错误 + 提交中标记
const errors = reactive({ email: '', password: '' })
const formError = ref('')
const loading = ref(false)

// 邮箱格式正则：前端只做基础拦截，账号是否存在交给后端判断
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/* ---------- 校验 ---------- */
// 逐项写入错误信息，返回是否全部通过
function validate() {
  const email = form.email.trim()
  errors.email = !email ? '请输入邮箱' : EMAIL_RE.test(email) ? '' : '邮箱格式不正确'
  errors.password = form.password ? '' : '请输入密码'
  return !errors.email && !errors.password
}

/* ---------- 提交 ---------- */
// 登录流程：防重复提交 → 本地校验 → 调用 store 登录 → 跳转；失败把错误挂到表单顶部
async function onSubmit() {
  if (loading.value) return
  formError.value = ''
  if (!validate()) return

  loading.value = true
  try {
    const user = await userStore.login({
      email: form.email.trim(),
      password: form.password
    })
    toast.success(`欢迎回来，${user.nickname || user.username}`)
    // 回到被拦截的页面；管理员直接进后台更顺手
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(redirect || (user.role === 'admin' ? '/admin/users' : '/notes'))
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

.form__error {
  font-size: var(--fs-sm);
  color: var(--danger);
  padding: 8px 10px;
  border-radius: var(--r-sm);
  background: var(--danger-soft);
}

/* ---------- 底部链接：注册 / 忘记密码 ---------- */
.links {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
}

.links__sep {
  color: var(--rule-strong);
}
</style>
