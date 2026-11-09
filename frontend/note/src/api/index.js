/**
 * 接口清单：页面对接后端的唯一入口。
 * 每个方法都是薄封装，返回 { data, total? }，失败抛 ApiError。
 * mock / http 两种模式的差异全部收敛在 request.js，这里不需要关心。
 */

import { request, upload } from './request.js'

/* =============================================================================
   接口分组
   下面按业务模块导出多个对象，页面只 import 对应模块，避免一坨大杂烩函数。
   ============================================================================= */

/* ---------------- 认证 ---------------- */
/**
 * 认证相关接口：注册、登录、退出、验证码、找回密码。
 * 全部为免登录接口，唯一例外是 logout（需要带上当前令牌）。
 */
export const authApi = {
  /* 注册：邮箱 + 验证码 + 密码（昵称可选） */
  register: (body) => request('/auth/register', { method: 'POST', body }),
  /* 登录：邮箱 + 密码，成功返回 { token, user } */
  login: (body) => request('/auth/login', { method: 'POST', body }),
  /* 退出：让后端作废当前令牌，本地状态由 store 负责清空 */
  logout: () => request('/auth/logout', { method: 'POST' }),
  /**
   * 发验证码。purpose 决定后端怎么校验这个邮箱：
   *   'register' 注册 —— 邮箱必须**未**注册
   *   'reset'    找回密码（缺省）—— 邮箱必须已注册
   * 换绑邮箱不走这里（要登录态），用 userApi.sendOldEmailCode / sendNewEmailCode。
   */
  sendCode: (email, purpose = 'reset') =>
    request('/auth/send-code', { method: 'POST', body: { email, purpose } }),
  resetPassword: (body) => request('/auth/reset-password', { method: 'POST', body })
}

/* ---------------- 用户中心 ---------------- */
/**
 * 用户中心接口：资料、密码、AI 额度、换绑邮箱、头像。
 * 全部需要登录态（request 会自动带上 Authorization）。
 */
export const userApi = {
  /* 读取当前登录用户资料，路由守卫刷新页面时用它恢复登录态 */
  profile: () => request('/user/profile'),
  // 注意：换邮箱不走这里，必须走「先验旧邮箱、再验新邮箱」两步（见下方 userApi）
  updateProfile: (body) => request('/user/profile', { method: 'PUT', body }),
  /* 修改密码：需要邮箱验证码，验证码由 sendPasswordCode 单独发 */
  changePassword: (body) => request('/user/password', { method: 'PUT', body }),
  /* 查询 AI 总结剩余次数，用于侧栏额度和禁用按钮 */
  quota: () => request('/user/quota'),

  /* ---- 更换绑定邮箱：先验旧邮箱，再验新邮箱，两步都过才生效 ---- */
  /** 第 1 步：给**当前**邮箱发验证码（不用传 email，服务端自己取） */
  sendOldEmailCode: () =>
    request('/user/email/code', { method: 'POST', body: { purpose: 'bind_old' } }),
  /** 第 1 步校验：码对了才能进入第 2 步（服务端记 10 分钟） */
  verifyOldEmail: (code) =>
    request('/user/email/verify-old', { method: 'POST', body: { code } }),
  /** 第 2 步：给**新**邮箱发验证码 */
  sendNewEmailCode: (email) =>
    request('/user/email/code', { method: 'POST', body: { purpose: 'bind_new', email } }),
  /** 第 2 步确认：新邮箱 + 新邮箱收到的码 */
  bindEmail: (body) => request('/user/email', { method: 'PUT', body }),

  /* ---- 修改密码：需要发到当前邮箱的验证码 ---- */
  /** 给当前邮箱发验证码 */
  sendPasswordCode: () => request('/user/password/code', { method: 'POST' }),

  /** 上传头像（multipart） */
  uploadAvatar: (file) => upload('/user/avatar', file)
}

/* ---------------- 笔记 ---------------- */
/**
 * 笔记核心接口：列表、详情、增删改、移动、回收站。
 * 列表与回收站都支持分页 + 筛选参数，直接透传给后端。
 */
export const noteApi = {
  /* 列表：支持 keyword / tagId / folderId / page 等查询参数 */
  list: (params) => request('/notes', { params }),
  /* 详情：编辑页按 id 拉取正文与元信息 */
  detail: (id) => request(`/notes/${id}`),
  /* 新建：返回创建后的笔记（含新 id） */
  create: (body) => request('/notes', { method: 'POST', body }),
  /* 更新：编辑保存时整体提交标题 / 正文 / 标签 */
  update: (id, body) => request(`/notes/${id}`, { method: 'PUT', body }),
  /** 把笔记放进文件夹；folderId 传 null 表示移出到未分类 */
  move: (id, folderId) => request(`/notes/${id}/move`, { method: 'POST', body: { folderId } }),
  /* 删除：默认软删（进回收站），hard=true 时彻底删除 */
  remove: (id, hard = false) => request(`/notes/${id}`, { method: 'DELETE', params: { hard } }),
  /* 从回收站恢复一条笔记 */
  restore: (id) => request(`/notes/${id}/restore`, { method: 'POST' }),
  /* 回收站列表：与正常列表同样带分页参数 */
  trash: (params) => request('/notes/trash', { params }),
  /* 清空回收站：不可撤销，调用前必须二次确认 */
  clearTrash: () => request('/notes/trash', { method: 'DELETE' })
}

/* ---------------- 标签 ---------------- */
/**
 * 标签接口：标签是笔记的一级分类，名称在用户内唯一。
 */
export const tagApi = {
  /* 全部标签（体量小，不做分页） */
  list: () => request('/tags'),
  /* 新建标签，只传名称 */
  create: (name) => request('/tags', { method: 'POST', body: { name } }),
  /* 重命名标签 */
  update: (id, name) => request(`/tags/${id}`, { method: 'PUT', body: { name } }),
  /* 删除标签：关联笔记的标签关系由后端一并清理 */
  remove: (id) => request(`/tags/${id}`, { method: 'DELETE' })
}

/* ---------------- 文件夹（笔记直接归入；自动整理按同名标签） ---------------- */
/**
 * 文件夹接口：与标签互补 —— 标签描述内容，文件夹描述存放位置。
 */
export const folderApi = {
  /* 全部文件夹 */
  list: () => request('/folders'),
  /* 新建文件夹，body 里一般包含 name */
  create: (body) => request('/folders', { method: 'POST', body }),
  /* 重命名 / 调整文件夹 */
  update: (id, body) => request(`/folders/${id}`, { method: 'PUT', body }),
  /* 删除文件夹：其中的笔记回到未分类 */
  remove: (id) => request(`/folders/${id}`, { method: 'DELETE' }),
  /** 把未分类笔记按「文件夹名 = 标签名」自动归入，返回归类数量 */
  autoOrganize: () => request('/folders/auto-organize', { method: 'POST' })
}

/* ---------------- AI 总结 ---------------- */
/**
 * AI 总结接口：按笔记 id 生成摘要，并保留历史供侧栏「页边批注」展示。
 * 会消耗用户 AI 额度，额度不足时后端返回失败，由页面弹提示。
 */
export const aiApi = {
  /* 生成总结：POST 触发一次调用，返回摘要正文 */
  summarize: (noteId) => request('/ai/summary', { method: 'POST', body: { noteId } }),
  /* 查询该笔记的历史总结：GET 同一路径，参数放在查询串里 */
  history: (noteId) => request('/ai/summary', { params: { noteId } })
}

/* ---------------- 公告（对所有登录用户可见） ---------------- */
/**
 * 公告接口：面向普通用户，只读；管理端的增删改在 adminApi 里。
 */
export const announcementApi = {
  /* 读取已发布的公告列表 */
  list: () => request('/announcements')
}

/* ---------------- 后台管理 ---------------- */
/**
 * 后台接口：仅 admin 角色可调用，路由层已用 meta.roles 拦住非管理员。
 * 分四块：用户、笔记巡检、AI 参数、公告；另有日志与统计。
 */
export const adminApi = {
  /* ---- 用户管理 ---- */
  /* 用户列表：支持关键字、状态筛选与分页 */
  users: (params) => request('/admin/users', { params }),
  /* 启用 / 禁用某个账号（enabled 为 false 时该用户无法登录） */
  setUserStatus: (id, enabled) => request(`/admin/users/${id}/status`, { method: 'PUT', body: { enabled } }),
  /* 调整某用户的 AI 调用额度 */
  setUserQuota: (id, aiQuota) => request(`/admin/users/${id}/quota`, { method: 'PUT', body: { aiQuota } }),

  /* ---- 笔记巡检 ---- */
  /* 全站笔记列表：支持按用户 / 标记状态筛选 */
  notes: (params) => request('/admin/notes', { params }),
  /* 标记违规笔记，flagged 为 true 时该笔记对用户侧隐藏或提示 */
  flagNote: (id, flagged) => request(`/admin/notes/${id}/flag`, { method: 'PUT', body: { flagged } }),

  /* ---- AI 参数配置 ---- */
  /* 读取当前模型参数（模型名、温度、上限等） */
  aiConfig: () => request('/admin/ai-config'),
  /* 保存模型参数，整体覆盖 */
  saveAiConfig: (body) => request('/admin/ai-config', { method: 'PUT', body }),

  /* ---- 公告管理 ---- */
  /* 公告列表：含未发布草稿，带分页 */
  announcements: (params) => request('/admin/announcements', { params }),
  /* 新建公告（草稿） */
  createAnnouncement: (body) => request('/admin/announcements', { method: 'POST', body }),
  /* 编辑公告内容 / 发布状态 */
  updateAnnouncement: (id, body) => request(`/admin/announcements/${id}`, { method: 'PUT', body }),
  /* 删除公告 */
  removeAnnouncement: (id) => request(`/admin/announcements/${id}`, { method: 'DELETE' }),

  /* ---- 日志与统计 ---- */
  /* 操作日志：按时间区间 / 操作人 / 类型筛选 */
  logs: (params) => request('/admin/logs', { params }),
  /* 概览统计：用户数、笔记数、AI 调用次数等聚合值 */
  stats: () => request('/admin/stats')
}
