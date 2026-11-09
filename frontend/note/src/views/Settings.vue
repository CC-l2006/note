<template>
  <!-- 设置页：集中处理账号安全，包括「更换绑定邮箱」（两步验证）与「修改密码」 -->
  <div class="page">
    <div class="page-title">
      <h1>设置</h1>
    </div>
    <p class="page-sub">账号与安全。这里的敏感操作都会往当前邮箱发一个验证码。</p>

    <!-- 单列自上而下 -->
    <div class="stack">
      <!-- ==================== 更换绑定邮箱 ==================== -->
      <section class="card sheet">
        <h2 class="card__title">更换绑定邮箱</h2>

        <!-- 当前邮箱：已绑定时显示原地址，未绑定时显示占位文案 -->
        <p class="email">
          <span class="email__label">当前邮箱</span>
          <span class="email__value" :class="{ 'is-empty': !currentEmail }">
            {{ currentEmail || '尚未绑定' }}
          </span>
        </p>

        <p class="card__hint">
          分两步：<strong>先验证当前邮箱</strong>确认是你本人，
          <strong>再验证新邮箱</strong>确认新地址归你，两步都通过才会更换。
        </p>

        <!-- 步骤条 -->
        <ol class="steps" aria-label="更换邮箱步骤">
          <li class="steps__item" :class="{ 'is-done': emailStep > 1, 'is-active': emailStep === 1 }">
            <span class="steps__dot">{{ emailStep > 1 ? '✓' : '1' }}</span>
            <span class="steps__text">验证当前邮箱</span>
          </li>
          <li class="steps__line" aria-hidden="true" />
          <li class="steps__item" :class="{ 'is-active': emailStep === 2 }">
            <span class="steps__dot">2</span>
            <span class="steps__text">绑定新邮箱</span>
          </li>
        </ol>

        <!-- ---------- 第 1 步 ---------- -->
        <!-- 验证当前邮箱：输入发到旧邮箱的验证码，通过后才允许进入第 2 步 -->
        <form v-if="emailStep === 1" class="form" @submit.prevent="submitOldCode">
          <div class="code-row">
            <AppField
              v-model="oldForm.code"
              label="当前邮箱的验证码"
              :maxlength="6"
              placeholder="6 位数字"
              :error="oldErrors.code"
            />
            <AppButton
              variant="ghost"
              :disabled="oldCountdown > 0 || oldSending"
              :loading="oldSending"
              @click="sendOldCode"
            >
              {{ oldCountdown > 0 ? `${oldCountdown}s` : '发送验证码' }}
            </AppButton>
          </div>
          <p v-if="oldHint" class="card__hint card__hint--ok">{{ oldHint }}</p>
          <div class="form__actions">
            <AppButton type="submit" variant="primary" :loading="oldVerifying">下一步</AppButton>
          </div>
        </form>

        <!-- ---------- 第 2 步 ---------- -->
        <!-- 绑定新邮箱：填写新地址并输入发到新邮箱的验证码，两步都通过才真正换绑 -->
        <form v-else class="form" @submit.prevent="submitNewBind">
          <AppField
            v-model="newForm.email"
            label="新邮箱"
            type="email"
            placeholder="要换成的新邮箱地址"
            :error="newErrors.email"
          />
          <div class="code-row">
            <AppField
              v-model="newForm.code"
              label="新邮箱的验证码"
              :maxlength="6"
              placeholder="6 位数字"
              :error="newErrors.code"
            />
            <AppButton
              variant="ghost"
              :disabled="newCountdown > 0 || newSending"
              :loading="newSending"
              @click="sendNewCode"
            >
              {{ newCountdown > 0 ? `${newCountdown}s` : '发送验证码' }}
            </AppButton>
          </div>
          <p v-if="newHint" class="card__hint card__hint--ok">{{ newHint }}</p>
          <p v-if="newErrors.form" class="form__error" role="alert">{{ newErrors.form }}</p>
          <div class="form__actions">
            <AppButton type="submit" variant="primary" :loading="binding">确认更换</AppButton>
            <AppButton variant="quiet" @click="resetEmailFlow">重新开始</AppButton>
          </div>
        </form>
      </section>

      <!-- ==================== 修改密码 ==================== -->
      <section class="card sheet">
        <h2 class="card__title">修改密码</h2>
        <p class="card__hint">
          需要三样东西：原密码、新密码、以及发到<strong>当前邮箱</strong>的验证码。
          验证码是为了确认这次修改是账号主本人发起的。
        </p>

        <!-- 修改密码表单：原密码 + 新密码 + 确认 + 发到当前邮箱的验证码 -->
        <form class="form" @submit.prevent="submitPassword">
          <AppField
            v-model="pwForm.oldPassword"
            label="原密码"
            type="password"
            :error="pwErrors.oldPassword"
            required
          />
          <AppField
            v-model="pwForm.newPassword"
            label="新密码"
            type="password"
            hint="至少 6 位"
            :error="pwErrors.newPassword"
            required
          />
          <AppField
            v-model="pwForm.confirm"
            label="确认新密码"
            type="password"
            :error="pwErrors.confirm"
            required
          />
          <div class="code-row">
            <AppField
              v-model="pwForm.code"
              label="邮箱验证码"
              :maxlength="6"
              placeholder="6 位数字"
              :error="pwErrors.code"
            />
            <AppButton
              variant="ghost"
              :disabled="pwCountdown > 0 || pwSending"
              :loading="pwSending"
              @click="sendPwCode"
            >
              {{ pwCountdown > 0 ? `${pwCountdown}s` : '发送验证码' }}
            </AppButton>
          </div>
          <p v-if="pwHint" class="card__hint card__hint--ok">{{ pwHint }}</p>
          <p v-if="pwErrors.form" class="form__error" role="alert">{{ pwErrors.form }}</p>
          <div class="form__actions">
            <AppButton type="submit" variant="primary" :loading="savingPassword">更新密码</AppButton>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
/**
 * Settings.vue —— 账号设置页
 * 职责：提供两类敏感操作 —— 更换绑定邮箱（先验旧邮箱、再验新邮箱）与修改密码；
 *       两者都需要邮箱验证码，确认操作者是账号本人。
 * 结构：一条可复用的倒计时工厂 + 两套彼此独立的表单状态与提交方法。
 */
import { computed, onUnmounted, reactive, ref } from 'vue'
import AppField from '@/components/AppField.vue'
import AppButton from '@/components/AppButton.vue'
import { userApi } from '@/api/index.js'
import { useUserStore } from '@/stores/user.js'
import { useToastStore } from '@/stores/toast.js'

/* ---------- 依赖与全局状态 ---------- */
const userStore = useUserStore()
const toast = useToastStore()

// 邮箱格式校验：前端只做宽松拦截，地址是否真实可达由验证码环节证明
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/* ---------- computed ---------- */
// 当前绑定邮箱：未绑定时为空串，模板据此决定显示「尚未绑定」还是真实地址
const currentEmail = computed(() => userStore.userInfo?.email || '')

/* ==================== 倒计时（两个独立按钮） ==================== */
// 倒计时工厂：换邮箱与改密码各用一套，互不影响；返回 start/stop 两个方法
function makeCountdown(state) {
  let timer = null
  const start = (seconds) => {
    state.value = seconds
    clearInterval(timer)
    timer = setInterval(() => {
      state.value -= 1
      if (state.value <= 0) clearInterval(timer)
    }, 1000)
  }
  return { start, stop: () => clearInterval(timer) }
}

/* ==================== 更换邮箱 ==================== */
// 没绑过邮箱的账号跳过第 1 步（没有旧邮箱可验）
const emailStep = ref(currentEmail.value ? 1 : 2)

/* 第 1 步状态：验证码、校验错误、成功提示、发送中、验证中、重发倒计时 */
const oldForm = reactive({ code: '' })
const oldErrors = reactive({ code: '' })
const oldHint = ref('')
const oldSending = ref(false)
const oldVerifying = ref(false)
const oldCountdown = ref(0)
const oldTimer = makeCountdown(oldCountdown)

/* 第 2 步状态：新邮箱与验证码、各类错误、成功提示、发送中、绑定中、重发倒计时 */
const newForm = reactive({ email: '', code: '' })
const newErrors = reactive({ email: '', code: '', form: '' })
const newHint = ref('')
const newSending = ref(false)
const binding = ref(false)
const newCountdown = ref(0)
const newTimer = makeCountdown(newCountdown)

/* ---------- 生命周期：离开页面时清掉换邮箱的倒计时，避免定时器泄漏 ---------- */
onUnmounted(() => {
  oldTimer.stop()
  newTimer.stop()
})

/* ---------- 方法：更换邮箱 ---------- */
/** 只展示后端提示；验证码永远不会回传前端，必须人工从邮箱/日志抄录 */
function absorbCode(res, hintRef, hintText) {
  hintRef.value = res?.data?.hint || hintText
}

// 第 1 步：请求把验证码发到当前邮箱，成功后按后端给的冷却时间开始倒计时
async function sendOldCode() {
  oldErrors.code = ''
  oldSending.value = true
  try {
    const res = await userApi.sendOldEmailCode()
    absorbCode(res, oldHint, '验证码已发送到当前邮箱')
    oldTimer.start(res?.data?.cooldown || 60)
  } catch (err) {
    oldErrors.code = err.message
  } finally {
    oldSending.value = false
  }
}

// 第 1 步：校验 6 位数字后提交，通过则切到第 2 步；失败把错误落回输入框下方
async function submitOldCode() {
  oldErrors.code = ''
  if (!/^\d{6}$/.test(oldForm.code.trim())) {
    oldErrors.code = '请输入 6 位数字验证码'
    return
  }
  oldVerifying.value = true
  try {
    await userApi.verifyOldEmail(oldForm.code.trim())
    emailStep.value = 2
    newHint.value = ''
    toast.success('当前邮箱验证通过，请填写新邮箱')
  } catch (err) {
    oldErrors.code = err.message
  } finally {
    oldVerifying.value = false
  }
}

// 第 2 步：先校验新邮箱格式再请求验证码，避免把验证码发到明显无效的地址
async function sendNewCode() {
  const email = newForm.email.trim()
  newErrors.email = !email ? '请先填写新邮箱' : EMAIL_RE.test(email) ? '' : '邮箱格式不正确'
  if (newErrors.email) return

  newSending.value = true
  newErrors.code = ''
  try {
    const res = await userApi.sendNewEmailCode(email)
    absorbCode(res, newHint, '验证码已发送到新邮箱')
    newTimer.start(res?.data?.cooldown || 60)
  } catch (err) {
    newErrors.email = err.message
  } finally {
    newSending.value = false
  }
}

// 第 2 步：两项校验都通过才提交换绑；成功后刷新用户信息并把流程重置回起点
async function submitNewBind() {
  const email = newForm.email.trim()
  newErrors.email = !email ? '请填写新邮箱' : EMAIL_RE.test(email) ? '' : '邮箱格式不正确'
  newErrors.code = /^\d{6}$/.test(newForm.code.trim()) ? '' : '请输入 6 位数字验证码'
  newErrors.form = ''
  if (newErrors.email || newErrors.code) return

  binding.value = true
  try {
    await userApi.bindEmail({ email, code: newForm.code.trim() })
    await userStore.fetchUserInfo()
    toast.success('绑定邮箱已更换')
    resetEmailFlow()
  } catch (err) {
    newErrors.form = err.message
  } finally {
    binding.value = false
  }
}

// 重置换邮箱流程：清空两边表单与提示，并按是否已绑定邮箱决定回到第 1 步还是第 2 步
function resetEmailFlow() {
  oldForm.code = ''
  newForm.email = ''
  newForm.code = ''
  newErrors.email = ''
  newErrors.code = ''
  newErrors.form = ''
  newHint.value = ''
  oldHint.value = ''
  emailStep.value = currentEmail.value ? 1 : 2
}

/* ==================== 修改密码 ==================== */
/* 表单状态：原密码、新密码、确认、邮箱验证码，以及各自的错误提示 */
const pwForm = reactive({ oldPassword: '', newPassword: '', confirm: '', code: '' })
const pwErrors = reactive({ oldPassword: '', newPassword: '', confirm: '', code: '', form: '' })
/* 成功提示、发送中、提交中、重发倒计时 */
const pwHint = ref('')
const pwSending = ref(false)
const savingPassword = ref(false)
const pwCountdown = ref(0)
const pwTimer = makeCountdown(pwCountdown)

// 生命周期：离开页面时停止密码表单的倒计时
onUnmounted(() => pwTimer.stop())

// 发送密码修改验证码：没有绑定邮箱时直接给出提示，不发请求
async function sendPwCode() {
  pwErrors.form = ''
  pwErrors.code = ''
  if (!currentEmail.value) {
    pwErrors.code = '当前账号还没有绑定邮箱，无法发送验证码'
    return
  }
  pwSending.value = true
  try {
    const res = await userApi.sendPasswordCode()
    absorbCode(res, pwHint, '验证码已发送到当前邮箱')
    pwTimer.start(res?.data?.cooldown || 60)
  } catch (err) {
    pwErrors.code = err.message
  } finally {
    pwSending.value = false
  }
}

// 校验四项输入：原密码非空、新密码至少 6 位、两次一致、验证码为 6 位数字
function validatePassword() {
  pwErrors.oldPassword = pwForm.oldPassword ? '' : '请输入原密码'
  pwErrors.newPassword = pwForm.newPassword.length >= 6 ? '' : '新密码至少 6 位'
  pwErrors.confirm = pwForm.confirm === pwForm.newPassword ? '' : '两次输入的新密码不一致'
  pwErrors.code = /^\d{6}$/.test(pwForm.code.trim()) ? '' : '请输入邮箱验证码'
  return Object.values(pwErrors).every((v) => !v)
}

// 提交修改密码：本地校验通过后请求接口，成功则清空表单避免明文残留
async function submitPassword() {
  pwErrors.form = ''
  if (!validatePassword()) return

  savingPassword.value = true
  try {
    await userApi.changePassword({
      oldPassword: pwForm.oldPassword,
      newPassword: pwForm.newPassword,
      code: pwForm.code.trim()
    })
    // 改完密码把表单清空，避免明文停留在输入框里
    pwForm.oldPassword = ''
    pwForm.newPassword = ''
    pwForm.confirm = ''
    pwForm.code = ''
    pwHint.value = ''
    toast.success('密码已更新，下次登录请用新密码')
  } catch (err) {
    pwErrors.form = err.message
  } finally {
    savingPassword.value = false
  }
}
</script>

<style scoped>
/* 与其它页面一致：整页留白 */
.page {
  padding: var(--sp-5) var(--sp-6);
}

/* 单列自上而下。
   宽度限制：表单行太长不好读，所以给一个阅读宽度；
   居中（margin-inline: auto）让两侧留白相等，不然 760 摆在 1040 的内容区里
   右边会空出一大块，看着偏。
   想铺满内容区：删掉下面三行里的 max-width 与 margin-inline 即可。 */
.stack {
  margin-top: var(--sp-4);
  max-width: 880px;
  margin-inline: auto;
}

/* .sheet 只负责「纸」的外观，内边距由页面自己给 */
.card {
  padding: var(--sp-5);
}

.card__title {
  font-size: var(--fs-md);
  padding-bottom: var(--sp-2);
  margin-bottom: var(--sp-4);
  border-bottom: 1px solid var(--rule);
}

.card__hint {
  font-size: var(--fs-xs);
  line-height: 1.8;
  color: var(--ink-3);
}

.card__hint--ok {
  color: var(--accent-ink);
}

/* ---------------- 表单通用 ---------------- */
.form {
  display: flex;
  flex-direction: column;
  gap: var(--sp-3);
  margin-top: var(--sp-4);
}

/* 验证码输入框 + 发送按钮并排 */
.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--sp-2);
  align-items: end;
}

.form__actions {
  display: flex;
  gap: var(--sp-2);
  flex-wrap: wrap;
  margin-top: var(--sp-1);
}

.form__error {
  font-size: var(--fs-sm);
  color: var(--danger);
  padding: 8px 10px;
  border-radius: var(--r-sm);
  background: var(--danger-soft);
}

/* ---------------- 当前邮箱 ---------------- */
.email {
  display: flex;
  align-items: baseline;
  gap: var(--sp-2);
  margin: var(--sp-2) 0 0;
}

.email__label {
  font-size: var(--fs-xs);
  color: var(--ink-3);
}

.email__value {
  font-family: var(--font-mono);
  font-size: var(--fs-sm);
  color: var(--ink);
  word-break: break-all;
}

.email__value.is-empty {
  color: var(--ink-3);
  font-family: inherit;
}

/* ---------------- 步骤条 ---------------- */
.steps {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  margin: var(--sp-3) 0 0;
  padding: 0;
  list-style: none;
}

.steps__item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-xs);
  color: var(--ink-3);
}

.steps__item.is-active {
  color: var(--accent-ink);
  font-weight: 500;
}

.steps__dot {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid var(--rule-strong);
  font-family: var(--font-mono);
  font-size: 11px;
  line-height: 1;
}

.steps__item.is-active .steps__dot {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent-ink);
}

.steps__item.is-done .steps__dot {
  border-color: var(--accent);
  background: var(--accent);
  color: var(--paper);
}

.steps__line {
  flex: 1;
  height: 1px;
  background: var(--rule);
}

/* ---------------- 窄屏适配：压缩留白，验证码行改为上下排列 ---------------- */
@media (max-width: 720px) {
  .page {
    padding: var(--sp-4) var(--sp-3);
  }

  .card {
    padding: var(--sp-4) var(--sp-3);
  }
}

@media (max-width: 480px) {
  .code-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
