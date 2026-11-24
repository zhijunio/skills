# `spring-boot-4` 扩成完整项目技能：主题调研

用户要求把 [`spring-boot-4`](../spring-boot-4/SKILL.md) 从「MVC / JPA / 应用授权 / 包边界」扩成完整 Spring Boot **项目**开发技能，并明确：不能只补 REST、OpenAPI、Docker，凡是 Spring Boot 项目会碰到的主题都要先纳入考虑。本文回答「该考虑哪些主题、归谁拥有」。选定的产品目标见 [重写方案](spring-boot-4-rewrite-plan.zh-CN.md)：按模板重写 `spring-boot-4`，退役另外两份技能，不搬家。

## 结论

完整项目技能要覆盖的，是一个 Boot 4 服务从生成到可交付的**应用层**工作，加上现在 SSO 技能承担的协议面。REST、OpenAPI、Docker 只是其中三项。现有六份短参考在自己范围内仍然够用，扩范围靠新增分组参考，不把 `web-mvc.md` 写成全栈手册。

Java、PostgreSQL、Spring Cloud、JVM 性能、CI/CD 仍是兄弟技能。SSO 不再是兄弟技能，协议参考按 [重写方案](spring-boot-4-rewrite-plan.zh-CN.md) 重写进 `spring-boot-4`。实施以该方案为准，不搬家。

## 这次调研回答什么

| 文档 | 问题 | 结论类型 |
| --- | --- | --- |
| [实现方案](spring-boot-4-skill-implementation-plan.zh-CN.md) | 首版做什么 | 首版只做 MVC/JPA/授权/架构/测试；明确排除版本升级、生产诊断、部署编排 |
| [跨仓库对照](spring-boot-4-cross-repository-review.zh-CN.md) | 现有六份参考还缺不缺通用规则 | 暂无；OpenAPI 同步、CORS、迁移修订已写进现有章节 |
| [SivaLabs 对照](sivalabs-agent-skills-spring-boot-developer-findings.zh-CN.md) | 能否整仓吸收外部 Spring Boot skill | 吸收短入口索引，不复制目录或默认技术偏好 |
| **本文** | 若范围变成「完整 Spring Boot 项目」，还要**考虑**哪些主题 | 主题目录 + 所有权边界 |

对照过的一手来源：本仓库各技能入口与 `spring-boot-developer` 参考地图；SivaLabs、spring-tools、Dancan254、rrezartprebreza、rajeshanji8、rynr、awesome-copilot、developer-kit、Amplicode、jdubois、a-pavithraa 等外部 `SKILL.md`；[Spring Boot 官方参考目录](https://docs.spring.io/spring-boot/reference/index.html) 的 Web / Data / Messaging / IO / Actuator / Packaging / Features / Dev Services 章。抓取失败项见文末。

## 官方产品面：一个 Boot 项目会碰到什么

以官方章节为「考虑清单」，避免只按用户点名的三项扫：

| 官方章 | 项目是否几乎总会碰到 | 现有 `spring-boot-4` |
| --- | --- | --- |
| Core：配置、profiles、日志、JSON、任务执行 | 是 | 无独立配置/日志参考；JSON 只在测试切片出现 |
| [Web](https://docs.spring.io/spring-boot/reference/web/index.html)：MVC、错误、CORS、静态资源 | MVC/错误/CORS 是；静态资源看是否托管前端 | 有 web-mvc；无静态资源/WebFlux |
| [Data](https://docs.spring.io/spring-boot/reference/data/index.html)：SQL/JPA | 有关系库时是；NoSQL 否 | 有 data-jpa；无 JDBC/R2DBC/NoSQL |
| [IO](https://docs.spring.io/spring-boot/reference/io/index.html)：校验、缓存、远程 REST、调度、邮件、Batch | 校验是；其余按任务 | 校验在 web-mvc；其余无 |
| [Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) | 交付/运维几乎总会碰到 | bootstrap 只在「已有探针」时验收路径 |
| [Dev Services](https://docs.spring.io/spring-boot/reference/features/dev-services.html) / 容器 | 本地依赖与镜像是 | PostgreSQL Compose 附在 bootstrap；无应用镜像 |
| [Messaging](https://docs.spring.io/spring-boot/reference/messaging/index.html) | 否，按任务 | 无 |
| [Packaging](https://docs.spring.io/spring-boot/reference/packaging/index.html)：jar、容器、native、AOT、CRaC | jar/容器是；native 否 | 无独立打包参考 |
| Spring Security（独立项目） | 有保护接口时是 | 有应用授权；无协议登录/JWT 签发/MFA |

## 本仓库技能地图

```text
语言     java-developer
数据库   postgresql-developer          ── schema / EXPLAIN / 锁 / RLS
性能     java-performance-developer    ── JVM / GC / JFR；框架配置交回 Boot
流水线   ci-cd-developer               ── commit→制品；停在已部署制品
云组件   spring-cloud-developer        ── Gateway / Config / Discovery / Stream
应用     spring-boot-4                 ── 目标：Boot 应用 + SSO + 前端集成 + 容器
退役     spring-boot-developer、spring-sso-developer
```

兄弟技能自己写的边界：

- [`java-developer/SKILL.md`](../java-developer/SKILL.md)：「language and standard-library guidance」；不做慢应用诊断。
- [`postgresql-developer/SKILL.md`](../postgresql-developer/SKILL.md)：schema、SQL、索引、`EXPLAIN`、锁感知迁移、RLS。Boot 技能只应写 JPA/Flyway **应用用法**，不写查询计划。
- [`spring-cloud-developer/SKILL.md`](../spring-cloud-developer/SKILL.md)：「application-level Spring Boot structure」归 Boot 技能；Cloud 组件归本技能。
- [`spring-sso-developer/SKILL.md`](../spring-sso-developer/SKILL.md)（待退役）：外部 IdP、OAuth2/OIDC client **或 resource server**、自建 AS、SAML。重写后 `security.md` 仍只做应用授权，协议正文进 `references/security/`。
- [`java-performance-developer/SKILL.md`](../java-performance-developer/SKILL.md)：先证明瓶颈；框架配置交 Boot，查询交 PostgreSQL。
- [`ci-cd-developer/SKILL.md`](../ci-cd-developer/SKILL.md)：「stops at the deployed artifact」。Boot 技能最多写 Dockerfile、Compose、健康探针契约，不写流水线/SBOM/推广链。

[`spring-boot-developer`](../spring-boot-developer/SKILL.md) 已经按任务路由了约 75 份参考。重写时按 [重写方案](spring-boot-4-rewrite-plan.zh-CN.md) 的模板新写，不按分组搬家。短参考继续当 Boot 4 默认路径。不要把 75 份正文贴进 `SKILL.md`。

## 主题全表

判定：

- **已有**：`spring-boot-4` 已有对等专章或明确规则。
- **应新增**：外部技能 ≥3 次出现，且典型 Boot 服务会碰到；当前无独立 reference。
- **按任务**：项目会碰到，但不是每个服务都有；有独立参考或入口指针即可。SSO、前端、MyBatis-Plus、消息等属于此类，退役后由 `spring-boot-4` 自己承载。
- **兄弟技能**：Cloud / Java / PostgreSQL / 性能 / CI/CD 仍外置。
- **专项**：点名才做；默认不进项目技能正文。

### 已有（扩范围时保留，不重写为新手册）

| 主题 | 文件 | 说明 |
| --- | --- | --- |
| 生成 / Initializr / 启动验收 | [`bootstrap.md`](../spring-boot-4/references/bootstrap.md) | 生成器 + PostgreSQL Compose；缺运行时配置模型和镜像 |
| 功能包 / 依赖方向 / ArchUnit | [`architecture.md`](../spring-boot-4/references/architecture.md) | Modulith、六边形只在项目已采用时跟随 |
| 入站 MVC / 校验 / ProblemDetail / 分页 | [`web-mvc.md`](../spring-boot-4/references/web-mvc.md) | 已有「项目维护 OpenAPI 时同步」一句，无规范所有权 |
| JPA / 事务 / 并发 / Flyway 与旧数据 | [`data-jpa.md`](../spring-boot-4/references/data-jpa.md) | 迁移纪律嵌在 JPA 里 |
| 应用授权 / CSRF / CORS | [`security.md`](../spring-boot-4/references/security.md) | 不覆盖协议登录 |
| 测试层级 / Testcontainers / 升级验证 | [`testing.md`](../spring-boot-4/references/testing.md) | 订单样例已可跑 |

跨仓库对照里建议过的事务测试边界、已应用迁移修订、HTTP 契约可达性，复核时已吸收进上述文件。本文不再把它们当缺口。

### 应新增：项目级、当前没有独立参考

这八项在外部技能里反复出现，官方产品面也对应独立章，且一个要交付的 Boot 服务几乎都会碰到。用户点名的 REST / OpenAPI / Docker 落在第 1、4、3 项；其余五项同等优先级。

| 候选参考 | 代理要决定什么 | 外部证据（入口，非二手摘要） | 本仓库已有正文可裁剪自 |
| --- | --- | --- | --- |
| **配置** | profiles、`@ConfigurationProperties`、环境变量、密钥不进仓库、虚拟线程开关 | [rynr `spring-boot`](https://github.com/rynr/spring-skills/blob/main/skills/spring-boot/SKILL.md) 把配置当成 Boot 技能主体；rajeshanji8 `configuration-properties.md`；awesome-copilot `configuration.md`；j-morgan6 `configuration-and-profiles` | [`configuration.md`](../spring-boot-developer/references/configuration/configuration.md) |
| **可观测性** | Actuator 暴露面、健康/就绪探针、结构化日志、指标；追踪仅在项目已有时跟随 | awesome-copilot [`spring-observability`](https://github.com/github/awesome-copilot/blob/main/skills/spring-observability/SKILL.md)；developer-kit [`spring-boot-actuator`](https://github.com/giuseppe-trisciuoglio/developer-kit/blob/main/plugins/developer-kit-java/skills/spring-boot-actuator/SKILL.md)；rrezartprebreza `production-observability`；官方 [Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) | [`observability.md`](../spring-boot-developer/references/observability/observability.md) |
| **Docker** | 本地 Compose（DB 与应用）、Dockerfile / Buildpacks、`.dockerignore`、镜像与 Testcontainers 镜像钉死 | sivalabs [`spring-boot-docker-compose.md`](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/main/skills/spring-boot/references/spring-boot-docker-compose.md)；rajeshanji8 `docker.md`；官方 [Dev Services](https://docs.spring.io/spring-boot/reference/features/dev-services.html) | [`docker.md`](../spring-boot-developer/references/deployment/docker.md)、[`local-development.md`](../spring-boot-developer/references/tooling/local-development.md) |
| **OpenAPI** | 规范所有权、生成或手写、与 HTTP 行为同步、只在项目维护规范时强制 | developer-kit [`spring-boot-openapi-documentation`](https://github.com/giuseppe-trisciuoglio/developer-kit/blob/main/plugins/developer-kit-java/skills/spring-boot-openapi-documentation/SKILL.md)；Dancan254 `api-design`；rrezartprebreza `openapi-first` | [`openapi.md`](../spring-boot-developer/references/web/openapi.md) |
| **出站 HTTP** | `@HttpExchange` / `RestClient`、超时、不把 WebClient 当默认阻塞客户端 | rajeshanji8 `http-client.md`；j-morgan6 `http-clients`；Dancan254 `http-resilience`；官方 [IO](https://docs.spring.io/spring-boot/reference/io/index.html) | [`http-clients.md`](../spring-boot-developer/references/integration/http-clients.md) |
| **迁移工作流** | 已发布版本不可改、用新版本纠正、共享环境与滚动发布 | rynr [`spring-flyway`](https://github.com/rynr/spring-skills/blob/main/skills/spring-flyway/SKILL.md)；rrezartprebreza `flyway-migrations` | [`flyway.md`](../spring-boot-developer/references/persistence/flyway.md)；规则摘要已在 `data-jpa.md`，扩范围后值得独立，避免 JPA 文件兼管发布纪律 |
| **异步 / 调度** | `@Async` / `@Scheduled`、执行器、代理自调用 | rajeshanji8 `async-scheduling.md`；adityamparikh `async-and-executors.md`；rrezartprebreza `async-execution` + `scheduled-tasks` | [`async-and-scheduling.md`](../spring-boot-developer/references/integration/async-and-scheduling.md) |
| **缓存抽象** | `@Cacheable` 边界、失效、不缓存实体；Redis 仅当项目已选 | rajeshanji8 `caching.md`；developer-kit [`spring-boot-cache`](https://github.com/giuseppe-trisciuoglio/developer-kit/blob/main/plugins/developer-kit-java/skills/spring-boot-cache/SKILL.md) | [`caching.md`](../spring-boot-developer/references/persistence/caching.md) |

可并入上表、不必再拆文件：结构化日志 → 可观测性；虚拟线程 → 配置；Maven wrapper 质量插件 → bootstrap；Boot Docker Compose 接线 → Docker 与 testing 交叉链接。

REST 本身已在 `web-mvc.md`。用户说的「缺 restapi」应读成：缺**公开 HTTP 契约工作流**（方法语义、OpenAPI 所有权、出站客户端），不是缺 `@RestController` 入门。

### 按任务触发：项目会碰到，但不是默认栈

入口给指针即可；没有对应任务时不读。

| 主题 | 处理 | 不这样处理的原因 |
| --- | --- | --- |
| Jackson 3 序列化 | 扩 web 或独立短参考 | 现仅 `@JsonTest`；Boot 4 默认 Jackson 3 |
| API 版本 | 项目引入破坏性变更时读 | Framework 7 有官方 version 路由；首版路径常不带版本 |
| 审计字段 | 项目要 created/updated 时读 | 不是每个实体模型都需要 |
| 邮件 / 文件上传 | 有出站邮件或上传时读 | IO 章存在，典型 CRUD 可不碰 |
| 限流 | 有滥用面时读 | 横切放置规则已在 developer；深度策略是产品决策 |
| 消息（Kafka/Rabbit/JMS） | 重写 `messaging.md`；Cloud Stream 仍归 Cloud 技能 | 进程内事件不走经纪 |
| MyBatis-Plus | 重写 `mybatis-plus.md`；新项目默认仍 JPA | 入口不把 Wrapper 写成总则 |
| GraalVM native / AOT | 重写 `native.md` | CRaC、Azure 不写 |
| Spring Modulith | 项目已采用时跟随 architecture | 不是默认 CRUD 结构 |
| Thymeleaf / 静态资源 / SPA 回退 | 有服务端页或打进 jar 的前端时读 | REST 服务通常不碰；前端框架本身见下 |
| WebFlux | 项目已是 reactive 服务端时沿用 | 新项目默认 MVC；adityamparikh 把 reactive 测试划出 |
| GraphQL / Batch / R2DBC / NoSQL | 用户点名再读 | 官方有章，外部技能也当独立 skill |
| 密码存储、方法安全深化 | 新建受保护服务时，从 developer 的 fundamentals 裁一节接到 security | 现 security 假定已有认证方案 |
| Gradle | 已有 Gradle 项目沿用并报告 | 生成器仍是 Maven；方案已允许沿用 |
| Boot 3.5.x 现有项目 | 识别版本、不静默升级；重写 `migration.md` | 技能名表示默认线是 Boot 4，不是拒绝 3.5 |
| OIDC / SAML / AS / SCIM / IdP | 重写 `references/security/`，应用授权仍走 `security.md` | 退役 SSO 后由本技能承载；不把协议正文写进 `security.md` |
| 前端 | 重写一篇 `frontend.md`（含 SSO BFF） | 不是五篇框架长文；新项目不默认生成前端 |

### 兄弟技能拥有：考虑过，不写进 `spring-boot-4` 正文

| 主题 | 所有者 | 项目技能只做 |
| --- | --- | --- |
| Java 语言、nullness、records | `java-developer` | 在 Spring 边界上跟项目约定 |
| PostgreSQL schema、索引、`EXPLAIN`、RLS | `postgresql-developer` | JPA 映射、应用事务、Flyway 文件 |
| Gateway / 注册中心 / Config Server / Stream | `spring-cloud-developer` | 服务内结构与应用安全 |
| JVM 旗标、GC、JFR、容器 cgroup | `java-performance-developer` | 应用层超时、连接池大小的**配置项**；调参有测量再交性能技能 |
| GitHub Actions / 签名 / SBOM / 环境推广 | `ci-cd-developer` | 镜像构建输入、健康探针路径、wrapper 测试命令 |

Cloud / Java / PostgreSQL / 性能 / CI/CD 继续外置。前端写一篇 `frontend.md`；Azure 不写。生成器默认不启用前端。

### 专项：点名才做

Spring AI / MCP、CRaC、Kotlin、IDE/MCP 专属（spring-tools beans/endpoints、Amplicode 插件）、OpenRewrite 大版本迁移、渗透测试/供应链扫描产品、Taskfile、Taikai、覆盖率阈值、AWS SDK、Neo4j。跨仓库对照与 SivaLabs 研究已排除这些作为通用默认。native 与 MyBatis-Plus、消息已列入 [重写方案](spring-boot-4-rewrite-plan.zh-CN.md) 必写参考，不是默认生成项。

MyBatis-Plus 在 `spring-boot-developer` 里是一等持久化选项，在 `spring-boot-4` 首版和外部多数 Boot 4 技能里不是。扩项目技能时：已有 MyBatis-Plus 的项目沿用并报告；新项目默认仍 JPA。不要把 developer 的 Mapper/Wrapper 规则设成 `spring-boot-4` 默认。

## 与旧技能的关系

当前仍是两套（加 SSO 第三套）并存。选定目标是重写后删除，不长期保留两个全栈入口。细节见 [重写方案](spring-boot-4-rewrite-plan.zh-CN.md)。

## 实施时仍有效的写作约束

来自 [writing-for-agents](../../.agents/skills/writing-for-agents/SKILL.md) 与跨仓库对照：

- 入口只做路由；参考按任务加载。
- 每条规则带可观察验收，不抄外部技能的覆盖率数字、TSID、`BaseEntity`、Taikai、Taskfile。
- 技能目录自包含；`docs/` 调研文件不链进 `SKILL.md`。
- 已有项目沿用其栈；技能默认值只填空缺。
- 不把「考虑过」写成「每个新项目都生成」。

## 来源与抓取限制

- 本地：[`spring-boot-4/SKILL.md`](../spring-boot-4/SKILL.md)、六份 references、[`versions.json`](../spring-boot-4/versions.json)、[`spring-boot-developer/SKILL.md`](../spring-boot-developer/SKILL.md) 参考地图、兄弟技能入口、[实现方案](spring-boot-4-skill-implementation-plan.zh-CN.md)、[跨仓库对照](spring-boot-4-cross-repository-review.zh-CN.md)、[SivaLabs 对照](sivalabs-agent-skills-spring-boot-developer-findings.zh-CN.md)。
- 外部技能入口：sivalabs、spring-tools、Dancan254、rrezartprebreza（目录 44 项，抽读 observability）、rajeshanji8、full-stack-skills、developer-kit、Amplicode、rynr、awesome-copilot（java-springboot / spring-security / testing / observability / create-project）、jdubois、a-pavithraa、adityamparikh、Jeffallan、pluginagentmarketplace。
- 官方：Web、Data、Messaging、IO、Actuator、Packaging、Features、Dev Services、Native Image 章 index 已取到。`htmlsingle` 与 `docs/4.0.0/reference/html/index.html` 返回 404，未当作 Boot 4 目录证据。
- 失败：`alivirgo/Major-AI-Skills` 的 `/main/` `SKILL.md` 404；`j-morgan6/spring-boot-guide` README 404（skills 目录 API 可读）。失败项未编造主题列表。

未改 `spring-boot-4` 技能文件。选定档位后再改入口和参考。
