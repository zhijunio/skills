# REST

Read this when the task adds or changes REST API contracts: paths, HTTP
status, request and response types, error shape, or API versions. Controller
and service roles live in [webmvc.md](webmvc.md). OpenAPI documents the API in
[openapi.md](openapi.md). JSON mapping stays in Core Rules until a Jackson
reference exists.

## When to use

- Add or change an HTTP path, status code, or `*Request` / `*Response` type
- Add or change RFC 9457 `ProblemDetail` or a `{ success, data, error }` envelope
- Add or change API versions or a breaking HTTP contract

## When not to use

- Controller, `@Transactional`, or exception-advice roles → [webmvc.md](webmvc.md)
- Springdoc coordinates or Swagger UI → [openapi.md](openapi.md)
- Jackson 3 mapper beans or `tools.jackson` → the skill's Core Rules
- Spec-first generators, HATEOAS, or Thymeleaf
- Outbound HTTP. Use `RestClient` per the skill's Core Rules.
- Testing conventions → [testing.md](testing.md)

## Rules

1. Map resources under `/api/{plural-nouns}`. Use kebab-case. Nest a path
   only when the parent owns or scopes the child (`/api/orders/{id}/items`).
   Independently managed resources get their own collection path. Put filters
   and pagination in query parameters. Use a verb path only for a real state
   transition, such as `POST /api/orders/{id}/cancel`.
2. Inbound is a `*Request` record with `@Valid`. Outbound is a `*Response`
   record. Do not accept or return a Mapper entity. Do not put `@JsonFormat`
   on Instant fields. JSON timestamp conventions are in the skill's Core
   Rules.
3. Return the payload. Create is 201 with the `*Response` body and no
   `Location` header (`@ResponseStatus(HttpStatus.CREATED)`). Use
   `ResponseEntity` only for 202, 204, or a status chosen at runtime. Missing
   resource is 404. Validation bind failures are 400. Domain rule violations
   are 422. Conflict is 409.
4. If the project already returns `{ success, data, error }` (or the same
   envelope under another name), keep that contract and stay consistent. If
   it does not, use RFC 9457 `ProblemDetail` and enable Boot's Problem
   Details. Do not mix an envelope with `ProblemDetail`.
5. First cut is unversioned `/api/{resources}`. When the task names versions
   or a breaking change, use Boot 4 API versioning: the `version` attribute on
   `@RequestMapping` / `@GetMapping` / `@PostMapping`, and
   `spring.mvc.apiversion` in `application.yaml`. The value is a semantic
   version string (`"1.0"`, `"1.1"`). `"1.2+"` is a baseline for that version
   and newer. Configure exactly one `use` source. Default is the
   `API-Version` header. Set `default` so existing clients do not get 400.
   Version only the mapping that changed. Do not add `V1`/`V2` controller
   classes, `/api/v1` path prefixes, a custom `RequestCondition`, or a filter
   that sniffs versions. Do not version additive optional fields.

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
	return orderService.place(request);
}

@GetMapping("/{id}")
OrderResponse getById(@PathVariable Long id) {
	return orderService.findById(id);
}
```

How the controller and service are wired is [webmvc.md](webmvc.md).

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

Use that YAML only when the project has no success/error envelope.

When versioning is in the task. Unversioned mappings still match any version.
A versioned mapping wins when it matches.

```java
@GetMapping("/{id}")
OrderResponse getById(@PathVariable Long id) {
	return orderService.findById(id);
}

@GetMapping(path = "/{id}", version = "1.1")
OrderResponseV1_1 getByIdV1_1(@PathVariable Long id) {
	return orderService.findByIdV1_1(id);
}
```

```yaml
spring:
  mvc:
    apiversion:
      use:
        header: API-Version
      default: "1.0"
```

If the user asked for a path version, use `use.path-segment` (segment index)
and a URI variable such as `/api/{version}/orders`. Do not also set
`use.header`. Prefer Boot properties over a `WebMvcConfigurer`; do not
configure both.

## Verify

Every numbered Rule in this file must hold on disk. Open the mappings,
request and response types, and `application.yaml`; do not infer. If
versioning is in the task, also open the version mappings and the single
`apiversion` strategy. Report each Rule that failed.
