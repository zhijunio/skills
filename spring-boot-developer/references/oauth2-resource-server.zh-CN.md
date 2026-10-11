# OAuth2 资源服务器

你的 API 验证由其他服务签发的 Token，例如 Keycloak、Auth0、Okta、Entra ID、Cognito。应用不需要编写任何 Token 代码：不签名、不存储密钥、不实现刷新流程，也不处理密码。只要存在身份提供商，**这就是首选方案**；需要自行签发时参见 [jwt-authentication.zh-CN.md](jwt-authentication.zh-CN.md)。

## 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

这是 Boot 4 的名称。Boot 3.5.x 使用 `spring-boot-starter-oauth2-resource-server`；Boot 4 Starter 重命名时才加上 `security-` 前缀（参见 [boot-versions.zh-CN.md](boot-versions.zh-CN.md)）。

## 配置

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://id.example.com/realms/production
          audiences: https://api.example.com
```

只配置 `issuer-uri` 就足够。Spring 会获取提供商的 OpenID 配置，发现 JWKS Endpoint、缓存密钥，并在出现未知 `kid` 时刷新密钥；因此提供商轮换密钥时你无需处理。

**始终设置 `issuer-uri`，不要只设置 `jwk-set-uri`。**只设置 JWKS URL 时，Spring 会验证签名，但不会验证 `iss` Claim，因此同一提供商为其他租户或 Realm 签发的有效签名 Token 也可能被接受。只有提供商没有 Discovery Endpoint 时，才单独使用 `jwk-set-uri`，并且要显式校验发行者。

**设置 `audiences`。**否则提供商为任意客户端签发的 Token 都可能用于访问你的 API。`aud` Claim 用来将 Token 限定到**你的服务**。

默认情况下，应用启动时会解析 `issuer-uri`，所以提供商不可达时应用无法启动。如果环境不能保证提供商先于应用就绪，可同时配置 `jwk-set-uri`，将密钥获取延迟到后续请求。

## Filter Chain

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class ResourceServerConfig {

    @Bean
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authoritiesConverter())))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(problemDetailEntryPoint())
                        .accessDeniedHandler(problemDetailAccessDeniedHandler()))
                .build();
    }
}
```

不需要 `UserDetailsService`、`PasswordEncoder` 或 `AuthenticationProvider`——Token 本身就是认证凭据。Filter Chain 规则和 JSON 401/403 Handler 参见[安全基础](security-fundamentals.zh-CN.md)。

## 将 Claims 映射为 Authorities

Spring 默认读取 `scope`（或 `scp`）Claim，并为每个值添加 `SCOPE_` 前缀。例如，`"scope": "orders:read orders:write"` 会映射为 `SCOPE_orders:read` 和 `SCOPE_orders:write`。

```java
@PreAuthorize("hasAuthority('SCOPE_orders:read')")
```

**Scope 不是 Role。**`hasRole('ADMIN')` 会查找 `ROLE_ADMIN`；Scope 不会自动添加 `ROLE_` 前缀。混淆两者是这里最常见的问题。

不同提供商会将角色放在不同位置，因此 Converter 往往需要处理实际映射。Keycloak 会将 Realm 角色放在 `realm_access.roles` 中：

```java
@Bean
JwtAuthenticationConverter authoritiesConverter() {
    var scopes = new JwtGrantedAuthoritiesConverter();     // 保留默认的 SCOPE_ 映射

    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        var authorities = new ArrayList<GrantedAuthority>(scopes.convert(jwt));

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            roles.stream()
                    .map(String::valueOf)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
        }
        return authorities;
    });
    converter.setPrincipalClaimName("sub");
    return converter;
}
```

应与 `JwtGrantedAuthoritiesConverter` 组合，而不是将其替换，否则 Scope Authority 会悄悄丢失。安全处理 Claim 类型：缺少 Claim 时不能在 Filter 中抛出 `ClassCastException` 并变成 500。

| 提供商 | 角色所在位置 |
|---|---|
| Keycloak | `realm_access.roles`；客户端角色位于 `resource_access.<client>.roles` |
| Auth0 | 命名空间自定义 Claim，例如 `https://example.com/roles` |
| Okta | 授权服务器配置了 Claim 时位于 `groups` |
| Entra ID | 应用角色位于 `roles`；`groups` 保存对象 ID，而不是名称 |

检查提供商实际解码出的 Token，不要猜测其格式。Entra 的 `groups` Claim 尤其需要注意：它包含 GUID，且群组数量超过一定阈值时会整个省略。

## 读取调用者

```java
@GetMapping("/me/orders")
List<OrderResponse> myOrders(@AuthenticationPrincipal Jwt jwt) {
    return orderService.findByCustomer(CustomerId.of(jwt.getSubject()));
}
```

`getSubject()` 是稳定的用户标识。不要用 email Claim 作为数据主键：邮箱会变化，而且某些提供商不保证它唯一。

不要让 `Jwt` 进入 Service 层。Controller 提取 Service 所需的值并传入领域类型；若 Service 接收 `Jwt`，就难以测试，也无法从批处理任务调用。

## 资源所有权检查

Scope 表示调用者可以执行什么操作，并不表示调用者可以访问哪些数据行。`SCOPE_orders:read` 不代表可以读取“这个”订单。

```java
@PreAuthorize("hasAuthority('SCOPE_orders:read') and @orderAccess.isOwner(#id, authentication)")
public OrderResponse getById(OrderId id) { … }
```

也可以在 Service 内检查，这通常更容易测试：

```java
public OrderResponse getById(OrderId id, CustomerId caller) {
    var order = orderRepository.getById(id);
    if (!order.customerId().equals(caller)) {
        throw new OrderNotFoundException(id);   // 返回 404 而非 403，不确认资源是否存在
    }
    return OrderResponse.from(order);
}
```

对其他用户的资源返回 404 而不是 403，可以避免确认该 ID 是否存在。只有调用者有权知道资源存在时才使用 403。

## 不透明 Token

部分提供商签发的是引用 Token，而不是 JWT：

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        opaquetoken:
          introspection-uri: https://id.example.com/oauth2/introspect
          client-id: ${INTROSPECTION_CLIENT_ID}
          client-secret: ${INTROSPECTION_CLIENT_SECRET}
```

每个请求都会产生一次到提供商的网络往返，因此应将 Introspection 结果缓存数秒。配置 `jwt` 或 `opaquetoken`，不要同时配置两者。

## 多租户

存在多个发行者时，使用 `JwtIssuerAuthenticationManagerResolver` 并显式设置允许列表：

```java
var resolver = JwtIssuerAuthenticationManagerResolver
        .fromTrustedIssuers("https://id.example.com/realms/eu", "https://id.example.com/realms/us");

http.oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(resolver));
```

绝不要不经允许列表就从 Token 本身推导发行者，否则攻击者可以指定任意发行者的 Token。

## 测试

不连接真实提供商，也不生成真实 Token：

```java
@Test
void rejectsTokenWithoutScope() {
    mockMvc.perform(get("/api/orders").with(jwt()))
            .andExpect(status().isForbidden());
}

@Test
void allowsTokenWithScope() {
    mockMvc.perform(get("/api/orders")
            .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_orders:read"))))
            .andExpect(status().isOk());
}

@Test
void mapsKeycloakRealmRoles() {                        // 直接测试 Converter
    var jwt = Jwt.withTokenValue("t").header("alg", "none")
            .claim("realm_access", Map.of("roles", List.of("admin")))
            .build();
    assertThat(authoritiesConverter().convert(jwt).getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .contains("ROLE_admin");
}
```

Converter 包含实际逻辑，因此应单独进行单元测试。完整端到端测试可在 Testcontainer 中启动 Keycloak；参见[集成测试](testing-integration.zh-CN.md)。

## 使用 Boot 3.5.x 时

Starter 名称为 `spring-boot-starter-oauth2-resource-server`。Spring Security 6 中的配置属性、`JwtAuthenticationConverter`、`JwtGrantedAuthoritiesConverter`、`JwtIssuerAuthenticationManagerResolver` 和 `jwt()` 测试后处理器都相同。只有[安全基础](security-fundamentals.zh-CN.md)中描述的 DSL 移除不同。

## 常见问题

- Agent 在 Boot 4 中添加 `spring-boot-starter-oauth2-resource-server`：应使用 `spring-boot-starter-security-oauth2-resource-server`。
- Agent 只配置 `jwk-set-uri`：此时 `iss` Claim 不会被验证；应设置 `issuer-uri`。
- Agent 忽略 `audiences`：该提供商为任何客户端签发的 Token 都可能被接受。
- Agent 对 Scope 使用 `hasRole('ADMIN')`：Scope 应使用 `hasAuthority('SCOPE_...')`，不会自动添加角色前缀。
- Agent 替换而不是组合 `JwtGrantedAuthoritiesConverter`：Scope Authority 会悄悄丢失。
- Agent 假设角色放在顶层 `roles` Claim：Keycloak 将角色嵌套在 `realm_access.roles`；检查真实 Token。
- Agent 不做 Claim 类型保护：缺少 Claim 会在 Filter 中变成 500。
- Agent 添加 `UserDetailsService` 或 `PasswordEncoder`：Resource Server 通过 Token 认证。
- Agent 使用 email Claim 关联数据：应使用 `sub`。
- Agent 将 `Jwt` 传入 Service 层：应在 Controller 中提取所需信息。
- Agent 把 Scope 当作资源所有权检查：Scope 是能力权限，不是行级权限。
- Agent 对其他用户的资源返回 403：404 可避免确认资源存在。
- Agent 同时配置 `jwt` 和 `opaquetoken`：选择其中一种。
- Agent 在多租户环境信任 Token 中任意发行者：应使用允许列表。
- Agent 针对真实在线提供商编写集成测试：使用 `jwt()` 后处理器，或在容器中启动 Keycloak。

## 相关文档

- [安全基础](security-fundamentals.zh-CN.md) · [JWT 认证](jwt-authentication.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [HTTP 客户端](http-clients.zh-CN.md) · [集成测试](testing-integration.zh-CN.md)
