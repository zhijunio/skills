# Outbound Email

Use Spring Boot's `JavaMailSender` for application email. Keep production SMTP settings in the
deployment environment. Use [Spring Boot Docker](../../spring-boot-docker/SKILL.md) to run Mailpit
locally.

## Configure SMTP

Add `spring-boot-starter-mail` and let Spring Boot manage its compatible version. Keep host, port,
credentials, sender address, and TLS settings in external configuration. Never commit credentials
or point production at Mailpit. Configure bounded connect/read/write timeouts and require TLS with certificate/hostname verification according to the provider transport. Do not disable verification to make delivery work.

Local configuration can target Mailpit on the host:

```yaml
spring:
  mail:
    host: localhost
    port: 1025
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false
```

When the application runs inside Compose, use `mailpit` as the host. Mailpit accepts SMTP on
`1025` and exposes its web UI on `8025`.

## Compose and send messages

Send through a constructor-injected `JavaMailSender` in an application service. Keep composition
out of controllers. Set the sender, recipient, subject, and text or HTML body explicitly, and
validate user-controlled addresses, reject header control characters, and authorize recipients and attachments. Bound message size and recipient count; avoid exposing recipient lists or message bodies in logs.

Treat delivery as an external side effect. Do not send before a transaction commits when the email
describes committed state. Use bounded retries for transient failures; use an outbox when durable
delivery is required. An outbox alone does not prevent duplicate SMTP delivery: a crash after provider acceptance but before marking sent leaves an uncertain outcome. Use provider idempotency when available, otherwise define reconciliation or an explicit duplicate-tolerance policy. SMTP acceptance is not proof of inbox delivery.

## HTML templates

Use a template engine when messages contain dynamic HTML. Add `spring-boot-starter-thymeleaf` only
if the project does not already include it. Store mail templates under
`src/main/resources/templates/mail/`, render them in a dedicated component, and keep subject,
template name, and model data in the mail application service. Do not concatenate HTML strings.

`th:text` escapes inserted values. Keep user-provided values out of `th:utext` unless they have
been sanitized for HTML.

```html
<!doctype html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<body>
<p>Hello, <span th:text="${displayName}">there</span>.</p>
<p>Welcome to <span th:text="${productName}">our service</span>.</p>
</body>
</html>
```

```java
@Component
class MailTemplateRenderer {

	private final SpringTemplateEngine templateEngine;

	MailTemplateRenderer(SpringTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	String renderWelcome(String displayName, String productName) {
		var context = new Context(Locale.ENGLISH);
		context.setVariable("displayName", displayName);
		context.setVariable("productName", productName);
		return templateEngine.process("mail/welcome", context);
	}
}

@Service
class WelcomeEmailService {

	private final JavaMailSender mailSender;
	private final MailTemplateRenderer renderer;

	WelcomeEmailService(JavaMailSender mailSender, MailTemplateRenderer renderer) {
		this.mailSender = mailSender;
		this.renderer = renderer;
	}

	void send(String recipient, String displayName) throws MessagingException {
		var message = mailSender.createMimeMessage();
		var helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
		helper.setFrom("no-reply@example.com");
		helper.setTo(recipient);
		helper.setSubject("Welcome");
		helper.setText(renderer.renderWelcome(displayName, "Example"), true);
		mailSender.send(message);
	}
}
```

## Test delivery

Test the renderer with a real Thymeleaf engine and classpath template. Assert that rendered values
are escaped. Test the sender service with a mocked `JavaMailSender` that returns a real
`MimeMessage`; capture the sent message and check its recipient, subject, and body.

Use Mailpit for SMTP integration checks. Send through the configured host and port, then confirm
the message appears at `http://localhost:8025` with the expected recipient, subject, and body. This
checks SMTP configuration and transport, not delivery by a production provider. Never send tests to
real addresses.

## Verify

Inspect mail configuration, templates, sender call sites, tests, and `compose.yaml` when local mail
is in scope. Confirm credentials are external, local mail routes to Mailpit, user content is
escaped, and production configuration does not point to the development capture service.
