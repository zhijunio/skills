---
name: spring-boot-webmvc
description: >
  Design or change Spring Boot MVC controllers, request binding and validation,
  REST responses, pagination, and RFC 9457 error handling. Use also for Spring
  application-service transaction boundaries, propagation, rollback, and commit-bound
  events. Preserve existing HTTP contracts.
---

# Spring Boot Web MVC

## Project contract

Inspect existing controllers, tests and OpenAPI. Preserve the established paths,
ID type, success shape, error policy, and pagination when adding or changing an
endpoint. For a new API, use resource DTOs as success bodies and plural,
kebab-case resource paths such as `/api/orders` or `/api/order-items`.

Use [API versioning](../spring-boot-api-versioning/SKILL.md) when introducing or changing
version resolution.

## Controllers and application services

Keep request binding, validation, and HTTP response mapping in the controller.
Use constructor injection and delegate business operations to an application
service. Validate request bodies with `@Valid` and put business rules in the
service. Map persistence models to DTOs. See the [controller example](examples/controller.java).
The [entity-leaking controller](examples/controller-leaks-entities.java) shows the response
coupling to avoid.

Place the transaction for a multi-step operation on the application service
method that coordinates the use case. Controllers call the service through its
Spring-managed bean and map its result or exception to HTTP. Read
[application service transactions](references/transactions.md) when changing
propagation, rollback, retries, or commit-bound events.

## Status codes

| Scenario | Status |
|----------|--------|
| Successful read or update with a body | 200 |
| Resource created | 201, with a Location header when the URI is known |
| Successful deletion | 204 with an empty body |
| Invalid input | 400 |
| Valid request rejected by a business rule | 422 |
| Unauthenticated | 401 |
| Forbidden | 403 |
| Resource missing | 404 |
| Conflict with current resource state | 409 |

## Pagination

For a new paginated Spring MVC endpoint, return `Page<ResponseDto>` and enable
Spring Data's stable DTO serialization. This yields `content` and a nested
`page` object with `number`, `size`, `totalElements`, and `totalPages`.
Use `Pageable` for zero-based page requests and choose a page-size cap
appropriate to the endpoint. Validate sort fields and break ties with a unique ID.
If Spring Data Commons is missing, add `org.springframework.data:spring-data-commons`
with the Boot-managed version.

```yaml
spring:
  data:
    web:
      pageable:
        serialization-mode: via-dto
        default-page-size: 20
        max-page-size: 100
```

If the application already exposes a custom page or success envelope, preserve that
JSON. [PageResponse](templates/PageResponse.java) illustrates a project-owned
page shape. Preserve an existing contract and assert its full shape in MVC tests.

## Error responses

Use RFC 9457 ProblemDetail for API errors. Inspect existing advice, security
entry points, and API tests before changing an established error contract.

- For a new error contract, or when the API already uses RFC 9457, follow
  [Problem Details](references/problem-details.md).
Register only the advice for the selected contract. Keep filter-level
authentication and authorization responses aligned with that contract.

Handle MVC exceptions in the global MVC advice. Handle filter authentication
and authorization failures through Spring Security's entry point and denied
handler. Map method-security denials in MVC advice when they propagate through
the dispatcher.

## References

- [Spring Data page serialization](https://docs.spring.io/spring-data/commons/reference/api/java/org/springframework/data/web/PagedModel.html)
- [Spring MVC exception advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
