# Gradle

本文整理 Spring Boot 项目使用 Gradle 时的依赖管理、测试、插件、多项目构建和性能约定。Gradle Wrapper 应提交到仓库，团队和 CI 使用同一版本与命令。

## 插件与依赖管理

使用 Spring Boot Gradle 插件和依赖管理插件，让 Spring Boot BOM 管理依赖版本。应用项目通常使用 `org.springframework.boot` 与 `io.spring.dependency-management`，避免为 Spring 生态依赖分别指定版本。

版本目录 `gradle/libs.versions.toml` 适合管理非 BOM 依赖；不要在多个 `build.gradle` 文件中重复写同一版本。插件版本、Java toolchain 和 Wrapper 版本都应明确。

## 依赖

- 使用 Spring Boot Starter 表达应用能力，例如 Web、JPA、Validation 和 Actuator。
- `implementation` 用于生产代码依赖；仅运行时需要的组件用 `runtimeOnly`；测试专用依赖放在 `testImplementation` / `testRuntimeOnly`。
- 不要把 Devtools、Docker Compose 支持或测试容器泄漏到生产运行时。
- 依赖版本优先由 Boot BOM 或版本目录统一管理，避免局部覆盖产生不兼容组合。

## 测试

Boot 4 的测试依赖按模块拆分。只添加需要的测试 Starter，例如 Web MVC 测试；不要同时添加完整测试 Starter 和拆分 Starter。配置 JUnit Platform，并根据测试类型分离快速单元测试与集成测试任务。

## Boot 插件

`bootJar` 负责生成可执行 Spring Boot JAR，`bootRun` 用于本地运行，`bootBuildImage` 使用 Buildpacks 构建 OCI 镜像。普通库模块不应应用 Boot 插件；仅应用模块需要可执行制品。

## 覆盖率与格式化

将 JaCoCo、格式化和静态检查纳入标准构建任务，并在 CI 执行。格式化任务应有一致的版本和配置，避免开发者本机与 CI 的结果不同。不要只依赖 IDE 自动格式化。

## 多项目构建

通过根项目集中管理插件和公共配置；子项目只声明自己的职责和依赖。按业务模块拆分，而非为了拆分而拆分。依赖方向应清楚，避免循环依赖。共享逻辑可放在 convention plugin 中，避免根构建脚本膨胀。

## 构建性能

- 使用配置缓存和构建缓存前，确保插件兼容并验证 CI 行为。
- 避免配置阶段执行外部命令或访问网络。
- 使用 Gradle Daemon、并行构建和增量任务；不要为追求速度而牺牲可重复性。
- 固定 Wrapper 发行版本及校验和；CI 使用 Wrapper，而不是机器预装 Gradle。

## `gradle.properties`

把可复用的 Gradle 属性放在 `gradle.properties`，例如 JVM 参数和并行执行设置。不要在其中存秘密。CI 与本地参数应保持可预期，避免过度分配内存导致构建机 OOM。

## 版本目录 `gradle/libs.versions.toml`

版本目录集中声明版本、依赖别名和插件别名，使构建文件保持简洁。Spring Boot BOM 已管理的库不应再重复锁定版本；仅为 BOM 未管理的工具显式添加版本。

## 使用 Spring Boot 3.5.x 时

遵循该版本的 Starter 与插件配置。Boot 4 将测试 Starter 拆分，并调整了一些模块和依赖坐标；升级时应按项目实际依赖更新，而不是把 Boot 4 配置原样复制到 3.5.x。

## 常见问题

- 不提交 Gradle Wrapper，或 CI 使用系统 Gradle。
- 每个依赖单独写版本，忽略 Boot BOM。
- 对普通库模块应用 Boot 插件并生成可执行 JAR。
- 测试依赖范围过宽，导致测试框架进入生产 classpath。
- Boot 4 同时添加完整测试 Starter 和拆分测试 Starter。
- 把 Docker Compose、Devtools 或 Testcontainers 当作生产依赖。
- 多项目构建把所有业务耦合在根 `build.gradle`。
- 启用缓存和并行后不在 CI 验证可重复性。
- 在 `gradle.properties` 或版本目录中存放凭据。

## 相关文档

- [Maven](maven.zh-CN.md) · [版本与升级](boot-versions.zh-CN.md) · [容器化与 Native Image](containerization-and-native.zh-CN.md) · [测试策略](testing-strategy.zh-CN.md)
