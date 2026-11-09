import MarkdownIt from 'markdown-it'

/**
 * 笔记正文 Markdown 渲染。
 *
 * - html: false：不允许原始 HTML，避免用户输入直接变成脚本；
 * - linkify: 自动识别链接；
 * - breaks: 单个换行也换行，更符合笔记输入习惯；
 * - 表格、删除线按 CommonMark 扩展启用。
 */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
}).enable(['table', 'strikethrough'])

const BLOCK_STYLES = {
  h1: 'font-size:42rpx;font-weight:700;color:#18212b;margin:24rpx 0 12rpx;line-height:1.35;',
  h2: 'font-size:36rpx;font-weight:700;color:#18212b;margin:22rpx 0 10rpx;line-height:1.4;',
  h3: 'font-size:31rpx;font-weight:700;color:#18212b;margin:20rpx 0 8rpx;line-height:1.4;',
  h4: 'font-size:28rpx;font-weight:700;color:#18212b;margin:18rpx 0 8rpx;',
  p: 'font-size:29rpx;line-height:1.85;color:#3f4a45;margin:10rpx 0;',
  blockquote: 'border-left:6rpx solid #c96a3d;background:#f1eadb;padding:16rpx 20rpx;margin:16rpx 0;color:#6b746f;font-size:27rpx;line-height:1.75;',
  ul: 'padding-left:36rpx;margin:10rpx 0;color:#3f4a45;',
  ol: 'padding-left:36rpx;margin:10rpx 0;color:#3f4a45;',
  li: 'font-size:29rpx;line-height:1.8;margin:6rpx 0;'
}

for (const tag of Object.keys(BLOCK_STYLES)) {
  const ruleName = `${tag}_open`
  const defaultRule = md.renderer.rules[ruleName]
  md.renderer.rules[ruleName] = (tokens, idx, options, env, self) => {
    tokens[idx].attrSet('style', BLOCK_STYLES[tag])
    if (defaultRule) return defaultRule(tokens, idx, options, env, self)
    return self.renderToken(tokens, idx, options)
  }
}

md.renderer.rules.code_inline = (tokens, idx) => {
  const text = md.utils.escapeHtml(tokens[idx].content)
  return `<code style="background:#ede5d5;color:#18212b;padding:2rpx 8rpx;border-radius:6rpx;font-size:25rpx;">${text}</code>`
}

md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx]
  const text = md.utils.escapeHtml(token.content)
  return `<pre style="background:#18212b;color:#f6f1e6;padding:20rpx;border-radius:10rpx;overflow-x:auto;font-size:24rpx;line-height:1.7;"><code>${text}</code></pre>`
}

md.renderer.rules.code_block = (tokens, idx) => {
  const text = md.utils.escapeHtml(tokens[idx].content)
  return `<pre style="background:#18212b;color:#f6f1e6;padding:20rpx;border-radius:10rpx;overflow-x:auto;font-size:24rpx;line-height:1.7;"><code>${text}</code></pre>`
}

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('style', 'color:#2e6b66;text-decoration:underline;')
  return self.renderToken(tokens, idx, options)
}

md.renderer.rules.image = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('style', 'max-width:100%;height:auto;border-radius:10rpx;margin:12rpx 0;')
  return self.renderToken(tokens, idx, options)
}

/** Markdown -> HTML，供 H5 v-html / App rich-text 使用 */
export function renderMarkdown(content) {
  return md.render(String(content || ''))
}

/** 列表摘要：去掉 Markdown 标记，避免显示 #、**、反引号等 */
export function markdownToText(content) {
  return String(content || '')
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^>\s?/gm, '')
    .replace(/[*_~]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}
