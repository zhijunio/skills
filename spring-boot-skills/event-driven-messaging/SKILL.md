---
name: event-driven-messaging
description: >
  Use when implementing Kafka, RabbitMQ, or JMS producers and consumers in Spring Boot 4, or
  maintaining an existing Pulsar integration.
  Covers broker integration, event contracts, delivery, and dead-letter handling. For detailed
  duplicate-effect handling use idempotency-patterns; for retry policy use resilience-retry.
---

# Event-Driven Messaging

Design for at-least-once delivery unless the complete system proves stronger semantics.
Spring Boot 4.2 deprecates its Apache Pulsar support. Do not add a new Boot-managed Pulsar
integration by default; check the [4.2 release guidance](https://spring.io/blog/2026/11/19/spring-boot-4-2-0-available-now/)
and the Spring Pulsar compatibility line first.

## Dependencies and contracts

- Use the Boot starter for the selected integration. Add its documented test
  support when available; do not assume every broker integration has a Boot test starter.
- Let Boot manage supported Spring Kafka, AMQP, and Integration versions. For Pulsar, check the
  compatibility guidance above instead of assuming Boot manages the selected integration.
- Publish immutable envelopes containing event ID, type, timestamp, schema version, and payload.
- Treat event schemas as public APIs and evolve them compatibly.
- Do not serialize JPA entities or internal Jackson configuration as contracts.
- For module boundaries and reliable in-process events, use
  [Spring Modulith](../spring-modulith/SKILL.md).

## Producer rules

- Publish only after the originating state is durable.
- Use a [transactional outbox](../spring-boot-webmvc/references/transactions.md#side-effects-after-commit) when a database write and
  message must agree.
- Choose a stable aggregate key when per-aggregate ordering matters.
- Configure acknowledgements, delivery timeout, and serialization failure behavior explicitly.

## Consumer rules

- Make every handler idempotent with a durable marker or naturally idempotent state transition;
  see [idempotency patterns](../idempotency-patterns/SKILL.md) for duplicate-effect guarantees.
- Commit the state change and idempotency marker in one transaction. Read
  [application transactions](../spring-boot-webmvc/references/transactions.md) for the
  consumer's local transaction boundary; broker acknowledgement is a separate concern.
- Retry only transient failures with bounded exponential backoff; see
  [resilience and retry](../resilience-retry/SKILL.md) for exception classification,
  attempt budgets, and deadlines.
- Route permanent or exhausted failures to a dead-letter destination with diagnostic headers.
- Build replay as an explicit operation with authorization and auditability.

## Testing

- Use broker-specific test support or Testcontainers for broker integration.
- Test duplicate, reordered, delayed, incompatible, and poison events.
- Verify schema compatibility independently from handler tests.

## Examples

- Compare the [idempotent consumer](examples/idempotent-consumer.java) with the
  [consumer without idempotency](examples/consumer-without-idempotency.java), and see the
  [Kafka configuration](examples/kafka-config.yml).
- The executable fixture demonstrates consumer-side idempotency; it does not
  configure broker acknowledgements, offset commits, delivery retries, or a
  dead-letter policy. Adapt those settings to the selected broker and test the
  real listener/container boundary.

## Official sources

- Boot 4 messaging starters: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#starters
- [Spring Boot messaging support](https://docs.spring.io/spring-boot/reference/messaging/index.html)
- Spring Kafka reference: https://docs.spring.io/spring-kafka/reference/
- Spring AMQP reference: https://docs.spring.io/spring-amqp/reference/
- [Spring Pulsar reference](https://docs.spring.io/spring-pulsar/reference/)
- [Spring JMS reference](https://docs.spring.io/spring-framework/reference/integration/jms.html)

## Verification scope

The workflow fixture compiles idempotent-consumer.java directly and checks duplicate delivery,
concurrent invocation and claim/effect rollback against PostgreSQL. It calls the consumer
directly: broker serialization, listener wiring, offsets, retries and dead-letter delivery
are not covered. The examples focus on Kafka; broker-specific delivery guarantees and test
support must be checked against the selected integration's documentation.
See the [coverage matrix](https://github.com/rrezartprebreza/spring-boot-skills/blob/b90c3dbd9699c0d0bfb81660c857ef58262168e6/verification/MATRIX.md)
for pinned versions and remaining gaps.

## Gotchas

- Agent assumes broker transactions make external side effects exactly once - consumers still need idempotency.
- Agent retries every exception - classify permanent failures before retrying.
- Agent publishes directly after a repository call - use an outbox when atomicity matters.
- Agent uses old Boot 3 transitive dependencies - use Boot 4 managed integration and test dependencies.
- Agent sends framework entities as events - publish a stable versioned contract.
