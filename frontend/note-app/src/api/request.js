/**
 * uni-app 统一请求层。
 *
 * H5 开发走 Vite 的 /api 代理；小程序/App 部署时用 VITE_API_BASE_URL 指向后端完整地址。
 * 这样页面里只写 /auth/login、/notes 这类业务路径，不重复处理 baseURL、token 和错误。
 */

import { API_BASE_URL, SSL_VERIFY } from '../config.js'

const BASE = API_BASE_URL
const API_PREFIX = '/api'
export const TOKEN_KEY = 'note_token'

export class ApiError extends Error {
  constructor(message, code) {
    super(message || '请求失败')
    this.name = 'ApiError'
    this.code = code
  }
}

export function getToken() {
  try {
    return uni.getStorageSync(TOKEN_KEY) || ''
  } catch {
    return ''
  }
}

export function clearToken() {
  try {
    uni.removeStorageSync(TOKEN_KEY)
  } catch {
    // 忽略隐私模式/存储异常
  }
}

function buildQuery(params) {
  if (!params) return ''
  const pairs = []
  Object.keys(params).forEach((key) => {
    const value = params[key]
    if (value !== undefined && value !== null && value !== '') {
      pairs.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    }
  })
  return pairs.length ? `?${pairs.join('&')}` : ''
}

function parseResponse(res) {
  const body = res.data
  if (res.statusCode === 401) {
    clearToken()
    throw new ApiError('登录已失效，请重新登录', 401)
  }
  if (res.statusCode !== 200 || !body || body.success !== true) {
    throw new ApiError(body?.errMessage || `请求失败（HTTP ${res.statusCode}）`, res.statusCode)
  }
  return { data: body.data, total: body.total }
}

/**
 * @param {string} path 业务路径，例如 '/notes'
 * @param {object} options method / data / params
 * @returns {Promise<{data:any,total?:number}>}
 */
export function request(path, { method = 'GET', data, params } = {}) {
  const url = `${BASE}${API_PREFIX}${path}${buildQuery(params)}`
  const header = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) header.Authorization = `Bearer ${token}`

  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method,
      data: method === 'GET' ? undefined : data,
      header,
      timeout: 60000,
      // 仅 App-Android 生效；H5/小程序会忽略该参数
      sslVerify: SSL_VERIFY,
      success: (res) => {
        try {
          resolve(parseResponse(res))
        } catch (error) {
          reject(error)
        }
      },
      fail: () => reject(new ApiError('网络请求失败，请检查连接'))
    })
  })
}

/**
 * 文件上传（头像等）。不要手写 Content-Type，交给 uni.uploadFile 自动带 boundary。
 */
export function upload(path, filePath, name = 'file') {
  const header = {}
  const token = getToken()
  if (token) header.Authorization = `Bearer ${token}`

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE}${API_PREFIX}${path}`,
      filePath,
      name,
      header,
      timeout: 60000,
      success: (res) => {
        let body
        try {
          body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
        } catch {
          reject(new ApiError('上传响应不是合法 JSON'))
          return
        }
        if (res.statusCode === 401) {
          clearToken()
          reject(new ApiError('登录已失效，请重新登录', 401))
          return
        }
        if (res.statusCode !== 200 || !body || body.success !== true) {
          reject(new ApiError(body?.errMessage || '上传失败', res.statusCode))
          return
        }
        resolve({ data: body.data })
      },
      fail: () => reject(new ApiError('上传失败，请检查网络'))
    })
  })
}

export default request
