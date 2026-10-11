# Gradle

这是 [maven.zh-CN.md](maven.zh-CN.md) 的 Gradle 对应指南。Maven 文件说明决策依据；本文件介绍 Kotlin DSL 语法和 Gradle 特有的注意事项。

新项目使用 **Kotlin DSL**（`build.gradle.kts`）：它提供类型安全的访问器、完整的 IDE 补全，并能在配置阶段而不是执行任务时发现错误。

## 插件与依赖管理

```kotlin
plugins {
    java
    id("org.springframework.boot") version "<current 4.x>"
    id("io.spring.dependency-management") version "<current>"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}
```

关键在于 **Toolchain**。它固定用于编译代码的 JDK，必要时会自动下载，因此不同开发机和 CI Agent 使用相同版本。`sourceCompatibility` 只设置字节码级别，实际编译 JDK 仍取决于机器；java-developer 也强调了这一点。

`io.spring.dependency-management` 会应用 Boot BOM，所以下面可以省略依赖版本。也可以使用 Gradle 原生 Platform 支持：

```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:<current 4.x>"))
}
```

两种方式都可用，不要同时使用。

## 依赖

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

[maven.zh-CN.md](maven.zh-CN.md) 中的 Starter 对照表同样适用，包括 **Boot 4 不会传递引入 Flyway** 这一点。

应正确选择依赖配置；Gradle 与 Maven 的最大区别就在这里：

| 配置 | 含义 |
|---|---|
| `implementation` | 编译和运行均需要，但不暴露给消费者 |
| `api` | 编译需要，且会暴露给消费者（仅 `java-library` 插件适用） |
| `runtimeOnly` | 仅运行时需要，例如 JDBC Driver、Flyway 数据库模块 |
| `compileOnly` | 仅编译时需要 |
| `annotationProcessor` | 在编译时运行 |
| `testImplementation` / `testRuntimeOnly` | 对应的测试配置 |

所有内容都放入 `implementation` 虽然能工作，却会拖慢增量构建；库中使用 `api` 会将依赖泄漏到每个消费者的编译 Classpath，因此应审慎使用。

## 测试

```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}
```

必须调用 `useJUnitPlatform()`；否则 Gradle 会查找 JUnit 4，并报告“未找到测试”，实际上所有测试都会被静默跳过。这是 Gradle 与 Spring Boot 项目集成时最常见的错误。

`exceptionFormat = FULL` 会在 CI 输出完整堆栈跟踪，因此可以直接从日志诊断失败，而不必先在本地复现。

## Boot 插件

```kotlin
springBoot {
    buildInfo()
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName = "${System.getenv("DOCKER_REGISTRY")}/${project.name}:${project.version}"
}
```

`buildInfo()` 会生成 Build 元数据，Actuator 会通过 `/actuator/info` 暴露（参见[可观测性](observability.zh-CN.md)）。

`bootBuildImage` 使用 Buildpacks 构建 OCI 镜像，不需要 Dockerfile（参见[容器化与 Native Image](containerization-and-native.zh-CN.md)）。

## 覆盖率与格式化

```kotlin
plugins {
    jacoco
    id("com.diffplug.spotless") version "<current>"
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        formatAnnotations()
        palantirJavaFormat()
    }
}
```

必须设置 `tasks.check { dependsOn(...) }`。与 Maven 的阶段绑定不同，Gradle 验证任务如果没有依赖关系就不会运行。只配置 JaCoCo 规则却未将它接入任务图，是一种常见的虚假安全感。

[maven.zh-CN.md](maven.zh-CN.md) 中关于把覆盖率作为下限而非目标的说明同样适用。

## 多项目构建

```kotlin
// settings.gradle.kts
rootProject.name = "my-app"
include("domain", "application", "infrastructure", "web")
```

```kotlin
// build.gradle.kts (root)
plugins {
    id("org.springframework.boot") version "<current 4.x>" apply false
    id("io.spring.dependency-management") version "<current>" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    the<DependencyManagementExtension>().imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}
```

```kotlin
// domain/build.gradle.kts - pure Java, no Spring
dependencies { }

// web/build.gradle.kts - the runnable application
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
}
```

在根项目使用 `apply false`，再只对可运行模块应用 Boot 插件；这相当于 Maven 中“Boot 插件只放在可运行模块”。如果对所有子项目应用插件，Gradle 会尝试把库模块也构建成可执行 JAR。

Gradle 会强制执行构建文件声明的依赖方向，但不会替你验证设计。使用 ArchUnit 检查（参见 [archunit.zh-CN.md](archunit.zh-CN.md)）。

## 构建性能

Gradle 相比 Maven 的优势是增量构建和缓存；应启用它们：

```properties
# gradle.properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configuration-cache=true
```

配置缓存对构建脚本在配置阶段读取系统状态有严格要求。迁移通常很小，但在多项目构建中收益很大。

**提交 Gradle Wrapper**（`gradlew`、`gradlew.bat`、`gradle/wrapper/`），确保所有人使用相同的 Gradle 版本。检查 `gradle/wrapper/gradle-wrapper.properties` 中的分发 URL 使用 HTTPS。

## Version Catalog

多项目构建应将版本集中管理：

```toml
# gradle/libs.versions.toml
[versions]
springBoot = "<current 4.x>"
testcontainers = "<managed by the BOM - omit>"

[libraries]
postgresql = { module = "org.postgresql:postgresql" }

[plugins]
springBoot = { id = "org.springframework.boot", version.ref = "springBoot" }
```

```kotlin
dependencies {
    runtimeOnly(libs.postgresql)
}
```

Version Catalog 类型安全、易于重构，且集中记录版本。Boot BOM 管理的依赖不要添加版本；带显式版本的 Catalog 条目会静默覆盖 BOM，而避免此问题正是 BOM 的用途之一。

## 使用 Boot 3.5.x 时

结构相同。差异与 Maven 一致：Starter 名称不同、Flyway 会传递引入、使用 `spring-boot-starter-test` 而非按技术拆分的 `-test` Starter，以及 Testcontainers 使用 1.x 坐标（参见 [testcontainers.zh-CN.md](testcontainers.zh-CN.md)）。

## 常见问题

- Agent 忘记 `useJUnitPlatform()`：所有测试会被静默跳过，构建仍显示成功。
- Agent 配置 JaCoCo 验证，却没有将其接入 `check`：规则不会运行。
- Agent 对每个子项目应用 Boot 插件：库模块无法重打包为可执行程序。
- Agent 使用 `sourceCompatibility` 而不是 Toolchain：编译 JDK 会随机器变化。
- Agent 将 JDBC Driver 放在 `implementation`：应使用 `runtimeOnly`。
- Agent 在库中对所有依赖使用 `api`：依赖会泄漏到每个消费者的编译 Classpath。
- Agent 为 BOM 管理的依赖添加版本，包括 Version Catalog 中的依赖：版本应由 BOM 决定。
- Agent 同时应用 `io.spring.dependency-management` 和 `platform(...)` BOM：二选一。
- Agent 未提交 Gradle Wrapper：不同机器上的构建版本不一致。
- Agent 在 Boot 4 的 `-test` Starter 旁又添加 `spring-boot-starter-test`。
- Agent 在 Boot 4 忘记 `spring-boot-starter-flyway`：迁移会静默停止运行。

## 相关文档

- [Maven](maven.zh-CN.md) · [Boot 版本](boot-versions.zh-CN.md) · [容器化与 Native Image](containerization-and-native.zh-CN.md) · [Testcontainers](testcontainers.zh-CN.md) · [ArchUnit](archunit.zh-CN.md)
