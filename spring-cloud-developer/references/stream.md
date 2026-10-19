# Spring Cloud Stream

Use Spring Cloud Stream when a service should communicate through broker-backed destinations with binder-managed
connectivity. Choose it for messaging semantics that help the application; do not add a broker or Stream merely to
make a synchronous service call appear asynchronous. For direct HTTP calls, use the
[service-to-service HTTP client guidance](http-clients.md).

## Start with the functional model

Define message handlers as `Supplier`, `Function`, or `Consumer` beans. Bind only the functions that should be
active, and configure their destinations explicitly:

```java
@Bean
Consumer<OrderPlaced> reserveInventory(InventoryService inventoryService) {
    return event -> inventoryService.reserve(event.orderId(), event.items());
}
```

```yaml
spring:
  cloud:
    function:
      definition: reserveInventory
    stream:
      bindings:
        reserveInventory-in-0:
          destination: orders.placed
          group: inventory-reservation
```

The function name determines binding names such as `reserveInventory-in-0`. An explicit consumer group gives instances
a shared durable subscription where the binder supports that model; anonymous groups commonly give each instance its
own subscription. Verify the exact behavior with the selected binder. Avoid binding every function on the classpath by
accident.

## Model delivery and failure behavior

- Assume a message can be delivered more than once. Make handlers idempotent with a durable event/message ID or an
  inbox/processed-message record when duplicate side effects matter.
- Define ordering and partition keys from the domain invariant. Ordering is generally limited to a partition, key, or
  queue, and increasing partitions can change ordering and parallelism.
- Configure bounded retries for transient failures only. Permanent validation or authorization failures should not be
  retried indefinitely. Retry behavior, backoff, requeue, and dead-letter support vary by binder and function model.
- Assign a dead-letter destination, retention, alert owner, and replay procedure. A DLQ without monitoring and a safe
  replay process is delayed data loss.
- Do not claim exactly-once business effects from broker delivery settings. A database commit and broker acknowledgment
  are not generally one atomic operation. Use an outbox for reliable publication and idempotent consumption for
  duplicate delivery.

## Schema, security, and operations

Treat message payloads as versioned service contracts. Prefer explicit event types, stable field semantics, and
backward-compatible evolution. Do not serialize arbitrary Java types or trust a message because it came through a
broker. Validate payloads and authorize sensitive actions using trusted service identity and domain rules.

Keep broker credentials and TLS material outside source-controlled configuration. Scope broker permissions to the
required topics, queues, consumer groups, and operations. Configure bounded producer/consumer concurrency and message
size limits. Monitor consumer lag, delivery failures, retry counts, DLQ depth, and processing latency with bounded
labels; do not use message IDs or customer identifiers as metric tags.

## Test the application and broker boundary

Use the Spring Cloud Stream test binder to verify function behavior and binding-level message flow without a broker.
It does not prove serialization compatibility, broker ACLs, partitioning, consumer-group behavior, redelivery, or the
selected broker's DLQ configuration. Add integration tests with the actual broker implementation when those guarantees
matter. Test duplicate delivery, poison messages, retries, recovery, and replay as well as the happy path.

## Gotchas

- Agent uses the legacy annotation/channel model for a new application - start with functional beans unless the existing
  project requires the legacy model.
- Agent omits the consumer group and assumes all binder subscriptions are durable shared work queues - specify and test
  group semantics.
- Agent assumes Kafka and Rabbit binders have identical retry, ordering, and DLQ settings - verify each selected binder.
- Agent retries a non-idempotent handler and causes duplicate writes - make side effects idempotent or use an inbox.
- Agent treats the test binder as proof of broker behavior - test the actual broker boundary for broker-specific claims.
- Agent claims exactly-once effects from delivery configuration - use outbox/inbox patterns across database and broker.
- Agent configures a DLQ without retention, monitoring, or replay ownership - define the complete operational path.

## Sources

- [Spring Cloud Stream reference](https://docs.spring.io/spring-cloud-stream/reference/)
- [Spring Cloud Stream functional binding names](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/functional-binding-names.html)
- [Spring Cloud Stream error handling](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/overview-error-handling.html)
- [Spring Cloud Stream test binder](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/spring_integration_test_binder.html)
- [Spring Cloud Stream Kafka dead-letter processing](https://docs.spring.io/spring-cloud-stream/reference/kafka/kafka-binder/dlq.html)
