# External Service Testing

Choose the test boundary based on the claim the test must prove. A mock can prove application branching; it cannot prove
HTTP serialization, authentication headers, timeout behavior or compatibility with the real provider. Use local
substitutes for deterministic failure cases and contract verification for the wire agreement.

## Test outbound HTTP clients at the boundary

Keep the HTTP client behind a small application port. Unit tests can fake that port to check domain decisions. Add
client-boundary tests that exercise the actual request factory and serialization against a local HTTP server, then
assert method, path, headers, body, response mapping, error translation and timeout behavior. Return representative
provider payloads, including unknown fields and malformed/error responses.

Use a local stub server such as WireMock or MockWebServer when tests need to control HTTP responses and verify requests.
Keep stubs deterministic, bind to an ephemeral port, reset mappings and scenarios between tests, and never share mutable
expectations across parallel test cases. Do not mock Spring's internal `RestClient` or `WebClient` chain; verify
behavior at the network boundary.

## Local development and provider contracts

For local development, provide a documented substitute or sandbox for each external dependency when feasible. A stub
should make its behavior visible and safe: use fake credentials, reject unintended calls, and avoid sending real email,
payments or customer data. Docker Compose can start compatible local services, but it does not prove compatibility with
the production provider.

For critical integrations, maintain a provider contract from the provider's OpenAPI/schema or a consumer-driven
contract. Validate that the client request and response assumptions still hold when the contract changes. If you own
both sides, run provider verification against the published consumer contracts. Contract tests complement a small number
of sandbox or controlled end-to-end checks; they do not prove provider availability or operational behavior.

Pin the provider API version where supported. Treat provider examples and recorded fixtures as data: remove secrets and
personal information, and update them intentionally when the contract changes.
See [HTTP clients](../integration/http-clients.md), [OpenAPI](../web/openapi.md) and [resilience](../integration/resilience.md).

## Messaging, email and object storage

Use Testcontainers when the production protocol or engine matters, such as a broker, database or S3-compatible object
store. Use a local mail sink for email rendering and delivery-flow tests. Keep a small provider sandbox test for
behavior that a local substitute cannot represent, and make it opt-in or isolated from ordinary CI if it is costly or
stateful.

Test failure and recovery as well as success: timeouts, connection refusal, 4xx/5xx mapping, malformed payloads,
duplicate delivery, retry limits, circuit state, and cleanup. Avoid tests that rely on public internet availability or
live shared credentials.

## Test configuration safely

Give test doubles explicit base URLs and fail fast if a test profile points to a production host. Do not let a fallback
configuration silently use real endpoints. Avoid logging full request/response bodies when they can contain tokens or
personal data. If parallel tests share one substitute, isolate scenarios by unique IDs and ensure cleanup cannot delete
another test's data.

## If on Spring Boot 3.5.x

The boundary-testing principles are the same. Boot-managed client and test APIs differ by Boot line;
follow [HTTP clients](../integration/http-clients.md) and the project's managed test dependencies rather than copying a Boot 4
snippet into a 3.5.x build.

## Gotchas

- Agent mocks the client internals and assumes the HTTP contract is verified - test through a local server at the
  network boundary.
- Agent points a test profile at a real endpoint when a stub is missing - fail closed instead of silently using
  production.
- Agent uses a local substitute as proof of provider compatibility - add a contract or controlled sandbox check for that
  claim.
- Agent shares mutable stub scenarios across parallel tests - isolate state and reset it between tests.
- Agent records real tokens or customer data in fixtures - sanitize fixtures and use fake credentials.
- Agent tests only successful responses - cover timeout, malformed response, status mapping, retries and duplicate side
  effects.

## Related

- [HTTP clients](../integration/http-clients.md) · [Integration testing](testing-integration.md) · [Testcontainers](testcontainers.md) · [Mail](../integration/mail.md) · [Messaging](../integration/messaging.md) · [Resilience](../integration/resilience.md)

## References

- [Spring Boot: Testing](https://docs.spring.io/spring-boot/reference/testing/)
- [WireMock Documentation](https://wiremock.org/docs/)
