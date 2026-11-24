# Spring Boot 4 项目开发技能实现方案

本文设计一个独立的 Agent Skill，目录名为 `spring-boot-4`。它支持从空目录生成可运行的 Spring Boot 4 服务，也支持在已有服务里新增或修改功能。两种任务都覆盖架构约束和测试。首版的交付物是一个可单独安装的技能目录，以及一组可重复执行的验收用例。

## 1. 首版支持范围

| 场景 | 首版约定 |
| --- | --- |
| 新建服务 | Spring Boot 4、Java 21、Maven Wrapper、Spring MVC 和 Bean Validation。Java 21 是本方案为首版选的默认值。 |
| 需要持久化的新服务 | 显式选择 JPA 和 PostgreSQL，使用 Flyway 初始化 schema。提供本地数据库配置，以及使用同一 PostgreSQL 主版本的 Testcontainers 测试。 |
| 已有服务 | 首版验收覆盖 Boot 4 Maven 项目。先识别 Java、Boot、模块、持久化、权限和 API 约定。遇到 Gradle、WebFlux、其他数据访问方式或未验证的版本组合时，沿用项目配置，按实际能完成的检查报告结果。 |
| 架构 | 新服务默认按业务功能分包。已有服务沿用当前模块和依赖方向。用 ArchUnit 表达已确认的规则。项目已采用 Spring Modulith 时，沿用它的模块验证。 |
| 测试 | 覆盖业务单元测试、MVC 切片、真实数据库测试、启动与关键路径集成测试，以及架构测试。每次改动按实际行为选择所需层级。 |
| 权限 | 支持给已有认证方案下的接口增加或调整授权。新项目按需求引入安全依赖。 |

版本升级、生产故障诊断、部署编排和开发流程规范属于其他任务。新增业务功能需要的数据库 schema 变更属于本技能的范围。

## 2. 文件结构与职责

```text
spring-boot-4/
├── SKILL.md
├── versions.json
├── references/
│   ├── bootstrap.md
│   ├── architecture.md
│   ├── web-mvc.md
│   ├── data-jpa.md
│   ├── security.md
│   └── testing.md
├── scripts/
│   └── create-project.mjs
└── assets/
    ├── templates/
    └── order-example/
```

| 文件 | 职责 |
| --- | --- |
| `SKILL.md` | 在 frontmatter 写清触发场景。正文定义新建与修改两个入口、项目约束的判定方式、参考文件的读取条件和交付检查。 |
| `versions.json` | 记录生成器使用的确切 Boot 版本、默认 Java 主版本、PostgreSQL 镜像和验证记录。首版实现时从官方支持的 Boot 4 稳定版本选定具体补丁版本，再用生成项目跑通构建后发布。后续更新沿用同一验证方式。 |
| `references/` | 按任务提供实现规则、适用条件和最小示例。正文链接到相关文件，在当前任务触及对应主题时读取。 |
| `scripts/create-project.mjs` | 用 Node.js 标准库完成参数校验、Spring Initializr 请求、模板应用和输出目录管理。脚本处理可重复的文件操作，代理依据用户需求选择参数和实现业务。 |
| `assets/templates/` | 保存生成器实际使用的文件，例如环境变量样例、本地 PostgreSQL Compose 配置。按选项应用，保持生成项目所需的依赖和配置一致。 |
| `assets/order-example/` | 保存可独立构建的最小订单服务，展示 HTTP、业务逻辑、数据库和测试怎样配合。代理可以读取其中的某个实现，也可以构建整个示例。 |

技能目录包含全部运行所需的指令、脚本和模板。Spring Initializr 是生成工程的外部服务。JDK、Node.js 和需要数据库时的 Docker 是对应操作的运行条件。

## 3. 代理如何执行任务

### 生成项目骨架

1. 从请求读取项目名、包名、目标目录和所需能力。用户指定 Java 或构建工具时，先检查对应组合是否在生成器支持范围内。默认生成不连接数据库的 MVC 工程。明确要求关系数据库时，选择 JPA 和 PostgreSQL 组合。
2. 运行生成器。它向 Spring Initializr 请求固定版本的 Maven 工程和 wrapper，在临时目录解包并应用模板。检查生成的构建文件、入口类和 wrapper 后，再将工程放到目标目录。
3. 有持久化需求时，按模型补上首个 Flyway 脚本。生成骨架时尚无业务模型的情况下，待实现首个实体时再创建对应 schema。数据库连接由本地配置提供。涉及真实数据库行为的测试使用 Testcontainers 或任务已指定的等价环境。
4. 用 wrapper 验证生成项目。交付信息包含启动方式、实际运行的检查和结果。需要 Docker 的检查无法运行时，记录原因。

生成器的计划接口如下，参数名与退出行为在第一阶段固定。

```bash
node scripts/create-project.mjs \
  --name orders-api \
  --group-id com.example \
  --output-dir /path/to/orders-api \
  --persistence jpa-postgresql
```

`--output-dir` 表示最终项目目录，必须尚不存在。`--persistence` 接受 `none` 或 `jpa-postgresql`，默认 `none`。脚本在发布输出前再次确认目标仍可用。版本不可用、下载失败、归档包含越界路径或生成结果缺文件时，命令返回非零退出码，清理临时文件。生成器本身与构建验证可以分别运行。

### 修改已有项目

1. 检查项目规则、构建文件与 wrapper，确认实际使用的 Boot、Java 和构建工具。检查目标模块以及最近似的控制器、服务、持久化代码和测试。
2. 依据用户明确要求和项目的可执行规则确定改动边界。项目约定和示例提供实现方式，技能默认值用于填补空缺。遇到约束冲突时，先说明冲突及其对当前功能的影响，再决定是否需要用户选择。
3. 针对当前行为实现改动。修改已有 API 时，核对请求和响应字段、HTTP 状态码、错误格式及调用方依赖。修改数据库时，考虑已有数据、事务边界和约束。涉及授权时，确认当前项目在哪里验证角色、对象归属或租户范围。
4. 用项目的 wrapper 运行与行为最贴近的测试。当前任务跨越 HTTP、持久化和权限等多个边界时，再运行代表性的集成检查。交付时列出代码行为和实际检查结果。

## 4. 参考文件怎么写

每份参考文件围绕代理需要做的决定编写。先写什么情况触发规则，再写代码放在哪一层、复用哪些项目约定，最后给出可以观察到的验收结果。版本相关的 starter、测试模块和 import 用对应 Boot 4 版本的官方文档与可构建示例核对。

| 参考文件 | 需要回答的问题 |
| --- | --- |
| `bootstrap.md` | 如何选择骨架能力和版本。生成器具体产出什么。如何启动服务、配置数据库、初始化 schema 和运行首次验证。 |
| `architecture.md` | 怎样从现有包和测试识别架构。怎样选择模块公共接口。什么时候加依赖约束，怎样用 ArchUnit 检查。 |
| `web-mvc.md` | 请求 DTO 如何校验，成功与失败如何响应，现有接口契约如何保持。示例覆盖创建接口以及给已有接口增加可选字段。 |
| `data-jpa.md` | 事务从哪里开始，何时使用数据库唯一约束，如何处理既有数据，以及如何用真实数据库验证写入、查询和约束。 |
| `security.md` | 如何识别项目现有认证和授权入口，在哪里验证角色、数据归属或租户范围，怎样验证允许与拒绝两条路径。 |
| `testing.md` | 如何在单元、切片、集成和架构测试之间选择。每类测试验证哪种行为，需要哪些运行条件。 |

`data-jpa.md` 的规则可以这样写。

> 当多个写入必须一起成功或回滚时，由负责该操作的应用服务定义事务边界。如果订单号必须唯一，在 schema 中建立唯一约束。应用层可以提前查询以改善错误提示，并发写入由数据库约束保证。测试应触发实际 SQL 执行，并验证项目约定的冲突响应。

架构参考使用同一订单示例。`order` 功能拥有订单持久化模型，其他功能通过订单公开的应用接口读取所需数据。架构测试同时检查两种情况。跨功能调用公开接口通过，直接引用订单 Repository 的测试夹具触发失败。若现有项目采用六边形架构，另按该项目的分层条件检查领域包是否依赖 Spring 或 JPA。

## 5. 实施顺序与验收

| 阶段 | 交付内容 | 完成条件 |
| --- | --- | --- |
| 1. 骨架 | 技能入口、版本清单、生成器和 `bootstrap.md` | 生成的最小 Maven 项目通过 `./mvnw -B verify`，并能启动。已有目标目录和无效参数返回明确错误，目标内容不受影响。 |
| 2. 纵向示例 | MVC、JPA、架构和测试参考，订单示例和 PostgreSQL 模板 | `POST /orders` 的成功、非法输入、重复订单号行为可验证。用真实 PostgreSQL 验证唯一约束。架构规则能识别故意违规的夹具。 |
| 3. 已有项目与权限 | 已有功能修改示例和 `security.md` | 在一个独立的 Boot 4 项目增加功能，原有 HTTP 契约继续通过。已有授权策略下，有权限与无权限的请求分别得到预期结果。 |
| 4. 技能验证 | 技能结构检查、可重复任务和结果记录 | 每条入口能找到所需参考文件。固定输入比较启用技能和未启用技能的产物，保留命令、结果与未运行原因。 |

技能验证与应用测试分开。应用测试判断生成代码是否正确。技能验证判断代理是否选对技术栈、保留项目约定、使用正确的参考文件，并如实报告验证结果。对照运行使用同一模型、任务、初始工程和工具权限，分别记录以下结果。

- 空工程能否构建和启动。无数据库任务生成的项目能否在没有 Docker 的环境完成 `verify`。
- 已有接口改动是否保留以前的响应和权限行为，是否增加了任务无关的依赖或构建配置改动。
- 架构检查是否覆盖目标包。故意加入违规依赖后检查是否失败，恢复合法写法后是否通过。
- 真实 PostgreSQL 的写入与约束是否经过执行。Docker 不可用时，是否明确记录数据库检查未运行。

已有项目的对照实测见[取消库存预留对照报告](spring-boot-4-cancellation-ab.zh-CN.md)、[旧订单数据迁移对照报告](experiments/spring-boot-4-legacy-migration/README.md)、[CORS 与资源授权对照报告](experiments/spring-boot-4-cors-authorization/README.md)和[可选备注对照报告](experiments/spring-boot-4-optional-note/README.md)。

## 参考来源

- Spring Boot 的版本与运行条件、项目生成接口、测试支持以官方资料为准。 [Spring Boot 系统要求](https://docs.spring.io/spring-boot/system-requirements.html) · [Spring Initializr](https://docs.spring.io/initializr/reference/) · [Spring Boot 测试](https://docs.spring.io/spring-boot/reference/testing/index.html)
- 现有仓库可用于对照生成器与模板的职责，以及真实应用样例的验证方式。 [现有 Spring Boot 技能设计](../spring-boot-developer/docs/skill-design.md) · [现有验证样例](../samples/spring-boot-skill-validation/README.md)
