# MCP Server

通过 Model Context Protocol 向 AI 客户端（Claude Code、Claude Desktop 和其他 MCP Host）暴露应用能力。

使用 **Spring AI MCP Starter**，不要直接调用独立 Java SDK。Starter 会自动配置服务端、传输方式和基于注解的工具扫描。只有非 Spring 应用才直接使用 SDK。

## 传输方式

选择一种传输方式；它们使用不同的 Starter，也对应不同的部署模式。

| 传输方式 | Starter | 适用场景 |
|---|---|---|
| **stdio** | `spring-ai-starter-mcp-server` | 客户端将应用 JAR 作为本地子进程启动，例如同一台机器上的 Claude Code 或 Claude Desktop |
| **Streamable HTTP** | `spring-ai-starter-mcp-server-webmvc` | 远程服务、多个客户端和常规服务端部署 |

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

1.0 之前的名称 `spring-ai-mcp-server-spring-boot-starter` 已废弃，不应使用。

```yaml
spring:
  ai:
    mcp:
      server:
        name: order-service-mcp
        version: 1.0.0
        protocol: STREAMABLE      # remote; prefer over legacy SSE
```

## stdio：保持 stdout 干净

stdio 模式下，协议本身使用 stdin/stdout。任何启动横幅、日志行或意外的 `System.out.println` 都会破坏 JSON-RPC 帧，客户端会断开连接且通常不给出有用错误。

```yaml
spring:
  main:
    banner-mode: off
logging:
  file:
    name: /var/log/order-mcp.log     # anywhere but stdout
  pattern:
    console:                          # empty - disable console appender
```

这是 stdio Server“无法工作”最常见的原因。

## 工具

```java
@Component
class OrderMcpTools {

    private final OrderService orderService;

    OrderMcpTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @McpTool(
            name = "get_order",
            description = """
                    Retrieve a single order by its id, including line items and current status.
                    Use when the user asks about a specific known order. To find an order without \
                    an id, use list_orders instead.""")
    OrderResponse getOrder(
            @McpToolParam(description = "Order id as a UUID string", required = true)
            String orderId) {

        return orderService.getById(OrderId.of(orderId));
    }

    @McpTool(
            name = "list_orders",
            description = "List a customer's orders, optionally filtered by status. Returns at most 50.")
    List<OrderResponse> listOrders(
            @McpToolParam(description = "Customer email address", required = true) String email,
            @McpToolParam(description = "One of: DRAFT, PLACED, SHIPPED, DELIVERED, CANCELLED",
                          required = false) String status) {

        return orderService.findForCustomer(email, status, Limit.of(50));
    }
}
```

- `@McpTool` 和 `@McpToolParam` 是 MCP Server 注解。`@Tool` 是 Spring AI 的**模型工具调用 API**，属于另一套机制。不要在同一个 Server 中混用。
- 标注了这些注解的 `@Component` 方法会自动发现。不要再为相同方法注册 `MethodToolCallbackProvider`。

### 描述就是提示词

工具描述不是普通文档，而是模型据此决定是否调用工具的文本。描述含糊会使工具从不被调用，或者在不合适的场景被调用。

说明工具**返回什么、何时应该调用、何时不应该调用**。上面的 `get_order` 示例明确指出，没有已知 ID 时应使用 `list_orders`；这句话能避免一类常见的错误调用。

参数描述也应如此：`"One of: DRAFT, PLACED, …"` 有用，而 `"the status"` 不够明确。

### 返回 DTO，并限制工具范围

返回 Response Record，不要返回实体；实体序列化会暴露 Schema、触发懒加载，并可能在事务结束后抛异常（参见 [spring-data-jpa.zh-CN.md](spring-data-jpa.zh-CN.md)）。

**限制所有结果集。**一个能返回一万行的工具就可能真的返回一万行，并占满模型上下文。限制结果数量，在描述中说明上限；调用方需要更多结果时采用分页。

与其提供许多重叠工具，不如提供少量名称清晰的工具。模型在二十个相似工具中容易选错。

## 错误

```java
@McpTool(name = "cancel_order", description = "Cancel a PENDING order. Fails if already shipped.")
CancellationResult cancelOrder(
        @McpToolParam(description = "Order id as a UUID string", required = true) String orderId) {
    try {
        orderService.cancel(OrderId.of(orderId));
        return CancellationResult.cancelled(orderId);
    } catch (OrderNotFoundException e) {
        return CancellationResult.failed("No order with id " + orderId);
    } catch (OrderNotModifiableException e) {
        return CancellationResult.failed("Order cannot be cancelled: " + e.getMessage());
    }
}
```

**返回结构化失败结果，不要直接抛出异常。**工具处理器抛出的异常会变成不透明的协议级错误；明确描述的失败结果能让模型理解问题并调整操作——这通常决定了模型能否恢复。

错误消息应能指导下一步。`"Order cannot be cancelled: already shipped"` 能告诉模型发生了什么；`"Internal error"` 则不能。

绝不要通过工具结果泄露堆栈或内部消息，原因与[错误处理](error-handling.zh-CN.md)相同。

## 安全

**MCP 工具是以模型为客户端的 API Endpoint。**[安全基础](security-fundamentals.zh-CN.md)中的全部原则都适用，此外还有两点需要特别注意：

- **模型会根据你无法控制的文本决定调用什么。**将每个工具参数都视为不可信输入。无论描述如何，一个接收原始 SQL 片段、文件路径或 Shell 参数的工具都可能构成漏洞。
- **读取工具可以广泛开放，写入工具必须设置门槛。**任何连接客户端都能调用破坏性工具，是不安全的默认配置。HTTP 传输应要求认证；真正具有破坏性的操作也可以完全不暴露为工具。

stdio 模式下，客户端已在本机启动进程，信任边界是这台机器；但参数仍由模型生成，仍必须校验。

不要在没有认证的情况下公开 MCP Server。

## 客户端配置

```json
{
  "mcpServers": {
    "order-service": {
      "command": "java",
      "args": ["-jar", "/opt/order-mcp/order-mcp-server.jar"],
      "env": {
        "SPRING_DATASOURCE_URL": "jdbc:postgresql://localhost:5432/orders",
        "SPRING_PROFILES_ACTIVE": "mcp"
      }
    }
  }
}
```

建议使用专用的 `mcp` Profile：关闭 Banner、重定向日志，并禁用任何向 stdout 写入内容的配置（参见 [configuration.zh-CN.md](configuration.zh-CN.md)）。

## 测试

工具是普通的 Spring Bean，可以直接测试：

```java
@Test
void getOrderReturnsLineItems() {
    var response = orderMcpTools.getOrder(orderId.value().toString());
    assertThat(response.lines()).hasSize(2);
}

@Test
void cancelOrderReportsFailureWhenShipped() {
    var result = orderMcpTools.cancelOrder(shippedOrderId.value().toString());
    assertThat(result.succeeded()).isFalse();
    assertThat(result.message()).contains("cannot be cancelled");
}
```

尤其要断言**失败路径**：工具抛异常而非返回有描述的失败结果，是本文重点防范的问题，只有测试能捕捉它。

端到端检查可以启动 Server 并使用 MCP Client 调用；这类检查不要放进常规构建。

## 使用 Boot 3.5.x 时

使用 Spring AI **1.x** 及其 MCP Starter。传输模型相同，但 1.x 使用 Spring AI 的 `@Tool` / `@ToolParam` 和 `MethodToolCallbackProvider`，而不是原生的 `@McpTool` / `@McpToolParam` 注解；远程传输配置为 SSE，而不是 `protocol: STREAMABLE`。应查阅 Spring AI 1.x 参考并使用对应配置，不要将 2.0 注解反向用于 1.x。

## 常见问题

- Agent 为 Java 项目生成 Python MCP 代码：应使用 Spring AI Starter。
- Agent 使用 `spring-ai-mcp-server-spring-boot-starter`：该名称已废弃，应使用 `spring-ai-starter-mcp-server[-webmvc]`。
- Agent 在 stdio Server 中向 stdout 写日志：会破坏 JSON-RPC 帧，导致客户端无提示断开。
- Agent 在需要 `@McpTool` 时使用 `@Tool`：这是不同机制，不要混用。
- Agent 又为已有注解的方法注册 `MethodToolCallbackProvider`：这些方法已经自动发现。
- Agent 为远程传输设置 `spring.ai.mcp.server.transport`：应使用 `protocol: STREAMABLE`。
- Agent 编写含糊的工具描述：描述就是提示词，应写明何时调用和何时不调用。
- Agent 不描述参数：模型无法理解参数用途。
- Agent 返回实体：会泄露 Schema 并触发懒加载。
- Agent 返回无上限结果：可能占满上下文；应限制并说明上限。
- Agent 从工具处理器抛出异常：应返回结构化且有说明的失败结果，让模型能够调整。
- Agent 在工具结果中返回内部消息或堆栈跟踪。
- Agent 暴露未认证的破坏性工具：工具是 API Endpoint，客户端是模型。
- Agent 信任工具参数：参数由模型根据不可控文本生成。
- Agent 只测试成功路径：常见缺陷往往出现在失败路径。

## 相关文档

- [Spring AI](spring-ai.zh-CN.md) · [AI 可观测性](ai-observability.zh-CN.md) · [安全基础](security-fundamentals.zh-CN.md) · [配置](configuration.zh-CN.md) · [错误处理](error-handling.zh-CN.md)
