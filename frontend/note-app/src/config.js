/**
 * 手机端后端地址。
 *
 * 正式地址：https://frp-box.com:64456
 * Nginx 会把 /api 前缀去掉后转给后端，所以请求层仍然以 /api 开头。
 *
 * H5 调试时留空，走 Vite 的 /api 代理；
 * 手机 App 没有本地代理，默认使用上面的公网地址。
 */
function detectPlatform() {
  try {
    const info = uni.getSystemInfoSync()
    return { platform: info.platform || '', uniPlatform: info.uniPlatform || '' }
  } catch {
    return { platform: '', uniPlatform: '' }
  }
}

const info = detectPlatform()
const isH5 = info.uniPlatform === 'web'

export const APP_API_BASE = 'https://frp-box.com:64456'

export const API_BASE_URL = String(
  import.meta.env.VITE_API_BASE_URL || (isH5 ? '' : APP_API_BASE)
)

/**
 * 仅 App-Android 的 uni.request 支持 sslVerify。
 * frp-box.com 当前证书不是系统信任链，开发阶段可在 .env.local 设 false；
 * iOS 仍会校验证书，正式环境应换成受信任的 HTTPS 证书。
 */
export const SSL_VERIFY = String(import.meta.env.VITE_SSL_VERIFY || 'true') !== 'false'

/** 头像等后端相对路径转成 App 可访问的完整 URL */
export function resolveAssetUrl(value) {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  if (!API_BASE_URL) return value
  return `${API_BASE_URL}${value.startsWith('/') ? value : `/${value}`}`
}
