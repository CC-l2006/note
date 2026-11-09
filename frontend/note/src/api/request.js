/**
 * 统一请求层
 *
 * 直接打真实后端：同源 /api/**，由 nginx / Vite 去掉前缀后反代到 note-app(8084)。
 * 返回约定沿用 cipherws 后端已有的信封格式：
 *   { success: true, data, total }
 *   { success: false, errMessage }
 */

/* ---------------- 基础配置 ---------------- */

/** 后端根地址，留空表示走同源（由 Nginx / Vite 代理） */
const BASE = String(import.meta.env.VITE_API_BASE_URL || '')

/**
 * 所有接口都以 /api 开头（Nginx / Vite 按这个前缀分流，并去掉前缀）。
 *
 * api/index.js 里写的是业务路径（'/auth/login'、'/notes'），
 * 前缀在这里统一拼 —— 之前这是个真 bug：路径没带 /api，
 * mock 模式下看不出来（mock 直接按 path 匹配），
 * 一切到真实后端就打到 /auth/login 上，Vite 返回 404 而不是接口响应。
 */
const API_PREFIX = '/api'

/* ---------------- 错误类型 ---------------- */

/**
 * 统一的接口错误。
 * 调用方一般只需要读 message 展示；code 里放的是 HTTP 状态码，便于按状态分支处理。
 */
export class ApiError extends Error {
  constructor(message, code) {
    super(message || '请求失败')
    this.name = 'ApiError'
    this.code = code
  }
}

/** 登录态令牌的存取 */
export const TOKEN_KEY = 'note_token'

/**
 * 读取本地令牌。
 * 隐私模式 / 禁用存储时 localStorage 会抛异常，这里降级成空串，
 * 让上层按「未登录」处理，而不是让整个应用崩掉。
 */
export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY) || ''
  } catch {
    return ''
  }
}

/**
 * 统一的响应解析：把 HTTP 状态 + 业务信封收敛成 {data,total} 或 ApiError
 *
 * 失败分两种：HTTP 层失败（res.ok 为假）和业务层失败（success 不为 true），
 * 这里合并成同一个 ApiError，上层不用各写一遍。
 */
async function parseResponse(res) {
  /* 先尝试解析 JSON：非 JSON 响应（网关错误页、404 HTML）单独给提示 */
  let json
  try {
    json = await res.json()
  } catch {
    throw new ApiError(`响应不是合法 JSON（HTTP ${res.status}）`)
  }
  /* HTTP 状态异常或业务信封 success 非真 → 统一抛错，优先用后端给的 errMessage */
  if (!res.ok || json.success !== true) {
    throw new ApiError(json.errMessage || `请求失败（HTTP ${res.status}）`, res.status)
  }
  return { data: json.data, total: json.total }
}

/**
 * 构造鉴权头。
 * 没有令牌时不带 Authorization，避免出现 "Bearer undefined"。
 */
function authHeader() {
  return getToken() ? { Authorization: `Bearer ${getToken()}` } : {}
}

/**
 * 通用 JSON 请求。
 *
 * @param {string} path      以 / 开头的接口路径，例如 '/notes'
 * @param {object} [options]
 * @param {string} [options.method]
 * @param {object} [options.params] 查询参数，undefined / null / '' 会被剔除
 * @param {object} [options.body]   请求体
 * @returns {Promise<{data:any,total?:number}>}
 */
export async function request(path, { method = 'GET', params, body } = {}) {
  /* ---------------- 1. 清洗查询参数 ----------------
     剔除空值，避免后端收到 "?keyword=&page=" 这种无意义参数。 */
  const cleanParams = {}
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      if (v !== undefined && v !== null && v !== '') cleanParams[k] = v
    }
  }

  /* ---------------- 2. 拼完整 URL ----------------
     BASE（可空）+ /api 前缀 + 业务路径 + 查询串。 */
  const qs = new URLSearchParams(cleanParams).toString()
  const url = `${BASE}${API_PREFIX}${path}${qs ? `?${qs}` : ''}`

  /* ---------------- 3. 发请求 ---------------- */
  let res
  try {
    res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  } catch {
    /* 断网、DNS 失败、被 CORS 拦截等都会走到这里，与后端返回的错误区分开 */
    throw new ApiError('网络请求失败，请检查连接')
  }
  /* ---------------- 4. 统一解析响应 ---------------- */
  return parseResponse(res)
}

/**
 * 上传文件（multipart/form-data）。
 *
 * **绝不能手写 Content-Type** —— 必须让浏览器自动带上
 * `multipart/form-data; boundary=...`，自己写会丢掉 boundary，后端直接解析失败。
 */
export async function upload(path, file) {
  /* 用 FormData 承载文件，字段名固定为 file（后端按这个名字取值） */
  const form = new FormData()
  form.append('file', file)

  /* 只带鉴权头，其余交给浏览器自动补全 */
  let res
  try {
    res = await fetch(`${BASE}${API_PREFIX}${path}`, {
      method: 'POST',
      headers: authHeader(),
      body: form
    })
  } catch {
    /* 网络层异常：与接口业务错误分开提示 */
    throw new ApiError('网络请求失败，请检查连接')
  }
  /* 上传接口的返回信封与普通请求一致 */
  return parseResponse(res)
}

export default request
