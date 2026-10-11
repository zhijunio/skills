# 安全基础

Spring Boot 4 搭载 **Spring Security 7**。配置只能使用 Lambda DSL——代码生成器常见的旧 Spring Security 写法大多无法编译。

| Security 7 中移除的 API | 替代方式 |
|---|---|
| `.and()` 链式配置 | Lambda 配置 |
| `authorizeRequests()` | `authorizeHttpRequests()` |
| `antMatchers()`、`mvcMatchers()` | `requestMatchers()` |
| `AntPathRequestMatcher`、`MvcRequestMatcher` | `PathPatternRequestMatcher`（`requestMatchers` 使用） |
| `WebSecurityConfigurerAdapter` | `SecurityFilterChain` Bean |
| `provider.setUserDetailsService(…)` | `new DaoAuthenticationProvider(userDetailsService)` |

## 过滤器链

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain apiFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(problemDetailEntryPoint())
                        .accessDeniedHandler(problemDetailAccessDeniedHandler()))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

- **`securityMatcher` 用于限定过滤器链范围。** 不加时它会接管所有请求，包括 Actuator 和 UI。
- **只有无状态 Token API 才关闭 CSRF。** 如果使用 Cookie Session，应保留 CSRF。基于 Session 的应用关闭 CSRF 是实际漏洞，不是模板代码。
- **设置 `STATELESS`**，避免 Token 认证创建 `HttpSession`。
- **`authorizeHttpRequests` 中的顺序很重要**——第一个匹配规则生效，因此 `anyRequest()` 必须放最后。
- 使用 **`PasswordEncoderFactories.createDelegatingPasswordEncoder()`**，不要直接 `new BCryptPasswordEncoder(12)`：前者会保存算法前缀（`{bcrypt}…`），将来可以迁移哈希算法；直接使用单一 Encoder 会锁定算法。

### 多条过滤器链

按匹配范围从具体到宽泛排序：

```java
@Bean
@Order(1)
SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
    return http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health/**").permitAll()
                    .anyRequest().hasRole("OPS"))
            .httpBasic(Customizer.withDefaults())
            .build();
}
```

第一个 `securityMatcher` 匹配请求的过滤器链负责处理请求，其余链不再检查。没有 Matcher 的链会匹配所有请求，因此必须放在最后。

## 认证是全局配置，授权属于功能

常见反模式是 `SecurityConfig` 罗列应用中的每个 URL，导致每个功能变更都要修改全局文件，而且该文件也无人能看懂。

| 关注点 | 所在位置 |
|---|---|
| 过滤器链框架、Session 策略、CSRF | 全局 `SecurityConfig` |
| 密码编码器、JWT Decoder、`UserDetailsService` | 全局 `SecurityConfig` |
| 401 和 403 响应 | 全局 `SecurityConfig` |
| 粗粒度的公开 / 认证后访问规则 | 全局 `SecurityConfig` |
| **谁可以做什么** | 功能模块，通过 `@PreAuthorize` 声明 |
| 整个 URL 空间有不同规则 | 功能自己的 `SecurityFilterChain`，并设置 `@Order` |

参见[代码组织](code-organization.zh-CN.md)。

## 方法级安全

```java
@Service
@Transactional(readOnly = true)
public class OrderService {

    @PreAuthorize("hasRole('ADMIN')")
    public void cancelAny(OrderId id) { … }

    @PreAuthorize("hasAuthority('SCOPE_orders:write')")
    public OrderId place(PlaceOrderCmd cmd) { … }

    @PreAuthorize("@orderAccess.isOwner(#id, authentication)")
    public OrderResponse getById(OrderId id) { … }
}
```

需要启用 `@EnableMethodSecurity`。忘记后 `@PreAuthorize` 会静默失效；这会形成安全漏洞。

`hasRole('ADMIN')` 会检查 `ROLE_ADMIN` 权限，前缀自动添加。`hasAuthority('ROLE_ADMIN')` 是显式写出相同检查。Scope 不会自动添加前缀，因此 OAuth2 Scope 应使用 `hasAuthority('SCOPE_orders:read')`（参见[OAuth2 资源服务器](oauth2-resource-server.zh-CN.md)）。

所有权规则应委托给 Bean，不要写成长 SpEL：

```java
@Component("orderAccess")
class OrderAccessRules {

    private final OrderRepository orders;

    boolean isOwner(OrderId id, Authentication authentication) {
        return orders.existsByIdAndCustomerId(id, CustomerId.of(authentication.getName()));
    }
}
```

SpEL 不会进行类型检查，也不支持安全重命名。参数改名会导致 `#id` 在运行时静默失效。

`@PreAuthorize` 基于代理，同一对象内部自调用会**完全跳过检查**（参见[代理与依赖注入](spring-proxies-and-di.zh-CN.md)）。所有自调用问题中，这是最严重的一种。

尽量避免 `@PostAuthorize`：它先执行方法再判断。读取方法已经加载数据；有副作用的方法则已经产生副作用。

## JSON 格式的 401 和 403

默认情况下，未认证 API 请求会得到空 401 或跳转到登录页，两者都不利于 API 客户端使用。并且 **`@RestControllerAdvice` 无法处理**：安全过滤器在 `DispatcherServlet` 之前执行，因此在那里抛出的异常不会到达 Advice（参见[错误处理](error-handling.zh-CN.md)）。

应在入口点写入 `ProblemDetail`：

```java
private AuthenticationEntryPoint problemDetailEntryPoint() {
    return (request, response, ex) -> writeProblem(response, HttpStatus.UNAUTHORIZED,
            "Unauthorized", "Authentication is required to access this resource.");
}

private AccessDeniedHandler problemDetailAccessDeniedHandler() {
    return (request, response, ex) -> writeProblem(response, HttpStatus.FORBIDDEN,
            "Forbidden", "You do not have permission to perform this action.");
}

private void writeProblem(HttpServletResponse response, HttpStatus status,
                          String title, String detail) throws IOException {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    jsonMapper.writeValue(response.getOutputStream(), problem);
}
```

错误消息应保持通用。“该邮箱没有用户”会泄漏哪些账号已注册。

## 密码

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

- 永远不要存储、记录或返回密码，无论是否哈希。
- 不要自行编写哈希算法。工厂支持 BCrypt、SCrypt 和 Argon2。
- 使用 `passwordEncoder.matches(raw, stored)` 比较密码，不要用 `equals`。
- 登录失败时也应对虚拟值执行哈希比较。如果“用户不存在”时提前返回，可通过响应时间探测账号是否存在。

## 响应头、CORS 与 CSRF

Spring Boot 会设置合理默认值，包括 `X-Content-Type-Options`、`X-Frame-Options` 和安全响应的 `Cache-Control`。生产环境终止 TLS 时应添加 HSTS：

```java
.headers(headers -> headers
        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31_536_000).includeSubDomains(true))
        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")))
```

CORS 应集中配置，**绝不要同时使用 `allowedOrigins("*")` 和凭据**——浏览器会拒绝这种组合；若绕过浏览器限制，就等于关闭同源策略：

```java
@Bean
CorsConfigurationSource corsConfigurationSource(WebProperties properties) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(properties.allowedOrigins());   // 从配置中显式指定
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

然后在过滤器链配置 `.cors(Customizer.withDefaults())`。不要在各处散布 `@CrossOrigin`。

**CSRF：** 只在无状态 Token API 中关闭。基于 Cookie 的 Session 认证必须启用。

## Actuator

Actuator 端点不会自动获得超出你配置范围的安全保护，其中一些端点有危险。应为其设置独立过滤器链，并有意选择暴露内容（参见[可观测性](observability.zh-CN.md)）。

## 测试

```java
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)              // 切片不会自动加载自定义安全配置
class OrderControllerSecurityTest {

    @Autowired MockMvcTester mockMvc;
    @MockitoBean OrderService orderService;

    @Test
    void rejectsAnonymous() {
        assertThat(mockMvc.get().uri("/api/orders/{id}", id)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsWrongRole() {
        assertThat(mockMvc.delete().uri("/api/orders/{id}", id)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdmin() {
        assertThat(mockMvc.delete().uri("/api/orders/{id}", id)).hasStatus(HttpStatus.NO_CONTENT);
    }
}
```

classpath 中需包含安全测试支持。要测试**拒绝场景**——只验证成功路径的测试套件，在安全规则完全缺失时也能通过。

Service 上的 `@PreAuthorize` 也可以在切片 `@SpringBootTest` 中通过 `@WithMockUser` 测试，参见[Web 切片测试](testing-slices-web.zh-CN.md)。

## 使用 Spring Boot 3.5.x 时

使用 Spring Security **6**，这是真实差异。Lambda DSL 已是推荐写法且行为相同，但表中移除的 API 在 6 中仍存在（已弃用），因此 3.5.x 的 `.and()` 和 `antMatchers()` 代码可能仍能编译。两个版本线都写 Lambda DSL，可免去升级工作。

Security 6 中 `DaoAuthenticationProvider` 仍有 `setUserDetailsService` Setter。`ProblemDetail`、`@EnableMethodSecurity` 和 `@WithMockUser` 没有变化。

## 常见问题

- 写 `http.csrf().disable().and()...`——Security 7 已移除，只能使用 Lambda DSL。
- 使用 `antMatchers()` 或 `authorizeRequests()`——应使用 `requestMatchers()` 和 `authorizeHttpRequests()`。
- 继承 `WebSecurityConfigurerAdapter`——应声明 `SecurityFilterChain` Bean。
- Security 7 调用 `provider.setUserDetailsService(...)`——将其传给构造器。
- 忘记 `@EnableMethodSecurity`——`@PreAuthorize` 会静默失效并形成漏洞。
- 通过 `this` 调用带 `@PreAuthorize` 的方法——授权检查会被跳过。
- 多条过滤器链中有一条未配置 `securityMatcher`——该链会接管所有请求，后面的链永远不会执行。
- 把 `anyRequest()` 放在具体规则前——第一个匹配规则生效，应先写具体规则。
- 基于 Session 的应用关闭 CSRF——这是真实漏洞。
- 忘记配置 `exceptionHandling`——客户端得到空 401 / 403，Advice 又无法捕获过滤器异常。
- 用 `hasAuthority('ADMIN')` 检查角色——角色带有 `ROLE_` 前缀，应使用 `hasRole('ADMIN')`。
- 用 `hasRole` 检查 OAuth2 Scope——Scope 使用 `hasAuthority('SCOPE_...')`。
- 在 `@PreAuthorize` 中写长 SpEL——委托给命名 Bean，SpEL 不支持安全重构。
- 在修改数据的方法上使用 `@PostAuthorize`——方法执行后才授权。
- 返回“该邮箱没有用户”——会泄漏账号注册信息；错误消息应通用。
- 直接使用 `new BCryptPasswordEncoder()`——应使用 Delegating Encoder，以支持之后迁移哈希算法。
- `allowedOrigins("*")` 与 `allowCredentials(true)` 同时配置——浏览器会拒绝；应显式列出源。
- 只测试成功路径——应测试 401 和 403。
- `@WebMvcTest` 未导入 `SecurityConfig`——切片不会加载该配置，测试使用的只是 Boot 默认规则。

## 相关文档

- [JWT 认证](jwt-authentication.zh-CN.md) · [OAuth2 资源服务器](oauth2-resource-server.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [可观测性](observability.zh-CN.md) · [代理与依赖注入](spring-proxies-and-di.zh-CN.md) · [Web 切片测试](testing-slices-web.zh-CN.md)
