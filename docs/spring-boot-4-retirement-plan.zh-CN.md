# 退役 `spring-boot-developer` 与 `spring-sso-developer`

目标已经选定：仓库里只保留一份 Spring Boot **应用**技能，目录名仍是 [`spring-boot-4`](../spring-boot-4/SKILL.md)。做法改为**按模板重写**，不把另外两份技能的文件迁进来。实施以 [重写方案](spring-boot-4-rewrite-plan.zh-CN.md) 为准。

本文保留退役后的技能地图。目录搬家、所有权表「迁入后删」、以及后文分阶段迁文件**不要执行**。主题盘点仍见 [完整项目主题调研](spring-boot-4-full-project-scope-research.zh-CN.md)。

## 退役后的技能地图

```text
语言     java-developer
数据库   postgresql-developer
性能     java-performance-developer
流水线   ci-cd-developer
云组件   spring-cloud-developer        ── Gateway / Config / Discovery / Stream
应用     spring-boot-4                 ── Boot 应用 + 安全/SSO + 前端集成 + 容器
已删     spring-boot-developer
已删     spring-sso-developer
```

Cloud、Java、PostgreSQL、JVM 性能、CI/CD **不**并入。它们的问题不是「做一个 Spring Boot 项目」，入口抢触发的成本大于合并的收益。`spring-boot-4` 的 `SKILL.md` 用一行指向它们。

## 目标形态

一份 model-invoked 技能。`SKILL.md` 只做工作流和任务路由，参考按任务加载。现有六份短参考继续当 **Boot 4 默认路径**（MVC、JPA、应用授权、包边界、测试、生成），它们经过对照实验，不拆散、不改写成百科。

developer 与 SSO 的专题进入 `spring-boot-4/references/` 分组目录，只在对应任务读取。禁止把 75+25 份文件的正文贴进入口。

生成器以 developer 的脚本和资产为超集，默认值仍用现在的 `spring-boot-4`：新项目 Boot 4、Maven、MVC；JPA/PostgreSQL、Docker、前端、OpenAPI、Actuator 按请求或项目已有选择加入。现有 3.5.x 项目不静默升级，迁移说明随 developer 一起搬进来；技能名表示默认线是 Boot 4，不是「拒绝 3.5」。

订单样例、workshop、doctor/smoke 脚本都进同一目录。技能必须自包含，安装时不依赖已删除的旧目录。

## 内容所有权（冲突时用这一列）

同一主题禁止两份正文。合并时只留一处，另一处改成指针或删除。

| 主题 | 权威来源 | 并入后的位置 | 丢掉什么 |
| --- | --- | --- | --- |
| 生成、版本、环境检查 | developer 脚本 + boot-4 `bootstrap.md` / `versions.json` | `scripts/`、`assets/`、`references/bootstrap.md` | 重复的 Initializr 说明；默认值以 boot-4 为准，能力以 developer 为准 |
| 功能包、ArchUnit | boot-4 `architecture.md` | 保留；developer 的 code-organization / archunit / modulith / hexagonal 作加深 | developer 里与 boot-4 重复的「按功能分包」段落 |
| 入站 HTTP、校验、错误 | boot-4 `web-mvc.md` | 保留为默认；developer 的 rest/validation/error/json/openapi/versioning 按任务加深 | 三份文件各写一遍 ProblemDetail |
| JPA、事务、旧数据 | boot-4 `data-jpa.md` | 保留；developer 的 flyway/transactions/concurrency/schema 按任务加深 | 重复的事务边界定义 |
| 应用授权、CSRF、CORS | boot-4 `security.md` | 保留为默认应用授权 | developer `security-fundamentals.md` 里与 CORS/CSRF 重复的短节，合并进 fundamentals 或 security.md，只留一处 |
| 过滤链 DSL、密码、方法安全 | developer `security-fundamentals.md` | `references/security/security-fundamentals.md` | 无 |
| 本地 JWT 签发 | developer `jwt-authentication.md` | `references/security/jwt-authentication.md` | 无。Authorization Server 不是这条 |
| MFA / WebAuthn | developer `mfa.md` | `references/security/mfa.md` | 无 |
| OIDC 登录、resource server 协议 | **SSO** `spring-security-oidc.md` + `security-oauth2-oidc.md` | `references/security/oidc/` | developer `oauth2-login.md`、`oauth2-resource-server.md` 降为指针或删除。SSO 更深、有 RFC/OWASP 出处 |
| SAML | **SSO** `spring-security-saml.md` + `security-saml.md` | `references/security/saml/` | developer oauth2-login 里的 SAML 短节 |
| 13 个 IdP + 对照表 | SSO `providers.md` 与 `provider-*.md` | `references/security/providers/` | 无 |
| Authorization Server | SSO `spring-authorization-server.md` | `references/security/spring-authorization-server.md` | developer 里「不要自建 AS」的一句话，改成指向这份 |
| SCIM | SSO `scim-provisioning.md` | `references/security/scim-provisioning.md` | 无 |
| SSO 加固、会话/租户、清单、Dex/Rose 测试 | SSO 其余文件 | `references/security/` 对应文件 | 无 |
| SSO 前端（BFF、logout 导航） | SSO `frontend-integration.md` | `references/security/frontend-integration.md` | 与 Vue/React 指南交叉链接，不合并成一篇 |
| 前端框架 | developer `frontend/` | `references/frontend/` | 无 |
| 配置、观测、Docker、出站 HTTP、异步、缓存、消息、Batch、AI… | developer 对应文件 | 原分组路径迁入 | 无。这就是调研里的项目级缺口 |
| Boot 3.5 迁移 | developer `spring-boot-migration.md` | `references/spring-boot-migration.md` | 无 |
| checklist | 两份清单按职责拆 | 应用清单 + SSO 清单，入口按任务指向 | 不要合成一份无法读完的表 |
| workshop | developer `workshop/` | `spring-boot-4/workshop/` | 文内技能名改成 `spring-boot-4` |

`spring-boot-4` 现有六份文件的 Prefer / Avoid / Verify 结构和订单样例链接保持。往里面只加路由，不加 SSO 协议正文。

## 目录草案

```text
spring-boot-4/
├── SKILL.md                 # 工作流 + 全量任务路由（含 SSO 症状表）
├── README.md                # 人读入口，从 developer README 改写
├── versions.json            # 合并；生成器默认取 boot-4 的 Boot/Java，能力取 developer
├── scripts/                 # developer 脚本迁入，生成器默认对齐现有 boot-4 行为
├── assets/                  # developer 模板 ∪ 现有 compose/order-example
├── workshop/
└── references/
    ├── bootstrap.md         # 现有短文，链接更深的 project-setup / local-development
    ├── architecture.md
    ├── web-mvc.md
    ├── data-jpa.md
    ├── security.md          # 现有应用授权；协议任务指向 security/*
    ├── testing.md
    ├── spring-boot-migration.md
    ├── checklist.md
    ├── architecture/
    ├── web/
    ├── persistence/
    ├── security/            # fundamentals, mfa, jwt, oidc/, saml/, providers/, …
    ├── testing/
    ├── integration/
    ├── observability/
    ├── frontend/
    ├── deployment/
    ├── tooling/
    └── configuration/
```

入口 description 必须同时触发现在两份技能会接到的任务：脚手架、改应用、REST、持久化、测试、Docker，以及 OIDC/SAML、resource server、自建 AS、SCIM、SSO 排障。否则退役后这些任务没有技能可发现。

## 分阶段（可单独验收）

每一阶段结束时：`spring-boot-4` 可独立安装；旧技能仍可用，直到最后一阶段删除。不在中途制造「两个全栈入口」。

### 1. 迁入 developer 的专题与工具，先不删

- 把 `references/` 分组、`scripts/`、`assets/`、`workshop/`、`README.md` 迁入 `spring-boot-4`。
- 重写 `spring-boot-4/SKILL.md` 路由表，覆盖 developer 现有地图，并保留现有六份短参考为默认路径。
- 合并 `versions.json` 与生成器：developer 的选项都还在，默认输出仍是现在 boot-4 的 MVC 骨架。
- 现有六份短参考只改链接，不改已经对照过的规则。
- 仓库内指向 `spring-boot-developer/...` 的链接改到新路径。`spring-boot-developer/SKILL.md` 暂时留作跳转（description 指向 `spring-boot-4`，正文五到十行），避免安装了旧目录的环境空转。

验收：从 `spring-boot-4` 目录能解析全部内部链接；生成器默认路径仍能 `verify`；订单样例仍能构建。

### 2. 迁入 SSO，去掉与 developer 安全文件的重复

- 按上一节所有权表移动 SSO 参考。
- 删除或改成指针：developer 的 `oauth2-login.md`、`oauth2-resource-server.md`。
- 把 SSO 入口的「先建立局面」五步、OIDC/SAML 选择表、症状表并入 `spring-boot-4/SKILL.md` 的 Security 段。
- `spring-sso-developer/SKILL.md` 同样先改成跳转。

验收：OIDC 登录、SAML、resource server、AS、SCIM、某个具体 IdP 都能从新入口路由到**一份**正文；没有两份互相打架的登录指南。

### 3. 改仓库入口，再删目录

- [`README.md`](../README.md) 技能表：用 `spring-boot-4` 替换那两行。
- `spring-cloud-developer`、`java-performance-developer`、`postgresql-developer`、`sdd-build`、samples 提示词里的技能名一并改。
- 删除 `spring-boot-developer/` 与 `spring-sso-developer/`。
- SkillsJars 扫描根目录 `SKILL.md`，删除后旧名字不再打包。

验收：全仓库搜索不到作为技能入口的旧名字（历史 `docs/` 调研可以保留旧称，并在文首注明已退役）。

### 4. 不在退役范围内

- 不把 Cloud / CI/CD / PostgreSQL / Java / 性能技能并进来。
- 不把 boot-4 的六份短参考重写成 developer 长文。
- 不把 13 个 IdP 文件合成一篇。
- 不在这一步重跑四轮 A/B 对照；对照覆盖的是核心六份。SSO 与前端迁入后，用现有 samples 提示词和订单样例做链接与构建检查，而不是复制实验。

## 仓库内必须改名的引用

实施阶段 3 时按文件改，不要提前改一半：

- [`README.md`](../README.md) 技能表三行（postgresql 配对、boot-developer、sso-developer）
- [`spring-cloud-developer/SKILL.md`](../spring-cloud-developer/SKILL.md) 文末两个链接
- `java-performance-developer`、`ci-cd-developer` 参考里指向 boot-developer 的交接句
- [`sdd-skills/sdd-build/SKILL.md`](../sdd-skills/sdd-build/SKILL.md) 示例技能名
- [`samples/spring-security-project-prompts.zh-CN.md`](../samples/spring-security-project-prompts.zh-CN.md) 的 `$spring-boot-developer`
- [`samples/spring-boot-skill-validation/README.md`](../samples/spring-boot-skill-validation/README.md)
- workshop 各篇里的技能名

## 明确不做的捷径

- 不在 `spring-boot-4` 里 `../spring-boot-developer` 外链凑合。退役后那些路径不存在，技能必须自包含。
- 不保留三个 `SKILL.md` 长期共存。跳转最多作为阶段 1–2 的过渡。
- 不把 SSO 压缩进现在的 `security.md`。协议正文有独立触发条件，塞进应用授权短文会毁掉两边。

下一步是阶段 1：迁入 developer 的专题与工具，并把 `spring-boot-4/SKILL.md` 改成全量路由器。
