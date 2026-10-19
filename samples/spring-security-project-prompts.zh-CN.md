# Spring Security 示例项目提示词库

可将下列任一代码块直接交给 `$spring-boot-developer` 技能使用。各提示词彼此独立，也允许与仓库现有项目或其他提示词重叠。

除非某个主题另有说明，示例统一使用 Spring Boot 4.1.1、Java 25、Maven，配置使用 YAML；创建前检查仓库约定，使用新的项目目录，不覆盖现有文件。README
应说明本地启动、认证与授权边界、配置项和可复现请求。凭据和密钥不得提交。完成后运行相关 Maven 测试与编译，并报告命令和实际结果。

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
| 14   | 应用数据加密密钥轮换                                   | 密文迁移、密钥版本与安全退役     | 难    |

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

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
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

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```

## 3. OAuth2 Client Credentials 服务间调用

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示服务使用 OAuth2 Client Credentials 调用受保护的下游 API。

使用 Spring Security OAuth2 Client 和 OAuth2AuthorizedClientManager 获取并复用
access token；过期后由 manager 重新获取，不假设 Client Credentials 一定有 refresh token。
不自行实现 token endpoint 或缓存。client ID、secret、issuer/token endpoint 和
scope 从环境变量读取。下游 base URL 只能来自可信配置 allowlist，
不能由用户输入拼接请求目标，以防 SSRF。

提供触发下游调用的端点，使用受管理的 HTTP client、明确超时和有限重试。区分下游 401、
403、超时和暂时性错误，不将敏感下游响应透传。支持本地 mock server 或可选真实 issuer。
README 明确 client credentials 表示服务身份，不代表最终用户。

测试 token 获取/复用/过期后重新获取、scope、下游错误和 SSRF URL 拒绝。不得提交密钥。README
提供本地配置与调用示例。

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
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

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```

## 5. 浏览器 Session 与 Bearer API 安全边界

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示浏览器 Session 应用与 Bearer REST API 共存时的 Spring Security 配置。

使用顺序明确、matcher 不重叠的 SecurityFilterChain：浏览器路由使用 session/form login
并保留 CSRF；/api/** 使用 Spring Security OAuth2 Resource Server 和无状态 Bearer token。
不要全局关闭 CSRF，也不要让 API chain 匹配浏览器路径。配置精确的 CORS origin allowlist
和安全响应头，不使用通配 origin 配合 credentials。

API chain 使用无状态 SecurityContext，不读取浏览器 session；只有有效 Bearer token 才能认证 API。
分别处理浏览器登录重定向与 API 的 401/403 RFC 9457 ProblemDetail；Bearer 401 保留
WWW-Authenticate。用 MockMvc 测试 chain matcher、浏览器 session cookie 不能认证 API、CSRF、CORS、未认证、scope
不足和安全响应头。README 说明 Session 与 Bearer 的威胁模型，并给出本地运行和请求示例。
不要使用真实第三方账号或提交密钥。

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，
使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```

## 6. OAuth2 / OIDC 登录与 provider 管理后台

```text
使用 $spring-boot-developer 技能，在 samples 目录创建一个独立的 Spring Boot 4.1.1、Java
25、Maven 示例项目。先检查仓库约定和现有项目，不能覆盖或修改已有目录。

项目演示本地账号登录、OIDC 登录、provider 模板和运行时 provider 管理，并覆盖账号映射与
provider 安全配置。使用 Spring Security 7 的 OAuth2 Client 和基于服务端 session 的 BFF
架构：前后端同源，保留 CSRF；不签发自定义 JWT，不实现兑换码或 refresh token。提供中文登录页、
首页和 provider 管理页，页面采用蓝白配色，演示本地/OIDC 登录、provider 模板创建、配置预检、
启停和用户身份映射状态。

协议与登录：
- 新 provider 仅支持 OIDC。OAuth 2.0 本身不定义用户身份；不把没有稳定身份声明的纯 OAuth
  provider 当作登录方式。
- 本地账号使用 Spring Security `DaoAuthenticationProvider`、`UserDetailsService` 和
  `PasswordEncoder`；不得自行比较明文密码。密码使用 DelegatingPasswordEncoder 存储。
- 使用 Spring Security OAuth2 Client 的 Authorization Code 登录。由 Spring Security
  处理 state、nonce、PKCE（适用时）、回调和协议验证；使用浏览器顶层导航启动登录，禁止自写
  OAuth 协议处理或以 AJAX 代替重定向。
- 登录身份以验证后的 `(issuer, subject)` 持久化映射到本地用户；`registrationId` 只用于选择
  provider 配置，不作为身份键。采用 JIT provisioning：首次 OIDC 登录成功后创建最低权限本地用户；
  不得仅凭相同邮箱自动合并账户，邮箱仅作资料字段。不得把 ID token 当作 API access token 或返回给前端。
- 普通用户只能读取自己的资料，并可解绑非唯一外部身份；管理员可停用用户，也可在不改变归属的
  前提下解除外部身份绑定，但不得代替用户绑定或合并账户。停用用户必须无法新登录，且现有
  session 在后续受保护请求中也必须被拒绝。
- 外部身份绑定必须由已登录的目标本地用户主动发起；开始绑定前，用户须通过当前账户已有的
  登录方式重新认证，且服务端确认该认证发生在最近 5 分钟内。OIDC 重新认证必须请求新鲜认证，
  并验证可信 `auth_time`/`max_age` 语义；仅有仍有效的 IdP SSO session 不算重新认证。绑定流程
  使用 Spring Security 管理的 state、nonce 和 PKCE，callback 后校验 issuer、subject，并以数据库
  唯一约束保证一个 `(issuer, subject)` 只能绑定一个本地用户。不得通过邮箱匹配、普通登录 callback 或管理员操作
  自动合并账户；已绑定到其他用户时拒绝绑定并给出通用错误。
- 解绑外部身份前确认该用户仍有至少一种可用登录方式（本地密码或其他启用的身份）；并发绑定
  必须由数据库唯一约束保证同一外部身份不会关联多个本地用户。

Provider 管理：
- 提供内置只读 provider 模板，至少包含本地 Dex 模板、Google OIDC 模板和通用 OIDC Discovery
  模板。模板保存展示名称、issuer/discovery 约定、默认 scope、受限资料 claim 映射和说明，
  不包含 client secret；管理员从模板创建独立的运行时 provider 配置。模板不可通过管理 API 修改
  或删除。
- 模板还要描述 provider 能力：支持的 token endpoint client-auth 方法、PKCE `S256`、OIDC
  `max_age`/`auth_time` 重新认证，以及 `email_verified` 等可选 claim 的名称/语义。注明每项能力
  来自标准 Discovery metadata、provider 官方文档还是本地验证；Discovery 未声明或文档未确认时
  标为“未知”，不得假设支持或静默降级。通用模板的能力初始为未知，由管理员预检或配置时确认；
  依赖某项能力的功能（例如 OIDC 重新认证后绑定身份）在能力未确认时应拒绝启用或明确不可用。
  模板能力只描述配置，不得关闭 state、nonce、issuer 等协议验证。
- `GET /api/auth/providers` 只返回启用 provider 的 `registrationId`、展示名称和登录按钮文案；
  管理员通过 `GET /api/admin/provider-templates` 查看模板，并在 `/api/admin/providers` 下
  创建、更新、停用和删除 provider 配置。所有管理路由要求 `SCOPE_ADMIN`。
- 提供管理员 provider 预检操作：验证 issuer/discovery metadata、必需 endpoint、allowlist 和
  网络超时/重定向策略，返回不含凭据的诊断结果。预检不执行完整授权登录，不能证明 client
  secret 正确或 provider 一定可完成登录；不得在日志或响应中泄漏 secret、token 或敏感响应体。
- PostgreSQL 持久化本地用户、外部身份和 OIDC client 配置；Flyway 管理 schema。provider
  创建、更新、停用和删除无需重启生效；使用数据库约束处理 registration ID 唯一性，并明确
  更新和删除后的缓存失效行为。
- 已使用的 provider issuer 不允许原地修改；issuer 变更必须创建新的 registration，并保留既有
  `(issuer, subject)` 映射，避免把新 issuer 下的主体错误映射到旧账户。停用或删除 provider 后
  拒绝新的授权和该 provider 尚未完成的 callback。删除 provider 只删除该 provider 配置及其
  client secret，不级联删除本地用户或外部身份映射；同一 issuer 以后重新配置时仍按原
  `(issuer, subject)` 映射识别用户。已建立的本地 session 按本地用户状态继续授权，不依赖每次
  API 请求联系 IdP。
- provider 只接受 OIDC issuer、client ID/secret、scope、登录按钮文案等必要字段。优先从
  issuer 的标准 discovery metadata 获取 authorization、token、userinfo 和 JWKS endpoint；
  不允许请求方提交任意 endpoint URL。允许管理员为显示名称、邮箱等非安全资料字段配置受限的
  claim 映射；身份 subject 和 authority 映射使用固定可信规则，不允许通过 claim 配置授予角色。
  client-auth 方法必须与 Discovery metadata 中声明的方式兼容；PKCE 使用 `S256`，不能由管理员
  关闭。能力确认状态与来源可由管理员查看。不要把未知可选能力作为支持；不实现纯 OAuth provider
  的任意 claim 映射。
- discovery URI 及其返回的 authorization、token、userinfo、JWKS endpoint 都必须使用 HTTPS，
  hostname 必须匹配环境变量配置的精确 allowlist；拒绝通配符 hostname、URL userinfo 凭据和
  IP 字面量。
  仅 local profile 中显式 allowlist 的本地 Dex 主机可使用 HTTP。网络请求设连接/读取超时、
  不跟随重定向；部署还必须限制网络出口，hostname allowlist 本身不能消除 DNS 重绑定或内网
  SSRF 风险。
- client secret 使用 AES-256-GCM 加密后存储。密钥由环境变量提供，使用 Base64 表示的 32-byte
  key 和随机 nonce；真实密钥不得提交。API 从不返回明文 secret；更新时 secret 留空表示保留现值。
  密钥解密失败时不得静默回退或打印密钥。
- provider 管理 API 要求 `SCOPE_ADMIN`。该 authority 仅授予由环境配置并安全初始化的本地
  管理员账户；普通本地账户和 OIDC 用户不得自行获得管理员权限。用户 API 不返回密码哈希、
  provider secret 或不必要的外部身份标识。
- 每次 OIDC 授权开始时，将不可变的 provider 配置版本及 issuer 绑定到服务端授权事务；callback
  必须使用发起时的配置语义验证并确认该版本仍有效。provider 在授权期间被修改、停用或删除时，
  安全地拒绝旧 callback 并要求重新发起登录，避免新旧 client 配置混用。

安全与 API：
- 本地登录和 OIDC 登录共用安全 session；保留 CSRF、session fixation 防护和安全 cookie 属性。
  `POST /logout` 清除本地 session；README 说明本地登出不一定结束 IdP 自身的 SSO session。`GET /api/me`
  返回当前用户资料；`/api/admin/providers/**` 和用户管理接口仅对管理员开放。provider 管理写操作
  必须使用 CSRF token。
- 管理 API 与业务 API 的授权失败、认证失败、校验失败和业务错误返回一致的 RFC 9457
  ProblemDetail。浏览器 OIDC 重定向及 callback 失败遵循 Spring Security 登录流程，并在登录页
  显示安全、通用的错误状态；不要强行把协议重定向改成 JSON，也不泄漏 provider 或账号是否存在。
- 所有配置输入使用 DTO 和边界校验。身份和 authority 从已认证主体或数据库读取，不信任请求体
  或自定义请求头中的用户 ID、用户名或角色。
- 不得记录密码、client secret、授权码、access/ID token、session ID 或敏感 provider 响应内容。

本地运行：
- 提供 Docker Compose PostgreSQL 和本地 Dex OIDC provider，配置只用于本地演示的 Dex client；
  README 写明 redirect URI、issuer 和 client 设置。
- 本地用户与管理员由环境变量提供的演示资料初始化，密码始终编码后存储。提供 `.env.sample`
  和变量说明，包含本地密码、AES-256-GCM 密钥和 provider hostname allowlist 占位值；真实凭据
  不得提交。
- 使用 YAML 配置。
- README 说明启动 PostgreSQL 和 Dex、运行应用、配置及预检 provider、登录、绑定/解绑外部身份
  和管理操作，提供 session/CSRF 行为、curl/API 示例及生产环境安全边界。不把本地 Dex、演示账号
  或单机配置描述为生产方案。

测试：
- 使用 Testcontainers 验证 PostgreSQL 与 Flyway；覆盖 OIDC `(issuer, subject)` 身份映射、
  相同邮箱不自动合并、绑定需近期重新认证、重复绑定和跨用户绑定拒绝、解绑保留最后一种登录方式、
  首次 OIDC 登录创建最低权限账户、停用账户拒绝新登录和现有 session 请求、非管理员被拒、
  provider 配置变更及时生效、issuer 不可原地更改、provider 停用或配置版本变化时拒绝旧 callback、
  删除 provider 不级联删除账户或身份映射、内置模板只读且不含 secret、从模板创建 provider、
  预检结果脱敏、secret 不回显、hostname allowlist、外部 discovery 超时/重定向拒绝、CSRF、
  logout 清除 session、模板能力来源和未知状态正确展示、未确认 `max_age` 时禁止依赖其完成身份绑定，
  以及 RFC 9457 ProblemDetail。
- 使用 Spring Security OIDC 测试支持覆盖授权行为，并使用本地 Dex 验证 discovery、登录跳转、
  callback、logout 和真实 claim 映射；覆盖授权期间 provider 修改后旧 callback 被拒绝。不连接
  真实 GitHub、Google、Facebook 或 Apple 账号。

完成后运行 Maven 测试和编译验证，并报告执行的命令、结果以及未覆盖的检查。
提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```

## 7. 本地账号密码凭据生命周期

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示本地账号注册、认证、改密和一次性密码重置。

使用 Spring Security PasswordEncoder、DaoAuthenticationProvider/UserDetailsService
或该版本推荐的等价机制，不自行比较明文密码。演示 DelegatingPasswordEncoder
对旧哈希的兼容和成功登录后的升级。使用 PostgreSQL 与 Flyway 持久化账号和重置 token，
账号唯一性由数据库约束保证，并使用 Testcontainers 验证迁移和并发重复注册。

改密时要求当前密码；重置 token 使用安全随机数、短有效期、单次使用，服务端只存 token 摘要。
存在与不存在的邮箱返回相同状态和响应内容，并实施速率限制。成功重置后撤销其他未使用的重置
token，并使既有登录 session 失效。邮件通过本地 Mailpit 等工具演示，生产凭据来自 secret store。
登录失败策略不得简单永久锁号造成拒绝服务。

测试密码不以明文保存、旧哈希升级、错误当前密码、重置过期/重放、重置后 session 失效、
账号枚举防护和重复注册并发。README
说明本地配置与生产边界。

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，
使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```

## 8. JWT 签发、刷新与撤销

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示本地认证后签发 JWT access token 并安全处理 refresh token。

先说明何时应委托现有 IdP；项目仅演示本应用的 token 签发和撤销，不作为通用 OAuth2
Authorization Server。使用 Spring Security OAuth2 Resource Server / JOSE 和受支持的
`JwtEncoder`，不编写自定义 JWT filter、签名或 parser。验证 issuer、audience、expiry、
not-before，并显式映射 scope/authority。

access token 使用短有效期。refresh token 使用高熵随机值，服务端只存摘要；刷新时轮换，
检测重放并撤销对应 token family。说明 logout/revocation 不会自动让自包含 JWT 失效。
使用 PostgreSQL 与 Flyway 持久化用户、refresh token 摘要和 token family 状态，保证轮换和重放
检测跨实例有效。密钥通过本地开发密钥或外部 secret 注入，生产密钥不得提交仓库。

测试伪造签名、错误 issuer/audience、过期、权限不足、并发刷新重放、旧 token 重用和 family
撤销。README 提供登录、刷新和受保护 API 示例及密钥轮换边界。

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，
使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```

## 9. MFA 与敏感操作 step-up

```text
使用 $spring-boot-developer 技能，在 samples 目录创建一个独立的 Spring Boot 4.1.1、Java
25、Maven MFA 示例项目。先检查仓库和技能文档，不覆盖或修改已有项目。

实现基于 Spring Security 7 的两步认证：第一步使用密码或 Passkey 完成主认证；
第二步必须再验证一种已配置因子，完成后才允许访问应用资源。支持 TOTP、邮件一次性验证码、
EIP-191 钱包签名、以及可重复使用的 BIP-39 12 词助记词。
Passkey 使用 Spring Security WebAuthn 支持；钱包签名使用成熟的以太坊签名库验证，不自行实现密码学。

明确区分认证阶段和 API 权限：
- 主认证完成但 MFA 尚未完成时，只允许访问 MFA challenge、验证和登出相关端点。
- MFA 完成后才能访问普通用户 API。
- 因子 enrollment、替换和安全设置读取还需要近期 MFA 验证，例如最近 5 分钟内完成的验证。
- 首次配置任何 MFA 因子时尚无近期 MFA：提供受限的首次 setup 状态，只允许主认证、首次因子
  enrollment/确认和登出；首个因子确认前不得访问普通资源，确认后仍需完成一次该因子的 MFA
  challenge 才进入完整认证状态。之后 enrollment、替换和移除都要求近期 MFA。
- MFA 状态绑定到服务端 session 和已认证主体；不信任请求体或自定义请求头中的
  `mfaVerified`、角色或用户名。

API 使用短路径并按用途分组，不使用长串重复的 `/users/me/mfa/...` 层级：
- `GET /api/auth/state` 返回当前认证阶段；`POST /api/auth/mfa/challenge` 与
  `POST /api/auth/mfa/verify` 处理登录第二步；通过 `/logout` 结束 session。
- `GET /api/me` 返回当前用户资料；`GET /api/me/security` 读取安全状态，要求近期 MFA。
- `GET /api/mfa/factors` 列出当前用户已配置因子，要求近期 MFA。
- 已注册因子使用 `DELETE /api/mfa/factors/{factorType}` 移除，要求近期 MFA；只允许移除当前主体已注册的因子。
- TOTP 使用 `POST /api/mfa/totp/enroll` 和 `/api/mfa/totp/confirm`；钱包使用
  `POST /api/mfa/wallet/enroll` 和 `/api/mfa/wallet/confirm`。
- Passkey 使用 Spring Security WebAuthn 的浏览器注册与认证流程；提供注册 options/verify
  接口，并校验 challenge、origin、RP ID 和当前主体绑定。
- 助记词使用 `POST /api/mfa/mnemonic/enroll`。
- 所有写 API 保留 CSRF 防护；`GET /api/csrf` 返回 token 和 header 名称。

使用简单内存数据，不引入数据库。保留 CSRF 防护，并提供 `/api/csrf` 等获取 token 的方式。
所有安全错误返回一致的 RFC 9457 ProblemDetail。邮件验证码只打印到本地日志，
明确标记为开发用途；TOTP secret、助记词摘要和 challenge 按各自安全要求处理。

助记词必须是有效的 BIP-39 12 词序列：使用成熟 BIP-39 库和 CSPRNG 生成 128 bit 熵及校验词，
不得自行实现词表、校验或密码学。明文只在 enrollment 成功时展示一次；服务端只存安全摘要。助记词可重复用于验证，
成功后不得消费、轮换或删除；连续失败应限制尝试或暂时锁定。替换时要求近期 MFA。明确说明这是为本应用新生成的
BIP-39 助记词；它也能被钱包用于派生密钥，必须妥善保管且不能输入钱包。绝不请求、记录或存储用户已有的钱包助记词，
也不得将应用生成的助记词用于钱包创建、恢复或签名。助记词是 bearer secret，不得称为抗钓鱼因子。本示例不实现一次性恢复码。
钱包认证通过带过期时间和 nonce 的一次性挑战，挑战绑定已认证主体及创建 challenge 的服务端 session；校验精确签名与恢复出的地址，并防止重放。因子移除也必须绑定当前已认证主体，且只有近期 MFA 后才允许。

提供中文演示页面，展示主认证、MFA challenge、TOTP enrollment、Passkey
浏览器流程、钱包签名流程、可重复验证的助记词流程。README 说明本地启动方式、演示账户、各因子的请求步骤、
session/CSRF 行为和生产环境仍需补齐的安全措施。不要把内存账户、
日志邮件或开发密钥描述成生产方案。
前端页面采用蓝白配色：白色内容卡片、浅蓝背景和蓝色交互强调色，并保证文本及控件状态的可读性。

添加测试覆盖：主认证后资源仍被拒绝、首次因子 setup 期间资源仍被拒绝、首次因子确认后仍需完成
challenge、完成 MFA 后访问成功、验证码错误/重放/过期、Passkey 注册 origin/RP ID 校验、
BIP-39 助记词有效且可重复验证、摘要不泄漏及连续错误后的临时锁定、step-up 过期、
钱包 challenge 跨 session 拒绝、重放拒绝、因子移除的近期 MFA 授权及未注册因子移除拒绝。完成后运行 Maven
测试与编译验证，并报告命令和实际结果。

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，
使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```

## 10. SAML 2.0 Service Provider

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示 Spring Security SAML 2.0 Service Provider 和本地测试 IdP。

使用 Spring Security SAML 支持处理协议，不自行解析 SAML XML、实现签名验证、解密或回调。
配置 SP/IdP metadata 与受信任证书；验证 issuer、audience、destination/recipient、
时间条件和 InResponseTo/replay。仅将已验证断言中的稳定 subject 映射成本地身份；authority
只从该 IdP 明确约定且经过 allowlist 的已验证属性映射，不直接信任任意断言属性中的角色名称。

包含登录、受保护页面/API 和登出，说明本地 session logout 与 IdP Single Logout 区别。
证书和私钥由本地文件或环境变量提供，不提交真实凭据，并说明证书轮换。测试错误签名、
issuer/audience/destination/recipient 不符、过期、InResponseTo 不匹配和重放断言；拒绝未请求的
IdP-initiated 响应，除非示例明确启用并覆盖其安全校验。提供本地 IdP、metadata 交换、配置和请求说明；
不要实现 SAML IdP。

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，
使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
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

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，
使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```

## 12. 多租户授权与数据隔离

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
REST API，演示多租户解析、授权和数据隔离。

租户从服务端配置的 Host/域名映射，并正确配置反向代理可信头；未知 Host 必须拒绝。不能把请求体、query 参数或任意
X-Tenant-Id 当作可信租户身份。将 tenant 与已认证 Authentication/principal 绑定，
在方法授权和数据查询层共同执行租户边界。普通用户不能通过猜测资源 ID 跨租户读写。

演示 tenant-scoped authorities 和管理操作。测试跨租户的 list/get/update/bulk 拒绝、缓存 key
与异步上下文，防止 tenant context 泄漏。若使用数据库，使用迁移、
必要索引和真实数据库测试。README 说明 Host/代理信任、成员关系、
隔离策略和本地多域名请求示例；不调用外部 DNS API。

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，
使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
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

测试无效 redirect URI、缺少/错误 PKCE verifier、授权码与 token 过期、授权码重放、client/redirect
不匹配、用户拒绝 consent 和权限不足。README
提供本地启动、授权请求、token 使用和生产替换清单。

提供中文前端演示页面，用于展示本提示词覆盖的核心功能和安全边界；页面采用蓝白配色，
使用白色内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```

## 14. 应用数据加密密钥轮换

```text
使用 $spring-boot-developer 技能，在 samples 目录创建 Spring Boot 4.1.1、Java 25、Maven
示例，演示数据库中应用级敏感配置的加密存储与加密密钥轮换。本示例只处理数据加密密钥，
不实现 JWT 签名密钥轮换或密码哈希升级。

使用 PostgreSQL 与 Flyway 保存示例敏感配置的密文、随机 nonce、key ID 和记录版本；使用
Spring Security / Spring Framework 支持的密码学 API 实现 AES-256-GCM，不自行实现密码学。
每条记录使用唯一随机 nonce；将用途和记录 ID 等上下文作为 Additional Authenticated Data，
防止密文被跨记录替换。明文只在必要的服务端操作中短暂存在，不返回管理 API、不写日志。

实现明确的密钥环状态：一个 active key 负责所有新写入，旧 key 仅用于解密。密钥材料只从
环境变量或本地 secret 文件加载，绝不经 API 传入、返回、记录或提交；只展示 key ID 和状态。
缺少 key、key ID 未知、GCM 校验失败时 fail closed，不回退到明文或其他 key 猜测。

提供受 `SCOPE_ADMIN` 保护并保留 CSRF 的轮换流程：先配置新 key 并验证所有应用实例都已加载，
再将其设为 active；使用有界批次把旧 key 密文解密后以新 key 重加密。轮换任务可安全重试、
暂停和续跑；使用事务/乐观锁避免与并发更新互相覆盖。校验旧 key 引用数归零后才允许标记退役，
并说明备份保留期内仍需保留恢复备份所需的旧 key。轮换失败不得删除旧 key 或丢失可解密数据。

提供只读状态接口和中文演示页面，显示 active/旧 key ID、各版本密文数量、轮换进度和错误状态，
绝不显示密钥材料或敏感明文。README 说明本地启动、密钥配置、轮换步骤、暂停/恢复和备份恢复
边界；明确本地密钥文件仅供演示，生产环境需要 secret manager/KMS、访问控制和密钥备份策略。

测试旧 key 密文可读、新 key 写入、重加密后明文语义不变、GCM/AAD 篡改拒绝、未知 key 拒绝、
重复执行和中断后续跑、并发更新不丢数据、旧 key 引用未清零时拒绝退役，以及轮换后备份恢复所需
旧 key 仍可用。使用 Testcontainers 验证 PostgreSQL 与 Flyway。完成后运行 Maven 测试和编译验证，
并报告命令与实际结果。

提供中文前端演示页面，用于展示本提示词覆盖的轮换流程与安全边界；页面采用蓝白配色，使用白色
内容表面、浅蓝背景和蓝色交互强调色，并保证足够的可读性。
```
