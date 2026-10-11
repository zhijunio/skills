# Maven

本文介绍 Spring Boot 项目的父 POM、Starter、插件、多模块、可重复构建和依赖卫生。提交 Maven Wrapper，并让开发者与 CI 使用相同的 Wrapper 命令。

## Parent POM

Spring Boot Parent 提供合理的插件默认值和依赖管理。如果项目不能继承它（例如已有企业 Parent），导入 `spring-boot-dependencies` BOM，并显式配置需要的插件。不要同时以互相冲突的方式管理依赖版本。

## Starters

用 Starter 表达应用实际使用的能力。依赖版本交给 Boot BOM；仅在 BOM 未管理时显式锁定版本。Boot 4 对 Starter 做了拆分，升级后应核对名称和传递依赖。

## Boot 插件

`spring-boot-maven-plugin` 用于重打包生成可执行 JAR、运行应用和通过 Buildpacks 构建镜像。普通库模块不应重打包成可执行应用。将插件绑定到合适的构建阶段，并让 CI 执行 `verify`。

## 值得配置的插件

### 配置元数据

添加 `spring-boot-configuration-processor`（optional），为 `@ConfigurationProperties` 生成 IDE 自动补全和文档。

### 测试覆盖率

使用 JaCoCo 等工具收集覆盖率，并在 CI 生成报告。覆盖率指标应配合测试质量审查，不应只为达到百分比而添加无意义断言。

### 格式化

使用 Spotless 或项目选定的格式化插件，固定版本和规则，并将格式检查纳入 `verify`。这样本地和 CI 执行一致。

### 构建来源信息

使用 `spring-boot-maven-plugin` 的 `build-info` 目标生成 `BuildProperties`，并在需要时生成 Git 信息。Actuator `/info` 可据此显示部署制品版本，方便确认正在运行的提交。

## 多模块

聚合 Parent 管理公共属性、依赖版本和插件；子模块表达清晰的领域或部署边界。不要只为目录整齐拆模块，也不要让多个模块形成循环依赖。应用模块负责可执行制品，库模块保持普通 JAR。

## 可重复构建与卫生

- 使用 Wrapper 并固定 Maven 版本。
- 固定构建插件版本，避免插件升级导致构建行为漂移。
- 不在 POM 中写凭据；使用环境变量或 CI 秘密管理。
- 清理不再需要的依赖，检查传递依赖和重复版本。
- CI 使用干净环境执行相同的 `./mvnw verify` 流程。

## 使用 Spring Boot 3.5.x 时

使用 3.5.x 对应的 Starter 名称、插件和 BOM。Boot 4 模块化后 Starter 名称与依赖边界有变化；不要将 4.x 依赖清单不加核对地复制到 3.5.x 项目。

## 常见问题

- 不使用 Maven Wrapper，导致本地和 CI 的 Maven 版本不同。
- 同时继承 Boot Parent 并以冲突方式导入其他 BOM。
- 手动锁定所有 Spring 依赖版本，破坏 Boot BOM 的兼容组合。
- 普通库模块执行 Boot 重打包。
- 配置处理器进入运行时依赖；它应为 optional。
- CI 只运行 `test`，没有执行完整 `verify` 生命周期。
- 插件版本不固定，构建随时间漂移。
- POM 或提交文件中包含秘密。
- 直接提交生成的源码，而不是让构建产生它。

## 相关文档

- [Gradle](gradle.zh-CN.md) · [配置](configuration.zh-CN.md) · [容器化与 Native Image](containerization-and-native.zh-CN.md) · [可观测性](observability.zh-CN.md)
