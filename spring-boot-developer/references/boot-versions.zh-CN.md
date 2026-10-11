# Boot 版本与迁移

## 写代码前确认版本

构建配置是权威来源。

| 构建工具 | 检查位置 |
|---|---|
| Maven | `spring-boot-starter-parent` 版本，或 `dependencyManagement` 中导入的 `spring-boot-dependencies` |
| Gradle | `org.springframework.boot` 插件版本，或 `spring-boot-dependencies` platform |
| 两者都适用 | `spring-boot.version` 属性、CI 工作流、Dockerfile 基础镜像 |

然后以相同方式确认 Java 版本，参见 java-developer 的 `java-versions.md`。Boot 4 的最低版本是 **Java 17**；虚拟线程和较新的语言指南需要 Java 21 及以上。

## Spring Boot 发布线

| 发布线 | 状态 | Spring Framework | 说明 |
|---|---|---|---|
| **4.1.x** | 当前 | 7 | 新项目默认目标 |
| 4.0.x | 维护中 | 7 | |
| 3.5.x | 最后一个 3.x 发布线 | 6 | 现实中常见的升级来源，仍广泛部署 |
| ≤ 3.4 | 本指南不覆盖 | | 先升级到 3.5.x，再升级到 4.x |

新项目应使用当前 4.x 发布线。不要在新项目中选择 3.5.x。

**不要固定 Boot BOM 已管理的依赖版本。**Parent 或导入的 BOM 会设置 Hibernate、Jackson、Testcontainers、JUnit、Micrometer 等数百个依赖的版本。自行覆盖这些版本容易造成运行时 `NoSuchMethodError`。只为 Boot 不管理的库指定版本。

## Boot 3.5 → 4.0 变更索引

本节同时用于迁移和判断某 API 是否存在于目标版本。

### 构建和 starter

| 变化 | 详情 |
|---|---|
| 模块化 starter | Boot 拆分为 `spring-boot-<technology>` 模块，根包为 `org.springframework.boot.<technology>` |
| `spring-boot-starter-web` | → `spring-boot-starter-webmvc` |
| `spring-boot-starter-aop` | → `spring-boot-starter-aspectj` |
| `spring-boot-starter-oauth2-*` | → `spring-boot-starter-security-oauth2-*`（client、resource-server、authorization-server） |
| `spring-boot-starter-web-services` | → `spring-boot-starter-webservices` |
| 测试 starter | 按技术拆分，如 `spring-boot-starter-webmvc-test`、`spring-boot-starter-data-jpa-test`。它们是完整套件，不要再同时添加普通 `spring-boot-starter-test` |
| Flyway / Liquibase | 不再由 JDBC/JPA starter **传递引入**。必须声明 `spring-boot-starter-flyway` 或 `-liquibase`，否则迁移可能悄然不运行 |
| 可选依赖 | 默认从重新打包的 JAR 中排除；依赖它们时设置 `<includeOptional>true</includeOptional>` |
| 自动配置内部成员 | 自动配置类中的成员不再是公开 API，不要直接依赖它们 |
| **自动配置随模块提供** | 如果所属模块缺失，`spring.*` 属性**不会绑定，也不会报告问题**。在 4.1.0 验证：仅有 `spring-boot-starter-web` 和 `spring-data-commons` 时，`spring.data.web.pageable.serialization-mode=VIA_DTO` 会被静默忽略；加入 `spring-boot-data-commons` 后才生效。与 Flyway 一行的问题形态相同，只是发生在更深一层 |
| 过渡方案 | `spring-boot-starter-classic` / `-test-classic` 打包旧的单体集合，只用于迁移辅助 |

改名的 starter 是**已弃用但未移除**：旧名称在 4.x 仍能解析。新代码应使用新名称，但旧名称不会导致构建失败。

### 语言与空值

| 变化 | 详情 |
|---|---|
| JSpecify | Framework 使用 `@NullMarked`。`org.springframework.lang.Nullable` 替换为 `org.jspecify.annotations.Nullable`。构建中的 null 检查器或 Kotlin 可能因此报告以前未显现的问题 |

### JSON

| 变化 | 详情 |
|---|---|
| Jackson 3 | `com.fasterxml.jackson.*` → `tools.jackson.*` |
| Mapper bean | 声明 `JsonMapper`，而不是通用 `ObjectMapper` |
| Customizer | `Jackson2ObjectMapperBuilderCustomizer` → `JsonMapperBuilderCustomizer` |
| 注解 | `@JsonComponent` → `@JacksonComponent` |
| `java.time` | 原生支持；不要再注册 `JavaTimeModule` |
| 属性 | `spring.jackson.*` 配置 Jackson 3；Jackson 2 对应项迁移到 `spring.jackson2.*` |

读 [json-and-jackson.zh-CN.md](json-and-jackson.zh-CN.md)。

### 安全：Spring Security 7

| 移除项 | 替代方式 |
|---|---|
| `and()` 链式调用、`authorizeRequests()` | 只使用 lambda DSL：`authorizeHttpRequests(auth -> …)` |
| `antMatchers()`、`mvcMatchers()` | `requestMatchers("/path/**")` |
| `AntPathRequestMatcher`、`MvcRequestMatcher` | `PathPatternRequestMatcher` |
| `WebSecurityConfigurerAdapter` | `SecurityFilterChain` bean |
| `provider.setUserDetailsService(…)` | `new DaoAuthenticationProvider(userDetailsService)` |

读 [security-fundamentals.zh-CN.md](security-fundamentals.zh-CN.md)。

### 持久化

| 变化 | 详情 |
|---|---|
| Jakarta Persistence 3.2、Hibernate ORM 7 | record 可作为 `@Embeddable`，即使没有该注解也可以 |
| Spring Data | 版本与 Boot 4 对齐 |

读 [spring-data-jpa.zh-CN.md](spring-data-jpa.zh-CN.md)。

### 批处理：Spring Batch 6

| 变化 | 详情 |
|---|---|
| 默认 `JobRepository` 是**无资源模式** | 存储在内存中，不支持重启，也无审计。需要 `BATCH_*` 表时加入 `spring-boot-starter-batch-jdbc` |
| `JobLauncher` + `JobExplorer` | 合并为 `JobOperator` |
| `chunk(n, txManager)` | 改为 `chunk(n)` 并调用 `.transactionManager(txManager)` |
| 序列改名 | `BATCH_JOB_SEQ` → `BATCH_JOB_INSTANCE_SEQ`；已有数据库需要迁移 |

读 [spring-batch.zh-CN.md](spring-batch.zh-CN.md)。

### 测试

| 变化 | 详情 |
|---|---|
| `@MockBean` / `@SpyBean` | **已移除**，改用 `@MockitoBean` / `@MockitoSpyBean` |
| `@SpringBootTest` 与 MockMvc | 不再自动提供 MockMvc，需添加 `@AutoConfigureMockMvc` |
| `RestTestClient` | Framework 7 新增，位于 `spring-boot-resttestclient` 模块；通过 `@AutoConfigureRestTestClient` 启用 |
| `TestRestTemplate` | 现在需要 `@AutoConfigureTestRestTemplate` |
| Testcontainers 2.x | `org.testcontainers:junit-jupiter` → `testcontainers-junit-jupiter`；`org.testcontainers:postgresql` → `testcontainers-postgresql`；`org.testcontainers.containers.PostgreSQLContainer` → `org.testcontainers.postgresql.PostgreSQLContainer` |
| JUnit 6 | 替代 JUnit 5；测试代码本身无需改变 |
| 上下文暂停 | Framework 7 会在缓存上下文的测试之间暂停 `@Scheduled` 任务和监听器 |
| 测试自动配置包 | 随模块化迁移；让 IDE 重新解析 import |

读 [testing-strategy.zh-CN.md](testing-strategy.zh-CN.md) 和 [testcontainers.zh-CN.md](testcontainers.zh-CN.md)。

### Boot 4 / Framework 7 新能力

| 功能 | 替代方式 |
|---|---|
| 原生 API 版本管理：映射上的 `version` 属性、`spring.mvc.apiversion.*` | 手工复制 `/v1` Controller。读 [api-versioning.zh-CN.md](api-versioning.zh-CN.md) |
| 核心韧性 API：`org.springframework.resilience.annotation` 中的 `@Retryable`、`@ConcurrencyLimit`、`@EnableResilientMethods` | 独立 Spring Retry 项目。**并非简单重新打包**，属性名和恢复行为都不同。读 [resilience.zh-CN.md](resilience.zh-CN.md) |
| `@ImportHttpServices` 分组注册 | 每个客户端手工建立一个 `HttpServiceProxyFactory` bean。读 [http-clients.zh-CN.md](http-clients.zh-CN.md) |

### 生态版本配对

| 项目 | Boot 3.5.x | Boot 4.x |
|---|---|---|
| Spring Framework | 6 | 7 |
| Spring Security | 6 | 7 |
| Spring Batch | 5 | 6 |
| Spring Modulith | 1.4.x | 2.x |
| Spring AI | 1.x | 2.0 |
| Jackson | 2 | 3 |
| Testcontainers | 1.x | 2.x |
| JUnit | 5 | 6 |

配错版本是依赖图无法解析的常见原因。Boot 4 上使用 Spring AI 1.x，或 Boot 3.5 上使用 2.0，都无法正常工作。

## 执行迁移

分开提交，按 **3.4 → 3.5 → 4.0** 迁移。跳过 3.5 会把彼此无关的破坏性变化混在一起。

[OpenRewrite](https://docs.openrewrite.org/recipes/java/spring/boot4) 提供升级及 starter 拆分的迁移 recipe：

| Recipe | 作用 |
|---|---|
| `org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0` | 总体迁移：构建文件、弃用 API，以及 Framework 7 / Security 7 / Batch 6 子 recipe |
| `org.openrewrite.java.spring.boot4.MigrateToModularStarters` | 将 starter 改写为模块化依赖 |
| `org.openrewrite.java.spring.boot4.SpringBootProperties_4_0` | 迁移重命名的配置属性 |

运行前查看 recipe 目录：部分 Boot 4 recipe 位于 Community Edition，需要额外仓库访问权限；不同 `rewrite-spring` 版本包含的 recipe 也会变化。

**始终审查生成的 diff。**OpenRewrite 会改 starter 名称和 import，但不会发现 `ObjectMapper` bean 类型变化导致自定义配置不再生效，也不会知道遗漏 JDBC starter 使 Batch 作业失去可重启能力。

### 推荐迁移顺序

1. 先升级到 Java 17+ 上的 3.5.x，并确保构建和测试通过。
2. 运行 Boot 4 总体迁移 recipe，让项目至少能编译。
3. 手工调整 Security 7：DSL 移除需要 recipe 无法提供的判断。
4. 添加 `spring-boot-starter-flyway`，确认迁移实际执行。
5. 有 Batch 作业时添加 `spring-boot-starter-batch-jdbc`，并迁移序列名称。
6. 将 `@MockBean` 替换为 `@MockitoBean`；如果 `@SpringBootTest` 依赖旧行为则添加 `@AutoConfigureMockMvc`。
7. 将 Testcontainers 升级为 2.x 坐标和类名。
8. 以上完成后，再采用 API 版本管理、核心韧性 API、`@ImportHttpServices` 等新功能。

## 常见误区

- 给 Boot BOM 已管理的依赖（Hibernate、Jackson、Testcontainers、JUnit）手工固定版本：删掉覆盖，让 BOM 管理。
- 认为改名的 starter 已被移除：它们在 4.x 中已弃用但仍可解析。
- 添加 `spring-boot-starter-data-jpa` 后就认为 Flyway 会运行：Boot 4 需要显式加入 `spring-boot-starter-flyway`。
- 混用生态版本（例如 Boot 4 上用 Spring AI 1.x，或 Modulith 1.x）：检查上方配对表。
- 从 3.4 直接升到 4.0：先经过 3.5，以便分别处理破坏性变化。
- 在 `spring-boot-starter-webmvc-test` 旁边再添加 `spring-boot-starter-test`：Boot 4 的 `-test` starter 已自包含。
- 没检查构建文件就给 3.5.x 项目生成 Boot 4 代码：`version` 映射、`@ImportHttpServices` 和核心 `@Retryable` 在 3.5.x 中不存在。
- 为了让迁移通过而添加 `spring-boot-starter-classic`：它是过渡工具，不是最终目标。

## 相关文档

[maven.zh-CN.md](maven.zh-CN.md) · [gradle.zh-CN.md](gradle.zh-CN.md) · [json-and-jackson.zh-CN.md](json-and-jackson.zh-CN.md) · [security-fundamentals.zh-CN.md](security-fundamentals.zh-CN.md) · [testing-strategy.zh-CN.md](testing-strategy.zh-CN.md)
