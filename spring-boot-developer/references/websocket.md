# WebSocket and STOMP

WebSocket is a long-lived, bidirectional connection. Use it when a client needs timely server push or interactive low-latency exchange. For one-way updates where occasional delay is acceptable, compare Server-Sent Events; for ordinary commands and queries, keep HTTP REST. A persistent connection adds session state, authorization, resource accounting and operational work.

## Choose STOMP or a raw WebSocket handler

| Option | Use when | Application owns |
|---|---|---|
| **STOMP over WebSocket** | Clients exchange named commands/events and benefit from destinations, subscriptions, message mapping and broker integration | Message contract, destination authorization, delivery semantics and broker topology |
| **Raw Spring WebSocket API** | A small bespoke protocol, binary frames, or precise control over connection lifecycle is required | Framing/schema, dispatch, correlation, subscriptions, authorization and send coordination |

Prefer STOMP for a typical business notification or collaboration feature. It provides a message protocol and Spring's annotated controller model without requiring the application to invent a framing protocol. A STOMP simple broker is an in-memory routing component, not a durable event broker. Use Kafka/RabbitMQ/JMS for durable inter-service work; use an external STOMP broker relay when STOMP fan-out must cross application instances.

## STOMP setup

For Spring MVC, use Boot's managed `spring-boot-starter-websocket`; do not pin Spring Framework versions independently. Configure a narrow endpoint origin allowlist, an application destination prefix, and broker destinations:

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

Map client commands under `/app/**` to application handlers. Publish server events to a documented topic or to a user destination; do not expose arbitrary internal broker destinations as an authorization model:

```java
@Controller
class OrderUpdatesController {

    private final SimpMessagingTemplate messaging;

    OrderUpdatesController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @MessageMapping("/orders/{orderId}/watch")
    void watch(@DestinationVariable UUID orderId, Principal principal) {
        // Authorize access to this order before subscribing or publishing user data.
    }

    void publishStatus(UUID userId, OrderStatusMessage message) {
        messaging.convertAndSendToUser(userId.toString(), "/queue/orders", message);
    }
}
```

Treat inbound messages as untrusted API requests: bind a dedicated command DTO, validate it, enforce domain authorization, and return explicit error events where appropriate. Keep persistence and business rules in services. Avoid accepting a user ID or tenant ID from the message body as proof of identity.

## Authentication and authorization

In the Servlet stack, Spring associates the authenticated HTTP handshake `Principal` with the WebSocket/STOMP session. For cookie/session authentication, enable Spring Security's WebSocket integration and authorize **inbound messages**, not only the HTTP handshake. Use explicit message-type and destination rules plus a deny-by-default fallback. Require authorization for subscriptions as well as commands: a read-only subscription can still disclose another user's data.

Spring Security's STOMP integration requires a CSRF token on `CONNECT` by default. Keep that protection for cookie-authenticated browser clients and teach the client how to send the token. The origin allowlist is a browser cross-origin defense, not authentication or authorization. Do not globally disable CSRF to make a WebSocket client connect.

Native browser WebSocket APIs cannot attach arbitrary HTTP `Authorization` headers. Prefer the normal authenticated HTTP session when suitable. If a bearer token must be carried in a STOMP `CONNECT` header, authenticate it in an inbound-channel interceptor before authorization, bind the resulting principal to the session, and prevent the token from entering logs or error payloads. Do not put access tokens in the URL query string. STOMP `login`/`passcode` headers are not automatically HTTP authentication.

Authorization must be checked against the requested resource and current principal. A valid connection does not grant access to every destination or object. Recheck access when permissions can change during a long-lived session, and close or revoke affected subscriptions when required by the threat model.

## Raw Spring WebSocket API

Implement `WebSocketHandler` (often `TextWebSocketHandler` for text frames) and register it through `WebSocketConfigurer` only when STOMP's protocol and broker abstraction are unnecessary. A `HandshakeInterceptor` can validate handshake attributes, but it is not a replacement for authentication and per-message authorization. Set allowed origins explicitly and apply the same resource limits as for STOMP.

Define a versioned message schema, maximum frame size, message validation, error response format, heartbeat/idle behavior and close-status policy. Do not block a handler on slow database or remote calls. Spring WebSocket sessions do not support concurrent sends: serialize writes or wrap sessions in `ConcurrentWebSocketSessionDecorator` with bounded send time and buffer limits. Remove connection-scoped state on close and transport errors.

## Scale and protect connections

- The simple STOMP broker keeps subscriptions in process memory. Each application replica has a separate view; a broadcast from one instance does not automatically reach clients connected to another. For multiple replicas, select an external broker relay or a deliberate shared fan-out design and verify user-destination/session routing.
- Configure connection and message limits: frame/message size, inbound channel queue and executor capacity, outbound send buffer and send time limit, concurrent sessions, idle timeout, and per-user/tenant connection quotas. Set finite values based on payload and load expectations.
- Apply per-principal message-rate and expensive-command limits. Reject overload promptly; do not create unbounded executor queues. See [rate-limiting.md](rate-limiting.md).
- Configure heartbeats and document client reconnect behavior. Clients should use bounded exponential backoff with jitter, resubscribe after reconnect, and tolerate duplicate or missed transient events. Persist events or expose a REST catch-up endpoint when clients need reliable recovery.
- Protect broker-relay credentials, restrict broker ACLs, configure TLS across network boundaries, and monitor broker connections and relay failures. Do not treat WebSocket delivery as durable exactly-once processing.
- Record connection counts, connect/disconnect/error rates, message rates, send-buffer saturation and broker health with bounded-cardinality dimensions. Never use session IDs, user IDs or destinations containing identifiers as metric tags. See [observability.md](observability.md).

## Testing

Test message handlers and authorization at the inbound-channel boundary, including valid and invalid payloads, unauthorized destinations, and `CONNECT` CSRF behavior. Add an end-to-end test with an embedded server and STOMP client for handshake, subscription, publish and disconnect behavior. For raw handlers, test handshake/origin policy, concurrent sends, close cleanup and frame limits. At least one integration test should exercise the selected broker topology when using an external relay. Do not substitute mocked `SimpMessagingTemplate` calls for the protocol boundary test.

## If on Spring Boot 3.5.x

Boot 3.5 uses Spring Framework 6.2 and Spring Security 6.x; the MVC starter coordinate and core STOMP/WebSocket model are the same, but use the APIs managed by the project's Boot BOM. Boot 3.5 also has separate WebSocket support for WebFlux applications; that reactive configuration is outside this MVC reference. Verify exact Security configuration signatures against the Boot-managed Spring Security version rather than copying Security 7-only code.

## Gotchas

- Agent chooses a simple broker for multiple replicas and assumes it broadcasts across the cluster.
- Agent secures only the HTTP handshake while leaving STOMP commands or subscriptions open.
- Agent treats allowed origins as authentication, or disables CSRF globally to get `CONNECT` working.
- Agent trusts a user or tenant ID supplied in a message body.
- Agent places bearer tokens in query strings or logs STOMP authentication headers.
- Agent assumes STOMP delivery is durable, ordered globally or exactly once.
- Agent allows unbounded connections, payloads, queues, send buffers or message rates.
- Agent sends concurrently on a raw WebSocket session without serialization or bounded buffering.
- Agent unit-tests a handler but never verifies handshake, authorization, subscription or actual message routing.

## Primary references

- [Spring Boot WebSockets](https://docs.spring.io/spring-boot/reference/messaging/websockets.html)
- [Spring Framework STOMP](https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html)
- [Spring Framework WebSocket server API](https://docs.spring.io/spring-framework/reference/web/websocket/server.html)
- [Spring Security WebSocket integration](https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html)
- [Spring Framework STOMP authentication](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication.html)
- [Spring Framework WebSocket testing](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/testing.html)
