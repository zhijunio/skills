# sb4-oauth2-login

Spring Boot 4.1.1 / Java 25 示例：本地账号经 Spring Security 过滤器登录并签发 JWT；也可配置 GitHub、Google、Facebook、Apple 或自定义 OAuth 2.0 / OIDC 提供方登录。OAuth client registration 存储在 PostgreSQL 中，可由管理员通过网页维护；本地用户和短期 OAuth 兑换码仍保存在内存中。不提供刷新令牌。

## 登录与用户映射

- 本地登录由 `AuthenticationFilter`、`DaoAuthenticationProvider` 和独立的成功/失败处理器实现。
- OAuth 回调与 `state` 校验由 Spring Security 处理。登录成功后生成 60 秒有效、一次性兑换码，前端兑换本应用签发的 JWT。JWT 只保存在当前页面内存中，刷新页面后需要重新登录。
- 外部身份使用 `registrationId + provider subject` 复合键关联；首次成功登录时即时创建用户，不根据邮箱自动合并。后续登录更新非空显示名和邮箱，邮箱只作资料字段。
- 停用用户会阻止后续登录，JWT 解码时也会检查用户状态；管理员可在管理页面查看用户、停用或恢复账号。
- 用户自行解绑时不能移除唯一登录方式；管理员可强制解绑，但如果移除唯一外部身份，该用户下次通过此提供方登录会创建新用户。该示例未实现外部账号关联流程。
- `/api/me` 是受 JWT 保护的示例接口。JWT 默认 15 分钟过期，退出会清除页面内存里的 token。

## 启动

需要 Docker、Java 25（或可用的 Maven Wrapper Java 版本）及 OpenSSL。

```bash
cp .env.sample .env
```

生成并填写 `.env` 中的 `JWT_SECRET` 与 `OAUTH2_CLIENT_SECRET_ENCRYPTION_KEY`（两者都需要独立的 32 字节随机值，Base64 编码），设置本地演示密码，并按将要录入的提供方端点设置 `OAUTH2_ALLOWED_HOSTS`：

```bash
openssl rand -base64 32
```

启动 PostgreSQL 并运行应用：

```bash
docker compose up -d postgres
set -a; source .env; set +a
./mvnw spring-boot:run
```

Flyway 会自动创建应用表；V8 会删除旧版域名分配表及其数据。升级已有数据库前请备份，域名与 provider 的分配不会保留。访问 <http://localhost:8080> 使用用户名 `demo` 和 `.env` 中的 `DEMO_PASSWORD` 登录；访问 <http://localhost:8080/admin.html> 使用 `admin` 和 `ADMIN_PASSWORD` 管理提供方。管理 API 要求管理员 JWT，普通用户无权访问。

测试使用 Testcontainers 启动 PostgreSQL：

```bash
./mvnw test
```

## 管理 OAuth 提供方

管理员页提供 **OAuth client 模板和 OAuth client 管理**：模板只填充初始值，当前为只读预设；client 保存协议类型（OAuth 2.0 / OIDC）、端点、密钥、scope、扩展授权参数、用户属性映射和可选的登录按钮文案/短文本图标。所有已启用的 client 都会显示为登录提供方，适用于单应用示例；此示例不包含租户或按域名分配提供方的模型。

支持 Authorization Code，客户端认证方式为 `client_secret_basic`、`client_secret_post` 和 `none`。scope 可用空格或逗号分隔。OIDC 需要 `openid` scope、issuer URI 和 JWK Set URI；OAuth 2.0 需要用户信息 URI 与用户名属性。授权、token、userinfo 和 JWK 等 URI 由管理员手工填写，不执行 OIDC discovery。模板可预填端点和按钮文案，管理员仍需录入凭据并确认提供方配置。

可配置不超过 20 个额外授权参数（JSON 对象），例如 `{"prompt":"consent"}`；协议保留参数不可覆盖。用户映射可指定稳定 subject、显示名和邮箱的 claim key；未指定时使用 principal name / `sub`、常见显示名字段和 `email`。身份键仍使用 `registrationId + subject`，邮箱不会用于自动合并账号。

客户端密钥在写入 PostgreSQL 前通过 AES-256-GCM 加密。加密密钥来自 `OAUTH2_CLIENT_SECRET_ENCRYPTION_KEY`，必须是 Base64 编码的 32 字节值。修改配置时，Client Secret 留空会保留当前密钥；该密钥不会通过列表或保存接口返回。更换加密密钥前需要先迁移/重新加密数据库中的所有密钥，否则已有配置无法解密。

每个提供方的回调地址使用：

```text
https://<应用域名>/login/oauth2/code/{registrationId}
```

本地验证时域名为 `http://localhost:8080`。示例记录如下：

| 提供方 | Registration ID | 典型 scope | 配置说明 |
|---|---|---|---|
| GitHub | `github` | `read:user user:email` | OAuth App 的 Client ID/Secret；userinfo URI 为 `https://api.github.com/user`，用户名属性为 `id`。 |
| Google | `google` | `openid profile email` | 使用 OIDC issuer `https://accounts.google.com`；从可信的 Google OIDC discovery 文档填入 authorization、token 和 JWK Set URI。 |
| Facebook | `facebook` | `public_profile email` | 使用 OAuth 2.0 userinfo 配置；按 Meta 当前 API 版本配置授权、token、userinfo URI 和返回字段。 |
| Apple | `apple` | `openid email` | Services ID 为 Client ID；Client Secret 是 Apple 签名 JWT。 |
| Dex | `dex` | `openid profile email` | 示例容器可通过下方步骤启动。 |

实际端点与权限以提供方当前配置文档为准。`OAUTH2_ALLOWED_HOSTS` 是逗号分隔的精确 hostname allow-list，不支持通配符；authorization、token、userinfo、issuer 和 JWK endpoint 的 host 都必须在名单内。例如登录 GitHub 和 Google 时，需要添加相应的授权、API、token 和 JWKS 主机名；本地 Dex 使用默认 `localhost`。修改名单后重启应用。

端点测试只执行带超时限制的 GET 连通性检查，不会发送 Client Secret，也不能证明 OAuth 凭证、scope 或登录流程有效；401/403/405 等 HTTP 响应仍说明网络端点有响应。检查不会跟随重定向。配置管理仅限可信管理员，部署网络层还应限制应用的出站访问；hostname allow-list 不能替代防火墙对内网地址的约束。

变更审计记录 actor、配置 ID、变更字段、密钥是否更改和时间；不会保存密钥值，页面最多显示最近 100 条。审计事件与配置写入在同一数据库事务提交。共享 PostgreSQL 是实例间的配置同步点：每次 provider 查询直接读取数据库，当前没有进程内缓存，因此不需要额外的广播或刷新服务。

### 使用 Docker 启动 Dex

```bash
docker compose up -d dex
```

Dex 管理控制台配置在 `dex/config.yaml`，默认演示用户为 `admin@example.com` / `password`，客户端为 `sample-web` / `sample-web-secret`，回调为 `http://localhost:8080/login/oauth2/code/dex`。在管理员页面选择 **Dex** 模板，填入 Client ID/Secret：`sample-web` / `sample-web-secret` 并启用配置。模板已提供 OIDC 类型、`openid profile email` scope 和本地 Dex endpoints。

这些 URI 适用于从宿主机运行本应用的本地演示。如果应用运行在另一个 Docker 容器中，容器间访问地址需要相应调整，并把实际 hostname 纳入 allow-list。启用配置后，首页会显示 Dex 登录按钮。演示账户和密钥仅用于本地验证，不可用于生产。停止容器：`docker compose down`。

## API

- `POST /api/auth/login`：本地用户名/密码登录，返回 JWT。
- `POST /api/auth/oauth2/exchange`：用一次性 OAuth 兑换码取得 JWT。
- `GET /api/auth/providers`：列出数据库中已启用且端点 host 符合 allow-list 的提供方（公开）。
- `GET /api/admin/oauth-clients`：管理员列出配置；密钥只返回是否已配置，不返回明文。
- `GET /api/admin/oauth-clients/templates`：获取预设模板。
- `POST /api/admin/oauth-clients/{registrationId}/test`：检查当前已保存配置的 endpoint 网络连通性。
- `POST /api/admin/oauth-clients`：管理员创建配置。
- `PUT /api/admin/oauth-clients/{registrationId}`：管理员修改配置；密钥留空时保留原密钥。
- `DELETE /api/admin/oauth-clients/{registrationId}`：管理员删除配置。
- `GET /api/admin/oauth-clients/audit`：查看最近 100 条配置审计记录。
- `GET /api/admin/users`：查看内存中的用户和已绑定的提供方，不返回密码哈希或外部 subject。
- `PUT /api/admin/users/{userId}/status`：停用或恢复用户；不能停用当前管理员或最后一个有效管理员。
- `DELETE /api/admin/users/{userId}/external-identities/{provider}`：管理员解绑用户的外部登录方式；移除唯一身份后，该用户下次使用该提供方登录会创建新用户。
- `GET /api/me`：返回当前用户资料与外部登录方式。
- `DELETE /api/me/external-identities/{provider}`：解绑当前用户的一个外部身份。

管理端点要求 `SCOPE_ADMIN`。新建或更改的 enabled 配置会在下一次认证请求中从数据库读取，无需重启应用。应用放在反向代理后时，只有在代理会清除用户自带的 `Forwarded` / `X-Forwarded-Host` 并写入可信主机信息后，才应设置 `SERVER_FORWARD_HEADERS_STRATEGY=framework`；否则 OAuth 回调地址可能使用错误的外部主机名，也可能被伪造。反向代理也应限制可接受的 Host。

## 示例限制

用户、身份映射与兑换码仅存在单实例内存中，应用重启后丢失；生产环境应持久化，并对兑换码使用具 TTL 的共享存储。OAuth client 模板为只读预设；目前没有 OIDC discovery、配置导入导出、密钥轮换工具或端到端登录探测，端点检查只验证 HTTP 可达性。OAuth client Secret 和 JWT 密钥应由密钥管理系统提供。轮换数据库加密密钥需离线迁移所有密钥并安排停机窗口。审计数据应按部署策略备份并设置保留期。浏览器 JWT 可能受 XSS 窃取，生产浏览器应用可评估 BFF + HttpOnly Cookie 架构。
