<script>
export default {
  onLaunch() {
    // 应用启动：只做一次轻量初始化，登录态由页面守卫处理。
    console.log('[note-app] launch')
  },
  onShow() {
    console.log('[note-app] show')
  },
  onHide() {
    console.log('[note-app] hide')
  }
}
</script>

<style>
/* 全局页面底色与文字，具体页面样式在各页面 scoped 中 */
page {
  background: #f6f1e6;
  color: #18212b;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', sans-serif;
}

view,
text,
input,
textarea,
button {
  box-sizing: border-box;
}

button::after {
  border: none;
}
</style>
