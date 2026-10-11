# Spring AI

Boot 4 需要 **Spring AI 2.0**；1.x 版本线面向 Boot 3。混用会导致依赖图无法解析；版本组合见 [boot-versions.zh-CN.md](boot-versions.zh-CN.md)。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version><!-- current 2.0.x --></version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-anthropic</artifactId>
    </dependency>
</dependencies>
```

Starter 坐标遵循 `spring-ai-starter-model-<provider>` 和 `spring-ai-starter-vector-store-<store>` 格式。1.0 前的 `spring-ai-<x>-spring-boot-starter` 格式在 Maven Central 中无法解析；基于旧资料训练的 Agent 常会生成它。

## 配置

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        model: claude-opus-5
        max-tokens: 4096
```

**模型 ID 不带日期后缀。**使用 `claude-opus-5`，而不是 `claude-opus-5-20260101`。添加日期会导致 404，这是生成代码中最常见的错误之一。

| 模型 | ID | 适用场景 |
|---|---|---|
| Claude Opus 5 | `claude-opus-5` | 默认选择；复杂推理、Agent 工作、编程 |
| Claude Sonnet 5 | `claude-sonnet-5` | 对成本敏感的大规模生产任务 |
| Claude Haiku 4.5 | `claude-haiku-4-5` | 简单且对延迟敏感的任务，如分类、路由 |

模型列表应以提供商文档为准，不要依赖硬编码表格；模型会按各自节奏发布和退役。

Spring AI 2.0 **扁平化了 Chat 属性**：旧的 `chat.options.*` 层级已移除，因此使用 `spring.ai.anthropic.chat.model`，而不是 `spring.ai.anthropic.chat.options.model`。

API Key 从环境注入（参见[配置](configuration.zh-CN.md)），不要放在 `application.yml` 中。

## ChatClient

```java
@Configuration
class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("You are a support assistant for an order management system.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }
}
```

`InMemoryChatMemory` 已不存在。`MessageWindowChatMemory` 会将历史限制在滑动窗口中；不设上限时，长对话会不断扩大 Prompt，直至超出上下文窗口，并让每次请求成本持续增加。

2.0 中 Advisor 使用 Builder 构造：`MessageChatMemoryAdvisor.builder(chatMemory).build()`，不要使用 `new MessageChatMemoryAdvisor(...)`。`PromptChatMemoryAdvisor` 已移除。

## 会话 ID 是必需的

```java
@Service
public class SupportAssistant {

    private final ChatClient chatClient;

    SupportAssistant(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String reply(ConversationId conversationId, String message) {
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId.value()))
                .user(message)
                .call()
                .content();
    }
}
```

Spring AI 2.0 移除了 `ChatMemory.DEFAULT_CONVERSATION_ID`；调用内存 Advisor 时未提供会话 ID 会抛出 `IllegalArgumentException`。

这项移除也是一项安全功能。共享默认 ID 会让**所有用户的对话历史进入同一个窗口**，导致一位客户的消息对下一位客户可见。会话 ID 应限定到用户或 Session，绝不能共享。

## Prompt 模板

Prompt 应从 Java 代码中分离。它们的变更周期与代码不同，内联字符串也容易诱发字符串拼接：

```
Analyse the following order and identify anomalies.

Customer: {customer}
Placed: {placedAt}
Total: {total}
Lines:
{lines}

Report only anomalies. If there are none, reply exactly: NONE.
```

```java
@Value("classpath:prompts/analyse-order.st")
private Resource analyseOrderPrompt;

public String analyse(ConversationId conversationId, OrderSummary order) {
    return chatClient.prompt()
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId.value()))
            .user(u -> u.text(analyseOrderPrompt)
                    .param("customer", order.customerReference())
                    .param("placedAt", order.placedAt())
                    .param("total", order.total())
                    .param("lines", order.formattedLines()))
            .call()
            .content();
}
```

**绝不要通过字符串拼接构造 Prompt。**这不仅难以阅读，还会把用户可控文本直接插入指令中，造成 Prompt Injection；这与 SQL Injection 属于同类错误，也适用于 java-developer 关于 Text Block 的警告。使用 `.param()`，清楚分隔不可信内容，不能让它出现在预期指令的位置。

## 结构化输出

```java
public record OrderAnomalies(List<Anomaly> anomalies, RiskLevel risk) {
    public record Anomaly(String code, String description) {}
}

public OrderAnomalies analyse(OrderSummary order) {
    return chatClient.prompt()
            .user(u -> u.text(analyseOrderPrompt).param("order", order.formatted()))
            .call()
            .entity(OrderAnomalies.class);
}
```

Record 可直接映射。对于泛型，类型擦除意味着必须提供 Type Reference：

```java
.entity(new ParameterizedTypeReference<List<Anomaly>>() {})
```

`.entity(List.class)` 可以编译，但运行时会失败。

结构化输出只是对目标形状的请求，不是保证；采取行动前必须验证返回内容。参见[校验](validation.zh-CN.md)。

## RAG

```java
@Bean
ChatClient ragChatClient(ChatClient.Builder builder, VectorStore vectorStore) {
    return builder
            .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(SearchRequest.builder().topK(5).similarityThreshold(0.7).build())
                    .build())
            .build();
}
```

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vector-store-advisor</artifactId>
</dependency>
```

`QuestionAnswerAdvisor` 在 2.0 中移到 `spring-ai-vector-store-advisor`（之前是 `spring-ai-advisors-vector-store`）。Starter 会自动配置 `VectorStore`，直接注入即可；不要调用已移除公开构造函数的 `new PgVectorStore(...)`。

`SearchRequest` 使用 Builder：`SearchRequest.builder().topK(5).build()`，而不是 `SearchRequest.defaults().withTopK(5)`。

设置 `similarityThreshold`。如果不设置，`topK` 会返回最不差的五个匹配项，即使它们都不相关；模型仍会基于无关上下文自信作答。

文档摄取应放在批处理作业或定时任务中，而不是请求路径里；嵌入大型文档耗时且每次调用都会产生成本。参见[Spring Batch](spring-batch.zh-CN.md)。

## 流式响应

```java
@GetMapping(value = "/assist", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<String> assist(@RequestParam String question) {
    return chatClient.prompt().user(question).stream().content();
}
```

这是 MVC 应用中合理使用 `Flux` 的场景：Server-Sent Events 天然是流式响应。但这并不意味着应用变成了 Reactive 应用。

## 故障处理

LLM 调用是一次耗时、收费且不可靠的第三方网络请求，应按此处理（参见[HTTP 客户端](http-clients.zh-CN.md)和[弹性模式](resilience.zh-CN.md)）：

| 异常 | 是否重试？ |
|---|---|
| `TransientAiException`（限流、过载、超时） | 可以，采用指数退避和抖动 |
| `NonTransientAiException`（请求错误、认证失败、内容被拒绝） | 不可以；每次重试都会以相同方式失败 |

设置超时，并决定模型不可用时该功能如何表现：降级到非 AI 流程、排队等待，或返回明确错误。“页面一直卡着”不是处理方案。

绝不要在事务中调用模型（参见[事务](transactions.zh-CN.md)）。

## 成本与安全

- **限制 `max-tokens`。**这是单次调用成本唯一的硬性上限。
- **充分使用缓存。**相同 Prompt 每次都会产生计费请求；对确定性 Prompt 使用缓存，往往是最大的一项成本节省（参见[缓存](caching.zh-CN.md)）。
- **未经审慎评估，不要在 Prompt 中放秘密或个人数据**；请求内容会离开你的基础设施。
- **将模型输出视为不可信数据。**不要执行它，不要插入 SQL 或 Shell 命令，也不要将其作为原始 HTML 渲染。
- **按操作记录 Token 用量**，在账单到来前看清成本；参见 [AI 可观测性](ai-observability.zh-CN.md)。

## 测试

测试中不要调用真实模型。它速度慢、会产生费用且结果不确定，因此对精确措辞的断言容易随机失败。

- Service 层测试 Mock `ChatClient`（或底层的 `ChatModel`），并断言代码如何使用结果。
- 断言 Prompt 构造，即传入了正确模板和参数，而不是断言响应文本。
- 测试故障路径：暂时性错误、非暂时性错误、超时、结构化输出格式错误。

如有需要，可保留一个针对真实提供商的端到端测试，但应为其添加标签并从常规构建中排除。

## 使用 Boot 3.5.x 时

使用 **Spring AI 1.x**。`ChatClient` Fluent API、Prompt 模板、结构化输出、RAG 和流式响应整体相近，但 2.0 引入了多项破坏性变更：扁平化 `chat.options.*` 属性、移除 `ChatMemory.DEFAULT_CONVERSATION_ID`、重命名 `spring-ai-advisors-vector-store`、Advisor 只能用 Builder 构造，以及移除 `PromptChatMemoryAdvisor`。应查阅项目所用 Spring AI 1.x 版本的参考文档，不要假设上面的 2.0 写法适用。

## 常见问题

- Agent 在 Boot 4 使用 Spring AI 1.x，或在 Boot 3.5 使用 2.0：两条版本线不能互换。
- Agent 生成 `spring-ai-anthropic-spring-boot-starter`：正确格式为 `spring-ai-starter-model-anthropic`。
- Agent 为模型 ID 添加日期后缀：当前 ID 不带后缀（例如 `claude-opus-5`）。
- Agent 配置 `spring.ai.anthropic.chat.options.model`：2.0 已扁平化属性，应移除 `.options`。
- Agent 使用 `new MessageChatMemoryAdvisor(new InMemoryChatMemory())`：两者都已移除；应使用 Builder 和 `MessageWindowChatMemory`。
- Agent 在内存 Advisor 调用中省略会话 ID：会抛异常；共享默认值还会泄漏不同用户的对话历史。
- Agent 使用无上限对话记忆：Prompt 会不断增长，直至超出上下文窗口。
- Agent 使用 `PromptChatMemoryAdvisor`：它已在 2.0 中移除。
- Agent 添加 `spring-ai-advisors-vector-store`：该依赖已重命名为 `spring-ai-vector-store-advisor`。
- Agent 调用 `new PgVectorStore(...)`：Starter 会自动配置它。
- Agent 编写 `SearchRequest.defaults().withTopK(n)`：应使用 Builder。
- Agent 未设置 `similarityThreshold`：会检索无关上下文，模型仍可能自信作答。
- Agent 使用 `.entity(List.class)`：泛型会被擦除；应传入 `ParameterizedTypeReference`。
- Agent 将用户输入拼接进 Prompt：这会造成 Prompt Injection；应使用 `.param()`。
- Agent 硬编码 API Key：应从环境或秘密管理系统读取。
- Agent 未配置 `max-tokens`：单次调用成本没有上限。
- Agent 重试 `NonTransientAiException`：每次都会以相同方式失败。
- Agent 在事务中调用模型：会让数据库连接等待数秒的远程请求。
- Agent 在请求处理器中嵌入文档：应放到批处理或定时任务中。
- Agent 将模型输出作为原始 HTML 渲染或传入 Shell：应视为不可信输入。
- Agent 编写调用真实模型的测试：应 Mock 模型，并断言 Prompt 构造和故障路径。

## 相关文档

- [AI 可观测性](ai-observability.zh-CN.md) · [MCP Server](mcp-server.zh-CN.md) · [弹性模式](resilience.zh-CN.md) · [缓存](caching.zh-CN.md) · [配置](configuration.zh-CN.md) · [Boot 版本](boot-versions.zh-CN.md)
