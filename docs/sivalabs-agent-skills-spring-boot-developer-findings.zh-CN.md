# SivaLabs Agent Skills 对 `spring-boot-developer` 的借鉴研究

## 研究结论

SivaLabs 仓库最值得借鉴的不是某一段 Spring Boot 代码，而是三件事：

1. 用一个很短的 `SKILL.md` 作为入口，只把当前任务需要的参考文件加载进来。
2. 把 Spring Boot 指南拆成“一个主题一个文件”，并在入口中用任务到文件的映射组织导航。
3. 把测试层级、真实基础设施、测试数据和版本差异写成明确规则，而不是只给一组代码片段。

当前仓库已经在这些方向上做得更完整：`spring-boot-developer` 已有版本路由、Maven/Gradle、生成器、环境检查、工作坊、前端、部署、安全、持久化和可观测性覆盖。因此不应把 SivaLabs 的技能目录或 Spring Boot 参考文件整体复制过来。最合理的做法是吸收其入口索引和少数边界清晰的规则，并继续使用当前仓库已有的版本感知、风险导向和生成器模式。

本文对目标仓库固定在提交 [`94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/tree/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef)；当前仓库的对照以工作区中的 [`spring-boot-developer/SKILL.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/SKILL.md) 及其引用文件为准。

## 1. 目录与技能组织方式

### 1.1 目标仓库的实际布局

目标仓库根目录很小，职责清楚：

```text
/
├── README.md
├── AGENTS.md
├── install.sh
├── .claude-plugin/
│   ├── marketplace.json
│   └── plugin.json
├── agents/
│   └── code-reviewer.md
└── skills/
    ├── apply-renovate-prs/
    ├── java-code-review/
    ├── jspecify/
    │   ├── references/
    │   └── scripts/verify_nullmarked.py
    ├── pair-programming/
    ├── progen/
    │   └── agents/openai.yaml
    ├── spring-boot/
    │   ├── SKILL.md
    │   └── references/        # 19 个主题文件
    └── spring-modulith-verifier/
```

这里的事实来自根目录的 [`README.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/README.md)、[`AGENTS.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/AGENTS.md) 和仓库中的目录树。README 只列出几个可安装的 skill；每个 skill 的入口都是同名目录中的 `SKILL.md`，较大的 skill 再拥有 `references/`、`scripts/` 或 agent 元数据。

隐藏的 [`.claude-plugin/marketplace.json`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/.claude-plugin/marketplace.json) 和 [`plugin.json`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/.claude-plugin/plugin.json) 把整个仓库声明为 Claude plugin，并挂载 `skills/` 与 `agents/code-reviewer.md`。这说明目标仓库同时服务“技能直接安装”和“插件整体安装”两种入口。

### 1.2 入口文件的组织方式

Spring Boot 的 [`SKILL.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/SKILL.md) 只有 YAML front matter、使用场景和主题索引。它没有把所有规则塞进入口文件，而是把 Maven、包结构、JPA、Service、REST、Modulith、Thymeleaf、测试、ArchUnit、Docker Compose 和 Taskfile 分别链接到聚焦文档。

它还有两个具体的导航设计值得保留：

- 开头写明“只在处理该领域时阅读链接的 reference”，降低 Agent 首次读取成本。
- 测试部分使用一张“测试对象 -> 测试层级 -> 参考文件”的表，把不同测试需求直接路由到文档。见 [`skills/spring-boot/SKILL.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/SKILL.md)。

### 1.3 安装与工具适配

根目录 [`install.sh`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/install.sh) 支持 `claude`、`codex`、`gemini` 和 `cursor`，并区分项目级目录与用户级目录；README 也给出 `npx skills add` 和手工复制路径。根目录 [`AGENTS.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/AGENTS.md) 描述了同一安装模型。

但这只是分发策略，不是 Spring Boot 技能内容。当前仓库已有自己的 [`README.md`](https://github.com/zhijunio/skills/blob/main/README.md)、Maven SkillsJars 打包和 `skills` CLI 使用说明；不需要把另一套复制脚本并入 `spring-boot-developer`。

### 1.4 脚本和 agent 元数据的实际情况

目标仓库并没有 Spring Boot 专用脚本。`spring-boot` 目录只有入口和 references；仓库里的脚本主要是 [`jspecify/scripts/verify_nullmarked.py`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/jspecify/scripts/verify_nullmarked.py) 和根目录安装脚本。`agents/openai.yaml` 只出现在 Progen 和 Renovate skill 下，例如 [`skills/progen/agents/openai.yaml`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/progen/agents/openai.yaml)。

项目生成也没有塞进 `spring-boot` skill，而是由独立的 [`skills/progen/SKILL.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/progen/SKILL.md) 调用外部 `progen` CLI，使用 `.progen.json` 描述 Maven/Gradle、JPA/JdbcClient/jOOQ、数据库、迁移、前端、Compose、CI 和 Kubernetes 选项。这是“核心开发指南”和“项目生成器”分离的具体例子；当前仓库则把生成脚本直接放在 `spring-boot-developer/scripts/` 下。

这和当前仓库形成明显差异：当前 `spring-boot-developer` 已有 [`scripts/create-project.mjs`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/scripts/create-project.mjs)、[`scripts/check-environment.mjs`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/scripts/check-environment.mjs)、smoke test、版本同步和前端规范化脚本，以及大量 assets。SivaLabs 的 Spring Boot skill 不提供可执行的项目生成或环境验证机制。

## 2. Spring Boot skill 的具体内容

### 2.1 入口索引覆盖的主题

入口 [`skills/spring-boot/SKILL.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/SKILL.md) 明确要求使用以下参考：

| 主题 | 目标仓库的文件 | 关键内容 |
| --- | --- | --- |
| Maven | [`spring-boot-maven-config.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-boot-maven-config.md) | Spotless、JaCoCo、Git commit id、Boot build-info 和 Buildpacks 镜像配置 |
| 包结构 | [`code-organization.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/code-organization.md) | 按业务模块组织、模块 API facade、包可见的实体/Repository/Mapper |
| JPA | [`spring-data-jpa.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-data-jpa.md) | 审计、值对象主键、TSID、`@Version`、聚合根 Repository、查询投影 |
| Service | [`spring-service-layer.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-service-layer.md) | Unit of Work、读写事务、`Cmd`/`Query`/`Result`、领域事件发布 |
| REST | [`spring-webmvc-rest-api.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-webmvc-rest-api.md) | 值对象转换、请求校验、服务委托、`ProblemDetail` 全局异常处理 |
| Modulith | [`spring-modulith.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-modulith.md) | `ApplicationModules.verify()`、Named Interface、模块 API、事件通信 |
| Thymeleaf | [`thymeleaf.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/thymeleaf.md) | Starter、Layout Dialect、Spring Security 支持 |
| 测试策略 | [`testing-strategy.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testing-strategy.md) | 单元、切片、集成、端到端互补；上下文缓存；容器成本 |
| 测试细节 | [`testing-unit-mocking.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testing-unit-mocking.md)、[`testing-slices-web.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testing-slices-web.md)、[`testing-slices-persistence.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testing-slices-persistence.md) | 无 Spring 上下文单测、Web/JSON/REST client 切片、真实数据库持久化切片 |
| 集成和 E2E | [`testing-integration.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testing-integration.md)、[`spring-boot-rest-api-testing.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-boot-rest-api-testing.md) | 窄上下文 `@SpringBootTest`、真实端口、`RestTestClient`、WireMock |
| Testcontainers | [`testcontainers-wiring.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testcontainers-wiring.md) | `@ServiceConnection` 优先、动态属性边界、动态端口、无 Docker 行为 |
| 测试数据 | [`test-data-setup.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/test-data-setup.md) | SQL fixture 优先，程序化数据通过 Service 或 Module API，避免测试直接依赖 Repository/JdbcTemplate |
| 架构测试 | [`archunit.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/archunit.md) | 用 Taikai 预置规则检查命名、依赖、注入、Controller/Service/Repository 关系 |
| 本地依赖 | [`spring-boot-docker-compose.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-boot-docker-compose.md)、[`taskfile.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/taskfile.md) | Boot Docker Compose 支持、PostgreSQL/Redis/LGTM/Mailpit、Taskfile 常用任务 |

### 2.2 最具体且可复用的规则

**包和模块。** SivaLabs 给出按业务模块组织的目录：每个模块内有 `domain`、`api` 和 `{Module}API.java` facade；Entity、Repository、Mapper 应为包可见，跨模块调用经过 facade。这个规则在 [`code-organization.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/code-organization.md) 和 [`spring-modulith.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-modulith.md) 中重复出现。

**JPA。** 文档不是只讲注解，而是给了一整套固定偏好：`BaseEntity` 的 created/updated 审计字段、值对象 `@EmbeddedId`、protected 无参构造、显式表名和列名、`EnumType.STRING`、值对象 `@Embedded`、`@Version`、只为聚合根建 Repository、JPQL/投影用于读取。具体规则和示例见 [`spring-data-jpa.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-data-jpa.md)。

**Service 和 Web。** Service 文档规定写方法使用 `@Transactional`、读方法使用 `@Transactional(readOnly = true)`，输入输出分别用 `XCmd`、`XQuery`、`XResult`，领域事件通过一个 `SpringEventPublisher` 发布。Web 文档要求 PathVariable/RequestParam 用 Converter 转换成值对象，请求体用带值对象的 Request object，控制器只做校验和委托，异常通过 `@RestControllerAdvice` 统一转成 RFC 7807 `ProblemDetail`。见 [`spring-service-layer.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-service-layer.md) 和 [`spring-webmvc-rest-api.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-webmvc-rest-api.md)。

**测试层级。** 其入口表把单元、切片、聚焦集成、真实端口 E2E 和容器 wiring 分开；策略文档明确说这些层级是互补关系，而不是“全是单测”或“全是全上下文测试”。它还强调上下文缓存、共享容器、少用 `@DirtiesContext`、至少一个启动 smoke test 和至少一个 request-level E2E。见 [`testing-strategy.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testing-strategy.md)。

**真实数据库和容器 wiring。** 目标仓库明确反对用 H2 代替生产数据库，持久化切片使用真实数据库 Testcontainers。其 Testcontainers 文档把规则拆成三类：容器能提供 Spring 的 ConnectionDetails 时用 `@ServiceConnection`；动态端口或外部 HTTP URL 才用动态属性；静态测试设置留在 profile/config 文件中。它还拒绝固定宿主机端口、未打标签的 `latest` 镜像和把静态值塞进 `@DynamicPropertySource`。见 [`testing-slices-persistence.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testing-slices-persistence.md)、[`testcontainers-wiring.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testcontainers-wiring.md)。

**可执行架构约束。** ArchUnit 参考不是抽象建议，而是一个 `ArchUnitTests` 示例：禁止废弃 API、循环 import、字段注入、被禁用的测试，要求 Controller/Service/Repository 命名和依赖方向，并要求应用类位于 base package。见 [`archunit.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/archunit.md)。

**构建和本地开发。** Maven 参考建议 Spotless + Palantir Java Format、JaCoCo、git commit id、Boot build-info 和 Buildpacks 镜像配置；它另给出跨平台 Taskfile 的 `build`、`start`、`stop`、`restart` 任务。见 [`spring-boot-maven-config.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-boot-maven-config.md) 和 [`taskfile.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/taskfile.md)。

## 3. 与当前 `spring-boot-developer` 的重叠和差异

### 3.1 主要重叠

| 领域 | 当前仓库已经具备的内容 | 判断 |
| --- | --- | --- |
| 入口导航 | 当前 [`SKILL.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/SKILL.md) 有核心规则和按架构、Web、持久化、测试、集成、观测、前端、部署分组的 reference map | 目标仓库的“短入口 + 聚焦 reference”模式已被采用，不应重复引入第二层索引 |
| 版本管理 | [`versions.json`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/versions.json)、[`spring-boot-migration.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/spring-boot-migration.md) 和入口中的 Boot 4/3.5 路由 | 当前比目标仓库的 Boot 4 主导写法更适合维护多版本技能 |
| 包与架构 | [`code-organization.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/code-organization.md)、[`domain-modelling.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/domain-modelling.md)、[`hexagonal-architecture.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/hexagonal-architecture.md)、[`spring-modulith.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/spring-modulith.md) | 已覆盖 feature-first、领域建模、模块边界、Hexagonal 和 Modulith；目标仓库的模块 facade 只是一个可比较的偏好，不是缺失能力 |
| JPA 和事务 | [`spring-data-jpa.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/persistence/spring-data-jpa.md)、[`transactions.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/persistence/transactions.md)、auditing、Flyway、并发和缓存 | 当前在实体生命周期、事务代理、迁移、并发、分页、N+1 和 UUIDv7 上更细；目标仓库没有足够理由替代这些规则 |
| REST 和错误处理 | [`rest-controllers.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/web/rest-controllers.md)、[`validation.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/web/validation.md)、[`error-handling.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/web/error-handling.md)、JSON/Jackson | 两边都主张 DTO、校验、边界层处理和 Problem Details；当前覆盖 API 版本、幂等、分页和浏览器行为更多 |
| 测试 | [`testing-strategy.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/testing/testing-strategy.md)、Web/持久化切片、集成、Testcontainers、unit testing | 当前已经包含真实数据库、H2 陷阱、`@ServiceConnection`、上下文缓存、`RestTestClient`、WireMock、测试数据和 native image 等目标仓库关心的绝大部分内容 |
| 架构验证 | [`architecture/archunit.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/archunit.md) 和 [`references/checklist.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/checklist.md) | 当前已有自定义 ArchUnit 规则和检查清单；目标仓库的 Taikai 只是另一种实现方式 |
| 工具和交付 | [`scripts/`](https://github.com/zhijunio/skills/tree/main/spring-boot-developer/scripts)、[`assets/`](https://github.com/zhijunio/skills/tree/main/spring-boot-developer/assets)、Maven/Gradle/mise、Docker/Kubernetes/GraalVM/Azure 参考 | 当前提供实际生成和验证工具，目标仓库只有静态建议 |

### 3.2 关键差异

**当前仓库更宽、更版本感知。** 当前入口把 Boot 4.x 作为新项目默认，同时保留 3.5.x 现有项目支持，并要求先读项目的 Boot、Java 和构建工具版本。目标 Spring Boot skill 的描述和若干示例直接以 Boot 4.x 为中心，虽在测试文档中补充了 3.5.x/4.x 差异，但入口本身没有当前仓库这样强的版本路由。见目标 [`SKILL.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/SKILL.md) 与当前 [`SKILL.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/SKILL.md)。

**目标仓库声明了 Security，但没有对应的专门 reference。** `spring-boot/SKILL.md` 的 front matter 将 Spring Security 列为使用场景，但其 19 个 Spring Boot reference 中没有 Security 文件；相关安全要求只零散出现在包结构、Web 或 code-review agent 示例中。当前仓库有独立的 security references，并且还有 `spring-sso-developer` skill，因此不应把目标仓库的主题声明当成覆盖范围证明。来源：目标 [`skills/spring-boot/SKILL.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/SKILL.md)、当前 [`security-fundamentals.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/security/security-fundamentals.md)。

**包结构的默认值不同。** 目标仓库把 domain-driven、module-first 结构写成推荐模板；当前仓库的 [`code-organization.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/code-organization.md) 采用按 feature 组织的默认值，并设置从平面 feature 到模块化/Hexagonal 的升级阶梯。这更能避免 CRUD 应用一开始就引入 `UsersAPI` facade、多个子包和模块治理成本。

**ID 和 JPA 的意见不同。** 目标仓库推荐 TSID + 值对象 `@EmbeddedId`，并为所有实体示例提供 `BaseEntity`；当前仓库明确以 Long 为默认 ID，只有项目选 UUID 时才使用 UUIDv7，并将审计、版本、集合、关联、分页和数据库类型分别说明。见目标 [`spring-data-jpa.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-data-jpa.md) 与当前 [`spring-data-jpa.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/persistence/spring-data-jpa.md)。

**测试文档的粒度相似，但当前内容更丰富。** 目标仓库把“测试数据”单独成文件，并用显式的 `@ServiceConnection`/动态属性三分法组织 wiring；当前仓库把同一类内容分布在 testing strategy、integration、persistence slice 和 Testcontainers 文件中，且加入 native image、MyBatis-Plus、WireMock、迁移和并发等内容。目标仓库最可能提供的是信息架构上的整理，而不是新的测试技术。

**目标仓库包含一个具体的 Taikai 选择，当前仓库保持 ArchUnit 自有规则。** 当前 [`architecture/archunit.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/archunit.md) 已按当前仓库的 feature/module/adapter 约定描述规则。直接换成 Taikai 会引入额外依赖、版本和规则语义，不是简单的文档增强。

**本地开发工具不同。** 目标仓库将 go-task 作为可选的跨平台任务入口，并给出 Docker Compose support 的最小例子；当前仓库采用 Maven/Gradle、mise、生成器 assets 和多个 Compose 变体。目标仓库不能证明 Taskfile 比当前工具链更适合本仓库。

## 4. 可借鉴项、不可直接照搬项和优先级

### P0：值得优先吸收

1. **保留并强化“任务 -> reference”路由表。** 目标仓库在测试部分把“测试什么”直接映射到测试层级和文件，降低 Agent 读错指南的概率。当前仓库已经有主题索引，可以在需要时把这种三列映射扩展到 Web、JPA、集成和部署，而不是再建一套目录。来源：目标 [`skills/spring-boot/SKILL.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/SKILL.md)，当前 [`spring-boot-developer/SKILL.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/SKILL.md)。

2. **把测试数据 setup 作为独立的可检索主题。** 目标仓库明确要求 SQL fixture 优先，程序化 setup 通过公开的 Service/Module API，避免测试直接抓 Repository 或手写事务。这条边界和“测试行为而不是内部实现”一致，值得在当前集成测试指南中保持一个独立小节或 reference。来源：目标 [`test-data-setup.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/test-data-setup.md)，当前 [`testing-integration.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/testing/testing-integration.md)。

3. **继续把容器 wiring 写成选择规则，而非 API 清单。** “`@ServiceConnection` 处理 Boot 能识别的 connection details；只有动态外部 URL/端口才注册动态属性；不固定宿主机端口；CI 不应静默跳过容器测试”是一组很适合 Agent 执行的规则。当前已有大部分内容，后续只需检查它们是否在一个入口可发现。来源：目标 [`testcontainers-wiring.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testcontainers-wiring.md)，当前 [`testcontainers.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/testing/testcontainers.md)。

4. **将“切片测试为主、全上下文测试少量使用”的决策说明放在入口附近。** 目标仓库的测试表和 testing strategy 形成了一个清晰的选择流程；当前已有同样理念，但可继续保持这一短句级别的导航规则，避免 Agent 直接生成大量 `@SpringBootTest`。来源：目标 [`testing-strategy.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testing-strategy.md)，当前 [`testing-strategy.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/testing/testing-strategy.md)。

### P1：按当前仓库形态选择性吸收

1. **补充“模块公开 API facade”作为 Spring Modulith 场景的可选模式。** 目标仓库的 `{Module}API.java` 规则很具体，能把包可见实现和跨模块依赖边界落到代码上；但它不应成为所有 Spring Boot feature 的默认文件。当前 Modulith 文档应只在模块真的需要跨边界调用时采用，并与 Named Interface、事件和 `ApplicationModules.verify()` 一起说明。来源：目标 [`code-organization.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/code-organization.md)、[`spring-modulith.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-modulith.md)，当前 [`spring-modulith.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/spring-modulith.md)。

2. **用一个可运行的 ArchUnit 示例展示规则如何落地，但不要默认引入 Taikai。** 目标仓库的示例覆盖了字段注入、依赖方向、命名和包位置，适合作为当前架构测试文档的示例审查清单；依赖仍应由当前仓库的 ArchUnit 规则和目标项目的实际架构决定。来源：目标 [`archunit.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/archunit.md)，当前 [`archunit.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/archunit.md)。

3. **将 Maven 的质量插件职责写成一张决策表。** Spotless、覆盖率、构建信息和 Git provenance 都是当前仓库已有或相关的能力；可借鉴目标文档把每个插件的用途和绑定阶段集中说明的写法。不要照搬版本号和覆盖率阈值。来源：目标 [`spring-boot-maven-config.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-boot-maven-config.md)，当前 [`maven.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/tooling/maven.md)。

4. **如果以后需要跨平台任务入口，再评估 Taskfile。** 目标 Taskfile 有 build、Buildpacks、Compose start/stop/restart 和 Windows/macOS/Linux 差异处理，作为参考很完整；但当前已经有 mise、Maven wrapper、生成器和 Compose assets。只有用户需求表明需要统一 `task` 命令时，才应增加它。来源：目标 [`taskfile.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/taskfile.md)，当前 [`local-development.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/tooling/local-development.md) 和 [`mise.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/tooling/mise.md)。

### P2：暂不引入或仅作为后续拆分候选

1. **独立的 Spring Modulith verifier skill。** 目标仓库把 `spring-modulith-verifier` 作为单独 skill，而不是把所有验证规则塞进 Spring Boot skill；如果当前仓库未来要提供“只验证模块边界”的工作流，可以借鉴这种拆分。当前仅为了比较目录组织，不建议现在新增目录或改变用户入口。来源：目标根 [`README.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/README.md) 和 [`skills/spring-modulith-verifier/SKILL.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-modulith-verifier/SKILL.md)。

2. **复制安装脚本或 `openai.yaml`。** 目标仓库的安装脚本解决的是它的多 Agent 分发模型；当前仓库已有自己的发布和打包方式。元数据只应在当前仓库真的需要显式 agent UI 配置时增加。来源：目标 [`install.sh`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/install.sh)、[`skills/progen/agents/openai.yaml`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/progen/agents/openai.yaml)，当前 [`README.md`](https://github.com/zhijunio/skills/blob/main/README.md)。

## 5. 不可直接照搬的内容和原因

| 目标仓库内容 | 不应直接照搬的原因 | 当前应采用的处理 |
| --- | --- | --- |
| “Spring Boot 4.x”作为统一前提 | 当前技能同时服务 Boot 4.x 新项目和 Boot 3.5.x 现有项目；测试 starter、Jackson、Testcontainers 坐标和测试 autoconfigure 包可能随版本变化 | 保留当前版本探测和迁移路由；技术规则必须标出 4.x、3.5.x 或共同部分。来源：目标 [`spring-boot/SKILL.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/SKILL.md)，当前 [`spring-boot-migration.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/spring-boot-migration.md) |
| TSID + `@EmbeddedId` + `BaseEntity` | 这是完整的领域建模偏好，不是所有应用的兼容性要求；它会影响数据库类型、迁移、查询和外部 ID 合约 | 维持当前 Long 默认和显式选择 UUIDv7 的规则；只在项目已选值对象主键时吸收相关实践。来源：目标 [`spring-data-jpa.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-data-jpa.md)，当前 [`spring-data-jpa.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/persistence/spring-data-jpa.md) |
| Taikai 预置规则 | 引入第三方规则库会增加依赖和升级面；其默认规则未必适合当前仓库 feature-first、MyBatis-Plus 和多种部署形态 | 用当前 ArchUnit 规则表达真正的模块边界；仅在目标项目已有 Taikai 时提供迁移建议。来源：目标 [`archunit.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/archunit.md) |
| 固定 `jacoco.minimum.coverage=80%` | 数字阈值是项目策略，不是 Spring Boot 最佳实践；目标仓库自己的 [`agents/code-reviewer.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/agents/code-reviewer.md) 又写着项目要求 70%，说明不能把示例数字当成统一规范 | 由当前项目风险、历史基线和新代码门禁决定阈值；测试质量仍以行为断言为主 |
| 目标测试示例中的 `RestTestClient` wiring | 目标 [`testing-integration.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testing-integration.md) 的 smoke 示例直接注入 `RestTestClient`，而专门的 REST 测试文档才展示 `@AutoConfigureRestTestClient`；示例之间存在配置不一致 | 复制代码前按当前 Boot 版本运行 smoke test，并以当前 integration/testcontainers 文档为准 |
| 目标示例中的 Testcontainers 泛型形式 | 目标 wiring 和 integration 示例使用 `PostgreSQLContainer<?>`；当前 Boot 4/Testcontainers 2 参考已针对新坐标和非泛型类形态做了版本说明 | 复制概念，不复制 import、泛型和依赖坐标；先读取当前版本 guide。来源：目标 [`testcontainers-wiring.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/testcontainers-wiring.md)，当前 [`testcontainers.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/testing/testcontainers.md) |
| `Taskfile.yml` 作为默认任务入口 | 当前仓库已经有 `mvnw`、Gradle、mise、Node 脚本和生成器；再加 go-task 会增加安装前置条件 | 仅借鉴任务命名和跨平台变量设计，不新增工具依赖。来源：目标 [`taskfile.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/taskfile.md) |
| 所有应用都采用模块 API facade、`shared` OPEN 模块 | `OPEN` 会放宽模块边界；CRUD 应用或简单 feature 不一定需要 facade，过早模块化会增加读者和实现成本 | 继续采用当前的 feature-first 升级阶梯；只有跨模块依赖和 Modulith 验证需要时才公开 facade/Named Interface。来源：目标 [`spring-modulith.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-modulith.md)，当前 [`code-organization.md`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/references/architecture/code-organization.md) |
| 直接复制目标文档中的依赖、镜像和插件版本 | 这些值随 Boot、Spring Modulith、Testcontainers、Docker 镜像和插件发布而变化；目标文件中的版本是该提交的示例值 | 继续以当前 [`versions.json`](https://github.com/zhijunio/skills/blob/main/spring-boot-developer/versions.json)、版本同步脚本和官方兼容性文档为准 |

## 6. 建议的落地顺序

1. **先做信息架构核对。** 确认 `spring-boot-developer/SKILL.md` 的每个任务入口都能指向一个主 reference，并在测试入口保留目标仓库那样的“测试对象 -> 层级 -> 文件”映射。无需新增目录。
2. **再检查测试数据边界。** 在现有集成测试或 Testcontainers 文档中确保 fixture、公开 Service/模块 API、真实数据库和 CI 不静默跳过的规则可被单独检索。可以参考目标 [`test-data-setup.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/test-data-setup.md)。
3. **然后审查 Modulith 的 facade 规则。** 仅当当前文档缺少“模块外部只能依赖公开 API/Named Interface”的明确例子时，补一段带 `ApplicationModules.verify()` 的示例；不要改变普通 feature 的默认结构。参考目标 [`spring-modulith.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/94f8d9fa21ba659e85cbb8a9b33f6e84883de5ef/skills/spring-boot/references/spring-modulith.md)。
4. **最后才评估工具。** Taikai、Taskfile、额外安装脚本都不是本次研究显示的必需项，只有出现明确维护问题或用户工作流需求时再单独评估。

## 最终判断

SivaLabs 的 Spring Boot skill 适合借鉴为“聚焦 reference 的入口设计 + 明确测试决策规则 + 可执行架构示例”的小型模板。它不适合成为当前 `spring-boot-developer` 的技术基线：当前技能的版本兼容范围、工具脚本、生产部署、前端、安全和数据库风险覆盖更广，且已有相同或更细的测试与 JPA 规则。

因此优先级是：**P0 吸收信息架构和测试边界表达；P1 选择性补强 Modulith facade、ArchUnit 示例和 Maven 职责说明；P2 暂不引入 Taskfile、Taikai、安装脚本或独立 verifier skill。**
