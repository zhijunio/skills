# 本地开发

目标是让新开发者克隆仓库后只需运行一条命令，就能获得连接真实数据库的可运行应用。Wiki 安装步骤、共享开发数据库、向同事索取凭据等做法都会让每位新成员花上一天，还会逐渐过时。

## Docker Compose 支持

满足以下前提时，Spring Boot 可以在应用启动流程中启动 `compose.yaml` 的服务并自动配置连接信息：运行时 classpath 中包含 `spring-boot-docker-compose` 依赖、已安装且可用 Docker Compose V2，并且 Boot 能从应用工作目录找到受支持的 Compose 文件。缺少任一条件（或禁用了 Compose 集成）时，启动应用**不会**自动启动数据库；应先手动运行 `docker compose up -d <服务名>`。README 应明确项目采用哪种模式，并保留手动启动命令作为备用方式。

启用 Compose 集成后，Boot 会为识别出的服务提供 `ConnectionDetails`，通常无需再配置 `spring.datasource.url`；手动设置的连接信息可能与发现的服务冲突。未启用该集成时，则应正常配置数据源，并自行启动 Compose 服务。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-docker-compose</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

`runtime` 和 `optional` 都很重要：依赖不能进入生产环境，因为生产服务应使用真实服务。

```yaml
# compose.yaml
services:
  postgres:
    image: 'postgres:18-alpine'
    environment:
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
      POSTGRES_DB: app
    ports:
      - '5432'

  redis:
    image: 'redis:7-alpine'
    ports:
      - '6379'

  mailpit:
    image: 'axllent/mailpit:v1.29'
    ports:
      - '1025'
      - '8025:8025'      # Web UI 固定端口，方便打开
```

Spring Boot 会按镜像名称识别容器，并像测试中的 `@ServiceConnection` 一样使用 `ConnectionDetails`（参见[Testcontainers](testcontainers.zh-CN.md)）。本地与测试使用同一套机制，因此可以自然保持一致。

**默认不要映射容器端口**（写 `- '5432'`，而不是 `- '5432:5432'`），只有需要外部客户端访问时才映射。Spring Boot 会读取映射端口；固定端口容易与本机已有 PostgreSQL 冲突。

**镜像标签应与生产环境一致。** 本地使用 PostgreSQL 18，生产部署 15，正是此配置要避免的环境分歧。

卷挂载目标也要匹配镜像版本。官方 PostgreSQL 18+ 镜像会在 `/var/lib/postgresql` 下使用带主版本号的数据目录；应按该镜像的建议把卷挂载到 `/var/lib/postgresql`，不要沿用旧版常见的 `/var/lib/postgresql/data`。不要假设新主版本镜像能直接使用旧主版本的数据卷；应按 PostgreSQL 升级流程操作，或为演示创建新卷。

```yaml
spring:
  docker:
    compose:
      lifecycle-management: start_and_stop   # 或 start_only，让容器保持运行
```

日常开发通常使用 `start_only` 更方便：应用重启时容器继续运行，可以保留数据并跳过等待启动。
默认的 `start_and_stop` 会随应用启动 Compose 服务，并在应用退出时停止服务。应有意识地选择：生命周期管理会影响本地数据库进程；停止容器不一定删除命名卷中的数据。若要手动只启动所需服务，可运行 `docker compose up -d postgres`；停止服务时运行 `docker compose down`（除非确实要删除数据，否则不要添加 `-v`）。

## Devtools

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

类发生变化时重启应用，并关闭模板和静态资源缓存。完整打包应用时会自动禁用，重打包 JAR 时也会排除；但仍应设置 `optional`，避免污染其他模块的 classpath。

Devtools 是**本地便利工具，不是开发平台**。其重启功能基于 ClassLoader 技巧，有时会产生难以理解的问题；经典例子是同一个类的两个副本之间抛出 `ClassCastException`。遇到无法解释的现象时，应先完整重启再排查。

## Profiles

```text
application.yml            公共默认值；不指定 Profile 运行也必须安全
application-local.yml      开发机
application-test.yml       自动化测试
application-prod.yml       生产环境（不含秘密）
```

通过 `-Dspring-boot.run.profiles=local` 启动，或设置 `SPRING_PROFILES_ACTIVE=local`。

不指定 Profile 的默认配置必须**安全**。任何人在未指定 Profile 运行 JAR 时，应用都不应连接生产环境。参见[配置](configuration.zh-CN.md)。

秘密绝不放在这些文件中，应存放于 gitignore 排除的 `.env` 或平台秘密存储中。

## SDKMAN!（可选）

在 macOS、Linux 或 Windows Subsystem for Linux 上，可以使用 [SDKMAN!](https://sdkman.io/install/) 管理用户级 JDK，以及 Maven、Gradle 等 JVM 工具。它只是本地便利工具，不应成为构建仓库的前置条件。

```bash
sdk list java                 # 查看可用发行版和标识符
sdk install java <candidate> # 替换为 `sdk list java` 中项目约定的标识符
sdk use java <candidate>     # 在当前 Shell 中使用同一个标识符
java -version
```

尖括号中的内容是占位符，不要原样输入尖括号。`.sdkmanrc` 和 `sdk env` 的用法参见 [SDKMAN! 使用指南](https://sdkman.io/usage/)。

团队约定使用某个 SDKMAN! 发行版时，可提交包含精确候选标识符的 `.sdkmanrc`。开发者在项目目录中运行 `sdk env install` 和 `sdk env` 即可安装并启用对应工具。该文件应与 Maven/Gradle 构建配置和 CI 中声明的 Java 版本一致；不要提交机器专属的绝对路径。

SDKMAN! 管理开发者 Shell 的 `PATH`；项目构建入口仍应使用 Maven 或 Gradle Wrapper，由 Wrapper 固定构建工具分发版本。构建中的 Java toolchain（受构建方式支持时）用于配置编译和测试所用的 JDK。排查本地应用或构建实际使用哪个 JDK 时，应检查 `java -version`。CI 和容器应显式选择 JDK，不能依赖开发者的 SDKMAN! 默认值。参见 [Maven](maven.zh-CN.md) 和 [Gradle](gradle.zh-CN.md)。

## 种子数据

```java
@Component
@Profile("local")
class DevDataSeeder implements ApplicationRunner {

    private final CustomerRepository customers;

    DevDataSeeder(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (customers.count() > 0) {
            return;
        }
        customers.save(Customer.create(EmailAddress.of("dev@example.com")));
    }
}
```

限制在本地 Profile，且必须幂等。开发数据**不属于 Flyway 迁移**：迁移会在所有环境运行，包括生产；之后也不能直接修改（参见[Flyway](flyway.zh-CN.md)）。

## 任务运行器

一组简短的命名命令比 Wiki 页面更好。[Task](https://taskfile.dev/) 是一种选择，也可以使用 `Makefile` 或 `scripts/` 目录。关键是命令要在仓库中，并且易于发现。Taskfile 的设计约定和可移植起始示例见[Taskfile](taskfile.zh-CN.md)。使用仓库实际的构建 Wrapper 和 Compose 服务名（参见[Maven](maven.zh-CN.md)）。

## 快速反馈

- 本地也将 `spring.jpa.open-in-view=false`。这样 N+1 问题会在开发阶段暴露，而不是在生产中静默出现（参见[Spring Data JPA](spring-data-jpa.zh-CN.md)）。
- 需要检查查询时设置 `logging.level.org.hibernate.SQL=DEBUG`，使用完应关闭；日志太多会掩盖真正输出。
- Spring Boot 的 Failure Analyzer 会将常见启动失败转换为可读信息。先完整阅读错误说明，再滚动到堆栈；答案通常已经写在那里。
- 本地也设置 `spring.jpa.hibernate.ddl-auto=validate`，使 Schema 不一致在启动时暴露，而不是直到首次查询才出现。

## 使用 Spring Boot 3.5.x 时

Docker Compose 支持（3.1+）、Devtools、Profiles 和 `ConnectionDetails` 的行为都相同。构建方面的差异见其他文档，例如 Flyway 会被传递引入、Starter 名称有所变化（参见[版本与升级](boot-versions.zh-CN.md)）。

## 常见问题

- 将 `spring-boot-docker-compose` 放在 `implementation`——应设为 `runtime` 且 `optional`，避免进入生产。
- 假设没有运行时集成依赖、Docker Compose V2 和可发现的 Compose 文件也会自动启动 Compose——应说明前提或提供手动 `docker compose up` 命令。
- 添加 Compose 集成后没有检查数据源配置与 `ConnectionDetails` 的优先关系——使用自动发现的连接，或清楚说明手动模式。
- 使用 `start_and_stop` 却不告知开发者——应用退出时服务会停止；应说明生命周期行为，并在合适时提供 `start_only`。
- 在 `compose.yaml` 中映射固定主机端口——可能与其他同类服务冲突。
- 本地和生产使用不同镜像版本——这正是此配置要避免的环境分歧。
- 对官方 PostgreSQL 18+ 镜像仍使用 `/var/lib/postgresql/data`——应遵循该镜像的主版本专属目录布局，将卷挂载到 `/var/lib/postgresql`。
- Compose 支持之外又设置 `spring.datasource.url`——Spring Boot 会自动配置，手动值可能冲突。
- 在 Flyway 迁移中添加开发数据——会进入生产且之后无法直接修改。
- 种子程序不幂等——每次重启都会插入重复数据。
- 提交 `.env` 或把秘密放在 `application-local.yml`。
- 强制要求使用 SDKMAN!，或将其当前 JDK 当成项目 Wrapper 和 toolchain 配置的替代品。
- 默认配置指向生产环境。
- 将 Devtools ClassLoader 问题当成应用 Bug 调试——先完整重启。
- 为消除 `LazyInitializationException` 在本地启用 `open-in-view`——这会把 N+1 问题隐藏到生产。

## 相关文档

- [配置](configuration.zh-CN.md) · [Flyway](flyway.zh-CN.md) · [Testcontainers](testcontainers.zh-CN.md) · [Maven](maven.zh-CN.md) · [Taskfile](taskfile.zh-CN.md) · [容器化与 Native Image](containerization-and-native.zh-CN.md)
