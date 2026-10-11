# 消息

## 何时应该使用 Broker

消息系统可以跨进程解耦、缓冲和重试；代价是最终一致性、每个队列新增一种故障模式，以及无法再从单一堆栈跟踪中完成的调试过程。

不要为了让同一应用中的两个类解耦就引入消息系统——应使用进程内事件（参见 [spring-modulith.zh-CN.md](spring-modulith.zh-CN.md)），它支持事务、有序且易于调试。Broker 用于跨进程边界、吸收负载峰值，或向不受你控制的消费者分发消息。

| Broker | 适用场景 |
|---|---|
| **Kafka** | 事件流、重放、高吞吐、按 Key 保序、多个独立 Consumer Group 读取同一数据 |
| **RabbitMQ** | 工作队列、逐消息路由、优先级、延迟投递、竞争处理离散任务的消费者 |
| **JMS**（Artemis、IBM MQ） | 必须集成的现有企业 Broker |

关键区别：Kafka 是消费者按各自 Offset 读取的**日志**；Rabbit 是消息投递并确认后即消失的**队列**。

## Kafka

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

版本由 Boot 管理，不要手动锁定。

### 生产消息

```java
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    OrderEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void publish(OrderPlaced event) {
        kafka.send("orders.placed", event.orderId().value().toString(), event)
                .whenComplete((result, failure) -> {
                    if (failure != null) {
                        log.error("Failed to publish OrderPlaced for {}", event.orderId(), failure);
                    }
                });
    }
}
```

**Key 决定分区，分区决定顺序。**以订单 ID 为 Key 可保证同一订单的所有事件按序处理。不设置 Key 会轮询分区，完全不保证顺序；这是默认行为，也是“事件乱序”的常见来源。

`send` 返回 Future。忽略它会导致发布失败悄悄发生。

### 消费消息

```java
@Component
class OrderPlacedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedConsumer.class);

    private final InventoryService inventoryService;
    private final ProcessedMessageStore processed;

    @KafkaListener(topics = "orders.placed", groupId = "inventory-service")
    void on(@Payload OrderPlaced event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        if (!processed.markIfNew(event.eventId())) {
            log.debug("Skipping already-processed event {}", event.eventId());
            return;
        }
        inventoryService.reserve(event.orderId());
    }
}
```

`groupId` 决定扩缩容单位和独立消费边界。两个使用不同 Group ID 的应用都会收到每条消息；共享 Group ID 的两个实例则会分摊分区。

### 序列化

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      properties:
        enable.idempotence: true
    consumer:
      group-id: inventory-service
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
        spring.json.trusted.packages: com.example.app.order.events
    listener:
      ack-mode: record
```

其中四项至关重要：

- **`spring.json.trusted.packages`**：绝不要设为 `*`。`JsonDeserializer` 会实例化消息 Header 中指定的类；向可被任意写入的 Topic 开放所有包，会形成反序列化 Gadget 攻击面。
- **用 `ErrorHandlingDeserializer` 包装实际反序列化器**。否则无法解析的消息会在 Listener 运行前抛异常，Error Handler 不会触发，Offset 也不会前进，Consumer 会无限重试该消息。这就是典型的毒丸消息卡死。
- **`acks: all` + `enable.idempotence: true`**：否则 Broker 故障转移可能静默丢失或重复发布记录。
- **`enable-auto-commit: false`**：自动提交按定时器确认；处理中途崩溃会丢失已标记为消费的消息。

### 错误处理与死信 Topic

```java
@Bean
DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> template) {
    var recoverer = new DeadLetterPublishingRecoverer(template,
            (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

    var handler = new DefaultErrorHandler(recoverer, new ExponentialBackOff(500L, 2.0));
    handler.addNotRetryableExceptions(ValidationException.class, IllegalArgumentException.class);
    return handler;
}
```

对暂时性故障重试，其他消息进入死信队列。格式错误的载荷不会因重试而成功；重试只会阻塞分区，使后面的消息全部等待。

长退避场景可使用 `@RetryableTopic`，它通过单独的延迟 Topic 重试，而不是阻塞 Consumer 线程：

```java
@RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2.0),
                dltTopicSuffix = ".DLT")
@KafkaListener(topics = "orders.placed", groupId = "inventory-service")
void on(OrderPlaced event) { … }
```

**无人监控的 DLT 是静默数据丢失通道。**应对 DLT 深度告警，参见[可观测性](observability.zh-CN.md)。

## RabbitMQ

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```java
@Configuration
class OrderQueueConfig {

    @Bean
    Queue orderQueue() {
        return QueueBuilder.durable("orders.placed")
                .deadLetterExchange("orders.dlx")
                .deadLetterRoutingKey("orders.placed.dead")
                .build();
    }

    @Bean
    Queue orderDeadLetterQueue() {
        return QueueBuilder.durable("orders.placed.dead").build();
    }

    @Bean
    MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new Jackson2JsonMessageConverter(jsonMapper);
    }
}
```

```java
@RabbitListener(queues = "orders.placed")
void on(OrderPlaced event) {
    inventoryService.reserve(event.orderId());
}
```

- **声明队列时同时声明死信 Exchange。**队列参数不可变；事后添加 DLX 必须删除并重建队列，线上操作会丢失队列现有内容。
- 使用**持久队列和持久消息**，否则 Broker 重启会丢弃消息。
- 启用**Publisher Confirm**（`spring.rabbitmq.publisher-confirm-type=correlated`），以便发现发布失败。
- Listener 抛异常默认会重新入队；遇到无法修复的消息时会高速无限循环。配置重试和 `RepublishMessageRecoverer`，让耗尽重试次数的消息进入 DLQ。

## Consumer 必须幂等

**所有 Broker 都至少投递一次。**崩溃、Rebalance 或确认消息时连接中断都会造成重投，这是正常情况而不是错误。Consumer 不幂等就可能重复扣款。

有两种可行方式：

```java
// 1. 按消息 ID 去重，由唯一约束负责并发竞争
@Transactional
public void handle(OrderPlaced event) {
    try {
        processed.save(new ProcessedMessage(event.eventId()));   // event_id 上有唯一索引
    } catch (DataIntegrityViolationException duplicate) {
        return;                                                   // 已处理
    }
    inventoryService.reserve(event.orderId());
}
```

```java
// 2. 让操作天然幂等，例如有条件的状态转换
@Transactional
public void handle(OrderPlaced event) {
    var reservation = reservations.findByOrderId(event.orderId())
            .orElseGet(() -> Reservation.create(event.orderId()));
    reservation.ensureReserved();     // 已预留时不执行操作
    reservations.save(reservation);
}
```

领域允许时优先采用第二种方式：不需要额外表，也不需要清理任务。

发布的每个事件都应包含稳定的 `eventId`，否则无法去重。

## 事务 Outbox

数据库写入和 Broker 发布属于两个没有共享事务的系统；任一方都可能在另一方成功后失败：

```java
// ❌ 发布失败后，订单已经存在却无人收到通知
@Transactional
public OrderId place(PlaceOrderCmd cmd) {
    var order = orderRepository.save(Order.create(cmd.customerId()));
    kafka.send("orders.placed", OrderPlaced.from(order));
    return order.id();
}
```

在**同一个事务中**写入消息，再由 Relay 投递：

```java
@Transactional
public OrderId place(PlaceOrderCmd cmd) {
    var order = orderRepository.save(Order.create(cmd.customerId()));
    outbox.save(OutboxMessage.of("orders.placed", order.id().value().toString(), OrderPlaced.from(order)));
    return order.id();                              // 两次写入在同一个事务中
}
```

```java
@Component
class OutboxRelay {

    @Scheduled(fixedDelay = 500)
    @Transactional
    void relay() {
        for (var message : outbox.findUnpublished(Limit.of(100))) {
            kafka.send(message.topic(), message.key(), message.payload());
            message.markPublished();
        }
    }
}
```

Relay 至少投递一次：消息可能已发布，但 Relay 在标记前崩溃。这正是 Consumer 必须幂等的原因。

**Spring Modulith 已实现该机制。**其事件发布注册表和事件外部化功能可以提供 Outbox，不需要手写表和 Relay。优先使用它，参见 [spring-modulith.zh-CN.md](spring-modulith.zh-CN.md)。不使用 Modulith 时再编写上面的手动实现。

## 不要在事务提交前直接发布

即使使用 Outbox，在事务中直接发布也会发送描述尚可能回滚状态的消息。应将发布绑定到事务提交时机，使用 `@TransactionalEventListener(AFTER_COMMIT)`（参见[事务](transactions.zh-CN.md)）。

## Schema 演进

你无法控制的 Consumer 可能仍在运行旧版事件。应将事件视为已发布契约：

- **只添加可选字段**，不要删除或重命名字段。
- 不要在保留字段名的同时改变字段含义。
- 破坏性变更应使用**新 Topic**或新事件类型，并与旧版本并行运行，直到 Consumer 完成迁移。
- 设置 `spring.json.value.default.type` 或忽略未知属性，避免新增字段导致旧 Consumer 失败。

使用 Avro 或 Protobuf 的 Schema Registry 可以强制执行兼容性。当超过两个团队消费事件时，通常值得引入。

## 测试

使用 Testcontainers 连接真实 Broker；嵌入式或 Mock Broker 无法复现 Rebalance、Offset 提交和 DLT 路由：

```java
@SpringBootTest
@Testcontainers
class OrderEventIntegrationTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

    @Test
    void publishesOrderPlaced() {
        orderService.place(cmd);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(reservations.findByOrderId(orderId)).isPresent());
    }
}
```

`@ServiceConnection` 无需注册属性即可连接 Broker（参见 [testcontainers.zh-CN.md](testcontainers.zh-CN.md)）。消息处理是异步的，因此应**等待断言，绝不要使用 `Thread.sleep`**。也要测试 DLT 路径：发布无法解析的消息，并断言它进入 DLT。

## 使用 Boot 3.5.x 时

Spring Kafka 和 Spring AMQP 的用法相同，只有两点不同：Rabbit JSON Converter 接收 Jackson 2 的 `ObjectMapper`，而不是 `JsonMapper`；Testcontainers 使用 1.x 坐标。两条版本线都支持 Kafka 和 RabbitMQ 的 `@ServiceConnection`。

## 常见问题

- Agent 使用 Broker 解耦同一应用中的两个类：应使用进程内事件。
- Agent 不设置 Key 却期望有序：Key 决定分区；没有 Key 就无法保证顺序。
- Agent 忽略 `send` Future：发布失败会静默发生。
- Agent 设置 `spring.json.trusted.packages=*`：这会形成反序列化 Gadget 攻击面。
- Agent 未配置 `ErrorHandlingDeserializer`：一条毒丸消息就可能永久卡住分区。
- Agent 保留 `enable-auto-commit`：Offset 会定时提交，崩溃时会丢失处理中消息。
- Agent 忽略 `acks: all` 和幂等：故障转移可能丢失或重复记录。
- Agent 重试格式错误的消息：它不会成功；应转入死信队列以保持分区继续运行。
- Agent 创建 DLT 后不监控：这会导致静默数据丢失；应对消息深度告警。
- Agent 编写非幂等 Consumer：每个 Broker 都是至少投递一次。
- Agent 不发布事件 ID：无法去重。
- Agent 在数据库事务中直接发布消息：应使用 Outbox 或绑定到 `AFTER_COMMIT`。
- Agent 在已有 Spring Modulith 时手写 Outbox：Modulith 已提供该能力。
- Agent 声明 Rabbit Queue 却未配置 DLX：队列参数不可变，之后添加必须重建队列。
- Agent 让 Rabbit Listener 每次失败都重新入队：无法修复的消息会高速循环。
- Agent 删除或重命名已发布事件的字段：旧版 Consumer 仍在运行；只添加字段。
- Agent 使用嵌入式 Broker 测试：应使用 Testcontainers；嵌入式 Broker 无法复现 Rebalance 和 DLT 路由。
- Agent 在消息测试中使用 `Thread.sleep`：应等待断言。

## 相关文档

- [Spring Modulith](spring-modulith.zh-CN.md) · [事务](transactions.zh-CN.md) · [弹性模式](resilience.zh-CN.md) · [领域建模](domain-modelling.zh-CN.md) · [可观测性](observability.zh-CN.md) · [Testcontainers](testcontainers.zh-CN.md)
