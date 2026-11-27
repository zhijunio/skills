---
name: spring-task-execution
description: >
  Implement or troubleshoot Spring Boot 4 @Async and @Scheduled work, including
  executor saturation, asynchronous failures, thread context, cron, time zones,
  overlapping runs, and multi-instance scheduling.
---

# Spring Task Execution

Use the project's Spring Boot BOM and preserve its executor and scheduler choices.
Check the effective Boot minor before changing auto-configuration. Virtual-thread
execution requires Java 21 or later and explicit Boot configuration.

- Read [async execution](references/async.md) for dispatch, bounded executors,
  failure handling, transaction boundaries, and context propagation.
- Read [scheduling](references/scheduling.md) for trigger configuration, overlap,
  replica coordination, and missed runs.
- When a schedule dispatches async work, apply both guides. Decide whether the next
  trigger can submit work before the previous job finishes, and make rejection visible.

If required work must survive a restart, persist its intent and progress, or use
the existing durable job or messaging framework. Keep Spring Batch chunk and
restart mechanics with the batch workflow. Use [event-driven messaging](../event-driven-messaging/SKILL.md)
for broker-backed delivery and [idempotency patterns](../idempotency-patterns/SKILL.md)
when retries or redelivery can repeat a business effect.
