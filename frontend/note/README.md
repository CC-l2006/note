# CipherWS 笔记前端

Vue 3 + Vite 的笔记站前端：登录/注册、笔记列表与编辑器、标签/文件夹、AI 总结、个人中心、公告和管理后台。

后端已合并为 **1 个 Spring Boot 应用**（端口 8084）；Nginx 是入口网关，开发时由 Vite 把 `/api` 代理到 8084，生产时由 Nginx 反代到同一个端口。

## 技术栈

| 技术 | 说明 |
| --- | --- |
| Vue 3.5 | Composition API + `<script setup>` |
| Vite 6.3 | 开发服务器 8002，构建产物 `dist/` |
| Vue Router 4 | history 模式，登录守卫 + 管理员守卫 |
| Pinia 2 | 用户 token / 用户信息 / Toast |
| 原生 fetch | `src/api/request.js` 统一封装 |
| 原生 CSS | 无 UI 组件库，自研纸感主题 |

## 目录

```text
frontend/note/
├── src/
│   ├── api/
│   │   ├── request.js          # fetch 封装、token、错误归一
│   │   └── index.js            # auth/user/note/tag/folder/ai/admin 接口
│   ├── components/             # AppButton/AppField/AppModal/AppPager...
│   ├── layouts/                # AuthLayout / MainLayout
│   ├── router/                 # 路由表与守卫
│   ├── stores/                 # user / toast
│   ├── styles/                 # 主题、排版、响应式
│   └── views/                  # auth / notes / admin / 用户中心
├── public/
├── dist/                       # 构建产物
├── vite.config.mjs
├── package.json
└── README.md
```

## 页面与功能

- 认证：登录、注册（邮箱验证码）、找回密码；
- 笔记：列表、搜索、编辑器、回收站；
- 分类：标签管理、文件夹、自动整理；
- AI：对当前笔记生成摘要、查看历史摘要；
- 用户中心：资料、头像、改密、换绑邮箱、AI 额度；
- 公告：登录用户查看启用中的公告；
- 后台：用户管理、笔记巡检、AI 配置、公告、日志、统计。

## 开发代理

`vite.config.mjs` 只有一个后端目标，`/api` 会去掉前缀后转到 8084：

| 前端请求 | 目标 |
| --- | --- |
| `/api/**` | `http://127.0.0.1:8084`（去掉 `/api`） |
| `/uploads/**` | `http://127.0.0.1:8084`（原样转发） |

服务内路径不带 `/api`，例如 `/api/notes` 到后端就是 `/notes`。

## 本地运行

要求 Node.js 20+，后端单体应用已启动（8084）。

```bash
cd frontend/note
npm install
npm run dev
```

打开 <http://127.0.0.1:8002>。

## 构建

```bash
npm run build
```

产物在 `dist/`。生产环境把 `dist/` 同步到 Nginx 静态目录，例如：

```bash
rsync -av --delete dist/ /opt/cipherws-note/frontend/note/dist/
nginx -s reload
```

## 环境变量

`.env.development` / `.env.production` 默认留空，表示同源：

```ini
VITE_API_BASE_URL=
```

开发时走 Vite 代理，生产时走 Nginx `/api` 反代。只有前后端不同域时才需要填完整地址；推荐同源部署。

## 请求约定

后端统一返回：

```json
{ "success": true, "data": {}, "total": 0, "errMessage": null }
```

`request.js` 会：

- 自动拼接 `/api` 前缀；
- 从 `localStorage` 读取 `note_token`，带 `Authorization: Bearer <token>`；
- 非 2xx 或 `success !== true` 时抛出 `ApiError`，页面统一提示 `errMessage`；
- 上传头像时使用 `FormData`，不手写 `Content-Type`。

## 说明

- 笔记编辑器是自研 Markdown 预览：先 HTML 转义，再渲染 Markdown，链接只允许 `http(s)`、`mailto:`、站内路径和锚点。
- token 存在 `localStorage`；退出登录只清前端 token，服务端 JWT 到期前仍有效。
- 前端不做业务权限判断，真正的越权和角色校验在后端。

## 相关文档

- [项目根 README](../../README.md)
- [架构说明](../../docs/ARCHITECTURE.md)
- [部署说明](../../docs/DEPLOYMENT.md)
- [接口清单](./src/api/index.js)
