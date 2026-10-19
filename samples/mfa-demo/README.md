# Spring Boot 4 MFA 示例

这是一个独立的 Spring Boot 4.1.1、Spring Security 7、Java 25、Maven 两步认证演示。项目仅使用内存账户和内存因子状态，不连接数据库。
中文演示页面使用蓝白配色。

> 仅供本地开发学习。演示账户、控制台邮件验证码和示例 relying-party 设置都不是生产方案。不要在公网部署。

## 本地启动

需要 JDK 25。运行：

```bash
./mvnw spring-boot:run
```

打开 <http://localhost:8080/>。本地演示账户为 `demo` / `changeit`。数据在内存中，重启即清除。构建验证：

OpenAPI JSON 位于 <http://localhost:8080/v3/api-docs>，Swagger UI 位于
<http://localhost:8080/swagger-ui/index.html>。在此本地、非生产演示中，文档页面和 OpenAPI 描述可匿名访问；描述中的业务 API 仍遵循两步认证和授权规则。生产环境应关闭 Swagger UI，并按需关闭或保护 OpenAPI 描述端点。

```bash
./mvnw test
./mvnw compile
```

## 两步认证与 API 边界

第一步可使用密码登录，或通过 Spring Security WebAuthn 的 passkey assertion 主认证。第二步验证 TOTP、开发用途邮件验证码、EIP-191 钱包签名或可重复验证的 BIP-39 助记词之一。MFA 完成状态保存在 Spring Security session 的认证主体与服务端 session 属性中；请求体和自定义请求头不能授予角色或 MFA 状态。

主认证未完成时，普通资源被拒绝；主认证完成但 MFA 未完成时，仅可访问 MFA challenge/验证流程、session/CSRF 引导信息及登出。完成第二因子后，普通 API 才开放。`/api/me/security`、`/api/mfa/**` 与 passkey 注册需最近 5 分钟内的 MFA 验证。因子替换走相同受保护 enrollment 流程；移除已注册因子使用 `DELETE /api/mfa/factors/{factorType}`，同样要求近期 MFA，支持 `TOTP`、`WALLET_SIGNATURE` 和 `BIP39_MNEMONIC`。示例不提供一次性恢复码。

浏览器 session 保持 CSRF 防护。先 `GET /api/csrf` 取得 token 和 header 名称，再通过同源请求在写操作中提交该 header。页面脚本会自动处理 token。登出为带 CSRF 的 `POST /logout`。

所有安全拒绝使用 RFC 9457 `application/problem+json`，包含 `type`、`title`、`status`、`detail` 字段。

## 因子请求流程

所有写请求都要附上 `/api/csrf` 返回的 CSRF header；以下 JSON 只展示请求体。

接口按用途分组：`/api/auth/mfa/*` 用于当前登录的第二步挑战与验证；`/api/mfa/*` 用于近期 MFA 保护的因子管理；`/api/me` 返回当前用户资料。浏览器状态可读 `GET /api/auth/state`，CSRF token 由 `GET /api/csrf` 获取。

### 邮件验证码

```http
POST /api/auth/mfa/challenge  {"factorType":"EMAIL_OTP"}
POST /api/auth/mfa/verify     {"factorType":"EMAIL_OTP","proof":"123456"}
Content-Type: application/json
{"code":"123456"}
```

邮件验证码只输出到本地应用日志，前缀为 `[DEVELOPMENT ONLY MFA EMAIL]`。验证码五分钟过期、仅能用一次、错误尝试最多五次，重新签发会替换旧码。生产环境必须接入邮件服务、限流并对账号地址做验证。

### TOTP

完成任一因子后五分钟内 enrollment：`POST /api/mfa/totp/enroll`，将返回 secret 和 `otpauth` URI；将其添加到认证器，再调用 `POST /api/mfa/totp/confirm`：

```json
{"code":"123456"}
```

之后使用 `POST /api/auth/mfa/verify`，请求体为 `{ "factorType":"TOTP", "proof":"123456" }` 提交新验证码。验证接受 ±1 个 30 秒时间步并拒绝已接受时间步重放。示例为了 enrollment 会将 TOTP secret 返回一次；不要记录该 secret。生产环境应加密静态存储并限制读取权限。

### Passkey

页面的 Passkey 注册按钮走 Spring Security WebAuthn `/webauthn/register/options` 和 `/webauthn/register` 浏览器凭据创建流程，要求近期 MFA。登录按钮走 `/webauthn/authenticate/options` 与 `/login/webauthn` assertion 流程，之后仍需一个不同的第二因子。浏览器 WebAuthn 需 localhost 或 HTTPS。凭据和 challenge 由 Spring Security WebAuthn 内存存储处理。演示 RP ID 为 `localhost`、origin 为 `http://localhost:8080`。

### 钱包签名

近期 MFA 后调用 `POST /api/mfa/wallet/enroll` 并提交地址：

```json
{"address":"0x..."}
```

用钱包对返回的**精确 message** 执行 EIP-191 `personal_sign`，然后 `POST /api/mfa/wallet/confirm`。服务端将 challenge 绑定到创建它的账户及 session，其他 session 提交签名会被拒绝：

```json
{"signature":"0x..."}
```

之后登录时调用 `/api/auth/mfa/challenge`（`{"factorType":"WALLET_SIGNATURE"}`）取得带随机 nonce 和过期时间的单次挑战，签署精确消息并向 `/api/auth/mfa/verify`（`{"factorType":"WALLET_SIGNATURE","proof":"0x..."}`）发送签名。服务端用 web3j 恢复签名地址并比对已绑定地址，校验当前 session 与创建 challenge 的 session 一致，成功即消费挑战以阻止重放。示例域名/URI 仅用于本地演示；生产环境应实现完整 SIWE 策略校验。

### BIP-39 助记词

近期 MFA 后调用 `POST /api/mfa/mnemonic/enroll`。服务端用 CSPRNG 生成 128 bit 熵，经 web3j 的 BIP-39 实现生成有效的 12 词助记词。它是专用于本应用 MFA 的登录凭据：创建时明文只显示一次，服务端保存 SHA-256 摘要；验证成功后仍可重复使用，不会被消费或轮换。连续五次验证失败会暂时锁定五分钟。用 `POST /api/auth/mfa/verify` 提交：

```json
{"factorType":"BIP39_MNEMONIC","proof":"word1 word2 ... word12"}
```

这是应用新生成的 BIP-39 助记词 MFA 凭据，不是一次性恢复码，也不用于找回本应用账户。它可重复用于 MFA 验证。BIP-39 助记词能够被钱包用于派生密钥，因此要像钱包种子一样保管：绝不能输入钱包，也绝不能向应用提交用户已有的钱包助记词。应用只校验新生成的凭据，不从中派生密钥，不执行钱包创建、恢复或签名。它是 bearer secret，不是抗钓鱼因子。

## 生产环境仍需补齐

- 使用持久化且访问受控的数据存储；对 TOTP secret 加密存储，并持久化 WebAuthn 凭据与 BIP-39 助记词摘要。
- 使用真实邮件提供商、已验证地址、发送频率控制、账号/IP 限速、审计告警和用户通知。
- 部署到 HTTPS，设置安全 session cookie、正确 RP ID/origin、域名和 SIWE chain/domain/URI/nonce 校验。
- 定义因子撤销、设备管理、恢复、备份和凭据轮换策略；增加并发安全的尝试限制和安全事件审计。
- 使用受管密钥、机密管理、备份恢复、监控与渗透测试流程；移除演示密码与控制台 OTP。

本示例的内存账户、`changeit` 演示密码、控制台邮件、内存存储和开发 RP 配置都不能作为生产实现。
