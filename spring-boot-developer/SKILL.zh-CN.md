---
name: spring-boot-developer
description: 面向 Spring Boot 4.x 和 3.5.x 的现代 Spring Boot 开发与架构指导。适用于编写或现代化 Spring Boot 应用，以及包结构、Spring Modulith、REST Controller、WebSocket、STOMP、RFC 9457 错误处理、校验、Jackson 3、Spring Data JPA、事务、Flyway、缓存、Spring Security、JWT、OAuth2、HTTP 客户端、韧性、消息、配置、Actuator 可观测性、异步、虚拟线程与结构化并发、Spring Batch、Spring AI、MCP、Testcontainers 测试和 Boot 3 到 Boot 4 迁移。
license: MIT
metadata:
  author: Avinay Basnet
  version: '1.0'
---

# Spring Boot Developer 指南

覆盖 Spring Boot 4.x（主要目标版本）和 3.5.x（最后一个 3.x 系列，也是常见的升级来源）。

1. **给出建议前，先确认 Boot、Java 和构建工具版本。**Boot 4 更改了 starter、采用 Jackson 3、Spring Security 7、Spring Batch 6、Testcontainers 2 和 JSpecify。适用于 4.x 的建议在 3.5.x 上可能无法编译，反之亦然。
2. **本技能与 [`java-developer`](https://github.com/zhijunio/skills) 配合使用。**本技能可以独立使用；若同时安装，java-developer 负责语言层（不可变性、record、sealed type、模式匹配、`Optional`、`var`、文本块），本技能负责框架层，不重述或覆盖语言规则。两者如何衔接见 [java-in-spring.zh-CN.md](references/java-in-spring.zh-CN.md)。
3. **不要在新代码中生成 Lombok。**不要使用 `@Data`、`@Getter`、`@RequiredArgsConstructor`、`@Builder` 或 `@Slf4j`，改为显式编写构造器、访问器和 logger。替代方式见 [java-in-spring.zh-CN.md](references/java-in-spring.zh-CN.md)。如果项目已经使用 Lombok，应沿用现有风格并指出这一点，不要悄悄混用。
4. **本技能针对 Spring MVC。**使用 Servlet 技术栈：Java 21+ 可用虚拟线程处理跨请求并发，用结构化并发处理单个请求内的 fan-out。本技能不提供 WebFlux 的推测性指导；若 Boot 同时提供两套配置，参考资料会指出 `spring.webflux.*` 配置键并止于此。真正的响应式需求不在范围内，应明确说明。
5. **错误响应使用 RFC 9457 `ProblemDetail`。**不要再套统一的成功/错误 envelope。成功响应使用 DTO，由 HTTP 状态码表达语义。见 [error-handling.zh-CN.md](references/error-handling.zh-CN.md)。
6. **生成代码后，用项目构建工具编译并运行测试。**Starter 改名、Jackson 3 包名迁移和 Security 7 API 移除会导致编译失败；实体映射错误则可能只在运行时暴露。

凡是 Boot 3.5.x 行为不同的参考文档，均包含 `## Boot 3.5.x 说明`；每份参考文档也包含 `## 常见误区`。即使只是快速浏览，也要阅读这些内容。

## 确认版本

**步骤 1：**读取构建配置，而不是本机安装的工具链。Maven 查看 `spring-boot-starter-parent` 版本，或 `dependencyManagement` 中的 `spring-boot-dependencies`；Gradle 查看 `org.springframework.boot` 插件版本。还要检查 `spring-boot.version` 属性和 CI 工作流。

**步骤 2：**同样确认 Java 版本。Boot 4 的最低要求是 Java 17，目标实践主要面向 21+；虚拟线程和 record patterns 的建议也受 Java 版本限制。Java 版本细节见 [`java-developer` 的版本表](https://github.com/zhijunio/skills)。

**步骤 3：**只生成目标版本中存在的 API。如果某项建议需要 Boot 4，要说明版本要求并给出 3.5.x 替代方案。不要默默为 3.5.x 项目生成 Boot 4 代码。

当前版本线和完整的 Boot 3.5 → 4 差异索引见 [boot-versions.zh-CN.md](references/boot-versions.zh-CN.md)。

## 基础

- **Boot 版本与迁移：**当前发布线、3.5 → 4 变更索引（starter 改名、Jackson 3、Security 7、Batch 6、Testcontainers 2、JSpecify、移除 `@MockBean`、Flyway 不再由其他 starter 传递引入）和迁移方法。读 [boot-versions.zh-CN.md](references/boot-versions.zh-CN.md)。
- **代码组织：**按功能组织包；从包级可见性到 Spring Modulith、Maven 模块的四级边界；全局 `config/` 与 `shared/` 的位置；以及为何由功能自己拥有授权逻辑优于一个巨型 `SecurityConfig`。读 [code-organization.zh-CN.md](references/code-organization.zh-CN.md)。
- **Spring Modulith：**模块基础包作为 API、嵌套包作为内部实现、`@NamedInterface`、为扩展后的功能建立嵌套 `@ApplicationModule`，以及事件基础设施。读 [spring-modulith.zh-CN.md](references/spring-modulith.zh-CN.md)。
- **Spring 代理与依赖注入：**不依赖 Lombok 的构造器注入，以及自调用陷阱：它会让 `@Transactional`、`@Async`、`@Cacheable`、`@Retryable` 和 `@PreAuthorize` 一并悄然失效。读 [spring-proxies-and-di.zh-CN.md](references/spring-proxies-and-di.zh-CN.md)。

## Spring 中的 Java

- **Java 在 Spring 中：**将 java-developer 的规则应用到 Spring 项目：DTO、命令、事件和值对象使用 record，实体使用类；Lombok 替代方案；Repository 返回 `Optional`；用 sealed type 表达领域状态。读 [java-in-spring.zh-CN.md](references/java-in-spring.zh-CN.md)。

## Web 层

- **REST Controller：**Controller 的职责与禁区、URL 和状态码约定、值对象 ID 的绑定，以及限制 `Pageable`。读 [rest-controllers.zh-CN.md](references/rest-controllers.zh-CN.md)。
- **WebSocket 与 STOMP：**何时选择服务端推送、STOMP broker messaging 或原生 Handler，消息授权、连接限额、扩展和协议边界测试。读 [websocket.zh-CN.md](references/websocket.zh-CN.md)。
- **文件上传与下载：**Multipart 限制、内容校验、不透明存储键、授权、流式传输和清理。读 [file-storage.zh-CN.md](references/file-storage.zh-CN.md)。
- **Thymeleaf：**服务端 MVC 视图、表单 DTO、Post/Redirect/Get、安全模板输出、CSRF 和 HTML 渲染测试。读 [thymeleaf.zh-CN.md](references/thymeleaf.zh-CN.md)。
- **国际化：**消息资源、Locale/时区选择、校验、本地化 API 错误和测试。读 [internationalization.zh-CN.md](references/internationalization.zh-CN.md)。
- **错误处理：**RFC 9457 `ProblemDetail`、全局与功能级 `@RestControllerAdvice` 的分工、领域异常层次，以及安全过滤器异常为何不会进入 Controller Advice。读 [error-handling.zh-CN.md](references/error-handling.zh-CN.md)。
- **校验：**Jakarta Validation，以及请求 DTO、record 紧凑构造器、实体不变量这三个校验位置及选择依据。读 [validation.zh-CN.md](references/validation.zh-CN.md)。
- **JSON 与 Jackson：**Boot 4 的 Jackson 3（`tools.jackson`、`JsonMapper`、`@JacksonComponent`）、record 与值对象序列化，以及 3.5.x 的 Jackson 2 方案。读 [json-and-jackson.zh-CN.md](references/json-and-jackson.zh-CN.md)。
- **API 版本：**Boot 4 原生 `version` 映射属性、兼容性检查、弃用流程与响应头，以及 3.5.x 方案。读 [api-versioning.zh-CN.md](references/api-versioning.zh-CN.md)。
- **HATEOAS：**Assembler、`PagedResourcesAssembler`，以及何时超媒体值得其成本。读 [hateoas.zh-CN.md](references/hateoas.zh-CN.md)。
- **OpenAPI：**使用 delegate 模式的 spec-first 生成，以及使用 springdoc 的 code-first 方式。读 [openapi.zh-CN.md](references/openapi.zh-CN.md)。

## 持久化

- **Spring Data JPA：**实体建模、使用 UUIDv7 的值对象 `@EmbeddedId`、`equals`/`hashCode` 规则、按聚合根设置 Repository、投影、N+1、keyset 分页和批量写入。读 [spring-data-jpa.zh-CN.md](references/spring-data-jpa.zh-CN.md)。
- **数据库并发：**约束、乐观锁和悲观锁、事务边界、重试及真实数据库并发测试。读 [database-concurrency.zh-CN.md](references/database-concurrency.zh-CN.md)。
- **数据库备份与恢复：**RPO/RTO、隔离恢复、数据库与文件存储的一致性，以及恢复演练。读 [database-backup-recovery.zh-CN.md](references/database-backup-recovery.zh-CN.md)。
- **审计字段：**Spring Data 审计元数据、当前与系统操作者、隐私边界，以及可变元数据和不可变历史的区别。读 [auditing.zh-CN.md](references/auditing.zh-CN.md)。
- **事务：**`@Transactional` 的位置、传播行为、`readOnly`、回滚规则、乐观锁，以及将副作用绑定到提交时机。读 [transactions.zh-CN.md](references/transactions.zh-CN.md)。
- **Flyway：**命名、expand/contract 安全迁移、`CONCURRENTLY` 的事务陷阱，以及 Boot 4 不再传递引入 Flyway 的变化。读 [flyway.zh-CN.md](references/flyway.zh-CN.md)。
- **缓存：**先讲 Spring Cache 抽象，再讲 Redis 特性，包括序列化、带抖动的 TTL 和热点 key 击穿。读 [caching.zh-CN.md](references/caching.zh-CN.md)。
- **缓存一致性：**提交顺序、持久化失效、陈旧读竞态、多实例一致性和缓存故障行为。读 [cache-consistency.zh-CN.md](references/cache-consistency.zh-CN.md)。

## 架构模式

- **领域建模：**聚合、值对象、提交后发布领域事件、Specification、防腐层，以及服务层的 `Cmd` / `Query` / `Result` 约定。读 [domain-modelling.zh-CN.md](references/domain-modelling.zh-CN.md)。
- **六边形架构：**将端口和适配器作为功能内部结构，与按功能组织包并不冲突。读 [hexagonal-architecture.zh-CN.md](references/hexagonal-architecture.zh-CN.md)。

## 安全

- **安全基础：**Spring Security 7 的 lambda-only DSL、过滤器链、方法安全、全局认证与功能内授权的分工、JSON 401/403，以及安全代码测试。读 [security-fundamentals.zh-CN.md](references/security-fundamentals.zh-CN.md)。
- **JWT 认证：**自签发令牌的签名、过滤器、访问令牌与刷新令牌、令牌轮换。读 [jwt-authentication.zh-CN.md](references/jwt-authentication.zh-CN.md)。
- **OAuth2 Resource Server：**验证外部 IdP 的令牌、issuer 和 JWK 配置，以及将 claim 映射为权限。读 [oauth2-resource-server.zh-CN.md](references/oauth2-resource-server.zh-CN.md)。
- **多租户：**解析可信租户上下文、在持久化和异步任务中执行隔离，以及测试跨租户访问。读 [multi-tenancy.zh-CN.md](references/multi-tenancy.zh-CN.md)。

## 集成

- **HTTP 客户端：**声明式 `@HttpExchange`、Boot 4 的 `@ImportHttpServices` 分组注册、3.5.x 手动代理工厂，以及 `RestClient` 和 `WebClient` 的选择。读 [http-clients.zh-CN.md](references/http-clients.zh-CN.md)。
- **韧性：**Spring Framework 7 内建的 `@Retryable` 与 `@ConcurrencyLimit`（并非重新打包的 Spring Retry），以及为何重试层应位于事务之外。读 [resilience.zh-CN.md](references/resilience.zh-CN.md)。
- **外部服务测试：**本地 HTTP Stub、提供方契约、安全的本地替身和失败路径验证。读 [external-service-testing.zh-CN.md](references/external-service-testing.zh-CN.md)。
- **消息：**Kafka、RabbitMQ 和 JMS 的序列化、消费者错误处理、死信主题、幂等消费者和 transactional outbox。读 [messaging.zh-CN.md](references/messaging.zh-CN.md)。
- **API 幂等性：**非幂等命令安全重试、持久化键范围、并发重复请求、响应重放授权和 Outbox 边界。读 [api-idempotency.zh-CN.md](references/api-idempotency.zh-CN.md)。
- **邮件：**SMTP 配置与超时、提交后投递、Outbox/重试保证、本地邮件收件箱和投递测试。读 [mail.zh-CN.md](references/mail.zh-CN.md)。

## 运行与运维

- **配置与密钥：**`@ConfigurationProperties`、Profile 和环境变量绑定、启动时校验、密钥注入、最小授权与凭据轮换。读 [configuration.zh-CN.md](references/configuration.zh-CN.md)。
- **异步与调度：**`@Async` 的代理陷阱、任务执行器、`@Scheduled`、虚拟线程何时能替代响应式编程，以及虚拟线程无法解决的单请求内 fan-out。读 [async-and-scheduling.zh-CN.md](references/async-and-scheduling.zh-CN.md)。
- **可观测性：**Actuator 端点、健康探针、Micrometer 指标、追踪、结构化日志、可执行告警与 SLO，以及生产环境安全暴露。读 [observability.zh-CN.md](references/observability.zh-CN.md)。
- **限流与资源保护：**Gateway 与应用内限额、租户配额、429 响应、硬资源上限和分布式故障行为。读 [rate-limiting.zh-CN.md](references/rate-limiting.zh-CN.md)。
- **CI/CD 与发布：**可重复制品晋级、数据库结构兼容、发布门槛、流水线最小权限和恢复证据。读 [ci-cd-release.zh-CN.md](references/ci-cd-release.zh-CN.md)。
- **JVM 性能由单独技能负责。**Actuator 和 Micrometer 能告诉你应用变慢了，但不会调优 JVM。GC 停顿、堆、`OutOfMemoryError`、本机内存、容器限制、CDS/AOT 启动、JFR/async-profiler、线程池容量等问题，使用 [java-performance-developer](../java-performance-developer/SKILL.md)。它负责证明瓶颈所在，再把框架配置问题交回本技能。
- **Spring Batch：**Batch 6 API、默认的无资源 JobRepository 如何影响可重启性、`JobOperator`、chunk 边界和 Reader 线程安全。读 [spring-batch.zh-CN.md](references/spring-batch.zh-CN.md)。

## AI

- **Spring AI：**`ChatClient`、聊天记忆与必需的会话 ID、外部化提示模板、结构化输出、RAG 和流式响应。读 [spring-ai.zh-CN.md](references/spring-ai.zh-CN.md)。
- **MCP Server：**使用 Spring AI starter 构建 MCP Server、`@McpTool`、传输方式，以及 stdio 模式下保持标准输出干净。读 [mcp-server.zh-CN.md](references/mcp-server.zh-CN.md)。
- **AI 可观测性：**GenAI 语义约定指标、advisor API、Token 与成本统计，以及避免将提示内容写入生产日志。读 [ai-observability.zh-CN.md](references/ai-observability.zh-CN.md)。

## 测试

不同测试层级相互补充，而非彼此替代。单元测试全绿，仍可能掩盖映射、序列化或配置错误。大多数框架相关测试应优先使用快速切片测试；完整上下文 `@SpringBootTest` 留给启动冒烟测试和少量请求级端到端测试。

- **测试策略：**层级互补、切片测试优先、上下文缓存及其缓存键、每个测试套件使用一个容器，以及 Native Image 的注意事项。读 [testing-strategy.zh-CN.md](references/testing-strategy.zh-CN.md)。

| 被测内容 | 层级 | 参考 |
|---|---|---|
| 纯业务逻辑，不依赖框架 | 单元测试，不启动 Spring | [testing-unit.zh-CN.md](references/testing-unit.zh-CN.md) |
| JPA / JDBC / Spring Data JDBC / jOOQ 查询 | 持久化切片 + Testcontainers | [testing-slices-persistence.zh-CN.md](references/testing-slices-persistence.zh-CN.md) |
| Controller、JSON 序列化、外部 REST 客户端 | Web 切片 | [testing-slices-web.zh-CN.md](references/testing-slices-web.zh-CN.md) |
| 少量真实组件协作、外部 HTTP 或启动冒烟 | 指定 classes 的切片式 `@SpringBootTest` | [testing-integration.zh-CN.md](references/testing-integration.zh-CN.md) |
| 真实端口上的完整 REST API | 端到端测试 | [testing-rest-api.zh-CN.md](references/testing-rest-api.zh-CN.md) |
| 容器接线 | — | [testcontainers.zh-CN.md](references/testcontainers.zh-CN.md) |
| 架构和包布局规则 | 构建时检查 | [archunit.zh-CN.md](references/archunit.zh-CN.md) |

持久化和集成测试都应使用 Testcontainers 连接真实数据库，而不是内存 H2；详见 [testcontainers.zh-CN.md](references/testcontainers.zh-CN.md)。

## 构建与交付

其余内容与构建工具无关；按项目选读：

- **Maven：**[maven.zh-CN.md](references/maven.zh-CN.md)，介绍 parent、starter 和值得配置的插件。
- **Gradle：**[gradle.zh-CN.md](references/gradle.zh-CN.md)，介绍 Kotlin DSL 对应写法。
- **本地开发：**Boot 的 Docker Compose 支持、devtools 和常用命令的 Taskfile。读 [local-development.zh-CN.md](references/local-development.zh-CN.md)。
- **Taskfile：**设计易发现、可移植且不重复构建逻辑的开发任务。读 [taskfile.zh-CN.md](references/taskfile.zh-CN.md)。
- **容器与 Native：**Buildpacks、分层 JAR、GraalVM Native Image 与 AOT。读 [containerization-and-native.zh-CN.md](references/containerization-and-native.zh-CN.md)。

## 清单

- **最佳实践清单：**审查现有代码时使用的汇总清单。读 [checklist.zh-CN.md](references/checklist.zh-CN.md)。
