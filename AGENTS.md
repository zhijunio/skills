# AGENTS.md

## 仓库定位

本仓库是面向 AI Coding Agent 的可复用软件工程 Skills 库，覆盖产品发现、需求分析、规格驱动开发、软件工程实践及 Spring Boot 开发。

优先保证技能的正确性、可组合性、可维护性和可验证性，而非技能数量。

## 目录结构

| 路径 | 职责 |
| --- | --- |
| `engineering/` | 通用软件工程技能及工作流 |
| `spring-boot-skills/` | Spring Boot 专项技能 |
| `collected-skills/` | 收集的通用技能 |
| `docs/` | 项目规范、规划、审计及验证文档 |
| `docs/prompts/` | 可复用 Prompt |
| `scripts/` | 自动化验证工具 |
| `.github/workflows/` | CI、构建及发布流程 |
| `pom.xml` | Maven 打包配置 |

以实际文件和配置为准，不维护重复的技能清单。

## 工作规范

开始相关任务前，按需阅读：

- [Skill 设计原则](docs/skill-design-principles.md)
- [Skill 开发指南](docs/skill-authoring-guide.md)
- [标准与兼容性](docs/skill-compatibility.md)
- [技能来源登记](docs/skill-provenance.md)
- [分发验证报告](docs/verification/distribution.md)

遵循以下规则：

- 优先采用、改造或组合现有开源技能，再考虑自主开发。
- 修改前检查现有技能的职责、触发条件及依赖关系。
- 保持单一职责，避免新增能力重叠的技能。
- 使用渐进式披露，按需加载参考资料。
- 保留已有业务决策、技术选型及文档契约。
- 对版本敏感的技术指导，优先参考官方资料及验证证据。
- 仅修改当前任务所需的文件，不进行未经请求的重构。

## Skill 规范

遵循 Agent Skills 开放规范和仓库设计原则。

- 每个技能包含 `SKILL.md`，技能名称与目录名称一致。
- `description` 明确技能能力、触发条件及必要的排除条件。
- 明确输入、输出、执行边界、依赖和完成标准。
- 保留必要的平台专属配置及调用限制。
- 确保引用文件在支持的安装布局中可访问。
- 不假设单独安装一个技能会自动安装其全部依赖。
- 不创建无实际用途的目录或辅助文件。

新增技能集合时，同步检查 Maven 打包、技能发现及自动化验证配置。

## 来源与许可证

导入或改造第三方 Skills 时：

- 记录上游仓库、原始路径、固定 Commit、许可证和本地修改。
- 保留原始 `LICENSE`、`NOTICE` 及版权声明。
- 按开发指南维护 `SOURCE.md` 和来源登记。
- 区分原创技能、第三方技能及改造后的技能。
- 不推断缺失的许可授权，不伪造版权信息。
- 未解决的再分发授权问题必须阻止相关内容发布。

## 构建与验证

Maven 构建使用 JDK 17，优先使用仓库提供的 Maven Wrapper。

```bash
./mvnw clean verify -B -ntp
```

验证技能元数据。在隔离的 Python 环境中安装已固定版本的官方参考实现：

```bash
python -m pip install 'skills-ref @ git+https://github.com/agentskills/agentskills.git@83021e2be457a9b6679148719c1f6074f0ad975e#subdirectory=skills-ref'

python scripts/validate-skill-metadata.py
```

运行自动化验证工具的测试：

```bash
python -m unittest discover -s scripts -p 'test_*.py'
```

验证 JAR 技能及资源完整性：

```bash
python scripts/verify-skill-jar.py target/skills-0.1.0-SNAPSHOT.jar
```

版本变化后使用实际构建产物路径。

验证技能发现：

```bash
npx --yes skills@1.9.1 add "$PWD" --list
```

完整安装及提取验证参考 [Skill Verification CI](.github/workflows/skill-verification.yml)。

根据变更范围选择相关检查。区分静态校验、分发验证和实际 Agent 行为验证。记录未运行的检查及原因，不将未执行或失败的验证标记为通过。

## Git 与 Pull Request

- 默认目标分支为 `main`。
- 优先使用独立分支和 Pull Request 审查实质性修改。
- 每次提交聚焦一个逻辑变更。
- 使用清晰的提交信息，遵循已有 Conventional Commits 风格。
- 保留工作区中的用户修改，不擅自覆盖、回退或删除。
- PR 说明修改目的、变更范围、验证结果及剩余风险。
- 同步更新受到影响的文档、技能索引和来源记录。

## 安全与发布

- 不提交凭证、Token、私密配置或用户数据。
- 不绕过系统安全机制、Agent 权限或访问控制。
- 不擅自执行破坏性操作或修改全局 Agent 配置。
- 未经授权，不执行 Maven deploy、正式发布或其他外部状态变更。
- 注意 `main` 分支提交会触发 Snapshot 发布工作流。
- 不通过合并发布分支来代替本地或 PR 验证。
- 许可证、发布权限或必要验证存在阻塞时，不进行发布。

## 完成标准

任务结束前：

1. 检查修改是否符合用户要求及技能设计原则。
2. 检查是否存在无关变更或失效引用。
3. 执行与修改范围相关的验证。
4. 同步受到影响的文档及来源记录。
5. 如实报告修改内容、验证结果、未解决问题及限制。

不得将代码或文档已编写等同于验证完成。
