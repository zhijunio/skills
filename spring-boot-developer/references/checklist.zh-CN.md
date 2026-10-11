# 最佳实践清单

使用此清单审查现有代码。每条规则都链接到说明例外情况的参考文档。

语言本身的规则（Record、Sealed Type、`Optional`、`var`、模式匹配）请同时使用 java-developer 的清单。

## 版本与构建

| # | 规则 | 参考 |
| - | ---- | --------- |
| 1 | 使用当前 Boot 4.x 版本线；不要用 3.5.x 开始新项目。 | [Boot 版本](boot-versions.zh-CN.md) |
| 2 | Boot BOM 管理的依赖不要声明版本。 | [Maven](maven.zh-CN.md) · [Gradle](gradle.zh-CN.md) |
| 3 | Boot 4 显式添加 `spring-boot-starter-flyway`：它不再通过传递依赖引入，缺少时迁移会静默停止运行。 | [Flyway](flyway.zh-CN.md) |
| 4 | 将 3.4 → 3.5 → 4.0 分成单独提交迁移，然后审查差异。 | [Boot 版本](boot-versions.zh-CN.md) |
| 5 | 提交构建 Wrapper。用 Gradle Toolchain 固定 JDK，不要用 `sourceCompatibility`。 | [Maven](maven.zh-CN.md) · [Gradle](gradle.zh-CN.md) |

## 结构

| # | 规则 | 参考 |
| - | ---- | --------- |
| 6 | 按功能组织包，而非按层。Entity 和 Repository 使用包级可见性。 | [代码组织](code-organization.zh-CN.md) |
| 7 | 逐级加强边界：包级可见性 → Modulith → 嵌套模块 → 构建模块。 | [代码组织](code-organization.zh-CN.md) |
| 8 | 使用 Modulith 时，全局 `config/` 和 `shared/` 标记 `@ApplicationModule(type = OPEN)`。 | [Spring Modulith](spring-modulith.zh-CN.md) |
| 9 | 认证是全局职责；**授权属于具体功能**。 | [安全基础](security-fundamentals.zh-CN.md) |
| 10 | 用 ArchUnit 或 Modulith 强制验证布局和依赖规则，不要只依赖代码审查。 | [ArchUnit](archunit.zh-CN.md) |

## Bean 与代理

| # | 规则 | 参考 |
| - | ---- | --------- |
| 11 | 只使用构造器注入。唯一构造器上不需要字段注入或 `@Autowired`。 | [Spring 代理与依赖注入](spring-proxies-and-di.zh-CN.md) |
| 12 | **不要在 `this` 上调用带注解的方法**：这会绕过代理，使 `@Transactional`、`@Async`、`@Cacheable`、`@Retryable` 和 `@PreAuthorize` 静默失效。 | [Spring 代理与依赖注入](spring-proxies-and-di.zh-CN.md) |
| 13 | 不用 Lombok。显式编写构造器、访问器和 Logger。 | [Java 在 Spring 中](java-in-spring.zh-CN.md) |
| 14 | DTO、命令、事件和值对象使用 Record；Entity 和 Bean 使用类。 | [Java 在 Spring 中](java-in-spring.zh-CN.md) |

## Web

| # | 规则 | 参考 |
| - | ---- | --------- |
| 15 | 每个 Endpoint 调用一个 Service。不要输入或输出 Entity，不放 `@Transactional`，也不要写 `try`/`catch`。 | [REST Controller](rest-controllers.zh-CN.md) |
| 16 | 错误使用 RFC 9457 `ProblemDetail`；不要统一包装成功/错误响应。设置 `type`。 | [错误处理](error-handling.zh-CN.md) |
| 17 | 扩展 `ResponseEntityExceptionHandler`；全局 Advice 处理框架错误，功能 Advice 处理领域错误。 | [错误处理](error-handling.zh-CN.md) |
| 18 | 500 响应中不要放 `ex.getMessage()`。将其写入日志，响应返回通用信息。 | [错误处理](error-handling.zh-CN.md) |
| 19 | 用 `max-page-size` 限制 `Pageable`。不要返回 `Page<Entity>`。 | [REST Controller](rest-controllers.zh-CN.md) |
| 20 | 使用 `@Valid` 校验请求 Record，并报告**所有**违反项。 | [校验](validation.zh-CN.md) |
| 21 | Boot 4 声明 `JsonMapper` Bean，而非 `ObjectMapper`；不要替换 Boot 的 Mapper Bean。 | [JSON 与 Jackson](json-and-jackson.zh-CN.md) |
| 22 | 对映射进行版本化，不要版本化 Controller。只选一种版本解析策略。 | [API 版本管理](api-versioning.zh-CN.md) |

## 持久化

| # | 规则 | 参考 |
| - | ---- | --------- |
| 23 | 值对象 `@EmbeddedId` 使用时间有序的 UUIDv7。高写入表不要使用 `GenerationType.UUID`（UUIDv4）。 | [Spring Data JPA](spring-data-jpa.zh-CN.md) |
| 24 | `@Version` 使用可空的 `Long`：它同时支持乐观锁和新状态检测。 | [Spring Data JPA](spring-data-jpa.zh-CN.md) |
| 25 | Entity 不实现 `equals` / `hashCode`，除非有稳定的自然键；实现时使用 `instanceof`，不要用 `getClass()`。 | [Spring Data JPA](spring-data-jpa.zh-CN.md) |
| 26 | 每个 to-one 关系都设 `fetch = LAZY`。枚举使用 `@Enumerated(STRING)`。不要在 `equals` / `hashCode` / `toString` 中包含关联。 | [Spring Data JPA](spring-data-jpa.zh-CN.md) |
| 27 | 所有环境（包括本地）都设置 `spring.jpa.open-in-view=false` 和 `ddl-auto=validate`。 | [Spring Data JPA](spring-data-jpa.zh-CN.md) |
| 28 | 每个聚合根一个 Repository。列表视图使用 Projection。Endpoint 中不要调用 `findAll()`。 | [Spring Data JPA](spring-data-jpa.zh-CN.md) |
| 29 | 不要修改已经应用的迁移。破坏性变更采用 Expand and Contract。 | [Flyway](flyway.zh-CN.md) |

## 事务与副作用

| # | 规则 | 参考 |
| - | ---- | --------- |
| 30 | `@Transactional` 放在 Service 上。类级别设置 `readOnly = true`，写操作在方法级别覆盖。 | [事务](transactions.zh-CN.md) |
| 31 | **不要在事务中发起网络调用。** | [事务](transactions.zh-CN.md) |
| 32 | 副作用绑定到事务提交：使用 `@TransactionalEventListener(AFTER_COMMIT)` 或 Modulith 的 `@ApplicationModuleListener`。 | [事务](transactions.zh-CN.md) |
| 33 | 领域异常使用非受检异常，以自动触发回滚。 | [错误处理](error-handling.zh-CN.md) |
| 34 | 不要在同一个方法上叠加 `@Retryable` 和 `@Transactional`；将重试放到调用方 Bean。 | [弹性模式](resilience.zh-CN.md) |

## 集成

| # | 规则 | 参考 |
| - | ---- | --------- |
| 35 | **每个 HTTP 客户端都设置连接和读取超时。**默认值实际上接近无限。 | [HTTP 客户端](http-clients.zh-CN.md) |
| 36 | 在客户端边界将远端失败转换成领域异常。不要泄漏上游错误消息。 | [HTTP 客户端](http-clients.zh-CN.md) |
| 37 | 只重试暂时性错误，并使用退避**和抖动**。设置 `includes`；默认配置会重试程序错误。 | [弹性模式](resilience.zh-CN.md) |
| 38 | 每个消息 Consumer 都必须幂等；Broker 至少投递一次。 | [消息](messaging.zh-CN.md) |
| 39 | 不要在事务中直接向 Broker 发布；使用 Outbox 或 Modulith 事件注册表。 | [消息](messaging.zh-CN.md) |
| 40 | 不要设置 `spring.json.trusted.packages=*`，并始终使用 `ErrorHandlingDeserializer` 包装。 | [消息](messaging.zh-CN.md) |

## 安全

| # | 规则 | 参考 |
| - | ---- | --------- |
| 41 | Security 7 只使用 Lambda DSL。每个 Filter Chain 都设置 `securityMatcher`；`anyRequest()` 放在最后。 | [安全基础](security-fundamentals.zh-CN.md) |
| 42 | 设置 `@EnableMethodSecurity`；否则 `@PreAuthorize` 会静默失效，这是安全漏洞而非效率问题。 | [安全基础](security-fundamentals.zh-CN.md) |
| 43 | 配置 `authenticationEntryPoint` 和 `accessDeniedHandler`；Advice 无法捕获 Filter 异常。 | [安全基础](security-fundamentals.zh-CN.md) |
| 44 | 授权 STOMP 入站命令和订阅；Cookie 认证的 `CONNECT` 要求 CSRF，并限制连接数和帧大小。 | [WebSocket](websocket.zh-CN.md) |
| 44 | 优先使用外部 IdP。自行签发时使用 Spring Security 的 Encoder / Decoder，不要手写 Filter。 | [JWT 认证](jwt-authentication.zh-CN.md) |
| 45 | 同时设置 `issuer-uri` 和 `audiences`；Scope 使用 `hasAuthority('SCOPE_…')`，而非 `hasRole`。 | [OAuth2 资源服务器](oauth2-resource-server.zh-CN.md) |
| 46 | 刷新 Token 使用不透明值、仅存哈希、每次使用都轮换，并将重放视为凭据泄漏。 | [JWT 认证](jwt-authentication.zh-CN.md) |

## 配置与运维

| # | 规则 | 参考 |
| - | ---- | --------- |
| 47 | 使用带 `@Validated` 的 `@ConfigurationProperties` Record，不要散落使用 `@Value`。启动时校验失败。 | [配置](configuration.zh-CN.md) |
| 48 | 仓库中不保存秘密。必需秘密不要提供默认值，缺失时应阻止启动。 | [配置](configuration.zh-CN.md) |
| 49 | 未指定 Profile 时的默认配置必须安全。不要用 `@Profile` 区分业务实现。 | [配置](configuration.zh-CN.md) |
| 50 | 不要设置 `management.endpoints.web.exposure.include: "*"`：这会暴露 `/env` 和 `/heapdump`。 | [可观测性](observability.zh-CN.md) |
| 51 | 数据库只加入**就绪探针**，不要加入存活探针。 | [可观测性](observability.zh-CN.md) |
| 52 | 指标不能使用用户 ID、实体 ID 或异常消息作为标签。 | [可观测性](observability.zh-CN.md) |
| 53 | 记录异常对象，而不是 `ex.getMessage()`。不要记录秘密或个人数据。 | [可观测性](observability.zh-CN.md) |
| 54 | Java 21+ 启用默认关闭的虚拟线程，然后调整连接池大小；连接池现在会成为瓶颈。 | [异步与定时任务](async-and-scheduling.zh-CN.md) |
| 55 | 虚拟线程不能并行化一个请求内的串行 Fan-out。`StructuredTaskScope` 才能解决：Java 25 中是 Preview；应放在 Service，不要放在 Controller，也不要在 `@Transactional` 中围绕 Repository 调用使用。 | [异步与定时任务](async-and-scheduling.zh-CN.md) |
| 56 | `@Scheduled` Job 会在每个副本运行一次。应保证幂等或显式认领任务。 | [异步与定时任务](async-and-scheduling.zh-CN.md) |
| 57 | 使用 `MaxRAMPercentage`，而非 `-Xmx`。以非 root 用户运行。优雅停机时间应短于编排器的宽限时间。 | [容器化与 Native Image](containerization-and-native.zh-CN.md) |

## 测试

| # | 规则 | 参考 |
| - | ---- | --------- |
| 58 | 分层测试：单元、切片、冒烟和一个端到端测试。单元测试全绿不代表应用可运行。 | [测试策略](testing-strategy.zh-CN.md) |
| 59 | 保护上下文缓存：共享 `application-test.yml`、将 Mock 放入基类、避免 `@DirtiesContext`。 | [测试策略](testing-strategy.zh-CN.md) |
| 60 | 使用 Testcontainers 测试真实数据库，绝不要用 H2；H2 也意味着迁移没有运行。 | [持久化切片测试](testing-slices-persistence.zh-CN.md) |
| 61 | 整个测试套件共享一个容器；使用 `static` 容器或共享 `@TestConfiguration`，并固定镜像 Tag。 | [Testcontainers](testcontainers.zh-CN.md) |
| 62 | 有对应工厂时使用 `@ServiceConnection`；只有配置值无法预先定义时才用 `DynamicPropertyRegistrar`。 | [Testcontainers](testcontainers.zh-CN.md) |
| 63 | `@WebMvcTest` 需要 `@Import(SecurityConfig.class)`，否则测到的是 Boot 默认安全配置。 | [Web 测试切片](testing-slices-web.zh-CN.md) |
| 64 | 断言负向安全场景：401 和 403。 | [Web 测试切片](testing-slices-web.zh-CN.md) |
| 65 | 异步行为使用 Awaitility。不要使用 `Thread.sleep`。 | [集成测试](testing-integration.zh-CN.md) |
| 66 | 注入 `Clock`；可测试代码中不要直接调用 `Instant.now()`。 | [单元测试](testing-unit.zh-CN.md) |
| 67 | 使用 `@MockitoBean`，不要使用 Boot 4 已移除的 `@MockBean`。 | [Boot 版本](boot-versions.zh-CN.md) |

## AI

| # | 规则 | 参考 |
| - | ---- | --------- |
| 68 | 版本配对：Boot 4 使用 Spring AI 2.0，Boot 3.5 使用 1.x。 | [Spring AI](spring-ai.zh-CN.md) |
| 69 | 会话 ID 按用户或 Session 隔离；共享 ID 会泄漏用户之间的历史记录。 | [Spring AI](spring-ai.zh-CN.md) |
| 70 | 不要将用户输入拼接进 Prompt；不要不转义地执行或渲染模型输出。 | [Spring AI](spring-ai.zh-CN.md) |
| 71 | 生产环境设置 `log-prompt: false`，指标标签不能包含 Prompt 内容。 | [AI 可观测性](ai-observability.zh-CN.md) |
| 72 | 限制 `max-tokens`，按操作追踪 Token 用量，并对费用告警。 | [AI 可观测性](ai-observability.zh-CN.md) |

## 跨领域 API 防护

| # | 规则 | 参考 |
| - | ---- | --------- |
| 73 | 使用面向资源的路径；过滤、分页和白名单排序放在查询参数中。凭据不要放入 URL。 | [REST Controller](rest-controllers.zh-CN.md) |
| 74 | 可重试的创建操作应定义有作用范围的幂等契约；原子保存幂等键，并在重放响应前重新检查授权。 | [API 幂等](api-idempotency.zh-CN.md) |
| 75 | 在合适的网关和应用层实施限流及硬资源上限；返回有文档说明的过载响应。 | [限流与资源保护](rate-limiting.zh-CN.md) |
| 76 | 数据库约束作为最终并发保护；处理失败事务边界后再转换约束冲突。 | [数据库并发](database-concurrency.zh-CN.md) |
| 77 | 租户上下文来自可信身份，并在持久化、缓存、存储和后台任务的每条路径强制隔离。 | [多租户](multi-tenancy.zh-CN.md) |
| 78 | CI 晋级同一份已验证制品摘要；数据库 Schema 变更在发布和回滚窗口内保持兼容。 | [CI/CD 与发布](ci-cd-release.zh-CN.md) |
| 79 | 公共 API 变更要做兼容性检查；弃用需有替代方案、负责人、日期和已测量的客户端用量。 | [API 版本管理](api-versioning.zh-CN.md) |
| 80 | 每个秘密都应有负责人、最小权限范围、轮换和撤销流程；CI 不向不可信代码暴露秘密。 | [配置](configuration.zh-CN.md) |
| 81 | 告警应有负责人、用户影响、可执行手册和符合 SLO 的阈值；校验客户端提供的关联 ID。 | [可观测性](observability.zh-CN.md) |
| 82 | 备份策略应满足所声明的 RPO/RTO；恢复演练应验证数据、Schema、访问控制和依赖存储。 | [数据库备份与恢复](database-backup-recovery.zh-CN.md) |
| 83 | 外部客户端测试应经过真实网络边界；本地替身不可用时不能静默调用真实提供商。 | [外部服务测试](external-service-testing.zh-CN.md) |

---

## 造成最大损害的五个问题

1. **自调用。**`this.method()` 会静默禁用 `@Transactional`、`@Async`、`@Cacheable`、`@Retryable`，以及作为安全漏洞的 `@PreAuthorize`。没有异常，也没有警告。
2. **持久化测试使用 H2。**迁移不会运行，数据库专属 SQL 会产生差异，但测试套件仍一直全绿。
3. **HTTP 客户端没有超时。**一个慢依赖会耗尽请求线程，并拖垮整个应用。
4. **`exposure.include: "*"`。**`/actuator/heapdump` 可以下载进程内存副本，其中包含所有秘密。
5. **保持 `open-in-view` 开启。**N+1 问题在开发环境中静默通过，最终在生产环境成为事故。
