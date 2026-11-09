<template>
  <!-- 认证页外壳：单列窄卡片居中，用于登录、注册等未登录场景 -->
  <div class="auth">
    <div class="auth__col">
      <div class="auth__brand">
        <!-- 直接用品牌图：logo 本身已经含「池禾笔记」字样，再单独排一段文字就重复了 -->
        <img class="auth__logo" src="/logo.png" alt="池禾笔记" width="76" height="74" />
      </div>

      <!-- 主卡片：标题、可选副标题与表单主体 -->
      <div class="auth__sheet sheet">
        <h1 class="auth__title">{{ title }}</h1>
        <p v-if="subtitle" class="auth__subtitle">{{ subtitle }}</p>

        <!-- 表单区：由页面把具体的登录/注册表单放进默认插槽 -->
        <div class="auth__body">
          <slot />
        </div>
      </div>

      <!-- 页脚：放「没有账号？」之类的辅助链接 -->
      <p class="auth__foot">
        <slot name="footer" />
      </p>
    </div>
  </div>
</template>

<script setup>
/**
 * AuthLayout —— 登录 / 注册等认证页的公共布局
 * 只提供居中的品牌区、标题卡片与页脚插槽，不包含任何表单逻辑，
 * 具体表单由路由页面通过默认插槽注入。
 */

/** 组件 props：页面标题与可选副标题 */
defineProps({
  /** 主标题，必填，用于说明当前是登录还是注册 */
  title: { type: String, required: true },
  /** 副标题，为空时不渲染 */
  subtitle: { type: String, default: '' }
})
</script>

<style scoped>
/* ---------------- 页面布局：整屏居中 ---------------- */
.auth {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--sp-8) var(--sp-4);
  background: var(--paper);
}

.auth__col {
  width: 100%;
  max-width: 400px;
}

/* ---------------- 品牌区 ---------------- */
.auth__brand {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--sp-5);
}

/* 品牌图自带圆角与米色底，所以这里不再另加边框/背景，避免出现双层圆角 */
.auth__logo {
  width: 76px;
  height: auto;
  display: block;
  border-radius: 14px;
}

/* ---------------- 卡片内容 ---------------- */
.auth__sheet {
  padding: var(--sp-6);
}

.auth__title {
  font-size: var(--fs-xl);
}

.auth__subtitle {
  margin-top: 6px;
  font-size: var(--fs-sm);
  color: var(--ink-3);
  line-height: 1.7;
}

.auth__body {
  margin-top: var(--sp-5);
}

/* ---------------- 页脚 ---------------- */
.auth__foot {
  margin-top: var(--sp-5);
  text-align: center;
  font-size: var(--fs-sm);
  color: var(--ink-3);
}
</style>
