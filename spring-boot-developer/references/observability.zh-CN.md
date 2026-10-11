# 可观测性

## Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

默认只通过 HTTP 暴露 `/actuator/health`。**其他端点必须有意选择暴露**，不要使用 `include: "*"`：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
      show-components: when-authorized
  server:
    port: 9090                    # 独立端口，不通过公网入口路由
```

`include: "*"` 会公开 `/actuator/env`（所有配置值，包括秘密）、`/actuator/configprops`、`/actuator/heapdump`（可下载进程内存，包含凭据和个人数据）以及 `/actuator/threaddump`。这相当于等待扫描器发现的一次数据泄露。

两道防线都要采用：

- **使用公网入口无法访问的独立管理端口。**
- **单独配置安全链**（参见[安全基础](security-fundamentals.zh-CN.md)）：

```java
@Bean
@Order(1)
SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
    return http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health/**").permitAll()   // 探针需匿名访问
                    .anyRequest().hasRole("OPS"))
            .httpBasic(Customizer.withDefaults())
            .build();
}
```

`show-details: when-authorized` 很重要：健康详情可能暴露数据库主机、消息代理和下游 URL。

## 健康、存活与就绪

两种探针回答的问题不同，混淆后会导致重启循环。

| 探针 | 问题 | 失败含义 |
|---|---|---|
| `/actuator/health/liveness` | 进程是否已损坏且无法恢复？ | **终止并重启我** |
| `/actuator/health/readiness` | 我现在是否能处理流量？ | **停止向我发送流量**，但不要重启 |

数据库故障属于**就绪状态**失败。如果同时导致存活检查失败，编排平台会在故障期间重启所有副本；冷启动和重连风暴会使故障加重。

```yaml
management:
  endpoint:
    health:
      group:
        readiness:
          include: readinessState,db,redis
        liveness:
          include: livenessState
```

存活探针依赖项应尽可能少。就绪探针只纳入确实无法缺少的依赖。可降级使用的可选下游若被加入就绪检查，会令服务因本可绕过的依赖而退出流量。

### 自定义健康指示器

```java
@Component
class InventoryServiceHealthIndicator implements HealthIndicator {

    private final InventoryClient client;

    @Override
    public Health health() {
        try {
            client.ping();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("reason", e.getClass().getSimpleName()).build();
        }
    }
}
```

健康检查必须**快速且开销低**——每个副本每隔几秒都会执行。不要运行真实查询，也不要调用下游完整健康端点，否则会形成分布式健康检查风暴。检查不够简单时应缓存结果。

只有确实导致应用无法服务的故障才报告 `DOWN`。

## 指标

项目已包含 Micrometer。Spring Boot 无需额外代码就会监控 HTTP 请求、连接池、JVM 内存、GC 和线程数量。

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
  prometheus:
    metrics:
      export:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

只有 HTTP 指标无法回答业务问题时，才添加自定义指标：

```java
@Component
public class OrderMetrics {

    private final Counter placed;
    private final Timer fulfilment;

    OrderMetrics(MeterRegistry registry) {
        this.placed = Counter.builder("orders.placed")
                .description("Orders successfully placed")
                .register(registry);
        this.fulfilment = Timer.builder("orders.fulfilment")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void recordPlaced() {
        placed.increment();
    }
}
```

在构造器中**只注册一次** Meter。每个请求都调用 `Counter.builder(…).register(registry)` 会反复查找 Registry，是可测量的热点开销。

### 基数

**标签值集合无界会拖垮指标后端。** 每种不同的标签组合都是一个单独的时间序列。

| 安全标签 | 绝不能作为标签 |
|---|---|
| `status`（有限的几个值） | 用户 ID |
| `endpoint`（固定路由模板） | 订单 ID 或其他实体 ID |
| `region`、`tenant`（取值范围固定且已知） | 展开路径参数后的原始 URI |
| `outcome` = success / failure | 邮箱地址、IP 地址 |
| | 异常**消息** |

URI 标签使用 `/api/orders/{id}` 模板正是出于此原因。实体粒度的信息应写入日志或 Trace，而不是指标。

### 告警指标

应针对用户感知到的症状告警，而不只关注原因：

- 每个端点的错误率和延迟百分位（p95、p99；不要用会掩盖问题的均值）。
- 饱和度：连接池使用率、队列深度、`@ConcurrencyLimit` 拒绝数。
- 死信主题深度（参见[消息](messaging.zh-CN.md)）。
- 业务信号——每分钟创建订单数降为零，通常是部署故障最快的探测方式。

每条告警都要指定负责人、用户影响、可执行的排障手册，以及只有需要人工响应时才触发的阈值。将可用性和延迟目标写成服务等级目标（SLO）；告警可采用错误预算或多窗口消耗速率策略，避免每次短暂抖动都触发值班。仪表板应标注部署时间，便于将回归与发布关联。定期清理重复告警和长期无需行动的告警。

## 链路追踪

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 0.1        # 生产环境 10%；开发环境 1.0
  otlp:
    tracing:
      endpoint: ${OTEL_ENDPOINT}
```

Trace 上下文会自动通过 `RestClient`、`WebClient`、HTTP 接口客户端和 Kafka 传播。但默认不会跨越 `@Async` 或 `@Scheduled` 边界，需要配置上下文传播（参见[异步与定时任务](async-and-scheduling.zh-CN.md)）。

开发环境采样率可设为 1.0，生产环境应较低。生产流量下 100% 采样成本高且大多冗余。

在日志中写入 Trace ID，使日志行能定位到 Trace：

```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]"
```

## 结构化日志

Spring Boot 原生输出 JSON 日志，无需 Encoder 配置或额外依赖：

```yaml
logging:
  structured:
    format:
      console: ecs        # 或 logstash、gelf
```

日志需要发送到可搜索存储时，应采用结构化格式。带有 `traceId`、`spanId` 和 MDC 字段的 JSON 行可以按字段查询；格式化字符串不行。

以下日志规则比格式更重要：

- **记录异常对象，而非 `ex.getMessage()`**，否则会丢失堆栈。
- **绝不记录秘密、令牌、密码或个人数据。** 在源头脱敏；日志存储通常不如数据库安全。
- **`ERROR` 表示需要人工查看。** 把 404 记为 `ERROR` 会让所有人忽略该级别，参见[错误处理](error-handling.zh-CN.md)。
- 使用占位符，例如 `log.info("Placed order {}", orderId)`，不要拼接字符串；日志级别关闭时就不必构造消息。

使用 trace/span ID 作为跨服务关联字段。如果客户端可以提交请求/关联 ID，应校验其格式和长度，否则生成服务端 ID；不能把不可信值用作指标标签，也不能将其用于授权。根据日志中的标识和业务数据设置合理的保留期与访问权限，并记录哪些字段有意省略或脱敏。

## info 端点

```yaml
management:
  info:
    build:
      enabled: true
    git:
      mode: full
    env:
      enabled: false        # 关闭，避免泄漏配置
```

构建时生成 Build 和 Git 信息（参见[Maven](maven.zh-CN.md)）后，`/actuator/info` 会告诉你正在运行的确切提交。仅这一点就能回答很多“修复是否已部署？”的问题。

## Observation API

一次调用可以同时创建自定义 Span、Timer 和（可选）日志：

```java
Observation.createNotStarted("order.fulfilment", observationRegistry)
        .lowCardinalityKeyValue("channel", channel)      // 成为指标标签；取值必须有限
        .highCardinalityKeyValue("orderId", id.toString()) // 仅是 Trace 属性
        .observe(() -> fulfilmentService.fulfil(id));
```

低 / 高基数的 API 划分正是为了强制执行标签规则：低基数键成为指标标签，高基数键只留在 Span 上。

## 使用 Spring Boot 3.5.x 时

Actuator、健康组、探针、Micrometer、Trace 和 Observation API 都相同。结构化日志从 Boot 3.4 起提供，因此 3.5.x 也可使用。Boot 4 更新了 OpenTelemetry 支持；Spring Batch 6 移除了 Micrometer 的全局静态 Registry，因此 Boot 4 的 Batch 应用需要一个连接到 `MeterRegistry` 的 `ObservationRegistry` Bean（参见[Spring Batch](spring-batch.zh-CN.md)）。

## 常见问题

- 设置 `management.endpoints.web.exposure.include: "*"`——会公开 `/env`、`/configprops` 和 `/heapdump`；堆转储包含内存中的秘密。
- Actuator 留在主端口且无安全保护——放到独立端口，并使用单独安全链。
- 设置 `show-details: always`——健康详情会泄漏数据库主机和下游 URL。
- 将数据库加入**存活**探针——故障会导致所有副本重启并恶化故障。
- 将可选依赖加入就绪检查——服务会因本可降级绕过的依赖而退出流量。
- 健康指示器执行真实查询——每个副本每隔几秒就会运行一次。
- 用用户 ID、订单 ID 或异常消息标记指标——无界基数会拖垮后端。
- 在请求路径中注册 Meter——应在构造器中注册一次。
- 根据平均延迟告警——均值会掩盖长尾，应使用 p95 / p99。
- 生产采样率设为 1.0——成本高且大多冗余。
- 期望 Trace 上下文跨越 `@Async`——必须配置上下文传播。
- 记录 `ex.getMessage()`——会丢失堆栈。
- 记录令牌、密码或个人数据——应在源头脱敏。
- 手写 JSON 日志 Encoder——Boot 内置结构化日志。
- 启用 `management.info.env`——可能泄露配置值。

## 相关文档

- [安全基础](security-fundamentals.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [异步与定时任务](async-and-scheduling.zh-CN.md) · [消息](messaging.zh-CN.md) · [配置](configuration.zh-CN.md) · [容器化与 Native Image](containerization-and-native.zh-CN.md)
