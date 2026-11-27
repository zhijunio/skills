# Spring AI MCP Server

Prefer Spring AI's MCP server starters and native annotations in Spring Boot applications.

## Dependencies

Let the Spring AI BOM manage every Spring AI and MCP transitive dependency. Do not override its MCP
SDK version independently.

```xml
<!-- Pick exactly one transport starter. -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
</dependency>
<!-- Remote MVC: spring-ai-starter-mcp-server-webmvc -->
<!-- Remote reactive: spring-ai-starter-mcp-server-webflux -->
```

Use the standalone Java SDK only when Spring integration is intentionally unnecessary;
follow its [current documentation](https://github.com/modelcontextprotocol/java-sdk) for
dependencies, versions, and protocol compatibility.

## Native Spring AI MCP tools

Use `@McpTool` and `@McpToolParam` for server capabilities. `@Tool` is Spring AI's model
tool-calling API; it is not the native MCP server annotation.

```java
@Component
final class OrderMcpTools {
    private final OrderService orderService;

    OrderMcpTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @McpTool(
        name = "get_order",
        description = "Get an order by UUID with line items and status history",
        generateOutputSchema = true,
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true))
    OrderResponse getOrder(
            @McpToolParam(description = "Order UUID", required = true) String orderId) {
        return OrderResponse.from(orderService.findById(UUID.fromString(orderId)));
    }
}
```

- Return DTOs or records, not persistence entities.
- Set tool hints accurately; clients must still treat them as untrusted metadata.
- Use stable, specific descriptions because the model uses them to select tools.
- Align return types with server mode: synchronous methods for `SYNC`, reactive types for `ASYNC`.
- For expected failures, return a stable structured result. Do not expose stack traces or secrets.
- Use the result construction API supported by the MCP SDK version managed by Spring AI.

## Transport configuration

```yaml
spring:
  main:
    banner-mode: "off"
  ai:
    mcp:
      server:
        name: order-service-mcp
        version: 1.0.0
        type: SYNC
        stdio: true
```

For a remote server, select the MVC or WebFlux starter and set
`spring.ai.mcp.server.protocol=STREAMABLE` or `STATELESS`. Prefer Streamable HTTP
for a new remote deployment; Spring AI deprecates SSE, so use it only when an
existing client requires it. A stdio server must keep stdout free of banners,
logs, and `System.out` output because stdout carries JSON-RPC frames.

For the shape of a local stdio MCP client configuration, see
[`stdio-client-config.json`](../templates/stdio-client-config.json). Replace its placeholders through the
client's supported secret configuration; do not commit credentials. The JSON shape
is client-specific, so adapt it to the selected MCP host.

## Security and operations

- HTTP transports do not add authentication or authorization. Secure remote MCP endpoints
  with Spring Security or an equivalent boundary, and validate request origins to prevent
  DNS rebinding.
- Validate tool arguments and apply domain authorization inside every capability.
- Keep destructive tools narrow and require explicit business preconditions.
- Bound execution time, result size, pagination, and downstream fan-out.
- Log tool name, outcome, duration, and authenticated actor without recording secrets or full data.
- Test initialization, discovery, invalid arguments, authorization, cancellation, and shutdown with a real MCP client.

## Official sources

- Spring AI MCP overview: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
- [Spring AI MCP server security and transports](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- Spring AI server annotations: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html
- MCP Java SDK releases: https://github.com/modelcontextprotocol/java-sdk/releases
- MCP Java SDK 2.0 migration: https://github.com/modelcontextprotocol/java-sdk/blob/main/MIGRATION-2.0.md

