# note 后端架构（单体）

## 1. 总览

```text
浏览器 / Vue
   │  /api/**  /uploads/**
   ▼
Nginx（生产） / Vite（开发）
   │  /api 去掉前缀，统一转到 8084
   ▼
note-app :8084（一个 Spring Boot 进程）
   ├─ user 包
   ├─ core 包
   ├─ ai 包
   └─ admin 包
   │
   └── MySQL CipherWS
```

原来的 user / core / ai / admin 四个服务已经合并成一个应用：

- 一个启动类 `NoteApplication`；
- 一个端口 8084；
- 一个数据源、一个 MyBatis 配置、一个 `application.yml`；
- 一个 Maven 模块 `server/pom.xml`；
- 不再有 Nacos、Spring Cloud、网关。

## 2. 包结构

| 包 | 职责 |
| --- | --- |
| `common` | `Result`、`BusinessException`、`GlobalExceptionHandler`、`JwtUtil`、`Auth`、`JwtAuthFilter`、实体 |
| `user` | 注册/登录/验证码/资料/头像/改密/换绑邮箱 |
| `core` | 笔记 CRUD、回收站、标签、文件夹、自动整理 |
| `ai` | AI 总结、额度扣减、总结历史 |
| `admin` | 用户管理、巡检、公告、AI 配置、日志统计 |

每个包内部保持原来的分层：

```text
<module>/
├── controller/
├── service/
│   └── impl/
├── mapper/
└── dto/
```

SQL 统一在 `src/main/resources/mapper/*.xml`。

## 3. 鉴权

1. `user` 包登录成功后签发 HS256 JWT，载荷含 `uid`、`sub`（邮箱）、`role`、`iat`、`exp`，默认 30 天。
2. `common` 包的 `JwtAuthFilter` 是 Servlet `OncePerRequestFilter`：
   - 白名单：`/auth/register`、`/auth/login`、`/auth/send-code`、`/auth/reset-password`、`/actuator/**`、`/uploads/**`；
   - 其余请求读取 `Authorization: Bearer <token>` 并验签；
   - 先删除客户端伪造的 `X-User-*`，验签通过后写回可信身份头。
3. 控制器用 `@RequestHeader` 读身份；`Auth.requireAdmin(role)` 拦截非管理员。
4. `NOTE_JWT_SECRET` 至少 32 字节；未设置或过短时启动失败。

## 4. 数据模型

10 张表：`note_user`、`note`、`note_tag`、`note_tag_rel`、`note_folder`、`note_ai_summary_record`、`note_ai_config`、`note_system_announcement`、`note_user_operation_log`、`note_admin_operation_log`。

单体应用直接用一个数据库，不再有跨服务读写和分布式事务问题。所有用户数据查询都带 `user_id` 条件。

## 5. 设计取舍

- **合并单体**：减少进程、端口、Nacos、配置和部署复杂度；代价是无法按服务独立扩容。
- **保留包边界**：user/core/ai/admin 仍按包隔离，后续要再拆服务时改动可控。
- **Nginx 做入口**：只反代 8084，去掉 `/api` 前缀；`/uploads` 原样转发。
- **单密钥**：一个应用只需要一个 `NOTE_JWT_SECRET`。
- **单库**：原来 4 个服务共享一个库，现在只是单体应用的一个库，跨服务读写和分布式事务问题自然消失。

## 6. 已知限制

- JWT 无 refresh/吊销；禁用、降权在 token 到期前不会即时生效。
- 验证码、发送冷却、旧邮箱验证标记在 JVM 内存；多实例需要 Redis。
- AI 先调模型、成功后再扣额度，存在并发/落库失败的窗口。
- 没有自动化测试与 CI。
- AI 密钥明文存库，接口只回掩码。
