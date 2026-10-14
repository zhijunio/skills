# Spring Boot Full-Stack Developer 设计方案

## 目标

创建一个面向实际交付的 Spring Boot Agent Skill：既能按用户约束生成可运行项目，也能在现有项目中实现和扩展业务功能。生成脚手架、版本选择、前后端集成和验证应形成一条可复现的路径。

本方案把项目生成、技术指导和验证闭环放在一个目录内。它可以独立复制到支持 Agent Skills 的环境中使用；不要求安装其他技能、仓库级辅助文件或某个特定插件。

## 设计原则

1. **脚本做确定性工作，Agent 做需求判断和业务实现。** 项目下载、模板应用、版本检查等重复操作由脚本完成；领域模型、API 和页面依据用户需求实现。
2. **区分新建与修改。** 新项目走生成流程；现有项目先检查构建配置、版本、架构和约定，再以最小改动接入功能。
3. **尊重用户和项目已有选择。** 不将默认技术栈当作要求。已有项目优先沿用其 Java、Boot、Maven/Gradle、数据库和前端方案。
4. **版本选择有依据且可追溯。** 版本清单集中管理；兼容性说明来源和验证状态，不无条件选择“最新版”。
5. **验证结果必须可见。** 记录实际执行的构建、lint、单元测试和集成测试，以及未执行的检查和原因。
6. **按需加载细节。** 主 Skill 保持精简；技术配方、脚手架选项和故障处理放到 references。

## 文档写作约定

- 从用户任务出发命名章节，优先使用“生成项目”“添加 CRUD”“运行验证”等动作标题。
- Spring Boot 框架知识按领域拆成聚焦的参考文件，并在 `SKILL.md` 中为每类任务给出明确入口；生成、前端、容器和部署操作各自归组。
- 先给推荐做法和适用条件，再列步骤、命令、结果和常见失败处理。
- 使用短段落、可复制命令和具体文件路径；避免空泛的最佳实践口号。
- 对版本、行为和兼容性结论标注官方来源或实测环境；未知项明确写为未验证。
- 记录容易踩到的实际问题及其诊断方法，使后续任务能直接采取行动。
- references 可以链接本目录内的其他文件和官方一手文档；不得依赖其他技能、外部提示词包或未随目录交付的模板。

## 触发场景

适用于用户要求：

- 创建 Spring Boot 项目或服务；
- 为 Spring Boot 项目增加业务功能，例如 REST CRUD、持久化、Vue 前端或 Docker 支持；
- 扩展或调整已有 Spring Boot 项目结构；
- 询问如何运行、构建或验证由本 Skill 管理的项目。

对大型或跨领域的行为变更，先在项目文档或任务记录中定义需求、范围和可验证验收标准，再拆成可审查的实现切片。本 Skill 自己包含该流程所需的工作指导。

## 工作流

### 1. 分类任务

识别为新建项目、现有项目功能开发、故障修复或技术咨询，并只加载该分支所需的 references。

### 2. 检查约束

检查 `AGENTS.md`、README、构建文件、应用配置、现有代码和测试。确定 Spring Boot、Java、构建工具、数据库、前端、部署目标和验证命令。对现有项目先遵循已有约定；关键决策无法从仓库推断时再向用户确认。

### 3. 新建项目

使用脚本生成 Spring Initializr 骨架，输出到用户指定的工作目录。根据需求选择后端、数据库、前端和容器化选项；不因模板提供某项能力就自动加入不需要的依赖。生成后应用选定的模板/配置，并给出启动方式。

### 4. 实现功能

按业务功能组织代码。例如产品目录 CRUD 可形成 `product/` 功能包，包含实体、请求/响应 DTO、Repository、Service、Controller 和局部错误处理。前端按 `services/`、`stores/`、`views/` 等边界组织 API 调用、状态和页面。

遵循项目现有 Spring 版本和编码规范。新增 API 应明确校验、状态码、错误格式和持久化行为；数据库变更遵循项目迁移机制。

### 5. 验证与交付

按技术栈运行相关检查：

- Java 编译和 Maven/Gradle 构建；
- 前端构建和 lint（若项目有前端）；
- Web 层或业务单元测试；
- 涉及 JPA/数据库时，使用 Testcontainers 或项目既有真实数据库集成测试；
- Docker Compose 配置检查；容器构建或启动仅在任务需要且环境可用时执行。

报告新增内容、运行命令与结果、未验证项目及其原因。不要把未执行的验证描述为通过。

## 当前目录

```text
spring-boot-developer/
├── README.md
├── SKILL.md
├── versions.json
├── scripts/                 # project generation, environment checks, doc sync
├── assets/                  # Docker, Compose, editor, CI and project templates
├── references/              # grouped by architecture, web, persistence, testing, etc.
└── workshop/                # prompt-driven CRUD workshop and appendices
```

`SKILL.md` owns task routing and the end-to-end workflow. Topic references own technical guidance; scripts and assets own repeatable project setup. The README is the human entry point, and the Workshop demonstrates the workflow on a generated application.

## 版本与验证记录

`versions.json` 作为脚本和模板的版本数据源。每项版本或兼容性结论在文档中标记证据级别：

- **已运行验证**：明确记录操作系统/工具版本及成功的命令；
- **官方文档确认**：有对应官方文档或发布说明依据；
- **未验证**：保留限制，避免把推断写成实测结论。

兼容矩阵至少覆盖 Spring Boot 与 Java 的支持关系、Maven/Gradle 选择、PostgreSQL、前端 Node/npm 版本和容器构建输入。默认值由兼容矩阵与用户约束共同决定；发现既有版本时不得静默升级。

## 技术边界

- 默认 REST API 使用 DTO 和 Bean Validation；HTTP 状态表达成功/错误语义，错误响应采用 RFC 9457 `ProblemDetail`，除非项目已有 API 契约要求其他格式。
- 持久化优先使用项目已选的数据访问方式。PostgreSQL 场景遵循迁移、索引、事务和连接池的既有指导，不把 Hibernate 自动建表作为生产迁移策略。
- Spring Security 按需加入，明确认证、授权和 CORS 边界；不为了“全栈模板”默认引入认证。
- Vue 场景提供 Vite 开发代理、生产静态资源集成、API service 层和必要的路由回退；前端版本由版本清单管理。
- Docker 支持区分本地开发数据库 Compose 与包含应用镜像的完整栈 Compose，命令明确指定配置文件，避免同目录多 Compose 文件时选错配置。
- JDTLS/LSP 属于开发工具增强项，不作为应用运行或构建的前置条件。

## 独立性

- `SKILL.md`、references、脚本、模板和练习均位于本目录。
- 所有工作分支都能在本目录找到完整的执行步骤，不要求加载其他技能。
- 只链接官方框架、工具和协议文档作为外部事实来源；运行流程和操作步骤保留在本目录。
- 不依赖仓库内其他技能的隐含约定、命令或文件布局。

## 交付范围

当前技能覆盖项目生成、Boot 4 与 Boot 3.5.x 指导、Maven/Gradle、JPA/MyBatis-Plus/无持久化选项、PostgreSQL、Vue/React/Next.js/Angular/原生 JavaScript 集成、BootUI 本地开发诊断、测试、安全、观测、Docker、Native Image、Azure 及提示词驱动 Workshop。Next.js 指南说明静态导出托管和独立 Node.js 运行时两种集成方式。BootUI 参考说明仅开发环境集成与生产隔离。版本和默认值由 `versions.json` 管理；具体能力以 `SKILL.md`、脚本帮助和实际文件为准。

## 完成标准

- 新用户可按文档生成项目并在目标工作目录启动；
- 新生成项目的 Java 和 Vue（若启用）构建成功；
- PostgreSQL 开发配置可通过 Compose 检查，真实数据库测试可执行；
- CRUD Workshop 覆盖创建、读取、更新、删除、校验错误和缺失资源响应；
- 每个模板/版本说明带有验证状态，Skill 不声称未实测的路径已验证；
- 主 `SKILL.md` 精简，并且所有文件引用都能解析。
