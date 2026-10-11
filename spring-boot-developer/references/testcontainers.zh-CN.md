# Testcontainers

本文说明如何让 Spring Boot 测试连接容器中的真实依赖。Testcontainers 有两种连接 Spring 的机制，选错是最常见的问题。

## 依赖——Boot 4 中坐标已变化

Boot 4 管理 **Testcontainers 2.x**，其中制品名称和容器类包名都已迁移：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

| Boot 3.5.x（Testcontainers 1.x） | Boot 4（Testcontainers 2.x） |
|---|---|
| `org.testcontainers:junit-jupiter` | `org.testcontainers:testcontainers-junit-jupiter` |
| `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| `org.testcontainers.containers.PostgreSQLContainer` | `org.testcontainers.postgresql.PostgreSQLContainer` |

不要锁定版本，由 BOM 管理。

## 优先使用 `@ServiceConnection`

只要 Spring Boot 为容器提供了 `ConnectionDetailsFactory`，就使用 `@ServiceConnection`。Boot 会读取映射后的主机、端口、URL 和凭据，并自动创建对应的 `*ConnectionDetails` Bean。**无需手动注册属性。**

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:18-alpine");
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redis() {
        return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    }
}
```

内置工厂支持的容器包括：JDBC 数据库（任意 `JdbcDatabaseContainer`，例如 PostgreSQL、MySQL、MariaDB、Oracle、SQL Server）及其 R2DBC 版本；MongoDB、Redis、Cassandra、Couchbase、Elasticsearch、Neo4j；Kafka、RabbitMQ、ActiveMQ、Artemis、Pulsar；Zipkin、OpenTelemetry、LDAP、Flyway 和 Liquibase。

工厂无法根据普通 `GenericContainer` 推断服务类型，因此要像上例一样提供名称提示。

**固定镜像标签。** 使用 `postgres:18-alpine`，不要用 `postgres` 或 `postgres:latest`。未固定标签意味着构建依赖 Docker Hub 当天提供的镜像，迟早会因与代码无关的变化而损坏。

镜像版本应与生产环境一致。用 18 测试、用 15 部署，会重新引入容器本来要避免的问题。

## 配置值应该放哪里：三类规则

静态配置经常误放在 `@DynamicPropertySource`，动态端口也可能被写死进属性文件。可以按下表决定：

| 值的类型 | 放置位置 | 原因 |
|---|---|---|
| 有内置工厂的容器连接信息 | **无需手动放置**；由 `@ServiceConnection` 处理 | 自动注入，手动注册冗余且可能冲突 |
| **容器启动后**才知道的值，例如无工厂容器映射端口 | `DynamicPropertyRegistrar` Bean | 端口在 `start()` 时分配，编写测试时尚未知 |
| **编写测试时**就固定的值，例如 `ddl-auto`、功能开关、日志级别、禁用调度器 | `application-test.yml` | 静态配置应放在属性文件 |

**心智模型：** `@DynamicPropertySource` 用于编写测试时尚不存在的值，因此 API 接收 `Supplier`。可以直接写成字面量的值，不应放进去。

### 无工厂的容器

```java
@Bean
DynamicPropertyRegistrar mailProperties(GenericContainer<?> mailpit) {
    return registry -> {
        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", mailpit::getFirstMappedPort);
    };
}
```

优先使用 `DynamicPropertyRegistrar` Bean（Boot 3.4+），而非静态 `@DynamicPropertySource` 方法：它可以依赖其他 Bean，能干净地放在 `@TestConfiguration` 中，并且与 `@ServiceConnection` 的顺序配合正确。

### 两种反模式

- **在 `@DynamicPropertySource` 中放静态值**，例如 `ddl-auto`、缓存 TTL 和功能开关——应写入 `application-test.yml`。
- **固定主机端口**，例如 `withFixedExposedPorts(5432, 5432)` 或硬编码端口——在 CI 和并行运行中必然冲突。让 Testcontainers 随机映射端口，并延迟读取。

## 整个套件共用一个容器

这是最大的性能因素。常用两种方法：

优先使用被所有测试导入的共享 `@TestConfiguration`（便于与上下文缓存配合）：

```java
@SpringBootTest
@Import(TestcontainersConfig.class)
class OrderIntegrationTest { … }
```

也可以在抽象基类中声明 `static` 容器，每个 JVM 启动一次：

```java
@Testcontainers
abstract class AbstractDatabaseTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");
}
```

`static` 是关键；非静态 `@Container` 会在每个测试方法前重启容器，速度会慢很多，而且通常不是预期行为。

一个套件中定义两种不同的容器配置，意味着两套数据库和两组上下文缓存，通常是意外造成的（参见[测试策略](testing-strategy.zh-CN.md)）。

## 测试之间共享状态

容器共享意味着**数据也共享**。切片测试会回滚；完整 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 不会，因为请求在线程中执行，不受测试事务控制。

按优先顺序选择：

1. **每个测试创建独有数据**，使用唯一 ID。无需清理，不依赖顺序，并且可以并行。
2. 使用 **`@Sql` 脚本**在每次测试前重置到已知状态。
3. 在 `@BeforeEach` 中清空表——可用，但只要有人忘记清理就会依赖顺序。

不要依赖测试按特定顺序执行。

## 没有 Docker 的环境

默认情况下，没有 Docker 时 Testcontainers 测试会直接失败。处理方式取决于容器声明方式：

```java
// @Testcontainers + static @Container：JUnit 跳过类，而不是失败
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
class OrderRepositoryTest { … }
```

采用 `@ServiceConnection` Bean 时，没有 `@Testcontainers` 注解可设置该选项，而且容器会在上下文加载时启动。应改为跳过整个测试类：

```java
static boolean dockerAvailable() {
    return DockerClientFactory.instance().isDockerAvailable();
}

@EnabledIf("com.example.app.DockerCondition#dockerAvailable")
class OrderIntegrationTest extends BaseIntegrationTest { … }
```

`@EnabledIf` 接收 `Class#staticMethod` 引用，不能内联调用 API。更简单的替代方法是在 `@BeforeAll` 中执行 `assumeTrue(DockerClientFactory.instance().isDockerAvailable())`，或通过 `@Tag("docker")` 在无 Docker 环境中排除。

**不要让 CI 悄悄跳过这些测试。** 跳过是本地开发便利；CI 静默跳过 Testcontainers 测试会制造虚假信心，与 H2 测试的陷阱相同。应确保 CI 提供 Docker，或使用 Testcontainers Cloud。

## 启动成本

- **复用容器**：使用 `withReuse(true)`，并在 `~/.testcontainers.properties` 中设置 `testcontainers.reuse.enable=true`，使容器在本地多次运行间保持启动状态。这是本地开发功能；CI 应关闭，以确保每次运行使用干净容器。
- 项目提供时优先使用 `-alpine` 镜像。
- 只有 Flyway 未负责初始化时才使用 `.withInitScript(...)`；两者同时执行会形成两个 Schema 信息来源。

## 使用 Spring Boot 3.5.x 时

`@ServiceConnection`（3.1+）和 `DynamicPropertyRegistrar`（3.4+）都存在且行为相同。差异是顶部表格中的 Maven 坐标和类包名；3.5.x 使用 Testcontainers 1.x，而 Boot 4 使用 2.x。

## 常见问题

- Boot 4 使用 Testcontainers 1.x 坐标——应使用 `testcontainers-postgresql`，容器类已迁移至 `org.testcontainers.postgresql`。
- 锁定 Testcontainers 版本——应由 BOM 管理。
- 对有内置工厂的容器手动注册 `spring.datasource.*`——`@ServiceConnection` 会处理，重复注册可能冲突。
- 普通 `GenericContainer` 使用 `@ServiceConnection` 却不提供名称——工厂无法推断服务类型。
- 在 `@DynamicPropertySource` 中写静态配置——应放进 `application-test.yml`。
- 使用 `withFixedExposedPorts` 或硬编码端口——CI 和并行运行会冲突。
- `@Container` 不是 static——容器会在每个测试方法后重启。
- 每个测试类都启动容器——整个套件应共用一个，并导入共享 `@TestConfiguration`。
- 镜像没有标签或使用 `latest`——构建会依赖 Docker Hub 当前内容。
- 测试与生产使用不同主版本——会重新造成容器本应解决的环境差异。
- 以为 `RANDOM_PORT` 测试会回滚——不会，必须显式清理。
- 让 CI 静默跳过 Testcontainers 测试——会造成虚假信心，与 H2 通过的假象相同。
- 在 CI 启用容器复用——CI 每次应使用干净容器。

## 相关文档

- [持久化切片测试](testing-slices-persistence.zh-CN.md) · [集成测试](testing-integration.zh-CN.md) · [REST API 测试](testing-rest-api.zh-CN.md) · [测试策略](testing-strategy.zh-CN.md) · [消息](messaging.zh-CN.md)

---

*Testcontainers 模式参考 **Siva Prasad Reddy Katamreddy** 的 [testcontainers-samples](https://github.com/sivaprasadreddy/testcontainers-samples) 及 **Philip Riecks** 的 Testing Spring Boot Applications Demystified。*
