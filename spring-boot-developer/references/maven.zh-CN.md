# Maven

本技能的其余内容与构建工具无关。本文件和 [gradle.zh-CN.md](gradle.zh-CN.md) 是本技能中说明构建配置的两个位置。

## Parent

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version><!-- current 4.x --></version>
    <relativePath/>
</parent>

<properties>
    <java.version>21</java.version>
</properties>
```

Parent 提供依赖管理、插件管理、合理的编译器设置和资源过滤。如果组织已有企业级 Parent，可改为导入 BOM：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version><!-- current 4.x --></version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

这样会失去插件管理，因此需要为 `spring-boot-maven-plugin` 声明版本。

**对于 BOM 管理的依赖不要声明版本**，例如 Hibernate、Jackson、Testcontainers、JUnit、Micrometer 及其他数百个依赖。覆盖其中一个版本可能导致构建未发现、运行时才出现的 `NoSuchMethodError`。参见 [boot-versions.zh-CN.md](boot-versions.zh-CN.md)。

## Starters

每种技术使用一个 starter。Boot 4 重命名了其中一些；旧名称仍可解析，但已弃用。

| 需求 | Starter |
|---|---|
| Web / REST | `spring-boot-starter-webmvc`（之前为 `-web`） |
| JPA | `spring-boot-starter-data-jpa` |
| **Flyway** | `spring-boot-starter-flyway`：**Boot 4 不会传递引入**；不添加时迁移不会运行，且不会明显报错 |
| Validation | `spring-boot-starter-validation`：Web starter 不会引入它 |
| Security | `spring-boot-starter-security` |
| OAuth2 Resource Server | `spring-boot-starter-security-oauth2-resource-server`（之前为 `-oauth2-resource-server`） |
| AOP | `spring-boot-starter-aspectj`（之前为 `-aop`） |
| Actuator | `spring-boot-starter-actuator` |
| Web 测试 | `spring-boot-starter-webmvc-test` |
| 持久化测试 | `spring-boot-starter-data-jpa-test` |

Boot 4 的 `-test` starter 已包含所需测试依赖，**不要再同时添加 `spring-boot-starter-test`**。`spring-boot-starter-classic` 是迁移辅助方案，不是新项目的目标依赖。

## Boot 插件

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <image>
            <name>${docker.registry}/${project.artifactId}:${project.version}</name>
        </image>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>build-info</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

`build-info` 会生成 `META-INF/build-info.properties`，Actuator 会通过 `/actuator/info` 暴露它，使运行中的应用能够报告自己的准确版本（参见 [observability.zh-CN.md](observability.zh-CN.md)）。此配置几乎没有成本，却能帮助快速定位不少问题。

在多模块构建中，该插件**只应放在可运行模块中**。如果放在 Parent 中，它会尝试重打包每个模块，包括库模块。

**Boot 4 会从重打包后的 JAR 中排除 optional 依赖。**如果应用运行时依赖其中某个依赖，请设置 `<includeOptional>true</includeOptional>`。

## 值得配置的插件

以下配置都不是必需的；每项都用于解决特定问题。

### 使用 Surefire 执行测试

Maven Surefire 在 `test` 生命周期阶段运行单元测试和测试切片。Spring Boot Parent 会管理插件版本；如果只导入 Boot BOM，应通过项目统一的插件管理声明 Surefire 版本。仅在项目需要非默认行为时，才配置 `maven-surefire-plugin`。

使用默认的测试类名匹配规则（`Test*`、`*Test`、`*Tests`、`*TestCase`），并让依赖 Docker 的集成测试使用 Failsafe 命名约定，例如 `*IT`。这样 `./mvnw test` 可以快速运行且不依赖 Docker，而 `./mvnw verify` 可以运行完整测试套件。除非测试类名确有特殊需求，否则避免自定义 include/exclude 列表；过时的匹配规则可能导致测试被静默跳过。

```xml
<!-- 通常不需要额外配置。只有需要自定义时才声明插件。 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <failIfNoTests>true</failIfNoTests>
    </configuration>
</plugin>
```

`failIfNoTests` 适用于可运行应用模块，但聚合模块或有意不包含测试的模块可能需要不同策略。Surefire 只运行符合其匹配规则的测试；它不会配置 Failsafe，也不会将集成测试移到 `verify` 阶段。

```bash
./mvnw test
./mvnw test -Dtest=OrderServiceTest
./mvnw test -Dtest='OrderServiceTest#rejectsExpiredOrder'
./mvnw verify
```

参见 Apache 官方 [Surefire 指南](https://maven.apache.org/surefire/maven-surefire-plugin/)和项目的[测试策略](testing-strategy.zh-CN.md)。Testcontainers 和完整上下文测试应另行配置 Failsafe，并通过 `verify` 运行。

#### 可选：JUnit 5 树状输出

若希望控制台逐个显示测试结果，可选用 `maven-surefire-junit5-tree-reporter` 插件依赖；它不会进入应用运行时 classpath。固定一个与项目管理的 Surefire 版本兼容的 reporter 版本。以下示例使用 reporter `1.5.1`；升级前请查看 [Maven Central](https://central.sonatype.com/artifact/me.fabriciorby/maven-surefire-junit5-tree-reporter)。

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <dependencies>
        <dependency>
            <groupId>me.fabriciorby</groupId>
            <artifactId>maven-surefire-junit5-tree-reporter</artifactId>
            <version>1.5.1</version>
        </dependency>
    </dependencies>
    <configuration>
        <reportFormat>plain</reportFormat>
        <consoleOutputReporter>
            <disable>true</disable>
        </consoleOutputReporter>
        <statelessTestsetInfoReporter
                implementation="org.apache.maven.plugin.surefire.extensions.junit5.JUnit5StatelessTestsetInfoTreeReporter">
            <theme>ASCII</theme>
            <printStacktraceOnError>true</printStacktraceOnError>
            <printStacktraceOnFailure>true</printStacktraceOnFailure>
            <printStdoutOnSuccess>false</printStdoutOnSuccess>
            <printStderrOnSuccess>false</printStderrOnSuccess>
        </statelessTestsetInfoReporter>
    </configuration>
</plugin>
```

`reportFormat=plain`、树状 reporter 和 `consoleOutputReporter.disable` 应配套使用，避免输出重复或为空。排查依赖测试输出的故障时，可临时将成功用例的 stdout/stderr 设置为 `true`。将配置合并到现有 Surefire 插件块中，不要重复声明插件。Failsafe 不会继承 Surefire 的插件配置；只有集成测试也需要树状输出时，才在 Failsafe 中重复添加依赖和配置。参见 Apache 官方 [JUnit Platform / Surefire 扩展指南](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html#external-extensions-for-the-plugin)。

### 配置元数据

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

为 `@ConfigurationProperties` 类型生成 IDE 自动补全和文档（参见 [configuration.zh-CN.md](configuration.zh-CN.md)）。这是本文中成本最低的改进。

### 测试覆盖率

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution><goals><goal>prepare-agent</goal></goals></execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

覆盖率是**防止回归的底线**，不是追逐的目标。若团队只优化数字，就会编写只执行代码却不做断言的测试，覆盖率上升而测试套件质量下降。将门槛设在当前水平附近，再逐步提高。

### 格式化

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <configuration>
        <java>
            <importOrder/>
            <removeUnusedImports/>
            <formatAnnotations/>
            <palantirJavaFormat/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
            <phase>validate</phase>
        </execution>
    </executions>
</plugin>
```

这是构建时格式化工具，不是运行时依赖。将 `check` 绑定到构建，并在本地运行 `spotless:apply`；这样可以避免代码审查中反复讨论格式。任何一致的格式化工具都可以，价值在于统一使用，而非具体选择哪一个。

### 构建来源信息

```xml
<plugin>
    <groupId>io.github.git-commit-id</groupId>
    <artifactId>git-commit-id-maven-plugin</artifactId>
    <configuration>
        <failOnNoGitDirectory>false</failOnNoGitDirectory>
        <generateGitPropertiesFile>true</generateGitPropertiesFile>
        <includeOnlyProperties>
            <includeOnlyProperty>^git.branch$</includeOnlyProperty>
            <includeOnlyProperty>^git.commit.id.abbrev$</includeOnlyProperty>
            <includeOnlyProperty>^git.commit.time$</includeOnlyProperty>
        </includeOnlyProperties>
    </configuration>
    <executions>
        <execution><goals><goal>revision</goal></goals></execution>
    </executions>
</plugin>
```

它会在 `/actuator/info` 中提供准确的提交信息。`failOnNoGitDirectory=false` 可确保从源码归档构建时仍能成功。限制生成的属性；完整属性集包含提交者姓名和电子邮件，通常不应暴露在端点上。

## 多模块

```
my-app/
├── pom.xml                 (packaging: pom)
├── my-app-domain/          pure Java
├── my-app-application/     use cases - depends on domain
├── my-app-infrastructure/  JPA, HTTP clients - depends on domain
└── my-app-web/             the runnable Boot application
```

| 模块 | 可依赖 |
|---|---|
| `domain` | 无 |
| `application` | `domain` |
| `infrastructure` | `domain`、`application` |
| `web` | 所有模块 |

- 在 Parent 中使用 `<dependencyManagement>`，不要使用 `<dependencies>`；后者会将依赖放入每个模块的 classpath。
- 模块间依赖使用 `${project.version}`。
- `spring-boot-maven-plugin` 只放在可运行模块中。
- Maven 不会阻止你设计出循环依赖，只会在构建时阻止它。使用 ArchUnit 强制依赖方向（参见 [archunit.zh-CN.md](archunit.zh-CN.md)）。

**有明确需要时再采用多模块。**它是[代码组织](code-organization.zh-CN.md)分级体系中的第四级：边界约束更强，也会带来更多维护工作。在边界确实需要由构建强制执行之前，默认应使用单模块和按功能组织的包。

## 可重复构建与依赖卫生

- **提交 Maven Wrapper**（`mvnw`、`mvnw.cmd`、`.mvn/`）。这样开发者和 CI 会使用相同的 Maven 版本构建。
- CI 使用 `mvn verify`，而不是 `mvn install`；`install` 会写入共享的本地仓库，并让不同构建之间产生顺序依赖。
- 依赖版本出现意外时运行 `mvn dependency:tree`；`mvn dependency:analyze` 可发现声明后未使用、以及使用但未声明的依赖。
- 在 CI 中添加 `dependency-check` 或平台提供的扫描器。对典型 Spring 应用而言，包含已知 CVE 的依赖是最常见的安全问题。

## 使用 Boot 3.5.x 时

整体结构相同。差异包括 Starter 名称（`-web`、`-aop`、不带 `security-` 前缀的 `-oauth2-*`）、Flyway 会传递引入，因此不需要显式 Starter、使用 `spring-boot-starter-test` 而不是按技术拆分的 `-test` Starter，以及 optional 依赖默认会包含在重打包 JAR 中。

## 常见问题

- Agent 为 BOM 管理的依赖声明版本：应由 BOM 决定；覆盖版本可能导致运行时 `NoSuchMethodError`。
- Agent 添加 `spring-boot-starter-data-jpa` 并期望 Boot 4 自动运行 Flyway：应添加 `spring-boot-starter-flyway`。
- Agent 认为 Web Starter 会引入 Validation：应添加 `spring-boot-starter-validation`。
- Agent 在 Boot 4 的 `-test` Starter 旁又添加 `spring-boot-starter-test`：这些 `-test` Starter 已自包含。
- Agent 在 Boot 4 使用 `spring-boot-starter-web` / `-aop` / `-oauth2-resource-server`：名称已更改（旧名称仍可解析，但已弃用）。
- Agent 将 `spring-boot-maven-plugin` 放在 Parent POM 中：只应放在可运行模块。
- Agent 在 Parent 中使用 `<dependencies>` 而非 `<dependencyManagement>`：会将依赖添加到每个模块。
- Agent 在 Boot 4 中依赖运行时使用 optional 依赖：设置 `<includeOptional>true</includeOptional>`。
- Agent 将覆盖率门槛设得远高于当前覆盖率：团队会为了指标而优化测试数量。
- Agent 暴露完整的 git-commit-id 属性集：其中包含提交者姓名和电子邮件。
- Agent 在 CI 中运行 `mvn install`：应使用 `verify`。
- Agent 未提交 Maven Wrapper：不同机器上的构建版本会不一致。
- Agent 在构建边界尚不需要时就采用多模块：先使用按功能组织的包。

## 相关文档

- [Gradle](gradle.zh-CN.md) · [Boot 版本](boot-versions.zh-CN.md) · [代码组织](code-organization.zh-CN.md) · [容器化与 Native Image](containerization-and-native.zh-CN.md) · [可观测性](observability.zh-CN.md) · [配置](configuration.zh-CN.md)
