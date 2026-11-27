# Problem Details - RFC 9457

## Enable Problem Details

For an API using Problem Details, enable Spring's built-in MVC exception handling:

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

For WebFlux use `spring.webflux.problemdetails.enabled` and reactive exception handling;
the servlet templates below are not WebFlux handlers.

## Domain errors

Use [DomainException](../templates/DomainException.java) together with
[ProblemDetailExceptionHandler](../templates/ProblemDetailExceptionHandler.java).
The handler maps all subclasses using their declared status and stable error code:

- [OrderNotFoundException](../templates/OrderNotFoundException.java): 404.
- [InsufficientInventoryException](../templates/InsufficientInventoryException.java): 422.
- [BusinessViolationException](../templates/BusinessViolationException.java): 422.

Each public class has its own file. These are API-facing exceptions with HTTP status metadata;
for a framework-free domain, keep domain exceptions independent and map them in the web adapter.
Do not expose arbitrary persistence or infrastructure exception messages.

The advice extends `ResponseEntityExceptionHandler` to preserve Spring's handling of framework
exceptions. Validation returns 400 with field violations; unexpected failures return a generic
500 while retaining the full exception only in server logs. Keep nullable validation messages safe.
Filter-level authentication failures need an `AuthenticationEntryPoint`; authorization failures
need an `AccessDeniedHandler`. Controller advice does not cover the security filter chain.
Method-security (`@PreAuthorize`) denials are the opposite case: `AccessDeniedException` is thrown
inside the dispatcher, so the advice must map it to 403 or the `Exception` catch-all returns 500.
The template uses Spring Security's `AccessDeniedException`. In an application without Spring
Security, omit that handler and its import.

## Response fields

```json
{
  "type": "https://api.example.com/errors/order_not_found",
  "title": "Not Found",
  "status": 404,
  "detail": "Order not found",
  "instance": "/api/orders/123",
  "errorCode": "ORDER_NOT_FOUND"
}
```

Use project-owned, stable URIs for custom problem types. An explicit `type` is optional;
when omitted it defaults to `about:blank`. Its title should then match the HTTP status phrase.
The HTTP status and the body status must agree. Use `application/problem+json` for JSON problems.
Use extensions such as `errorCode` or `violations` for machine-readable details instead of
requiring clients to parse human-readable messages.

See the [anti-pattern](../examples/exception-handler-exposes-details.java).

## Verification

Test actual HTTP responses for domain 404/422, validation 400, unexpected 500, and framework
errors such as malformed JSON and unsupported methods. Assert content type, status, stable
error codes and absence of stack traces or internal messages. Separately test filter 401/403.

## Official sources

- [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)
- [Spring MVC error responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

## Gotchas

- Agent handles a different exception class than the domain throws - test the concrete exceptions.
- Agent requires an explicit type for every error - about:blank is the default.
- Agent relies on advice for filter exceptions - configure the security handlers separately.
- Agent returns internal exception messages in 500 responses - log privately and return generic detail.
