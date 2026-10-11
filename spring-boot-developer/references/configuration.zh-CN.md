# 配置

## 使用 `@ConfigurationProperties`，而不是 `@Value`

```java
@Validated
@ConfigurationProperties("app.orders")
public record OrderProperties(
        @NotNull @Positive Integer maxLinesPerOrder,
        @NotBlank String defaultCurrency,
        @NotNull Duration reservationTimeout,
        @NotNull @Valid Retry retry) {

    public record Retry(@Positive int maxAttempts, @NotNull Duration initialDelay) {}
}
```

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application { … }
```

```yaml
app:
  orders:
    max-lines-per-order: 100
    default-currency: GBP
    reservation-timeout: 15m
    retry:
      max-attempts: 3
      initial-delay: 200ms
```

Record 会自动使用构造器绑定，因此属性不可变，并且在创建时全部赋值。应用类上的 `@ConfigurationPropertiesScan` 可以自动注册配置类型，不必对每种类型分别使用 `@EnableConfigurationProperties`。

为什么不写 `@Value("${app.orders.max-lines}")`：

- 键名拼错会在注入时才失败，堆栈很深，而不是在启动时失败。
- 转换之前，值始终是 `String`。
- 相关键分散在各处，没有集中展示配置项的地方。
- 无法执行统一校验。
- 不容易脱离 Spring 上下文测试；属性 record 可以直接 `new OrderProperties(…)`。

`@Bean` 方法中读取一个无关紧要的单值时可以使用 `@Value`。只要有两个相关配置项，就应定义属性类型。

**在启动时校验。** 属性类上的 `@Validated` 会在值缺失或不合理时让应用启动失败，并指出出错的键，而不是等首次请求使用时才抛出 `NullPointerException`。参见[校验](validation.zh-CN.md)。

`Duration` 和 `DataSize` 可以从易读字符串绑定，例如 `15m`、`200ms`、`10MB`；不要保存原始的毫秒 `long`。

## Profiles

Profile 用于选择**运行环境**，不是功能开关。

```
application.yml                 # 公共默认值
application-local.yml           # 开发机
application-test.yml             # 自动化测试
application-prod.yml             # 生产环境
```

- **默认配置必须安全。** 如果有人不指定 Profile 运行 JAR，应用不能连接生产环境。
- **任何 Profile 文件中都不要存秘密。** 这些文件位于代码仓库中。
- **避免用 Profile 切换业务实现。** 在 `PaymentGateway` 上添加 `@Profile("prod")`，会导致测试无法覆盖生产路径。应通过属性选择实现，并分别测试两者。
- Profile 列表应尽量精简。大多数应用使用 `local`、`test` 和 `prod` 就够了；Profile 会组合叠加，八种配置难以理解。

```java
@Component
@Profile("local")                     // ✅ 仅用于开发便利功能
class DevDataSeeder implements ApplicationRunner { … }
```

部署需要多个相关 Profile 时，可以定义组：

```yaml
spring:
  profiles:
    group:
      prod: [prod-db, prod-metrics, prod-security]
```

## 配置值的来源

后面的来源优先级更高：

1. JAR 中的 `application.yml`
2. Profile 专属的 `application-{profile}.yml`
3. JAR 外的配置数据（`./config/`、`--spring.config.additional-location`）
4. 环境变量
5. 命令行参数

**环境变量是部署接口。** `app.orders.max-lines-per-order` 对应 `APP_ORDERS_MAXLINESPERORDER`——宽松绑定会转成大写并去掉连字符。不要写成 `APP_ORDERS_MAX_LINES_PER_ORDER`；每个下划线代表一级嵌套，因此它会绑定到不同配置键，并被静默忽略。

安全且合理的值可以设默认值，必需值则应强制提供：

```yaml
app:
  orders:
    max-lines-per-order: ${MAX_ORDER_LINES:100}   # 可选，有合理默认值
  stripe:
    api-key: ${STRIPE_API_KEY}                    # 必需；缺失时快速失败
```

第二种写法是有意为之：缺少必需值时应阻止启动，而不是等到凌晨才暴露配置错误。

## 秘密配置

**绝不能放进代码仓库。** `application.yml`、Profile 文件、测试夹具和注释中都不应包含秘密。

| 平台 | 方式 |
|---|---|
| Kubernetes | 将 Secret 挂载为文件；通过 `spring.config.import=configtree:/etc/secrets/` 把目录文件读作属性 |
| Docker Compose | 使用版本控制之外的 env 文件 |
| AWS / GCP / Azure | 使用 Secrets Manager 或 Key Vault，并在部署时注入环境变量 |
| 本地开发 | 使用加入 gitignore 的 `.env` 或 `application-local.yml`，并填入虚拟值 |

在 Kubernetes 上，Config Tree 是最清晰的选择：每个秘密对应一个只读挂载文件，文件名就是属性名。

```yaml
spring:
  config:
    import: optional:configtree:/etc/secrets/
```

从 Actuator 输出中隐藏秘密。`/actuator/env` 和 `/actuator/configprops` 可能直接打印秘密（参见[可观测性](observability.zh-CN.md)）。

### 秘密的生命周期

秘密注入只是秘密管理的一部分。为每个凭据指定负责人和轮换流程；平台支持时优先使用短期凭据或工作负载身份，并按服务最小授权。若提供方允许两个凭据短暂并存，可先部署新凭据并验证流量，再撤销旧凭据。还要规划紧急撤销流程，并在事故发生前演练。

启动日志和支持诊断包中不要输出解析后的配置。限制能够读取部署变量和挂载秘密文件的人员；环境变量可能通过进程检查或崩溃诊断暴露。不要假设 Actuator 脱敏能保护自定义端点或应用日志，参见[可观测性](observability.zh-CN.md)。

本地开发使用个人专属、已加入 gitignore 的文件和非生产凭据。CI 使用自身的秘密存储或联合工作负载身份，不要提交 `.env`。参见[本地开发](local-development.zh-CN.md)。

## 功能开关

布尔属性配合 `@ConditionalOnProperty` 可控制整个 Bean：

```java
@Bean
@ConditionalOnProperty(name = "app.features.recommendations.enabled", havingValue = "true")
RecommendationEngine recommendationEngine() { … }
```

如果开关需要在运行时切换，应注入属性类型并据此分支。`@ConditionalOn*` Bean 只在启动时决定是否创建：

```java
if (features.recommendations().enabled()) { … }
```

让属性支持 `@RefreshScope`，或者接受需要重启。需要运行时切换并按用户定向的真正功能开关，应使用开关服务，不要用配置属性拼装一套。

## 测试配置

```java
@Test
void bindsNestedProperties() {
    new ApplicationContextRunner()
            .withUserConfiguration(OrderConfig.class)
            .withPropertyValues(
                    "app.orders.max-lines-per-order=50",
                    "app.orders.default-currency=EUR",
                    "app.orders.reservation-timeout=5m",
                    "app.orders.retry.max-attempts=2",
                    "app.orders.retry.initial-delay=100ms")
            .run(context -> assertThat(context.getBean(OrderProperties.class).maxLinesPerOrder())
                    .isEqualTo(50));
}

@Test
void rejectsNegativeMaxLines() {
    new ApplicationContextRunner()
            .withUserConfiguration(OrderConfig.class)
            .withPropertyValues("app.orders.max-lines-per-order=-1")
            .run(context -> assertThat(context).hasFailed());
}
```

`ApplicationContextRunner` 不会启动整个应用。要测试校验是否真的**拒绝**错误值，否则这些约束只是摆设。

测试间共享的配置放在 `application-test.yml`，并使用 `@ActiveProfiles("test")`，不要到处散布 `@TestPropertySource`。每组不同属性都会创建独立的上下文缓存项，并增加一次完整启动成本（参见[测试策略](testing-strategy.zh-CN.md)）。

## IDE 元数据

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

该处理器会从属性 record 生成 `spring-configuration-metadata.json`，为 `application.yml` 提供自动补全和 Javadoc。配置成本低，还能在输入时发现拼写错误。

## 使用 Spring Boot 3.5.x 时

构造器绑定、`@ConfigurationPropertiesScan`、`@Validated`、Config Tree、宽松绑定和 `ApplicationContextRunner` 都相同。Spring Boot 4 更改了一些**属性名称**；OpenRewrite 属性迁移配方可处理重命名（参见[版本与升级](boot-versions.zh-CN.md)）。

## 常见问题

- 用 `@Value` 管理一组相关配置——应使用 `@ConfigurationProperties` record。
- 配置类使用 getter/setter——record 使用构造器绑定且不可变。
- 忘记 `@ConfigurationPropertiesScan` 或 `@EnableConfigurationProperties`——配置类型未注册。
- 忘记 `@Validated`——缺少值会在运行时导致 `NullPointerException`，而不是启动失败。
- 环境变量写成 `APP_ORDERS_MAX_LINES_PER_ORDER`——下划线表示嵌套层级；正确值是 `APP_ORDERS_MAXLINESPERORDER`。
- 将秘密存进 `application.yml` 或 Profile 文件——这些文件都在代码仓库中。
- 给必需秘密设置默认值——缺少时应让应用启动失败，而不是以错误配置运行。
- 用原始 `long` 保存超时——绑定为 `Duration`，例如 `15m`。
- 用 `@Profile` 切换业务实现——生产路径将无法得到测试。
- 默认配置指向生产环境——默认配置必须安全。
- 用 `@ConditionalOnProperty` 控制运行时切换的开关——条件只在启动时评估。
- 未隐藏 `/actuator/env` 的输出——端点可能打印包含秘密的配置值。
- 使用完整 `@SpringBootTest` 测配置——`ApplicationContextRunner` 无需启动整个应用。
- 在测试中散布 `@TestPropertySource`——每组不同属性都会占用新的上下文缓存项。

## 相关文档

- [校验](validation.zh-CN.md) · [可观测性](observability.zh-CN.md) · [测试策略](testing-strategy.zh-CN.md) · [本地开发](local-development.zh-CN.md) · [容器化与 Native Image](containerization-and-native.zh-CN.md)
