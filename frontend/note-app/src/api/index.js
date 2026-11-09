import { request, upload } from './request.js'

/** 认证：注册、登录、发验证码、找回密码 */
export const authApi = {
  register: (body) => request('/auth/register', { method: 'POST', data: body }),
  login: (body) => request('/auth/login', { method: 'POST', data: body }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  sendCode: (email, purpose = 'reset') =>
    request('/auth/send-code', { method: 'POST', data: { email, purpose } }),
  resetPassword: (body) => request('/auth/reset-password', { method: 'POST', data: body })
}

/** 用户中心 */
export const userApi = {
  profile: () => request('/user/profile'),
  updateProfile: (body) => request('/user/profile', { method: 'PUT', data: body }),
  changePassword: (body) => request('/user/password', { method: 'PUT', data: body }),
  quota: () => request('/user/quota'),
  sendOldEmailCode: () =>
    request('/user/email/code', { method: 'POST', data: { purpose: 'bind_old' } }),
  verifyOldEmail: (code) =>
    request('/user/email/verify-old', { method: 'POST', data: { code } }),
  sendNewEmailCode: (email) =>
    request('/user/email/code', { method: 'POST', data: { purpose: 'bind_new', email } }),
  bindEmail: (body) => request('/user/email', { method: 'PUT', data: body }),
  sendPasswordCode: () => request('/user/password/code', { method: 'POST' }),
  uploadAvatar: (filePath) => upload('/user/avatar', filePath)
}

/** 笔记 */
export const noteApi = {
  list: (params) => request('/notes', { params }),
  detail: (id) => request(`/notes/${id}`),
  create: (body) => request('/notes', { method: 'POST', data: body }),
  update: (id, body) => request(`/notes/${id}`, { method: 'PUT', data: body }),
  move: (id, folderId) =>
    request(`/notes/${id}/move`, { method: 'POST', data: { folderId } }),
  remove: (id, hard = false) => request(`/notes/${id}`, { method: 'DELETE', params: { hard } }),
  restore: (id) => request(`/notes/${id}/restore`, { method: 'POST' }),
  trash: (params) => request('/notes/trash', { params }),
  clearTrash: () => request('/notes/trash', { method: 'DELETE' })
}

/** 标签 */
export const tagApi = {
  list: () => request('/tags'),
  create: (name) => request('/tags', { method: 'POST', data: { name } }),
  update: (id, name) => request(`/tags/${id}`, { method: 'PUT', data: { name } }),
  remove: (id) => request(`/tags/${id}`, { method: 'DELETE' })
}

/** 文件夹 */
export const folderApi = {
  list: () => request('/folders'),
  create: (body) => request('/folders', { method: 'POST', data: body }),
  update: (id, body) => request(`/folders/${id}`, { method: 'PUT', data: body }),
  remove: (id) => request(`/folders/${id}`, { method: 'DELETE' }),
  autoOrganize: () => request('/folders/auto-organize', { method: 'POST' })
}

/** AI */
export const aiApi = {
  summarize: (noteId) => request('/ai/summary', { method: 'POST', data: { noteId } }),
  history: (noteId) => request('/ai/summary', { params: { noteId } })
}

/** 公告 */
export const announcementApi = {
  list: () => request('/announcements')
}

/** 后台管理：仅 admin */
export const adminApi = {
  users: (params) => request('/admin/users', { params }),
  setUserStatus: (id, enabled) =>
    request(`/admin/users/${id}/status`, { method: 'PUT', data: { enabled } }),
  setUserQuota: (id, aiQuota) =>
    request(`/admin/users/${id}/quota`, { method: 'PUT', data: { aiQuota } }),

  notes: (params) => request('/admin/notes', { params }),
  flagNote: (id, flagged) =>
    request(`/admin/notes/${id}/flag`, { method: 'PUT', data: { flagged } }),

  aiConfig: () => request('/admin/ai-config'),
  saveAiConfig: (body) => request('/admin/ai-config', { method: 'PUT', data: body }),

  announcements: (params) => request('/admin/announcements', { params }),
  createAnnouncement: (body) => request('/admin/announcements', { method: 'POST', data: body }),
  updateAnnouncement: (id, body) =>
    request(`/admin/announcements/${id}`, { method: 'PUT', data: body }),
  removeAnnouncement: (id) =>
    request(`/admin/announcements/${id}`, { method: 'DELETE' }),

  logs: (params) => request('/admin/logs', { params }),
  stats: () => request('/admin/stats')
}
