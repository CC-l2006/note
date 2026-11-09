# note 部署说明（单体）

## 1. 前置条件

- JDK 17（可用 `deploy/.tools/fetch-jdk17.ps1` 下载到项目内，再用 `deploy/.tools/dev-env.ps1` 激活）
- MySQL 8，字符集 `utf8mb4`
- Node.js 20+（仅前端）
- Nginx（生产静态站 + 反代 8084）

不需要 Nacos、Spring Cloud 或 Java 网关。

## 2. 数据库

全新库：

```bash
mysql -u root -p --default-character-set=utf8mb4 < server/sql/note-schema.sql
```

脚本会建库 `CipherWS`、10 张表、索引和种子账号：

- `admin@cipherws.icu / admin123`
- `demo@cipherws.icu / demo123`

上线前改掉这两个口令。存量库按需执行 `server/sql/` 下的迁移脚本，执行前先备份。

## 3. 环境变量

推荐直接用一键脚本 `deploy\note.cmd`：它会检查环境、安装缺失依赖、提示输入 MySQL 密码并生成/读取 `deploy\note.env.local`。

也可以手动执行 `copy deploy\note.env.example deploy\note.env.local`，编辑后在启动服务的 shell 或 IDE 运行配置里导入：

| 变量 | 说明 |
| --- | --- |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 数据库连接 |
| `NOTE_JWT_SECRET` | **必填**，≥32 字节 |
| `NOTE_JWT_TTL` | token 有效期，默认 2592000 秒（30 天） |
| `NOTE_UPLOAD_DIR` | 头像目录，建议绝对路径 |
| `NOTE_AI_TIMEOUT` | AI 读超时，默认 60 秒 |
| `SPRING_MAIL_*` / `NOTE_MAIL_ENABLED` / `MAIL_FROM` | 可选 SMTP |

## 4. 构建

```powershell
cd server
.\mvnw.cmd -DskipTests package
cd ..
```

产物只有一个：

```text
server/target/cipherws-note-0.0.1-SNAPSHOT.jar
```

## 5. 启动

推荐：

```cmd
deploy\note.cmd
```

脚本会后台启动 8084 后端和 8002 前端，日志在 `deploy\.logs`，`deploy\note.cmd stop` 停止。

手动启动：

```powershell
$env:NOTE_JWT_SECRET = '至少32字节的随机字符串'
$env:DB_PASSWORD = '你的数据库密码'
$env:NOTE_UPLOAD_DIR = 'D:\GraduationProject\note\server\uploads'

java -jar server\target\cipherws-note-0.0.1-SNAPSHOT.jar
```

## 6. 验证

```powershell
# 受保护接口，无 token 应 401
curl.exe -s -o NUL -w "%{http_code}`n" http://127.0.0.1:8084/notes

# 健康检查（MySQL 未启动时可能 503）
curl.exe -s -o NUL -w "%{http_code}`n" http://127.0.0.1:8084/actuator/health
```

## 7. 前端开发

```bash
cd frontend/note
npm install
npm run dev
```

打开 `http://127.0.0.1:8002`。Vite 把 `/api` 去掉前缀后转到 8084，`/uploads` 原样转到 8084。

## 8. Nginx 生产

`deploy/nginx/note.conf` 只有一个 upstream：

```nginx
upstream note_backend {
    server 127.0.0.1:8084;
}
```

`/api/**` 去掉 `/api` 后转发，`/uploads/**` 原样转发，其余是前端静态文件。

如果后端在另一台机器，只需要给 8084 开一条隧道，把 upstream 改成隧道地址即可。

## 9. 常见问题

| 现象 | 排查 |
| --- | --- |
| 启动报 `Could not resolve placeholder 'NOTE_JWT_SECRET'` | 没设 JWT 密钥 |
| 登录成功但接口 401 | JWT 密钥长度不足/不一致，或请求没带 `Authorization` |
| 所有接口 404 | Nginx/Vite 没去掉 `/api`，或端口不是 8084 |
| 头像 404 | `NOTE_UPLOAD_DIR` 与启动工作目录不一致；用绝对路径 |
| 8084 被占用 | 先 `deploy\note.cmd stop`，再检查旧 Java 进程 |
| 前端 8002 没起来 | 看 `deploy\.logs\frontend.err.log` |
| 后端启动失败 | 看 `deploy\.logs\backend.err.log` |
