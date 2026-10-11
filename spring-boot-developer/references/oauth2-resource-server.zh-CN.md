# OAuth2 资源服务器

资源服务器负责验证访问 Token，并根据 Scope / Authority 授权 API 请求。它不负责用户登录或签发 Token；这些工作应由授权服务器或 OIDC 身份提供商承担。

## 依赖与配置

使用 Spring Security OAuth2 Resource Server Starter，并优先通过发行者 URI 或 JWK Set URI 配置 JWT 验证。让 Spring 根据发行者元数据获取密钥，并验证签名、有效期、发行者及按需验证的受众。

不要自行解析 JWT，也不要仅解码载荷就认为 Token 有效。

## 过滤器链

使用 `SecurityFilterChain` 和 Lambda DSL。明确公开端点与认证端点；默认所有未声明为公开的 API 都必须认证。无状态 Bearer Token API 通常配置 `SessionCreationPolicy.STATELESS`。CSRF 是否关闭取决于凭据如何发送：Cookie 会自动携带，需要 CSRF 防护；显式 Authorization Header 的无状态 API 通常可关闭。

## 将 Claims 映射为 Authorities

OAuth2 Scope 通常映射为 `SCOPE_` 前缀权限，例如 `orders:read` 映射为 `SCOPE_orders:read`。通过 `JwtAuthenticationConverter` 自定义映射时，应明确接受的 Claim 与权限来源，避免把未经信任的 Claim 当作角色。

## 读取调用者身份

在服务层通过 `Authentication` 或 `JwtAuthenticationToken` 获取已验证主体信息。只使用经过验证且用途明确的 Claim；不要仅因字段名看起来像用户 ID 就信任它。服务到服务调用应考虑 Client Credentials 与独立服务身份。

## 资源所有权检查

认证只回答“调用者是谁”，不代表调用者能访问任意资源。每个敏感操作还需检查资源所有权或权限，例如将当前主体 ID 与目标订单所属客户 ID 比较。可用方法安全或领域服务实施，不要只在 UI 隐藏按钮。

## 不透明 Token

无法本地验证 JWT 或需要即时撤销时，可采用不透明 Token 并通过 Introspection 向授权服务器查询。它增加网络依赖和延迟，但可集中控制 Token 状态。设置超时并考虑故障行为。

## 多租户

租户身份必须来自可信 Token Claim 或可信请求上下文，并在每次数据访问时执行隔离。不能只依赖客户端传入的 `tenantId`。数据库查询、缓存键、审计和后台任务都应保留租户边界。

## 测试

覆盖有效和无效 Token、401 / 403、Scope 映射、资源所有权和跨租户拒绝。测试配置应使用专门测试密钥或 Mock JWT，不使用生产凭据。

## 使用 Spring Boot 3.5.x 时

使用 Spring Security 6 与该版本线对应的依赖和配置。方法级安全、JWT Resource Server 和 Lambda DSL 的基本做法一致；Spring Security 7 移除的 API 需在升级时替换。

## 常见问题

- 把 Resource Server 当作登录服务器或 Token 签发器。
- 只解码 JWT，不验证签名、发行者、有效期或受众。
- 把 Scope 与角色混为一谈；OAuth Scope 通常使用 `SCOPE_` 前缀。
- 认证通过后不检查目标资源归属。
- 接受客户端任意提供的租户 ID。
- 将不透明 Token Introspection 配置成无超时网络调用。
- 将真实密钥用于测试或提交测试配置。

## 相关文档

- [安全基础](security-fundamentals.zh-CN.md) · [JWT 认证](jwt-authentication.zh-CN.md) · [HTTP 客户端](http-clients.zh-CN.md) · [可观测性](observability.zh-CN.md)

> 中文要点版。完整配置代码和版本差异请参阅[英文原文](oauth2-resource-server.md)。
