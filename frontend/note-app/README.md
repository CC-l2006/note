# CipherWS Note · uni-app 端

这是 `frontend/note`（Vue 3 Web 端）之外新增的 **uni-app** 客户端，复用同一套后端接口。

## 技术栈

- uni-app（Vue 3 + Vite）
- Pinia
- 目标端：H5、微信小程序（可继续扩展 App）

## 页面

| 页面 | 路径 | 功能 |
| --- | --- | --- |
| 登录 | `pages/login/login` | 邮箱 + 密码登录 |
| 注册 | `pages/register/register` | 邮箱验证码注册 |
| 笔记 | `pages/notes/notes` | 列表、搜索、下拉刷新、上拉加载、长按删除 |
| 编辑 | `pages/notes/edit` | 新建/编辑、标签选择、AI 总结 |
| 我的 | `pages/profile/profile` | 资料、AI 额度、退出登录 |

## 运行 H5

先启动后端 8084，然后在 `frontend/note-app` 下：

```bash
npm install
npm run dev:h5
```

打开终端提示的地址（默认 `http://localhost:5174`）。

H5 开发时 `vite.config.js` 已把 `/api` 代理到 `http://127.0.0.1:8084`，所以：

- 不需要改后端 CORS；
- 页面里请求 `/api/...`，代理会去掉 `/api` 前缀。

演示账号：

- 管理员：`admin@cipherws.icu / admin123`
- 普通用户：`demo@cipherws.icu / demo123`

## 微信小程序

```bash
npm run dev:mp-weixin
```

用微信开发者工具导入 `dist/dev/mp-weixin`。小程序没有 Vite 代理，需要把后端地址通过环境变量注入：

```ini
VITE_API_BASE_URL=https://你的后端域名
```

并保证后端允许小程序请求（正式环境建议用 HTTPS 域名，不要把 8084 直接暴露公网）。

## 环境变量

复制 `.env.example` 为 `.env.local`：

```ini
# H5 同源/走 Vite 代理时留空
# 小程序、App 或前后端不同域时填完整地址
VITE_API_BASE_URL=
```

## 目录

```text
note-app/
├── src/
│   ├── api/
│   │   ├── request.js      # uni.request 封装、token、错误
│   │   └── index.js        # auth/user/note/tag/folder/ai
│   ├── pages/
│   │   ├── login/
│   │   ├── register/
│   │   ├── notes/
│   │   └── profile/
│   ├── stores/user.js      # Pinia 登录态
│   ├── App.vue
│   ├── main.js
│   ├── pages.json
│   └── manifest.json
├── index.html
├── vite.config.js
├── package.json
└── README.md
```

## 与 Web 端的关系

- `frontend/note`：PC/H5 Web 版，端口 8002；
- `frontend/note-app`：uni-app 版，H5 端口 5174，也可构建小程序；
- 两者共用 `note-app` 后端（8084）与同一套 `/api` 契约。

## 说明

- 当前移动端先实现核心闭环：登录/注册 → 笔记列表 → 编辑/保存 → 标签 → AI 总结 → 个人中心。
- 文件夹管理、回收站、后台管理等复杂页面暂未搬到移动端，可后续按 `frontend/note` 的交互继续补齐。
- 小程序端如果遇到跨域/域名校验，按微信开发者工具与后端部署配置处理。
