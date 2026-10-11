# AI 可观测性

大语言模型调用速度慢、按 Token 计费，而且结果不确定。没有监控时，就无法回答“为什么这么慢？”“账单为什么翻倍？”“实际发送了什么？”——这些问题迟早都会出现。

本文在[可观测性](observability.zh-CN.md)的基础上补充 AI 场景，那里提到的原则同样适用。

## 内置监控

Spring AI 自带 Micrometer 监控，无需编写代码即可获取指标：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

| 指标 | 提供的信息 |
|---|---|
| `gen_ai.client.operation` | 按提供商和模型标记的调用延迟 |
| `gen_ai.client.token.usage` | 输入和输出 Token 数 |
| `spring.ai.chat.client` | `ChatClient` 级别的计时和 Span |

这些指标遵循 OpenTelemetry GenAI 语义约定，可以与现有监控数据对齐。

这里的延迟**不等于**端点延迟——模型调用通常占主要耗时。应把两者放在一起观察，否则会错误归因端点变慢的原因。

## 生产环境绝不记录提示词

```yaml
spring:
  ai:
    chat:
      client:
        observations:
          log-prompt: false
          log-completion: false
      observations:
        log-prompt: false
        log-completion: false
        include-error-logging: true
```

提示词包含用户输入和附加上下文，其中可能有个人信息、订单详情和内部文档。记录提示词会把这些内容复制到日志存储中，而日志存储通常不如数据库安全，也经常会发送给第三方服务。

可以有意在开发环境启用提示词日志，但不要让开发配置残留到生产环境。保留 `include-error-logging` 是安全且有价值的。

## Token 用量与成本

账单按 Token 计费。应按操作记录 Token 数，以便在账单到来前归因成本：

```java
@Component
public class AiUsageMetrics {

    private final MeterRegistry registry;

    AiUsageMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(String operation, String model, Usage usage) {
        Counter.builder("ai.tokens")
                .tag("operation", operation)      // 有限取值：固定的一组功能
                .tag("model", model)              // 有限取值：少量模型
                .tag("direction", "input")
                .register(registry)
                .increment(usage.getPromptTokens());

        Counter.builder("ai.tokens")
                .tag("operation", operation)
                .tag("model", model)
                .tag("direction", "output")
                .register(registry)
                .increment(usage.getCompletionTokens());
    }
}
```

[可观测性](observability.zh-CN.md)中的基数原则在这里尤其重要：**绝不能把会话 ID、用户 ID 或提示词内容作为指标标签。** `operation` 和 `model` 的取值有限；每个请求都不同的值则不行。

`Usage` 提供 `getPromptTokens()` 和 `getCompletionTokens()`。旧版 GA 前 API 中的 `getGenerationTokens()` 已不存在。

### 成本估算

价格会变，应放在配置中，不能编译进代码：

```yaml
app:
  ai:
    pricing:
      # 每百万 Token 的美元价格——请按提供商当前价格核实
      claude-opus-5:   { input: 5.00, output: 25.00 }
      claude-sonnet-5: { input: 3.00, output: 15.00 }
      claude-haiku-4-5: { input: 1.00, output: 5.00 }
```

```java
@Validated
@ConfigurationProperties("app.ai.pricing")
public record AiPricingProperties(Map<String, ModelPrice> models) {
    public record ModelPrice(@NotNull BigDecimal input, @NotNull BigDecimal output) {}
}
```

```java
public BigDecimal estimate(String model, Usage usage) {
    var price = pricing.models().get(model);
    if (price == null) {
        log.warn("No pricing configured for model {}; cost not estimated", model);
        return BigDecimal.ZERO;
    }
    return price.input().multiply(BigDecimal.valueOf(usage.getPromptTokens()))
            .add(price.output().multiply(BigDecimal.valueOf(usage.getCompletionTokens())))
            .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
}
```

使用 `BigDecimal` 而不是 `double`，这与系统中其他金额的规则相同（参见[领域建模](domain-modelling.zh-CN.md)）。估算值用于告警和成本归因；提供商账单才是最终依据。

## 自定义 Advisor

如需对每次调用进行内置指标之外的审计，可以实现 Advisor：

```java
@Component
public class AiAuditAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AiAuditAdvisor.class);

    private final AiUsageMetrics metrics;

    AiAuditAdvisor(AiUsageMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        var started = System.nanoTime();
        try {
            var response = chain.nextCall(request);
            var chatResponse = response.chatResponse();

            if (chatResponse != null && chatResponse.getMetadata() != null) {
                var usage = chatResponse.getMetadata().getUsage();
                metrics.record("chat", chatResponse.getMetadata().getModel(), usage);
                log.info("AI call completed in {}ms, inputTokens={}, outputTokens={}",
                        Duration.ofNanos(System.nanoTime() - started).toMillis(),
                        usage.getPromptTokens(), usage.getCompletionTokens());
            }
            return response;
        } catch (Exception e) {
            log.error("AI call failed after {}ms",
                    Duration.ofNanos(System.nanoTime() - started).toMillis(), e);
            throw e;
        }
    }

    @Override
    public String getName() {
        return "aiAudit";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

GA 版本的 Advisor API 使用 `CallAdvisor`、`ChatClientRequest` 和 `ChatClientResponse`。GA 前的 `CallAroundAdvisor`、`AdvisedRequest` 和 `AdvisedResponse` 无法编译。由于许多公开示例早于重命名，代码生成工具经常会生成旧接口。

记录 Token 数和耗时，**不要记录提示词或生成文本**。

## 持久化审计记录

在受监管领域、需要成本分摊或可能需要对系统生成内容进行争议处理时，可以持久化审计记录：

```java
@Entity
@Table(name = "ai_call_audit")
class AiCallAudit extends BaseEntity {

    @EmbeddedId
    private AiCallAuditId id;

    @Column(name = "operation", nullable = false, length = 64)
    private String operation;

    @Column(name = "model", nullable = false, length = 64)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "estimated_cost_usd", nullable = false, precision = 12, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "succeeded", nullable = false)
    private boolean succeeded;
}
```

存储**元数据，不存储内容**。如果确实必须留存提示词，这属于需要制定保留期限的数据保护决策，不应只是顺手记录日志。

异步写入审计数据，避免增加调用延迟。同时要记住，`@Async` 必须放在另一个 Bean 上，否则会绕过代理并在当前线程中执行（参见[异步与定时任务](async-and-scheduling.zh-CN.md)）。

## 链路追踪

Trace 上下文会通过 Spring AI 调用传播，因此模型调用会显示为触发请求中的一个 Span。这样可以看出，一个耗时三秒的端点中有 2.8 秒是在等待模型提供商。

```yaml
management:
  tracing:
    sampling:
      probability: 0.1
```

可以考虑以高于普通流量的采样率记录 AI 调用：这类调用相对少、单次成本高，也更值得追踪。

## 告警指标

| 信号 | 原因 |
|---|---|
| 每小时支出与预算对比 | 故障表现为财务损失，而且发生很快 |
| 按异常类型统计的错误率 | 区分限流、认证失败和错误请求 |
| p95 延迟 | 提供商变慢时，首先会在这里显现 |
| 限流（429）比例 | 在用户察觉前提示退避或提高配额 |
| 按操作统计并跟踪 Token 用量 | 提示词变更导致上下文翻倍时，可以及时发现，而不是等账单 |

失控的循环可能会在短时间内产生大量模型费用。支出告警必不可少。

## 使用 Spring Boot 3.5.x 时

Actuator、Micrometer、GenAI 指标约定和链路追踪的工作方式相同。Advisor API 不同：Spring AI 1.x 有自己的 Advisor 接口；`CallAroundAdvisor` 到 `CallAdvisor` 的重命名发生在 1.0 GA，因此 GA 前后 1.x 代码彼此也不兼容，更不用说与 2.0 的差异。请核对实际使用版本的参考文档。

## 常见问题

- 在生产环境启用 `log-prompt`——提示词可能包含用户数据和内部上下文，会被写入日志存储。
- 实现 `CallAroundAdvisor` 或 `AdvisedRequest`——这是 GA 前的 API；应使用 `CallAdvisor` 和 `ChatClientRequest`。
- 调用 `usage.getGenerationTokens()`——现有方法是 `getCompletionTokens()`。
- 将会话 ID、用户 ID 或提示词作为指标标签——会造成无限基数。
- 把 Token 价格写死在代码中——价格会变，应放在配置中。
- 用 `double` 表示成本——应使用 `BigDecimal`。
- 在审计表中保存完整提示词，却没有制定保留策略。
- 同步写入审计记录——会增加每次调用的延迟。
- 在同一 Bean 内部调用标有 `@Async` 的方法——代理被绕过，调用会在当前线程执行。
- 只跟踪延迟——成本指标才是容易造成意外的信号。
- 没有设置支出告警——失控循环可能在被发现前就造成高额费用。
- 将 AI 延迟与端点延迟分开看——模型调用通常占主要耗时。

## 相关文档

- [Spring AI](spring-ai.zh-CN.md) · [可观测性](observability.zh-CN.md) · [配置](configuration.zh-CN.md) · [异步与定时任务](async-and-scheduling.zh-CN.md) · [缓存](caching.zh-CN.md)
