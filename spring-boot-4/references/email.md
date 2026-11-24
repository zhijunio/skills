# Email

Read this when sending email from a Spring Boot application. The local Compose
service is Mailpit, which captures messages for development and testing;
production SMTP settings come from the deployment environment.

## When to use

- Send transactional or notification email
- Configure SMTP or `JavaMailSender`
- Test email delivery locally with Mailpit

## When not to use

- Inbound email processing or mailbox polling
- Email identity, deliverability, or provider administration

## Rules

1. Add `spring-boot-starter-mail`; let Spring Boot dependency management provide a compatible
   version when available; otherwise use the project's dependency convention. Use Boot's `JavaMailSender` rather than constructing a mail
   transport in application code.
2. Keep SMTP host, port, credentials, sender address, and TLS settings in
   external configuration. Never commit credentials. Use environment-backed
   properties for deployment configuration.
3. Send email from an application service with constructor-injected
   `JavaMailSender`. Keep message composition separate from HTTP controllers.
   Use explicit sender, recipient, subject, and text/HTML content; validate
   user-controlled addresses. If HTML is assembled from data, use a template
   engine with context-appropriate escaping.
4. Treat SMTP delivery as an external side effect. Do not send before a
   database transaction commits when the message describes committed state.
   Handle transient delivery failures with bounded retries or an outbox when
   delivery guarantees require it; avoid duplicate sends on retries.
5. Use Mailpit from [docker.md](docker.md) for local
   development. Configure the app to use host `mailpit` and SMTP port `1025`
   inside Compose networking. The Mailpit web UI is exposed on port `8025`.
   Do not configure Mailpit as a production relay.
6. Test message composition and delivery intent without sending to real
   addresses. Unit tests use a mock sender and assert the constructed message;
   Mailpit verifies SMTP configuration and actual delivery in integration
   checks. Never send test messages to real addresses.
7. For template-based messages, use Thymeleaf templates under
   `src/main/resources/templates/mail/`. Add `spring-boot-starter-thymeleaf`
   only when the application does not already include it. Render templates in
   a dedicated component and keep subject, template name, and model data in
   the mail application service. Do not build HTML with string concatenation.

Local application configuration:

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

When the app itself runs in Compose, use `mailpit` as the host instead of
`localhost`. Supply production host, credentials, and TLS requirements through
the deployment environment.

## Thymeleaf template example

Template at `src/main/resources/templates/mail/welcome.html`:

```html
<!doctype html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<body>
<p>Hello, <span th:text="${displayName}">there</span>.</p>
<p>Welcome to <span th:text="${productName}">our service</span>.</p>
</body>
</html>
```

Thymeleaf's `th:text` escapes inserted text. Keep user-provided values out of
`th:utext` unless they have been sanitized for HTML.

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

## Testing

Unit test the renderer using a real Thymeleaf engine and the classpath
template; assert the rendered output contains escaped values. Unit test the
sender service with a mocked `JavaMailSender`, returning a real
`MimeMessage` from `createMimeMessage()`, then capture the sent message and
assert recipient, subject, and HTML body. Keep these tests independent of
Spring and SMTP.

For an SMTP integration check, start Compose and send a message through the
configured host and port. Verify it appears in Mailpit at `http://localhost:8025`
with the expected recipient, subject, and rendered body. This checks the SMTP
connection and transport; it does not prove delivery by a production mail
provider.

## Verify

Open the active build configuration, mail configuration, templates, sender call sites, tests, and
`compose.yaml`. Confirm credentials are externalized, local mail routes to
Mailpit, templates escape user content, and no production configuration
points at the development capture service.
