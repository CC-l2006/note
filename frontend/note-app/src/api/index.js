import { request } from './request.js'

/** 认证：注册、登录、发验证码 */
export const authApi = {
  register: (body) => request('/auth/register', { method: 'POST', data: body }),
  login: (body) => request('/auth/login', { method: 'POST', data: body }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  sendCode: (email, purpose = 'reset') =>
    request('/auth/send-code', { method: 'POST', data: { email, purpose } })
}

/** 用户中心 */
export const userApi = {
  profile: () => request('/user/profile'),
  quota: () => request('/user/quota')
}

/** 笔记 */
export const noteApi = {
  list: (params) => request('/notes', { params }),
  detail: (id) => request(`/notes/${id}`),
  create: (body) => request('/notes', { method: 'POST', data: body }),
  update: (id, body) => request(`/notes/${id}`, { method: 'PUT', data: body }),
  remove: (id, hard = false) => request(`/notes/${id}`, { method: 'DELETE', params: { hard } }),
  restore: (id) => request(`/notes/${id}/restore`, { method: 'POST' })
}

/** 标签 / 文件夹 */
export const tagApi = {
  list: () => request('/tags')
}

export const folderApi = {
  list: () => request('/folders')
}

/** AI 总结 */
export const aiApi = {
  summarize: (noteId) => request('/ai/summary', { method: 'POST', data: { noteId } }),
  history: (noteId) => request('/ai/summary', { params: { noteId } })
}
