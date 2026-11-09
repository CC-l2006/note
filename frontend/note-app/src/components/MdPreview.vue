<template>
  <!-- #ifdef H5 -->
  <view class="md-preview" v-html="html"></view>
  <!-- #endif -->
  <!-- #ifndef H5 -->
  <rich-text class="md-preview" :nodes="html"></rich-text>
  <!-- #endif -->
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '../utils/markdown.js'

const props = defineProps({
  content: { type: String, default: '' }
})

const html = computed(() => renderMarkdown(props.content))
</script>

<style scoped>
.md-preview {
  font-size: 29rpx;
  line-height: 1.85;
  color: #3f4a45;
  word-break: break-word;
}

/* H5 v-html 兜底；App rich-text 已在 markdown.js 写内联样式 */
.md-preview h1,
.md-preview h2,
.md-preview h3,
.md-preview h4 {
  color: #18212b;
  font-weight: 700;
}

.md-preview p {
  margin: 10rpx 0;
}

.md-preview blockquote {
  border-left: 6rpx solid #c96a3d;
  background: #f1eadb;
  padding: 16rpx 20rpx;
  margin: 16rpx 0;
}

.md-preview ul,
.md-preview ol {
  padding-left: 36rpx;
  margin: 10rpx 0;
}

.md-preview code {
  background: #ede5d5;
  padding: 2rpx 8rpx;
  border-radius: 6rpx;
}

.md-preview pre {
  background: #18212b;
  color: #f6f1e6;
  padding: 20rpx;
  border-radius: 10rpx;
  overflow-x: auto;
}

.md-preview a {
  color: #2e6b66;
}
</style>
