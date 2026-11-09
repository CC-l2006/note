/**
 * 手机端后端地址。
 *
 * - H5 调试：留空，走 Vite /api 代理或同源 Nginx。
 * - Android 模拟器：默认 10.0.2.2 指向宿主机的 8084。
 * - iOS 模拟器：默认 127.0.0.1。
 * - 真机 / 正式包：务必用 VITE_API_BASE_URL 指向电脑局域网 IP 或线上域名。
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

const DEFAULT_APP_BASE = info.platform === 'ios'
  ? 'http://127.0.0.1:8084'
  : 'http://10.0.2.2:8084'

export const API_BASE_URL = String(
  import.meta.env.VITE_API_BASE_URL || (isH5 ? '' : DEFAULT_APP_BASE)
)
