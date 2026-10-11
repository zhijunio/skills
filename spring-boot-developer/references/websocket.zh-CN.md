# WebSocket 与 STOMP

WebSocket 是长连接、双向通信协议。客户端需要及时接收服务端推送或进行低延迟交互时可使用它。如果只是单向更新且允许一定延迟，应比较 Server-Sent Events；普通命令和查询仍应使用 HTTP REST。长连接会增加会话状态、授权、资源核算和运维成本。

## 选择 STOMP 还是原生 WebSocket Handler

| 方案 | 适用场景 | 应用需要负责 |
|---|---|---|
| **基于 WebSocket 的 STOMP** | 客户端交换具名命令/事件，需要 destination、订阅、消息映射和 broker 集成 | 消息契约、destination 授权、投递语义和 broker 拓扑 |
| **原生 Spring WebSocket API** | 小型自定义协议、二进制帧，或需要精细控制连接生命周期 | 帧格式/Schema、分发、关联、订阅、授权和发送协调 |

典型业务通知或协作功能优先考虑 STOMP。它提供消息协议和 Spring 注解式 Controller，无需应用自行发明帧格式。STOMP simple broker 是进程内路由组件，不是持久化事件 broker。跨服务的持久任务使用 Kafka/RabbitMQ/JMS；STOMP 消息需要跨应用实例扇出时，使用外部 STOMP broker relay。

## STOMP 配置

对于 Spring MVC，使用 Boot 管理版本的 `spring-boot-starter-websocket`，不要独立指定 Spring Framework 版本。配置范围明确的端点来源白名单、应用 destination 前缀和 broker destination：

```java
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("https://app.example.com");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }
}
```

将 `/app/**` 下的客户端命令映射到应用 Handler。服务端事件发布到有文档说明的 topic 或用户 destination；不要把任意内部 broker destination 当作授权模型：

```java
@Controller
class OrderUpdatesController {

    private final SimpMessagingTemplate messaging;

    OrderUpdatesController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @MessageMapping("/orders/{orderId}/watch")
    void watch(@DestinationVariable UUID orderId, Principal principal) {
        // 订阅或发布用户数据前，先校验当前用户对此订单的访问权限。
    }

    void publishStatus(UUID userId, OrderStatusMessage message) {
        messaging.convertAndSendToUser(userId.toString(), "/queue/orders", message);
    }
}
```

将入站消息视为不可信 API 请求：绑定专用命令 DTO、执行校验和领域授权，并在需要时返回明确的错误事件。持久化和业务规则应放在 Service。不能把消息体中的用户 ID 或租户 ID 当作身份凭据。

## 认证与授权

在 Servlet 技术栈中，Spring 会将经过认证的 HTTP 握手 `Principal` 关联到 WebSocket/STOMP 会话。使用 Cookie/Session 认证时，启用 Spring Security 的 WebSocket 集成，并授权**入站消息**，不能只保护 HTTP 握手。显式配置消息类型和 destination 规则，并设置默认拒绝的兜底规则。订阅也必须授权：只读订阅同样可能泄露其他用户的数据。

Spring Security 的 STOMP 集成默认要求 `CONNECT` 携带 CSRF Token。对基于 Cookie 的浏览器客户端应保留该保护，并说明客户端如何发送 Token。来源白名单是浏览器跨源防护，不代表认证或授权。不要为了让 WebSocket 客户端连通而全局关闭 CSRF。

浏览器原生 WebSocket API 不能附加任意 HTTP `Authorization` 请求头。适用时优先使用已认证的 HTTP Session。如果必须在 STOMP `CONNECT` 头中携带 Bearer Token，应在入站 Channel Interceptor 中认证，在授权前把认证后的 Principal 关联到会话，并确保 Token 不进入日志或错误响应。不要将访问令牌放在 URL 查询参数中。STOMP 的 `login`/`passcode` 头默认不会自动成为 HTTP 认证。

授权必须基于请求的资源和当前 Principal。连接有效不代表可以访问所有 destination 或对象。若长连接期间权限可能变化，应按威胁模型重新检查访问权限，并在必要时关闭或撤销相关订阅。

## 原生 Spring WebSocket API

只有在不需要 STOMP 的协议和 broker 抽象时，才实现 `WebSocketHandler`（文本帧常用 `TextWebSocketHandler`），并通过 `WebSocketConfigurer` 注册。`HandshakeInterceptor` 可校验握手属性，但不能替代认证和逐消息授权。明确设置允许的来源，并应用与 STOMP 相同的资源限制。

定义有版本的消息 Schema、最大帧大小、消息校验、错误响应格式、心跳/空闲行为和关闭状态策略。不要在 Handler 中等待慢速数据库或远程调用。Spring WebSocket Session 不支持并发发送：应串行化写入，或使用 `ConcurrentWebSocketSessionDecorator` 并设置有界发送时间和缓冲上限。连接关闭或传输错误时清理连接级状态。

## 扩展与连接保护

- STOMP simple broker 将订阅保存在进程内存中。每个应用副本拥有独立视图；一个实例发出的广播不会自动送到连接在其他实例上的客户端。多副本场景应选择外部 broker relay 或明确设计共享扇出，并验证用户 destination 和 Session 路由。
- 为连接和消息配置上限：帧/消息大小、入站 Channel 队列与执行器容量、出站发送缓冲和时间限制、并发会话数、空闲超时，以及按用户/租户的连接配额。根据消息大小和负载预期设置有限值。
- 按 Principal 限制消息速率和昂贵命令。及时拒绝过载，不创建无界执行器队列。参见[限流与资源保护](rate-limiting.zh-CN.md)。
- 配置心跳并说明客户端重连行为。客户端应采用有界、带随机抖动的指数退避，重连后重新订阅，并能容忍重复或遗漏的瞬时事件。若客户端需要可靠恢复，应持久化事件或提供 REST 补拉端点。
- 保护 broker relay 凭据，限制 broker ACL，在网络边界配置 TLS，并监控 broker 连接和 relay 故障。不要将 WebSocket 投递视为持久化或 exactly-once 处理。
- 记录连接数、连接/断开/错误率、消息速率、发送缓冲饱和度和 broker 健康状态。指标标签使用低基数维度；不要使用 Session ID、用户 ID 或含标识符的 destination 作为标签。参见[可观测性](observability.zh-CN.md)。

## 测试

在入站 Channel 边界测试消息 Handler 和授权，包括有效/无效负载、未授权 destination 及 `CONNECT` 的 CSRF 行为。使用嵌入式服务端和 STOMP 客户端增加端到端测试，验证握手、订阅、发布和断开。原生 Handler 应测试握手/来源策略、并发发送、关闭清理和帧大小限制。使用外部 relay 时，至少有一个集成测试覆盖所选 broker 拓扑。仅 Mock `SimpMessagingTemplate` 调用不能代替协议边界测试。

## Boot 3.5.x 说明

Boot 3.5 使用 Spring Framework 6.2 和 Spring Security 6.x；MVC starter 坐标与 STOMP/WebSocket 核心模型相同，但应使用项目 Boot BOM 管理的 API。Boot 3.5 也支持 WebFlux 的独立 WebSocket 方案；响应式配置不属于本 MVC 参考范围。不要直接复制仅适用于 Security 7 的配置签名，应根据 Boot 管理的 Spring Security 版本核对。

## 常见误区

- 在多副本部署中使用 simple broker，却认为它会跨实例广播。
- 只保护 HTTP 握手，没有保护 STOMP 命令或订阅。
- 把来源白名单当作认证，或为了连接而全局关闭 CSRF。
- 信任消息体提供的用户 ID 或租户 ID。
- 在查询参数中放 Bearer Token，或记录 STOMP 认证头。
- 假设 STOMP 投递是持久化、全局有序或 exactly-once。
- 允许连接、负载、队列、发送缓冲或消息速率没有上限。
- 在原生 WebSocket Session 上并发发送，却没有串行化或有界缓冲。
- 只测试 Handler 单元逻辑，没有验证握手、授权、订阅或实际消息路由。

## 主要参考资料

- [Spring Boot WebSockets](https://docs.spring.io/spring-boot/3.5/reference/messaging/websockets.html)
- [Spring Framework STOMP](https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html)
- [Spring Framework WebSocket 服务端 API](https://docs.spring.io/spring-framework/reference/web/websocket/server.html)
- [Spring Security WebSocket 集成](https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html)
- [Spring Framework STOMP 认证](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication.html)
- [Spring Framework WebSocket 测试](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/testing.html)
