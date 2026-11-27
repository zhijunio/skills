# Security Audit Events

Check the application's Actuator version before wiring audit event listeners.
The example record uses Java 17 and the standard library.

For general service telemetry, see [Spring Boot observability](../../spring-boot-observability/SKILL.md).

## Event contract

Choose events needed for the requested security or business requirement: authentication
decisions, access denial, privilege changes, sensitive exports and administrative actions.
Separate attempted, denied, successful and failed operations. Do not log SUCCESS before
the authoritative transaction commits.

Prefer typed fields: event ID, UTC time, verified actor ID, tenant ID, action, target ID,
outcome and server-generated correlation ID. Unauthenticated events may have no actor;
do not mislabel a supplied username as verified identity. Record only identifiers needed
for the investigation, with controlled access and retention.

[SecurityAuditRecord](../templates/SecurityAuditRecord.java) demonstrates bounded typed fields
and a restricted correlation identifier. It intentionally has no arbitrary message, headers,
token, request body or exception field. This is an event contract, not a durable audit sink.
Use a structured serializer, never concatenate JSON or interpolate untrusted text into lines.
The sample assumes tenant-scoped operations and UUID identities. Adapt those types and the
success invariant to the application's actual identity model; do not invent tenants for a
single-tenant system. Correlation IDs must be server-generated, not arbitrary caller text.

## Delivery and access policy

- Keep credentials, cookies, bearer tokens, passwords, session secrets, sensitive prompts,
  private documents and full configuration out of audit records. Sanitize controls and bound
  any separately approved free-text fields. Hashing arbitrary identifiers is not automatic anonymization.
- Spring Boot's in-memory audit repository is for development, not durable evidence.
  Authentication events alone do not capture every business authorization decision.
- For committed mutations requiring durable audit, use an atomic outbox alongside the
  [business transaction](../../spring-boot-webmvc/references/transactions.md#side-effects-after-commit).
  Deliver idempotently by event ID. Handle duplicate delivery, backpressure, retention,
  and retries. An after-commit listener can still lose events on process failure.
- Explicitly choose failure behavior by operation. A required audit write may need to prevent
  a privileged mutation; optional read-event loss may instead trigger bounded buffering and alerts.
  Do not globally fail every request or silently drop required audit events.
- Separate application write permissions from investigator read and retention/delete permissions.
  Define export controls, clock synchronization, access auditing and integrity checks.
  Append-only configuration alone is not proof of tamper resistance.
- Do not publish Actuator auditevents, env or configprops to solve an investigation.
  Collection and external export require approved scope and a redacted handoff.

## Delivery checks

Exercise malformed/oversized correlation IDs, missing fields, all outcomes and stable event IDs.
Application integration tests must also prove rollback emits no committed-success event,
outbox retry deduplicates, and sink failures follow the selected policy.

Provide a field inventory, access/retention policy, delivery semantics, failure matrix and
tests run. Use the [shared evidence criteria](../SKILL.md#5-verify-the-path)
when reporting gaps.
See [good design](../examples/audit-logging.md) and [bad design](../examples/audit-logging-data-loss.md).

## Official sources

- [Boot auditing](https://docs.spring.io/spring-boot/reference/actuator/auditing.html)
- [OWASP logging guidance](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [Spring transaction-bound events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
