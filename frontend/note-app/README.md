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
| 登录 | `pages/login/login` | 邮箱 + 密码登录 |
| 注册 | `pages/register/register` | 邮箱验证码注册 |
| 笔记 | `pages/notes/notes` | 列表、搜索、下拉刷新、上拉加载、长按删除 |
| 编辑 | `pages/notes/edit` | 新建/编辑、标签选择、AI 总结 |
| 我的 | `pages/profile/profile` | 资料、AI 额度、退出登录 |

## 手机端运行（主方式）

### 1. 启动后端

后端仍是 8084。手机和电脑要在同一个局域网，确保防火墙放行 8084。

```powershell
cd server
java -jar target\cipherws-note-0.0.1-SNAPSHOT.jar
```

### 2. 配置后端地址

手机 App 没有 Vite 代理，必须告诉它后端在哪里。复制 `.env.example` 为 `.env.local`：

```ini
# 改成电脑的局域网 IP，例如 192.168.1.20
VITE_API_BASE_URL=http://192.168.1.20:8084
```

不配置时会走默认值：

- Android 模拟器：`http://10.0.2.2:8084`
- iOS 模拟器：`http://127.0.0.1:8084`
- 真机：必须手动配置局域网 IP

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

默认 `http://localhost:5174`。H5 模式下 `/api` 会由 Vite 代理到 `http://127.0.0.1:8084`，只用于快速看页面，不是最终交付形态。

## 微信小程序（可选）

```bash
npm run dev:mp-weixin
```

微信开发者工具导入 `dist/dev/mp-weixin`，并在 `.env.local` 里配置可访问的 HTTPS 后端地址。

## 环境变量

```ini
# 手机 App：电脑局域网 IP 或线上域名
# H5 调试：可留空
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

- **真机必须配 `VITE_API_BASE_URL`**，不能用 `localhost`（那是手机自己）。
- Android 模拟器用 `10.0.2.2`，iOS 模拟器用 `127.0.0.1`。
- 开发阶段用 HTTP 时，Android 可能需要在 HBuilderX 的 manifest 中允许明文流量；iOS 需要 ATS 例外。正式环境建议直接上 HTTPS。
- 8084 只应在局域网/内网开放，不要直接暴露公网。
- 后端目前没有专门给 App 的 OAuth/刷新 token，仍复用 Web 端 JWT。

## 与 Web 端的关系

- `frontend/note`：PC/H5 Web 版，端口 8002；
- `frontend/note-app`：uni-app 手机 App，H5 预览端口 5174；
- 两者共用同一个后端 8084 与 `/api` 契约。

## 当前范围

已实现移动端核心闭环：

登录/注册 → 笔记列表 → 编辑/保存 → 标签 → AI 总结 → 个人中心。

文件夹管理、回收站、后台管理等复杂页面暂未搬到手机端，可继续按 `frontend/note` 的交互补齐。
