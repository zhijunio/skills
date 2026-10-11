# JWT 认证

## 首先：是否真的需要自己签发 Token？

如果已有身份提供商——Keycloak、Auth0、Okta、Entra、Cognito——就使用它，并让应用作为 Resource Server（参见 [oauth2-resource-server.zh-CN.md](oauth2-resource-server.zh-CN.md)）。这样应用完全不需要编写 Token 代码。

自己签发意味着需要负责密钥管理、轮换、撤销、刷新 Token 重放检测和账户恢复。对于没有 IdP、使用本地账户的独立应用，这是合理选择；只要有可用的 IdP，就不应自行签发。

## 使用 Spring Security 自带的 JWT 支持

**不要**添加第三方 JWT 库并手写 `OncePerRequestFilter`。Spring Security 已提供 Encoder、Decoder 和 Bearer Token Filter。使用它们可以去掉额外依赖、自定义 Filter，以及手动解析 Token 带来的整类错误，例如过期 Token 异常逃出 Filter 并变成 500、忘记清除安全上下文，或把刷新 Token 当作访问 Token 接受。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

这是 Boot 4 的依赖名称；Boot 3.5.x 使用 `spring-boot-starter-oauth2-resource-server`。该依赖会引入 `JwtEncoder` 和 `JwtDecoder` 所需的 Nimbus JOSE 支持。

## 密钥

优先使用 **RSA 或 EC**，而不是共享密钥。非对称密钥让私钥只留在签发方；还可以发布 JWKS Endpoint，让其他服务在不持有秘密的情况下验证 Token。

```java
@Configuration
class JwtKeyConfig {

    @Bean
    JWKSource<SecurityContext> jwkSource(JwtKeyProperties properties) {
        var rsaKey = new RSAKey.Builder(properties.publicKey())
                .privateKey(properties.privateKey())
                .keyID(properties.keyId())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(JwtKeyProperties properties) {
        return NimbusJwtDecoder.withPublicKey(properties.publicKey()).build();
    }
}
```

```java
@Validated
@ConfigurationProperties("app.jwt")
record JwtKeyProperties(
        @NotNull RSAPublicKey publicKey,
        @NotNull RSAPrivateKey privateKey,
        @NotBlank String keyId,
        @NotNull Duration accessTokenTtl) {
}
```

Spring 可以直接将 PEM 资源转换为 `RSAPublicKey` / `RSAPrivateKey`，因此属性值使用文件引用：

```yaml
app:
  jwt:
    public-key: ${JWT_PUBLIC_KEY}     # 本地开发时可使用 classpath:/keys/public.pem
    private-key: ${JWT_PRIVATE_KEY}
    key-id: ${JWT_KEY_ID}
    access-token-ttl: 15m
```

密钥从环境或秘密管理系统获取，绝不能放进代码仓库或 `application.yml`。参见 [configuration.zh-CN.md](configuration.zh-CN.md)。

`keyID` 很重要：它会以 `kid` 的形式进入 Token Header，从而支持密钥轮换。在轮换窗口中，同时发布新旧密钥到 JWKS，确保两者签发的 Token 都能通过验证。

如果是单个应用且没有其他验证方，可使用 HMAC：`NimbusJwtEncoder(new ImmutableSecret<>(secretKey))` 和 `NimbusJwtDecoder.withSecretKey(secretKey)`。HS256 要求密钥至少为 256 位；较短的密钥会在启动时抛错，这是正确行为。

## 签发访问 Token

```java
@Service
public class AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final JwtKeyProperties properties;

    AccessTokenIssuer(JwtEncoder encoder, JwtKeyProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedToken issue(UserAccount account) {
        var now = Instant.now();
        var expiresAt = now.plus(properties.accessTokenTtl());

        var claims = JwtClaimsSet.builder()
                .issuer("https://api.example.com")
                .subject(account.id().value().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())          // jti，用于撤销列表
                .claim("scope", String.join(" ", account.scopes()))
                .build();

        var token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
```

- **TTL 应短**：15 分钟或更短。JWT 无法撤销，因此有效期就是撤销窗口。
- **`subject` 使用稳定的用户 ID**，不要使用邮箱；邮箱可能变化。
- **绝不要在 Payload 中放秘密信息。**JWT 经过签名而非加密，持有者都能读取所有 Claim。
- **控制 Token 大小。**每个请求都会携带它；将五十项权限放入其中会持续增加带宽开销。
- 如果将来可能需要撤销列表，应加入 `jti`。

## 验证：不要自定义 Filter

将配置指向 Decoder，剩下的验证由 Spring Security 的 `BearerTokenAuthenticationFilter` 完成：

```java
@Bean
SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    return http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(problemDetailEntryPoint())
                    .accessDeniedHandler(problemDetailAccessDeniedHandler()))
            .build();
}
```

签名、过期时间和发行者都会自动验证；无效 Token 会经由 Entry Point 返回 401，而不会在 Filter 中变成 500。Scope Claim 会自动映射成 `SCOPE_*` Authorities；自定义 Claim 映射见 [oauth2-resource-server.zh-CN.md](oauth2-resource-server.zh-CN.md)。

在 Controller 中读取当前调用者：

```java
@GetMapping("/me")
AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
    return accountService.getById(AccountId.of(jwt.getSubject()));
}
```

## 刷新 Token

**刷新 Token 不应是 JWT。**它有效期长、必须可撤销，还必须支持重放检测；无状态签名 Token 无法满足这些要求。应使用随机的不透明值，存储其哈希，并以数据库记录作为事实来源：

```java
@Transactional
public TokenPair refresh(String presentedToken) {
    var hash = sha256(presentedToken);
    var stored = refreshTokens.findByTokenHash(hash)
            .orElseThrow(() -> new InvalidRefreshTokenException());

    if (stored.isRevoked()) {
        // Token 已经使用过。它可能被盗后重放，也可能是合法客户端重复提交。
        // 按凭据已泄漏处理，撤销整个 Token 家族。
        refreshTokens.revokeFamily(stored.familyId());
        throw new RefreshTokenReuseException();
    }
    if (stored.isExpired()) {
        throw new InvalidRefreshTokenException();
    }

    stored.revoke();                                            // 轮换：仅允许使用一次
    var next = RefreshToken.issue(stored.accountId(), stored.familyId());
    refreshTokens.save(next);

    return new TokenPair(accessTokenIssuer.issue(stored.account()), next.plaintext());
}
```

- **每次使用都轮换**并撤销上一个 Token。
- **重用已撤销 Token 表示可能已泄漏**：撤销整个 Token 家族，要求用户重新认证。
- **只存储哈希。**数据库泄露时，攻击者也不能直接使用 Token。
- 使用 `SecureRandom` 生成至少 256 位随机值，并进行 Base64 URL 编码。

## 客户端如何保存 Token

| 存储方式 | 结论 |
|---|---|
| `httpOnly`、`Secure`、`SameSite=Strict` Cookie | 最佳选择。JavaScript 无法读取，因此 XSS 不能窃取；但必须处理 CSRF。 |
| 内存中的 JavaScript 变量 | 适合保存访问 Token；页面刷新后会丢失，但可通过刷新 Token 获取新 Token。 |
| `localStorage` | 页面上的任何脚本都可以读取。一次 XSS 就可能导致整个账户被接管。 |

即使在示例中也不要推荐 `localStorage`。

## 退出与撤销

JWT 会一直有效到过期。“退出”应执行以下步骤：

1. 撤销刷新 Token（服务端立即生效）。
2. 在客户端丢弃访问 Token。
3. 接受访问 Token 在过期前仍然技术上有效——因此 TTL 必须短。

如果需要立即撤销，可在每个请求中检查 Redis 中的 `jti` 拒绝列表。这会重新引入 JWT 原本试图避免的逐请求查询，因此只在确有需求时使用。

## 测试

测试中不要生成真实 Token。使用 JWT 后处理器：

```java
@Test
void allowsCallerWithScope() {
    mockMvc.perform(get("/api/orders")
            .with(jwt().jwt(builder -> builder.subject(accountId.toString()))
                       .authorities(new SimpleGrantedAuthority("SCOPE_orders:read"))))
            .andExpect(status().isOk());
}
```

还要测试**过期** Token 返回 401，以及缺少 Scope 的 Token 返回 403。这些路径很容易悄悄出错。

## 使用 Boot 3.5.x 时

Starter 名称为 `spring-boot-starter-oauth2-resource-server`（没有 `security-` 前缀）。Spring Security 6 已提供 `JwtEncoder`、`NimbusJwtEncoder`、`JwtDecoder`、`JwtClaimsSet` 和 `oauth2ResourceServer(…)`，行为相同。[安全基础](security-fundamentals.zh-CN.md)中描述的 DSL 移除是主要差异。

## 常见问题

- Agent 添加第三方 JWT 库并编写自定义 `OncePerRequestFilter`：Spring Security 已提供 Encoder、Decoder 和 Bearer Filter。
- Agent 让 Token 解析异常逃出自定义 Filter：过期 Token 会变成 500 而不是 401；这也是不应自己写 Filter 的原因。
- Agent 将签名密钥硬编码：密钥只能来自环境或秘密管理系统。
- Agent 使用过短的 HMAC 密钥：HS256 要求至少 256 位，否则会抛错。
- Agent 在其他服务需要验证时使用 HMAC：应改用 RSA/EC 并发布 JWKS。
- Agent 未设置 `kid`：密钥轮换会变成需要同时切换所有服务的停机操作。
- Agent 在 Claim 中放入敏感数据：JWT 经过签名，不是加密。
- Agent 设置数小时的访问 Token TTL：JWT 不能撤销，TTL 就是撤销窗口。
- Agent 使用邮箱作为 `subject`：应改用稳定 ID。
- Agent 将 JWT 作为刷新 Token：刷新 Token 必须可撤销，应使用存储在服务端的不透明值。
- Agent 不轮换刷新 Token：泄漏的 Token 会一直有效到过期。
- Agent 以明文存储刷新 Token：应存储哈希。
- Agent 忽略刷新 Token 重放：重放表示可能已泄漏，应撤销整个 Token 家族。
- Agent 在 Bearer Endpoint 接受刷新 Token：两种 Token 类型应分开处理。
- Agent 推荐 `localStorage`：一次 XSS 就可能完全接管账户。
- Agent 声称退出会使访问 Token 失效：实际不会，只有刷新 Token 被撤销。
- Agent 使用真实 Token 测试：使用 `jwt()` 后处理器，并显式测试 401 与 403。

## 相关文档

- [安全基础](security-fundamentals.zh-CN.md) · [OAuth2 资源服务器](oauth2-resource-server.zh-CN.md) · [配置](configuration.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [缓存](caching.zh-CN.md)
