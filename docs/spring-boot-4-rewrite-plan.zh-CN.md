# `spring-boot-4` 重写方案

目标不变：仓库只留一份 Spring Boot 应用技能，退役 [`spring-boot-developer`](../spring-boot-developer/SKILL.md) 与 [`spring-sso-developer`](../spring-sso-developer/SKILL.md)。做法不是搬家。现有六份参考的写法已经对照过；另外两份技能和外部调研是**取材**，每一份新参考都按同一套模板重写。

[主题调研](spring-boot-4-full-project-scope-research.zh-CN.md) 仍提供「要考虑哪些主题」。[退役方案](spring-boot-4-retirement-plan.zh-CN.md) 里的目录搬家不要执行。本文是唯一实施说明。

## 复核（相对本文前一稿）

对照待退役两份技能的入口地图、现有六份参考和写作约束之后，前一稿有这些必须改掉的矛盾。下面已写进后文清单。

1. **退役条件要求覆盖的主题，不能放在「有任务再写」。** `migration.md`（3.5 不静默升级）、`frontend.md`（SSO 的 BFF/logout/HRD，以及打进 jar 的 SPA）、`checklist.md`（developer 跨层交付）与 messaging / native / MyBatis-Plus 同一档：没有它们就不能删旧技能。框架专文（Vue/React/…）仍可后补。
2. **不要再拆 `flyway.md`。** [`data-jpa.md`](../spring-boot-4/references/data-jpa.md) 已有已发布版本不改、新版本纠正、存量升级验收。再写一篇会变成两处权威。Flyway 纪律留在 `data-jpa.md`；独立文只在出现 Liquibase 或非 JPA 迁移工具时才加。
3. **SSO 有三份文件没进清单。** `security-sessions-and-tenancy.md`、`testing-oidc-with-local-provider.md`、`frontend-integration.md` 分别并入安全索引 / `oidc-login.md` 的 Verify / `frontend.md`，不丢规则、不各开一篇百科。
4. **IdP 卡片与退役条件不一致。** 清单写了全部 `provider-*.md`，退役条件却写「至少一个」。SSO 的独特价值是对照表加 13 张卡片。退役前要有 `providers.md` 和全部卡片，按供应商名加载。
5. **生成器版本要先钉死。** 当前 `spring-boot-4` 是 Boot **4.2.0**、Java **21**。developer 是 Boot **4.1.2**、Java **25**。重写以现有 `spring-boot-4/versions.json` 为生成默认；3.5 只作为现有项目线。native 构建需要与 Boot 4 兼容的 GraalVM（developer 记的是 25），即使应用 `--release` 仍是 21。不要为了对齐 developer 把默认 JDK 改成 25。
6. **第 1 组不要提前改 description。** 参考文件不存在时，把 MyBatis-Plus / Kafka / native / OIDC 写进 description 会让代理读到空指针。每组文件落地后再加对应分支。
7. **CSRF/CORS 的权威仍是 `security.md`。** 对照实验改过拒绝预检的断言。`security/fundamentals.md` 只写过滤链 DSL 和密码，不重写 CORS。
8. **邮件、上传、限流等到 AI/MCP 按任务分类，见「按任务专文」。** 不进 description；退役前要有专文，或明确交给兄弟技能。原先把它们写进「退役后没有专文」是错的。

## 为什么不搬家

`spring-boot-developer` 的入口有 17 条核心规则，其中 MyBatis-Plus Wrapper、限流横切、Taikai 基线对多数 Boot 4 任务是噪声。`spring-sso-developer` 的协议深度有价值，但声口、版本钉死和症状表与 `spring-boot-4` 的 Prefer / Avoid / Verify 不是同一套文件。把两组 `references/` 拷进来，会得到两套重叠正文，入口继续膨胀。

对照实验证明有效的是短规则加可观察验收，不是专题百科。CORS 那一轮里，补上「拒绝预检看许可头」之后启用组才改断言；规则改的是验收，不是多一份 Docker 手册。重写沿用这个杠杆。

## 从调研里取什么

只取改变代理行为的杠杆，不取目录树。

| 来源 | 取 | 不取 |
| --- | --- | --- |
| 当前 `spring-boot-4` | 短入口；修改三步；Prefer / Avoid / Verify；订单样例；按任务读一份参考 | 把范围冻在 MVC/JPA/授权 |
| [跨仓库对照](spring-boot-4-cross-repository-review.zh-CN.md) | 已写入六份文件的规则维持；范围按任务触发 | 再往六份文件里堆泛化条款 |
| SivaLabs | 入口是索引；「只在该分支读链接文件」；测试对象 → 层级 → 文件 | Taskfile、Taikai、TSID、`BaseEntity`、80% 覆盖率、整份 19 文件 |
| rynr | Boot 技能主体是配置；安全/数据/Web 分开加载 | 拆成多个独立技能（本仓库要退役的是技能入口，不是主题） |
| `spring-sso-developer` | 动手前先钉死 Boot/Security 版本、SP 还是 IdP、协议、单租户还是多租户；不编造属性名；默认值随版本翻转要写明；协议攻击分文件；症状表；IdP 卡片（issuer、角色从哪来、主键） | 七条总则放进 `SKILL.md`；把应用授权写进协议文件 |
| `spring-boot-developer` | 生成器 + `versions.json`；新项目 Boot 4、现有 3.5 不静默升级；可选能力按请求加；清单在跨层交付时用 | 17 条入口规则；MyBatis-Plus 当默认；每个前端框架一篇长文原样保留 |
| awesome-copilot / developer-kit / rajeshanji8 | 配置、Actuator、Docker、OpenAPI、出站 HTTP 各自成文 | 强制每个端点写 Swagger 注解；把 CI/SBOM 写进 Boot 技能 |
| spring-tools / jdubois | Initializr 依赖 ID 与 Boot 版本兼容；生成后核对 JDK | IDE/MCP 专属工作流 |
| writing-for-agents | 指针带分支；完成标准可检查；一份意思一处写；环境能查到的不缓存 | 在入口复述参考正文 |

Cloud、Java、PostgreSQL、JVM 性能、CI/CD 仍外置。入口各一行，任务属于那些技能时不要在本技能展开。

## 入口

`SKILL.md` 继续只做两件事：工作流，路由。核心规则不进入口。版本、DTO、MVC 默认、测试层级已经分别写在修改步骤和对应参考里；再写一遍是重复。

### description

model-invoked，分支只保留真正不同的任务，不列同义词：

- 生成或改 Spring Boot 应用（MVC、JPA、MyBatis-Plus、测试、包边界）
- 配置、Actuator、Docker Compose / 镜像、GraalVM native / AOT
- 消息（Kafka、RabbitMQ、JMS）
- 认证与联邦：过滤链、OIDC、SAML、JWT、MFA、resource server

前端框架、以及「按任务专文」整表，都不进 description。它们只走路由表。否则每个 Boot 任务都会带上 Batch、WebFlux、AI 这些分支。

### 工作流

保留现在的「新建 / 修改」。调试从 developer 收成第三条，但缩短为：从症状出发 → 最小复现 → 对证据改那一条路径 → 用 wrapper 验证。SSO 的「先建立局面」不放在总工作流里，放在安全路由的第一句：涉及认证协议时，先读安全索引里的局面表。

修改步骤保持四步。第三步「读匹配的参考」后面接路由表。交付完成标准仍是：报告实际跑过的命令和结果，以及没跑的检查。

### 路由表

按任务一行一个指针。现有六行保留。新增行只指向下面清单里的文件，其中必含：

- Mapper、Wrapper、非 JPA 持久化 → `mybatis-plus.md`
- Kafka、RabbitMQ、JMS、outbox、消费者重投 → `messaging.md`
- native image、AOT、GraalVM → `native.md`

SSO 症状表不进入口全文，进安全索引；入口只留「登录、联邦、令牌、SAML、SCIM 走安全索引」。

## 写作模板

除 `SKILL.md` 外，每份参考同一骨架。现有六份已经接近这个形状，重写时只补缺的节，不改已经对照过的 Prefer / Avoid / Verify。

```markdown
# 祈使标题（代理要做的事）

何时读：用一句话列出触发分支。

## <一项决定>
Prefer. …
Avoid. …          # 只在无法改成正向规则时保留
Verify. …         # 可观察：请求、库状态、头、日志、构建输出
```

版本敏感的文件加 `## Version notes`：默认按 Boot 4 / Security 7 写；现有 3.5 / Security 6.5 只写差异和「先读迁移说明」。协议文件加 `## Gotchas`：只列代理会编造或选错默认值的那几条，并引官方或源码出处。禁止粘贴长教程。

示例以订单样例或最小片段为证。新主题需要可运行证据时，扩订单样例或加一个最小夹具，不新建第二套百科式 demo 树。

## 拟写参考清单

不是从旧技能迁文件。每行是一篇要重写的文章。现有六份是修订，其余是新写。取材列只表示重写时对照哪些正文和官方文档。

### 保留并轻修（对照过，不重写成百科）

| 文件 | 轻修 |
| --- | --- |
| `bootstrap.md` | 链到配置、Docker、探针；生成器选项变了就改表 |
| `architecture.md` | 不拆 Modulith/六边形专文；项目已采用时的现有段落保留 |
| `web-mvc.md` | 链到 OpenAPI、出站 HTTP；HTTP 契约规则不动 |
| `data-jpa.md` | 不拆 Flyway 专文；映射/事务/已发布迁移/旧数据规则不动 |
| `security.md` | 只做应用授权、过滤链验收、CSRF/CORS；协议任务指向安全索引 |
| `testing.md` | 层级表保留；容器接线细节链到 Docker/Testcontainers 短节 |

### 项目交付（调研里缺的独立主题）

| 文件 | 代理要决定什么 | 取材 |
| --- | --- | --- |
| `configuration.md` | profiles、`@ConfigurationProperties`、密钥外置、虚拟线程 | developer 配置文、rynr Boot 技能 |
| `observability.md` | Actuator 暴露面、健康/就绪、结构化日志；追踪仅项目已有时 | developer 观测文、awesome-copilot observability、官方 Actuator |
| `docker.md` | 本地 Compose、应用镜像、与 Testcontainers 钉同一镜像、`.dockerignore` | developer docker/local-dev、sivalabs compose、官方 Dev Services |
| `openapi.md` | 规范归谁、与 HTTP 行为同步；项目无规范则不强加 | developer openapi、developer-kit openapi 技能 |
| `http-clients.md` | `@HttpExchange` / `RestClient`、超时 | developer http-clients、官方 IO |
| `async.md` | `@Async` / `@Scheduled`、代理自调用 | developer async；自调用与 `data-jpa.md` 事务代理交叉时只在一处写清、另一处指针 |
| `caching.md` | 何时缓存、如何失效、不缓存实体；Redis 仅项目已选 | developer caching |
| `mybatis-plus.md` | Mapper/`BaseMapper`、Lambda Wrapper、批量查询、与 JPA 的选型 | developer mybatis-plus；官方 Boot 4 starter。新项目默认仍 JPA，此文在用户点名或项目已用时读 |
| `messaging.md` | 是否该上经纪、Kafka/Rabbit/JMS 选型、outbox、消费者幂等、真实经纪测试 | developer messaging、官方 Messaging。进程内事件仍走 `architecture.md`。Cloud Stream 仍归 `spring-cloud-developer` |
| `native.md` | 是否值得 native、AOT/反射限制、本机可执行文件与容器构建、对照 JVM 镜像测量 | developer graalvm、官方 Native Image。不把 CRaC/Azure 写进来 |

Jackson 3、API 版本、审计字段：先作为 `web-mvc.md` / `data-jpa.md` 的短节。只有写不下或任务经常单独触发时再拆文件。

`mybatis-plus.md`、`messaging.md`、`native.md` 与配置、Docker 同一档：技能必须能独立交办。生成器默认仍是 JPA；`--persistence mybatis-plus`、消息依赖、native 构建只在请求或项目已有时打开。

另三篇同样是退役前必写，不放在「有任务再写」：

| 文件 | 代理要决定什么 | 取材 |
| --- | --- | --- |
| `migration.md` | 现有 3.5 项目怎么翻译本技能的 Boot 4 示例；何时才升级 | developer `spring-boot-migration.md` 压成差异清单 |
| `frontend.md` | 同源 API、SPA 回退、SSO 场景下的 BFF / 不能用 `fetch` 起登录 / logout 必须导航 / HRD | developer `spring-boot-integration.md` + SSO `frontend-integration.md`。不是五篇框架长文 |
| `checklist.md` | 跨层交付或改认证时要观察哪些项 | 两份旧清单抽可观察项，按任务分节 |

### 安全（取代 SSO 技能 + developer 五份安全文）

先写索引，再写分协议文件。索引承担 SSO 入口里「建立局面」和症状表。

| 文件 | 触发 | 取材 |
| --- | --- | --- |
| `security/index.md` | 任何认证/联邦任务的第一份 | SSO 局面五步、OIDC/SAML 选择表、症状表、hardening 两个静默默认、会话/登出/租户隔离要点 |
| `security/fundamentals.md` | 新加 Security、密码、方法安全、DSL | developer fundamentals。CSRF/CORS 不在这里重写 |
| `security/oidc-login.md` | 外部登录、OAuth2 client | SSO `spring-security-oidc.md` + oauth2 攻击文 + 本地 IdP 测试（mock / Dex / Rose）；developer `oauth2-login.md` 只作对照 |
| `security/resource-server.md` | 校验外部令牌 | 同上，resource server 分支单独成文，避免和登录叠在一起 |
| `security/jwt.md` | 为本系统账号签发 JWT | developer jwt-authentication；不是 Authorization Server |
| `security/mfa.md` | MFA、WebAuthn、分阶段登录 | developer mfa |
| `security/saml.md` | SAML SP | SSO saml 实现 + saml 攻击 |
| `security/authorization-server.md` | 是否自建 IdP、以及决定要做时 | SSO AS；默认答案仍是「通常不要」 |
| `security/scim.md` | 供应/停用 | SSO scim（RFC 7644 的 DELETE vs PATCH active） |
| `security/providers.md` | 选 IdP 或角色/issuer 对不上 | SSO providers 对照表 |
| `security/provider-*.md` | 点名某个供应商 | SSO 全部 vendor 卡片重写：角色从哪来、issuer 陷阱、用什么当主键、未验证项。一篇一个供应商，按需加载。缺卡片则不能退役 SSO |

`security.md`（应用授权）与 `security/index.md`（协议局面）分开。过滤链上「这个人能不能动这条记录」走前者；「和哪个 IdP 用哪种协议」走后者。

### 按任务专文（分类）

下面这批**要考虑、要有落点**，但不是默认栈。分类用三个问题：谁拥有、是否单独成文、进不进 description。结论先说：全部**不进 description**；除标明交给兄弟技能的部分外，退役前都要有一篇按模板重写的参考，由路由表触发。

| 主题 | 分类 | 落点 | 代理要决定什么 | 不放在这里的 |
| --- | --- | --- | --- | --- |
| 邮件 | 本技能 · 出站集成 | `mail.md` | SMTP 外置、超时、事务外发送 | 不把发信写进业务事务 |
| 上传 | 本技能 · 出站集成 | `file-storage.md` | 大小/类型边界、落盘或对象存储、不进 Git | 不是 CDN 产品手册 |
| 限流 | 本技能 · 横切 | `rate-limiting.md` | 过滤链 / 拦截器 / AOP 放哪、键是 IP 还是已认证主体 | 网关粗限流可交给 Cloud；配额与对象授权仍在应用 |
| Batch | 本技能 · 作业 | `batch.md` | 何时用 Spring Batch、chunk 边界、失败重跑 | 不是定时 `@Scheduled`（那是 `async.md`） |
| Resilience | **拆开** | `resilience.md` | Framework 7 `@Retryable` / 并发限制：先超时和幂等，再重试 | 跨服务熔断、网关重试放大 → `spring-cloud-developer` resilience |
| WebSocket | 本技能 · Web | `websocket.md` | STOMP 还是裸 WebSocket、目的地授权；跨实例才上外部 broker | 持久化领域事件走 `messaging.md` |
| Thymeleaf | 本技能 · Web | `thymeleaf.md` | 服务端 HTML；新项目默认仍是 MVC JSON | 不是 SPA 框架指南 |
| i18n | 本技能 · Web | `i18n.md` | 消息源、locale、错误文案 | 不是整站翻译流程 |
| WebFlux | 本技能 · Web | `webflux.md` | **已有**响应式服务端则沿用；新项目默认 MVC；出站 HTTP 不因此改 WebFlux | developer 没有专文，只有入口第 8 条，必须写成专文以免乱猜配置 |
| GraphQL | 本技能 · Web | `graphql.md` | schema 与 HTTP 边界、错误形状 | developer 无专文；官方有 starter，按任务写短文 |
| NoSQL | 本技能 · 持久化 | `nosql.md` | Mongo 等文档库、Redis **作数据**时的仓储边界 | Redis **作缓存**走 `caching.md`；PostgreSQL/JPA 仍是默认 |
| 多租户 | **拆开** | `multi-tenancy.md` | 租户从可信身份来、共享表/schema/库怎么隔离、查询必带租户 | RLS/`EXPLAIN` → `postgresql-developer`；断言密钥隔离 → 安全索引 |
| K8s | **拆开** | `kubernetes.md` | 应用侧：探针、优雅关闭、配置用环境变量、与镜像标签的关系 | Deployment 摘要、rollout、流水线 → `ci-cd-developer` `target-kubernetes.md`。探针细节与 `observability.md` 交叉时只在一处写清 |
| BootUI | 本技能 · 本地工具 | `bootui.md` | 仅本地开发控制台，不替代 Actuator，不进生产 | 不是 APM |
| AI | 本技能 · 集成 | `spring-ai.md` | Boot 4 配 Spring AI 2.x、ChatClient、密钥外置 | 1.x 只用于 Boot 3.5；观测并进此文 |
| MCP | 本技能 · 集成 | `mcp.md` | stdio 与 Streamable HTTP 选型、用 Spring AI starter | 不是通用 MCP 宿主产品 |

与已有文件的边界：限流 ≠ resilience（到达 vs 在途/重试）；WebSocket 的 in-memory STOMP broker ≠ Kafka；Thymeleaf ≠ `frontend.md` 的 SPA；多租户 ≠ `security.md` 的单资源归属。

### 退役后仍无专文

HATEOAS、Azure、CRaC、备份恢复、R2DBC 专文、doctor-project、workshop、每个前端框架长文、设计模式/领域建模/六边形专文、`git.md` / `jdtls.md` / `mise.md`、CI 发布专文。环境能查到的留给环境。Cloud Stream、schema/`EXPLAIN`、JVM 旗标、流水线仍走兄弟技能。

## 生成器与样例

扩展现在的 `scripts/create-project.mjs` 和 `versions.json`，不替换成 developer 那份脚本。默认输出仍是 MVC Maven 骨架。可选能力用旗标打开（JPA/PostgreSQL、MyBatis-Plus、Docker、Actuator），与 `bootstrap.md` 的表一致。消息与 native 不作为生成默认值；参考写清接到已有项目或用户点名时怎么加。`check-environment` 只在 Docker/数据库路径需要时调用。

订单样例继续做 MVC+JPA+授权+架构的可运行核。新主题的 Verify 优先挂到这个核上的测试，而不是复制 workshop。workshop 不是技能能工作的前提；退役后若还要教学路径，另开一阶段按新参考重写，不拷贝 developer/workshop。

## 和旧技能的关系（重写期）

重写完成前，三份技能暂时并存。`spring-boot-4` 的 description 在覆盖 SSO 分支之前，不要抢 `spring-sso-developer` 的触发。每写完一组参考，就扩路由表；只有项目交付和联邦协议两组才改 description。

禁止：

- `spring-boot-4` 链到 `../spring-boot-developer` 或 `../spring-sso-developer` 充数
- 把旧文件改名拷进来再「略改标题」
- 在入口堆积 developer 的 17 条规则

允许：

- 重写时打开旧文件当取材，核对版本钉死和出处
- 订单样例、生成器、现有六份参考作为起点继续改

## 退役条件

同时满足才删旧目录：

1. 路由表能覆盖旧 `spring-boot-developer` 入口里仍要交办的任务，以及上表按任务专文（邮件、上传、限流、Batch、应用层 resilience、WebSocket、Thymeleaf、i18n、WebFlux、GraphQL、NoSQL、多租户、K8s 应用契约、BootUI、Spring AI、MCP），外加 `frontend.md`、`migration.md`、`checklist.md`。
2. 路由表能覆盖旧 `spring-sso-developer` 的协议任务（OIDC 登录含本地 IdP 测试、resource server、SAML、AS 决策、SCIM、会话/租户隔离、BFF 前端约束、`providers.md` 与全部 IdP 卡片、加固索引）。
3. 新项目默认路径仍能用现有生成器 `verify`；订单样例仍能构建。
4. 仓库 README、Cloud/性能/SDD/samples 提示词改为指向 `spring-boot-4`。
5. `spring-boot-4` 目录自包含，安装它不必带着两个旧目录。

未到条件时，旧技能保持可安装。不设长期跳转页。

## 实施顺序

一次写一组，每组可单独审查。先方案（本文），再按组重写，最后才删旧技能。

1. **入口骨架。** 扩 `SKILL.md` 工作流和路由表占位。description 暂不增加新分支，避免指向尚未写成的文件，也不抢 SSO 触发。
2. **项目交付。** configuration、observability、docker、openapi、http-clients、async、caching、mybatis-plus、messaging、native。落地后才把这些分支写进 description。生成器只加需要的旗标；MyBatis-Plus 不是新项目默认。
3. **安全索引 + 应用侧。** `security/index.md`、fundamentals、jwt、mfa；`security.md` 只加指针。
4. **联邦协议。** oidc-login（含攻击面与本地 IdP 测试）、resource-server、saml、authorization-server、scim、providers 索引与全部卡片。此时才把 OIDC/SAML 写进 description。
5. **退役前必写的其余三篇。** migration、frontend、checklist。
6. **按任务专文。** 邮件、上传、限流、Batch、resilience、WebSocket、Thymeleaf、i18n、WebFlux、GraphQL、NoSQL、多租户、K8s 应用契约、BootUI、Spring AI、MCP。只加路由，不改 description。
7. **退役。** 满足上一节条件后删除两个旧目录，改仓库引用。

现有六份参考不在第 1 组重写。除非对照失败，否则只加链接。
