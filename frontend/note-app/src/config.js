/**
 * 手机端后端地址。
 *
 * 正式地址：https://note.cipherws.icu（与网页同域，证书受信任）
 * Nginx 会把 /api 前缀去掉后转给后端，所以请求层仍然以 /api 开头。
 *
 * 不要默认直连 https://frp-box.com:64456：那张证书不在系统信任链，
 * Android / iOS 的 App 会直接 TLS 握手失败。
 *
 * H5 调试时留空，走 Vite 的 /api 代理；
 * 手机 App 没有本地代理，默认使用上面的公网域名。
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

export const APP_API_BASE = 'https://note.cipherws.icu'

export const API_BASE_URL = String(
  import.meta.env.VITE_API_BASE_URL || (isH5 ? '' : APP_API_BASE)
)

/**
 * 生产默认 true：note.cipherws.icu 是受信任证书，不需要跳过校验。
 * 只有临时直连自签名地址（如 frp-box.com:64456）时才在 .env.local 设 false；
 * 该参数仅 App-Android 生效，iOS 仍会校验证书。
 */
export const SSL_VERIFY = String(import.meta.env.VITE_SSL_VERIFY || 'true') !== 'false'

/** 头像等后端相对路径转成 App 可访问的完整 URL */
export function resolveAssetUrl(value) {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  if (!API_BASE_URL) return value
  return `${API_BASE_URL}${value.startsWith('/') ? value : `/${value}`}`
}
