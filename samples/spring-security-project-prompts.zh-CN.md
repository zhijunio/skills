# Spring Security 示例项目提示词库

可将下列任一代码块直接交给 `$spring-boot-developer` 技能使用。各提示词彼此独立，也允许与仓库现有项目或其他提示词重叠。

除非某个主题另有说明，示例统一使用 Spring Boot 4.1.1、Java 25、Maven，配置使用 YAML；创建前检查仓库约定，使用新的项目目录，不覆盖现有文件。README 应说明本地启动、认证与授权边界、配置项和可复现请求。凭据和密钥不得提交。完成后运行相关 Maven 测试与编译，并报告命令和实际结果。

| 顺序 | 主题                                                   | 重点                             | 难度  |
|------|--------------------------------------------------------|----------------------------------|-------|
| 1    | URL 与方法级资源授权                                   | 单一服务的 URL 与方法授权        | 易    |
| 2    | Actuator 管理面隔离                                    | 管理端点边界与最小暴露           | 易—中 |
| 3    | OAuth2 Client Credentials 服务间调用                   | 服务身份与下游调用               | 中    |
| 4    | Opaque Token Resource Server                           | Introspection 与 scope 映射      | 中    |
| 5    | 浏览器 Session 与 Bearer API 安全边界                  | Session、CSRF、CORS 与 API chain | 中    |
| 6    | OAuth2 / OIDC 登录与 provider 管理后台                 | 外部身份映射与动态 provider 管理 | 中—难 |
| 7    | 本地账号密码凭据生命周期                               | 密码存储、重置和抗枚举           | 中—难 |
| 8    | JWT 签发、刷新与撤销                                   | 密钥、刷新轮换与撤销             | 难    |
| 9    | MFA 与敏感操作 step-up                                 | 多因子、恢复和强化认证           | 难    |
| 10   | SAML 2.0 Service Provider                              | 企业联邦登录和断言验证           | 难    |
| 11   | 多 issuer JWT Resource Server                          | 多个 issuer 的信任和 claim 映射  | 难    |
| 12   | 多租户授权与数据隔离                                   | 租户上下文和全链路隔离           | 难    |
| 13   | OAuth2 Authorization Server：Authorization Code + PKCE | 授权服务器协议与 PKCE            | 最难  |

## 1. URL 与方法级资源授权

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
REST API，演示 URL 级授权和方法级授权如何配合。

使用 Spring Security request authorization、@EnableMethodSecurity 和 @PreAuthorize。
普通用户只能读取本人资源；具有指定 authority 的用户才能执行管理操作。资源 owner 与
authorities 必须从已认证 Authentication/principal 及可信数据中判断，
不接受请求体或自定义请求头传入角色。所有权检查放在可复用的服务/访问策略层，保证绕过
controller 也受保护。

使用简单内存数据，不加无关数据库。统一返回 401、403 RFC 9457 ProblemDetail。
测试本人/他人资源、普通用户管理拒绝、管理员成功、缺失凭证和伪造角色输入。README 说明主体、
authority 映射、URL 与方法规则各自职责，并给出 curl 示例。
```

## 2. Actuator 管理面隔离

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示业务 API 与 Actuator 管理端点的安全隔离。

使用 matcher 与顺序明确的 SecurityFilterChain 单独保护 Actuator，并要求专用管理
authority。只公开明确批准的 liveness/readiness 信息；metrics、env、configprops、
mappings 等默认不公开，不向匿名用户显示 health details。说明独立 management port
是网络边界之一，不能替代认证和网络访问控制。

测试各 chain 的匹配/顺序、匿名请求、authority 不足和获准访问，
确认管理路由不会落入宽泛业务规则。401/403 使用 RFC 9457 ProblemDetail。README 提供配置、
请求示例和生产网络边界说明。无需数据库。
```

## 3. OAuth2 Client Credentials 服务间调用

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示服务使用 OAuth2 Client Credentials 调用受保护的下游 API。

使用 Spring Security OAuth2 Client 和 OAuth2AuthorizedClientManager 获取、缓存和续期
token，不自行实现 token endpoint 或缓存。client ID、secret、issuer/token endpoint 和
scope 从环境变量读取。下游 base URL 只能来自可信配置 allowlist，
不能由用户输入拼接请求目标，以防 SSRF。

提供触发下游调用的端点，使用受管理的 HTTP client、明确超时和有限重试。区分下游 401、
403、超时和暂时性错误，不将敏感下游响应透传。支持本地 mock server 或可选真实 issuer。
README 明确 client credentials 表示服务身份，不代表最终用户。

测试 token 获取/续期、scope、下游错误和 SSRF URL 拒绝。不得提交密钥。README
提供本地配置与调用示例。
```

## 4. Opaque Token Resource Server

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
REST API，演示 OAuth2 Opaque Token Resource Server。

使用 Spring Security Resource Server 的 opaque token introspection，不自行解析 token、
实现 introspection 协议或缓存。introspection URI、client ID 和 secret 从环境变量/secret
source 注入，必填配置缺失时应用应清晰失败。测试可使用 mock introspection 响应，
但不得将固定 token 匹配描述成真实身份验证。

配置 scope 到 authorities 的明确映射，并实现至少两个 scope 不同的 API。测试缺失/无效
token、introspection 不可用和 scope 不足。返回 RFC 9457 ProblemDetail，保留 Bearer
WWW-Authenticate。README 说明 issuer 信任边界、配置和请求示例。无需数据库。
```

## 5. 浏览器 Session 与 Bearer API 安全边界

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示浏览器 Session 应用与 Bearer REST API 共存时的 Spring Security 配置。

使用顺序明确、matcher 不重叠的 SecurityFilterChain：浏览器路由使用 session/form login
并保留 CSRF；/api/** 使用 Spring Security OAuth2 Resource Server 和无状态 Bearer token。
不要全局关闭 CSRF，也不要让 API chain 匹配浏览器路径。配置精确的 CORS origin allowlist
和安全响应头，不使用通配 origin 配合 credentials。

分别处理浏览器登录重定向与 API 的 401/403 RFC 9457 ProblemDetail；Bearer 401 保留
WWW-Authenticate。用 MockMvc 测试 chain matcher、CSRF、CORS、未认证、scope
不足和安全响应头。README 说明 Session 与 Bearer 的威胁模型，并给出本地运行和请求示例。
不要使用真实第三方账号或提交密钥。
```

## 6. OAuth2 / OIDC 登录与 provider 管理后台

```text
使用 $spring-boot-developer 技能，在 samples 目录创建一个独立的 Spring Boot 4.1.1、Java
25、Maven 示例项目。先检查仓库约定和现有项目，不能覆盖或修改已有目录。

项目演示本地账号登录，以及通过可管理的 OAuth 2.0 / OpenID Connect 提供方登录。
提供中文登录页、首页和管理员页面；前后端使用同源接口。使用 PostgreSQL 保存 OAuth client
配置和审计记录，使用 Flyway 管理 schema；本地用户与短期 OAuth 兑换码可保存在内存，并在
README 中说明重启丢失和多实例限制。

认证与账号映射：
- 本地用户名/密码认证使用 Spring Security 的 DaoAuthenticationProvider、
  UserDetailsService 和 PasswordEncoder，不自行比较明文密码。
- 使用 Spring Security OAuth2 Client 完成 Authorization Code 流程；让 Spring Security
  处理 state、回调和协议校验，不自行实现 OAuth 协议。
- 支持 OAuth 2.0 和 OIDC。管理员配置授权、token、userinfo、issuer 和 JWKS endpoint，
  不实现自动 discovery。
- 外部身份使用 `registrationId + provider subject` 作为稳定身份键。
  首次登录可创建本地用户，但不得仅凭相同邮箱自动合并账户；邮箱只作为资料字段。
- OAuth 登录成功后签发短时、一次性兑换码，由前端兑换为本应用 JWT。JWT 使用短有效期；
  不实现 refresh token。JWT 只保存在当前页面内存，不写入 localStorage/sessionStorage。
- 停用用户应无法继续登录；JWT 验证时也检查用户是否仍启用。
  普通用户可查看自己的资料和解绑非唯一外部身份；管理员可管理用户状态和外部身份。

OAuth provider 管理：
- 提供只读 client 模板和管理员维护的 client 配置，支持 client ID/secret、scope、
  协议类型、扩展授权参数、userinfo claim 映射及可选登录按钮文案。
- client secret 使用 AES-256-GCM 加密后存入数据库；加密密钥从环境变量读取。API
  不得返回明文 secret；修改时 secret 留空表示保留现值。说明密钥轮换需要迁移已存密钥。
- 对 authorization、token、userinfo、issuer 和 JWKS URI 做严格 hostname allowlist 校验；
  allowlist 只接受精确 hostname，不接受通配符。端点连通性检查必须设超时、不跟随重定向、
  不发送 client secret，并明确它只验证网络可达性，不证明 OAuth 登录配置有效。
- 管理配置变更写入审计记录，只记录 actor、变更字段和密钥是否变化，不记录密钥值。
- 管理 API 要求 `SCOPE_ADMIN`；用户管理 API 不返回密码哈希、外部 subject 或客户端密钥。

安全与 API：
- OAuth 登录及回调使用 Spring Security 默认保护，不自行处理 state/nonce。
- 对授权失败、认证失败、验证错误和业务异常返回一致的 RFC 9457 ProblemDetail；保留适当的
  `WWW-Authenticate` 行为。
- 管理接口只能由管理员访问。通过可信认证主体读取用户身份与权限，
  不信任请求体或自定义请求头中的角色。
- 所有配置输入使用 DTO 和边界校验。对外部 endpoint 使用连接和读取超时；阻止任意 URL
  探测及内网 SSRF 风险。
- 限制 Host / Forwarded headers 的信任：只有部署在会清除并重写转发头的可信代理后，
  才允许使用框架处理 forwarded headers。

本地运行：
- 提供 Docker Compose PostgreSQL 和本地 Dex OIDC provider，包含仅用于本地演示的用户与
  client。
- 提供 `.env.sample`，包含 JWT secret、AES 密钥、演示密码、provider hostname allowlist
  等占位项；真实密钥不得提交。
- 使用 YAML 配置。生成 Dockerfile 或 Compose build context 时，同时创建根目录
  `.dockerignore`，排除 `.env`、构建产物、依赖目录和 `.git`。
- README 说明启动数据库和 Dex、运行应用、配置 Dex client、登录和管理员操作，并给出
  curl/API 示例及生产环境限制。

测试：
- 使用 Testcontainers 验证 PostgreSQL 与 Flyway；测试 OAuth 身份映射、邮箱不自动合并、
  停用用户拒绝、非管理员被拒、密钥不回显、hostname allowlist、审计不含 secret、
  一次性兑换码过期/重放和统一 ProblemDetail。
- 不需要连接真实 GitHub、Google、Facebook 或 Apple 账号；这些 provider 只作为 README
  中可配置的示例。

完成后运行 Maven 测试和编译验证，并报告执行的命令、结果以及未覆盖的检查。
```

## 7. 本地账号密码凭据生命周期

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示本地账号注册、认证、改密和一次性密码重置。

使用 Spring Security PasswordEncoder、DaoAuthenticationProvider/UserDetailsService
或该版本推荐的等价机制，不自行比较明文密码。演示 DelegatingPasswordEncoder
对旧哈希的兼容和成功登录后的升级。账号唯一性由数据库约束保证；若使用数据库，
采用迁移并使用真实数据库测试。

重置 token 使用安全随机数、短有效期、单次使用，服务端只存 token 摘要。
存在与不存在的邮箱返回相同响应，并实施速率限制。邮件通过本地 Mailpit 等工具演示，
生产凭据来自 secret store。登录失败策略不得简单永久锁号造成拒绝服务。

测试密码不以明文保存、旧哈希升级、重置过期/重放、账号枚举防护和重复注册并发。README
说明本地配置与生产边界。
```

## 8. JWT 签发、刷新与撤销

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示本地认证后签发 JWT access token 并安全处理 refresh token。

先说明何时应委托现有 IdP；项目作为教学用授权服务。使用 Spring Security OAuth2 Resource
Server / JOSE，不编写自定义 JWT filter、签名或 parser。验证 issuer、audience、expiry、
not-before，并显式映射 scope/authority。

access token 使用短有效期。refresh token 使用高熵随机值，服务端只存摘要；刷新时轮换，
检测重放并撤销对应 token family。说明 logout/revocation 不会自动让自包含 JWT 失效。
密钥通过本地开发密钥或外部 secret 注入，生产密钥不得提交仓库。

测试伪造签名、错误 issuer/audience、过期、权限不足、并发刷新重放和旧 token 重用。
按需加入持久化和迁移。README 提供登录、刷新和受保护 API 示例及密钥轮换边界。
```

## 9. MFA 与敏感操作 step-up

```text
使用 $spring-boot-developer 技能，在 samples 目录创建一个独立的 Spring Boot 4.1.1、Java
25、Maven MFA 示例项目。先检查仓库和技能文档，不覆盖或修改已有项目。

实现基于 Spring Security 7 的两步认证：第一步使用密码或 Passkey 完成主认证；
第二步必须再验证一种已配置因子，完成后才允许访问应用资源。支持 TOTP、邮件一次性验证码、
EIP-191 钱包签名和一次性应用恢复码。Passkey 使用 Spring Security WebAuthn 支持；
钱包签名使用成熟的以太坊签名库验证，不自行实现密码学。

明确区分认证阶段和 API 权限：
- 主认证完成但 MFA 尚未完成时，只允许访问 MFA challenge、验证和登出相关端点。
- MFA 完成后才能访问普通用户 API。
- 因子 enrollment、替换和安全设置读取还需要近期 MFA 验证，例如最近 5 分钟内完成的验证。
- MFA 状态绑定到服务端 session 和已认证主体；不信任请求体或自定义请求头中的
  `mfaVerified`、角色或用户名。

使用简单内存数据，不引入数据库。保留 CSRF 防护，并提供 `/api/csrf` 等获取 token 的方式。
所有安全错误返回一致的 RFC 9457 ProblemDetail。邮件验证码只打印到本地日志，
明确标记为开发用途；TOTP secret、挑战和恢复码按各自安全要求处理。

恢复码应随机生成、只展示一次、安全摘要存储、单次使用并限制尝试次数。
使用恢复短语必须明确不是钱包助记词；绝不请求、记录或存储钱包助记词。
钱包认证通过带过期时间和 nonce 的一次性挑战，校验精确签名与恢复出的地址，并防止重放。

提供中文演示页面，展示主认证、MFA challenge、TOTP enrollment、Passkey
浏览器流程和钱包签名流程。README 说明本地启动方式、演示账户、各因子的请求步骤、
session/CSRF 行为和生产环境仍需补齐的安全措施。不要把内存账户、
日志邮件或开发密钥描述成生产方案。

添加测试覆盖：主认证后资源仍被拒绝、完成 MFA 后访问成功、验证码错误/重放/过期、
恢复码单次使用、step-up 过期、钱包 challenge 重放拒绝和未授权因子管理。完成后运行 Maven
测试与编译验证，并报告命令和实际结果。
```

## 10. SAML 2.0 Service Provider

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示 Spring Security SAML 2.0 Service Provider 和本地测试 IdP。

使用 Spring Security SAML 支持处理协议，不自行解析 SAML XML、实现签名验证、解密或回调。
配置 SP/IdP metadata 与受信任证书；验证 issuer、audience、destination/recipient、
时间条件和 InResponseTo/replay。仅将已验证断言中的稳定 subject 映射成本地身份和
authorities。

包含登录、受保护页面/API 和登出，说明本地 session logout 与 IdP Single Logout 区别。
证书和私钥由本地文件或环境变量提供，不提交真实凭据，并说明证书轮换。测试错误签名、
issuer/audience 不符、过期和重放断言。提供本地 IdP、metadata 交换、配置和请求说明；
不要实现 SAML IdP。
```

## 11. 多 issuer JWT Resource Server

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
REST API，演示 Resource Server 接受多个 allowlist issuer 签发的 JWT。

使用 Spring Security OAuth2 Resource Server 的 AuthenticationManagerResolver
等官方扩展点，根据受信 issuer 选择验证器。拒绝未知 issuer；每个 issuer
分别校验签名/JWKS、issuer、audience、expiry 和允许算法，并显式映射 scope/role
authorities。不得仅解码 JWT，也不得根据未验证的 issuer 动态信任任意密钥。

保护不同 scope 的 API。测试各 issuer 成功路径、未知 issuer、错误 audience、
过期/伪造签名、issuer 间权限混淆和 scope 不足。统一 401/403 RFC 9457 ProblemDetail 并保留
WWW-Authenticate。README 说明 issuer allowlist、配置和本地测试密钥边界。
```

## 12. 多租户授权与数据隔离

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
REST API，演示多租户解析、授权和数据隔离。

租户从可信 Host/域名映射，并正确配置反向代理可信头；不能把请求体、query 参数或任意
X-Tenant-Id 当作可信租户身份。将 tenant 与已认证 Authentication/principal 绑定，
在方法授权和数据查询层共同执行租户边界。普通用户不能通过猜测资源 ID 跨租户读写。

演示 tenant-scoped authorities 和管理操作。测试 list/get/update/bulk、缓存 key
与异步上下文，防止 tenant context 泄漏。若使用数据库，使用迁移、
必要索引和真实数据库测试。README 说明 Host/代理信任、成员关系、
隔离策略和本地多域名请求示例；不调用外部 DNS API。
```

## 13. OAuth2 Authorization Server：Authorization Code + PKCE

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
OAuth2 Authorization Server 示例，演示 Authorization Code + PKCE。

先确认 Spring Authorization Server 与 Boot 4.1.1 / Spring Security 7 的版本兼容性；
使用官方兼容依赖，不自行实现 OAuth2 端点、授权码、签名或 token 序列化。配置 public client
的 PKCE、精确 redirect URI、用户登录、consent、scope 和短期 access token；
在所用版本明确支持时加入 refresh token rotation/reuse detection。

提供受保护 API 或独立 Resource Server 验证 token，校验 issuer/audience/scope/JWKS。
签名密钥不得提交仓库；区分本地开发密钥和生产密钥管理。浏览器流程保留 session/CSRF。

测试无效 redirect URI、缺少/错误 PKCE verifier、过期 token 和权限不足。README
提供本地启动、授权请求、token 使用和生产替换清单。
```
