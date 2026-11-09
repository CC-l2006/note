# CipherWS Note · uni-app 手机端

这是 `frontend/note`（Vue 3 Web 端）之外新增的 **uni-app 手机 App**，不是 H5 网页端。

- 主目标：Android / iOS App（uni-app 原生 App）
- 调试预览：H5（仅用于不装模拟器时快速看页面）
- 可选扩展：微信小程序

## 技术栈

- uni-app（Vue 3 + Vite）
- Pinia
- `uni.request` / `uni.setStorageSync` 等原生能力
- 目标端：Android / iOS App

## 页面

| 页面 | 路径 | 功能 |
| --- | --- | --- |
| 登录 | `pages/login/login` | 邮箱 + 密码登录，不再提供演示账号入口 |
| 注册 | `pages/register/register` | 邮箱验证码注册 |
| 找回密码 | `pages/forgot/forgot` | 邮箱验证码重设密码 |
| 笔记 | `pages/notes/notes` | 列表、搜索、文件夹/标签筛选、下拉刷新、上拉加载、移动、删除 |
| 编辑 | `pages/notes/edit` | 新建/编辑、标签、文件夹、AI 总结与历史 |
| 标签 | `pages/tags/tags` | 标签增删改查 |
| 文件夹 | `pages/folders/folders` | 文件夹增删改查、按同名标签自动整理 |
| 回收站 | `pages/trash/trash` | 恢复、永久删除、清空 |
| 公告 | `pages/announcements/announcements` | 查看已发布公告 |
| 我的 | `pages/profile/profile` | 资料、AI 额度、功能入口、退出登录 |
| 账号设置 | `pages/settings/settings` | 资料、头像、改密、换绑邮箱 |
| 管理后台 | `pages/admin/admin` | 用户、巡检、公告、AI 配置、日志、统计（仅 admin） |

## 笔记 Markdown

编辑页支持“编辑 / 预览”双模式：

- 用 `markdown-it` 渲染正文，关闭原始 HTML，避免 XSS；
- H5 用 `v-html`，App/小程序用 `rich-text`；
- 支持标题、加粗、斜体、行内代码、代码块、引用、有序/无序列表、链接、表格、删除线；
- 列表摘要会先去掉 Markdown 标记，不会直接显示 `#`、`**`、反引号。

## AI 摘要

编辑已有笔记时可生成 AI 摘要，摘要用与正文相同的 Markdown 渲染：

- 底部面板，不再使用简陋的系统弹窗；
- 支持“预览 / 编辑”切换，摘要可直接修改 Markdown；
- 可查看历史摘要，点击历史记录载入；
- “替换正文”会用当前摘要覆盖笔记正文，确认后再点“保存笔记”落库；
- 摘要和正文都关闭原始 HTML，避免 XSS。

## 手机端运行（主方式）

### 1. 后端地址

默认直接使用已部署的公网后端：

```text
https://frp-box.com:64456
```

请求路径仍然是 `/api/...`，Nginx 会去掉 `/api` 前缀后转给后端。

不需要本机再启动后端；只有本地二次开发时才需要起 8084。

### 2. 配置（可选）

默认已经使用 `https://frp-box.com:64456`。如需覆盖，复制 `.env.example` 为 `.env.local`：

```ini
VITE_API_BASE_URL=https://frp-box.com:64456
```

### 3. 编译 App 资源

```bash
npm install
npm run build:app
```

产物在 `dist/build/app`。

### 4. 用 HBuilderX 打包/运行

uni-app 的 **APK / IPA 原生打包需要 HBuilderX**（云打包或本地打包）。CLI 只负责把 Vue 页面编译成 App 资源。

`src/manifest.json` 里的 `appid` 目前是占位值；正式云打包前，在 HBuilderX 中申请 DCloud AppID 并替换。

两种方式：

1. 用 HBuilderX 直接打开 `frontend/note-app`，选择“运行 → 运行到手机或模拟器”；
2. 或把 `dist/build/app` 导入 HBuilderX，按 HBuilderX 的 App 打包流程生成 APK/IPA。

> 如果你希望我继续把 HBuilderX 的 `manifest.json` 打包参数（应用图标、启动图、Android 权限、iOS 证书）也配好，可以下一步继续。

### 关于 HTTPS 证书

`frp-box.com:64456` 当前证书不在系统信任链里。Android 开发包可以临时设置：

```ini
VITE_SSL_VERIFY=false
```

让 `uni.request` 跳过证书校验（仅 App-Android 生效，且仅建议开发阶段使用）。

iOS 仍会校验证书，需要给 frp 换一张受信任的 HTTPS 证书，或使用带正式证书的域名。

## 图标

直接复用 Web 端 `frontend/note/public` 的品牌资源，不另做一套：

- 登录页品牌图：`src/static/logo.png`
- App 桌面图标：`src/static/icon-180.png` 生成到 `src/static/icons/`，已在 `manifest.json` 的 `app-plus.distribute.icons` 中配置 Android / iOS / iPad 各尺寸
- TabBar：从 Web `favicon.ico` 生成的 `tab-notes*.png` / `tab-profile*.png`

所以 App 的登录页、桌面图标和底部 Tab 用的都是 Web 端同一套品牌图标。

## 运行 H5（仅调试）

```bash
npm run dev:h5
```

默认 `http://localhost:5174`。H5 模式下 `/api` 由 Vite 代理到远程 `https://frp-box.com:64456`，只用于快速看页面，不是最终交付形态。

如果要用本地后端调 H5，启动前设置：

```bash
set VITE_PROXY_TARGET=http://127.0.0.1:8084
npm run dev:h5
```

注意：改了 `vite.config.js` 后必须重启 dev server，HBuilderX 里要“重新运行”，只刷新页面不会生效。

## 微信小程序（可选）

```bash
npm run dev:mp-weixin
```

微信开发者工具导入 `dist/dev/mp-weixin`，并在 `.env.local` 里配置可访问的 HTTPS 后端地址。

## 环境变量

```ini
# 默认公网后端
VITE_API_BASE_URL=https://frp-box.com:64456

# 仅 App-Android；开发阶段可跳过不受信任证书
VITE_SSL_VERIFY=true
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
│   ├── config.js           # App / H5 后端地址
│   ├── App.vue
│   ├── main.js
│   ├── pages.json
│   └── manifest.json
├── index.html
├── vite.config.js
├── package.json
└── README.md
```

## 手机端注意事项

- 默认后端是 `https://frp-box.com:64456`，请求路径为 `/api/...`。
- 该地址当前证书不受系统信任：Android 开发包可设 `VITE_SSL_VERIFY=false`；iOS 需要受信任证书。
- 正式环境建议给 frp 换正式 HTTPS 证书，不要长期关闭证书校验。
- 后端目前没有专门给 App 的 OAuth/刷新 token，仍复用 Web 端 JWT。

## 与 Web 端的关系

- `frontend/note`：PC/H5 Web 版，端口 8002；
- `frontend/note-app`：uni-app 手机 App，H5 预览端口 5174；
- 两者共用同一套 `/api` 契约；手机 App 默认走远程 frp，本地二次开发可切 8084。

## 当前范围

已对齐 Web 端的主要功能：

- 认证：登录、注册、找回密码
- 笔记：列表、搜索、文件夹/标签筛选、新建/编辑、移动、软删除
- 组织：标签 CRUD、文件夹 CRUD、自动整理
- 回收站：恢复、永久删除、清空
- AI：生成摘要、查看历史、额度
- 账号：资料、头像、改密、换绑邮箱
- 公告：查看
- 管理后台（admin）：用户、巡检、公告、AI 配置、日志、统计

登录页不再放演示账号；正式账号由注册或后台创建。复杂交互（例如 Web 端的分栏编辑器、拖拽排序）已改为适合手机的上下布局。
