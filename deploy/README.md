# deploy 脚本说明

后端已合并为 **1 个 Spring Boot 应用（8084）**，不再需要 Nacos、网关和 4 个服务进程。`note.cmd` 负责环境检查、安装缺失依赖、初始化数据库、启动/停止后端与前端。

## 文件

| 文件 | 用途 |
| --- | --- |
| `note.cmd` | 环境检查、安装缺失依赖、初始化数据库、启动/停止后端和前端 |
| `.tools/fetch-jdk17.ps1` | JDK 17 下载与解压（由 `note.cmd` 调用） |
| `note.env.example` | 环境变量模板 |
| `mail.env.example` | SMTP 模板 |
| `nginx/note.conf` | 生产 Nginx 配置（单 upstream 8084） |

## 快速开始

在项目根目录打开 cmd，或直接双击 `deploy\note.cmd`：

```cmd
deploy\note.cmd
```

默认执行 `start`：检查环境 → 安装缺失的 JDK/Node → 初始化数据库 → 打包后端 → 后台启动 8084 后端和 8002 前端。

首次运行会要求输入：

- MySQL 用户名（默认 `root`）；
- MySQL 密码；
- JWT 密钥（留空自动生成）。

输入后会生成 `deploy\note.env.local`，后续启动直接读取。

## 命令

| 命令 | 说明 |
| --- | --- |
| `deploy\note.cmd` | 同 `start` |
| `deploy\note.cmd check` | 只检查 JDK、Node、MySQL、端口，不安装、不启动 |
| `deploy\note.cmd setup` | 检查/安装依赖、写环境变量、初始化数据库、打包后端 |
| `deploy\note.cmd start` | 启动后端（8084）和前端（8002），后台运行 |
| `deploy\note.cmd start --no-pause` | 同上，但不等待按键，适合从 IDE / 终端调用 |
| `deploy\note.cmd build` | 强制重新打包后端 |
| `deploy\note.cmd stop` | 停止后端和前端 |
| `deploy\note.cmd status` | 查看 8084 / 8002 端口状态 |
| `deploy\note.cmd help` | 查看帮助 |

## 脚本会检查/安装什么

| 组件 | 行为 |
| --- | --- |
| JDK 17 | 查找 `JAVA_HOME`、`deploy\.tools\jdk-17`、常见 JDK 目录；找不到时询问是否下载 |
| Node.js 18+ / npm | 查找 PATH、`Program Files\nodejs`、`D:\tool\node\nodejs`、`deploy\.tools\node`；找不到时询问是否下载 Node 22 |
| MySQL 8 | **只检查，不自动安装**；要求 `mysql.exe` 可用且 3306 正在监听；检测到 `MySQL80` 服务时会尝试 `net start`（需要管理员权限） |
| Maven | 使用项目自带 `server\mvnw.cmd`，不单独安装 |
| 前端依赖 | `node_modules` 不存在时执行 `npm install` |

不再检查/安装 Nacos 和网关。

## 环境变量

脚本读取/生成：

```text
deploy\note.env.local
```

主要变量：

```ini
DB_URL=jdbc:mysql://127.0.0.1:3306/CipherWS?...
DB_USERNAME=root
DB_PASSWORD=你的密码
NOTE_JWT_SECRET=至少32字节的随机字符串
NOTE_JWT_TTL=2592000
NOTE_UPLOAD_DIR=D:\GraduationProject\note\server\uploads
```

`NOTE_JWT_SECRET` 至少 32 字节；不设置或过短会启动失败。
`note.env.local` 已被 `.gitignore` 忽略，不要提交。

## 数据库

脚本会连接 MySQL 检查 `CipherWS` 是否有表：

- 有表：跳过；
- 没有表：询问是否导入 `server\sql\note-schema.sql`。

也可以手工初始化：

```cmd
mysql -u root -p --default-character-set=utf8mb4 < server\sql\note-schema.sql
```

种子账号：`admin@cipherws.icu / admin123`、`demo@cipherws.icu / demo123`，首次登录后请修改。

## 启动结果

| 地址 | 说明 |
| --- | --- |
| http://127.0.0.1:8002 | 前端 Vite / 生产静态站 |
| http://127.0.0.1:8084 | 单体后端 |
| http://127.0.0.1:8084/actuator/health | 后端健康检查 |

脚本后台启动后端和前端（不弹多个 Java 窗口），只保留当前控制窗口；日志和 PID 写到 `deploy\.logs`，运行 `deploy\note.cmd stop` 停止。

## 常见问题

| 现象 | 处理 |
| --- | --- |
| `Could not resolve placeholder 'NOTE_JWT_SECRET'` | 检查 `note.env.local` 是否设置，且长度 ≥32 字节 |
| 连接 MySQL 失败 | 确认 `MySQL80` 服务启动；检查用户名/密码 |
| 数据库为空 | 同意脚本导入，或手工执行 `note-schema.sql` |
| 8084 被占用 | 先运行 `deploy\note.cmd stop`，或检查是否有旧 Java 进程 |
| 前端 8002 没起来 | 查看 `deploy\.logs\frontend.err.log`；确认 Node 18+，必要时删除 `node_modules` 重装 |
| 修改代码后没生效 | 运行 `deploy\note.cmd build` 重新打包，再 `start` |
| 后端启动失败 | 查看 `deploy\.logs\backend.err.log` |

## 说明

- 入口是 `.cmd`，但下载/解压、写环境变量等步骤内部调用 PowerShell；Windows 10/11 自带。
- 脚本不会自动安装 MySQL，也不会修改系统环境变量、注册表或服务。
- 自动下载的 JDK/Node 都放在 `deploy\.tools`，不污染系统。
- 生产部署用 Nginx 反代 8084，参考 `deploy/nginx/note.conf`。
