# JWT 认证

## 首先确认：是否真的需要自己签发 Token？

如果系统已有 OAuth2 / OIDC 身份提供商，应优先由它签发和轮换 Token，应用作为 Resource Server 验证。自己实现签发、刷新、撤销和密钥轮换，会承担完整认证系统的安全责任。

## 使用 Spring Security 内置 JWT 支持

使用 Spring Security OAuth2 Resource Server 的 JWT 解码与验证机制，不要编写自定义 JWT Filter。签名、发行者、受众和有效期都要校验；角色与 Scope 应通过转换器映射为 Spring Authorities。

## 密钥

- 使用非对称密钥签名，使验证服务只持有公钥，不具备签发能力。
- 私钥来自秘密管理系统，不放在仓库、镜像或日志中。
- 配置密钥轮换和 `kid`；验证端应能在切换期间识别当前与待淘汰公钥。
- 密钥强度和算法遵循所用身份提供商及 Spring Security 支持的配置。

## 签发访问 Token

仅在确实需要自建签发服务时使用 Spring Security 的 `JwtEncoder`。Token 应包含稳定的 `sub`、`iss`、`aud`、`iat`、`exp` 等声明，并只授予必要的 Scope。访问 Token 应短时有效；不要放入秘密或不必要的个人信息。

## 验证：不要写自定义 Filter

配置 `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`，让 Spring Security 验证签名和标准声明。自定义 Filter 容易遗漏过期、发行者、受众、错误响应和安全上下文等细节。具体 Resource Server 配置见[OAuth2 资源服务器](oauth2-resource-server.zh-CN.md)。

## 刷新 Token

刷新 Token 与短期访问 Token 的职责不同。刷新凭据应保存在服务端可撤销的持久化存储中，使用轮换和重放检测；退出或泄漏时应支持撤销。不要把长期刷新 Token 当作普通无状态 JWT 长期有效。

## 客户端存储

浏览器中应谨慎权衡 HttpOnly / Secure Cookie 与浏览器存储的风险。Cookie 需要配合 CSRF 防护；可被脚本读取的存储会受到 XSS 窃取风险。移动端和服务端客户端应使用平台安全存储，不要将 Token 写入日志或 URL。

## 退出与撤销

无状态访问 Token 在过期前默认无法撤销。短有效期、撤销列表、Token 版本或服务端会话记录各有成本，应按威胁模型选择。刷新 Token 必须可撤销，退出流程应使其失效。

## 测试

测试有效、过期、签名错误、错误发行者 / 受众、缺少 Scope、权限不足等路径。Web 切片可使用 Spring Security JWT 后处理器，无需每个测试都生成真实 Token；端到端测试验证整个 HTTP 安全链。

## 使用 Spring Boot 3.5.x 时

Spring Security 6 提供相同的 Resource Server 思路和 JWT 支持。配置 DSL 推荐使用 Lambda 风格，升级到 Security 7 时避免旧 API。具体依赖和配置按项目版本核对。

## 常见问题

- 在已有 OIDC 提供商时自己实现 Token 签发。
- 自定义 JWT Filter，遗漏标准声明或签名检查。
- 把密钥提交到仓库或镜像。
- 访问 Token 长期有效且权限过宽。
- 将完整 Token 或个人信息放进日志、URL 或不安全的客户端存储。
- 把刷新 Token 当成不可撤销的长期 JWT。
- 只测试成功登录，没有测试失效 Token 和授权拒绝。

## 相关文档

- [安全基础](security-fundamentals.zh-CN.md) · [OAuth2 资源服务器](oauth2-resource-server.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [REST API 测试](testing-rest-api.zh-CN.md)

> 中文要点版。完整实现示例与版本细节请参阅[英文原文](jwt-authentication.md)。
