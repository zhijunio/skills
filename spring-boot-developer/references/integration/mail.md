# Email

Email is an outbound integration, not part of a database transaction. Keep message composition behind an application service and a small mail adapter so business code does not depend on SMTP details.

## Configuration

Use Spring Boot's managed mail starter and let Boot select compatible client versions. Configure host, port, TLS and credentials through external configuration; never commit production credentials. Set finite connection, read and write timeouts because mail defaults can block indefinitely:

```yaml
spring:
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}
    properties:
      "[mail.smtp.connectiontimeout]": 5000
      "[mail.smtp.timeout]": 3000
      "[mail.smtp.writetimeout]": 5000
```

The local values above target a development SMTP sink. Use TLS and secret-store credentials in deployed environments. Do not enable startup connection checks unless SMTP availability should gate application readiness.

## Message boundary

Expose a domain operation such as `sendOrderConfirmation(OrderId)` rather than accepting arbitrary sender, recipient and body values from controllers. Validate recipient addresses, use an application-owned sender identity, and make templates handle HTML escaping. Keep subject and body free of untrusted header input; CR/LF in headers can enable injection.

Use `JavaMailSender` / MIME messages for text, HTML and attachments. Keep large attachments in file storage and link to an authorized download when practical. Avoid logging full message bodies, attachments, credentials or personal data.

Render templates with the recipient's intended locale and escape untrusted HTML values. Validate attachment count and size before queueing; prefer short-lived authorized download links for large files. Treat SMTP acceptance as a handoff, not proof of inbox delivery. Track provider delivery/bounce events where available, distinguish permanent recipient failures from transient transport failures, and suppress addresses that repeatedly hard-bounce.

## Transaction timing and delivery guarantees

Do not send email inside a database transaction. A rollback cannot recall a message already accepted by the SMTP server. For non-critical notifications, publish an application event and handle it after commit; asynchronous delivery prevents the request thread from waiting, but an in-memory event can be lost if the process stops after commit.

For important business communication, write an outbox record in the same database transaction as the business change. A worker sends pending messages and records attempts/status. Design retries for ambiguous SMTP outcomes: the server may have accepted a message even if the client timed out. Use a stable message/event ID, bounded exponential backoff, a maximum attempt policy and operator-visible dead-letter state. SMTP itself does not guarantee exactly-once delivery.

Make worker claiming safe across instances with a lease or row-lock strategy, and make duplicate sends detectable. Monitor oldest pending age, attempt count, permanent failures and bounce rate; queue depth alone can hide a stuck queue. Define retention for message metadata and avoid storing full bodies unless required.

See [transactions.md](../persistence/transactions.md), [messaging.md](messaging.md) and [async-and-scheduling.md](async-and-scheduling.md).

## Local development and tests

Use [Mailpit](https://mailpit.axllent.org/docs/) as a local SMTP sink so developers can inspect messages without sending external email. Its default SMTP port is `1025`; the web UI is available on `8025`. When the application runs on the host, configure `localhost:1025`; when it runs in the same Compose network, use `mailpit:1025`. See [Docker](../deployment/docker.md) for the Compose service and port mappings, and [local-development.md](../tooling/local-development.md) for the development workflow. Keep the same mail adapter in production and local profiles, changing only configuration.

Test message construction and business decisions without a live SMTP server. Add an integration test against a local mail container for MIME encoding, template rendering and actual delivery handoff. Test failure/retry state and ensure a transaction rollback does not send a notification. Do not replace delivery-boundary tests with mocks alone.

## Gotchas

- Agent sends email before the database transaction commits - bind delivery to after-commit or persist an outbox record.
- Agent uses an in-memory async event for critical delivery - persist intent so a crash cannot lose it.
- Agent retries every SMTP exception as if delivery definitely failed - acceptance may be ambiguous; make the operation idempotent and bounded.
- Agent leaves SMTP timeouts infinite - set connection, read and write timeouts.
- Agent logs message bodies or secrets - log message IDs and delivery state, not sensitive content.
- Agent uses a user-controlled sender or subject - keep sender identity application-owned and reject header control characters.
- Agent sends real emails from tests or local development - use a local sink and isolated test configuration.

## Related

- [transactions.md](../persistence/transactions.md) · [messaging.md](messaging.md) · [async-and-scheduling.md](async-and-scheduling.md) · [local-development.md](../tooling/local-development.md) · [observability.md](../observability/observability.md)

## References

- [Spring Boot: Sending Email](https://docs.spring.io/spring-boot/reference/io/email.html)
- [Jakarta Mail](https://jakarta.ee/specifications/mail/)
