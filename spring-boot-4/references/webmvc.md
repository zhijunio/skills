# Web MVC

Read this when the task adds or changes a controller, application service, or
`@RestControllerAdvice`. HTTP paths, status codes, request/response types, and
API versions live in [rest.md](rest.md). Persistence stays in
[mybatis-plus.md](mybatis-plus.md). Boot 4 web is
`spring-boot-starter-webmvc`.

## When to use

- Add or change a `@RestController` or application `@Service`
- Place `@Transactional`
- Add or change exception advice

## When not to use

- Resource paths, `*Request` / `*Response`, status codes, envelopes, or
  Boot API versioning → [rest.md](rest.md)
- Springdoc → [openapi.md](openapi.md)
- Testing conventions → [testing.md](testing.md)

## Rules

1. The controller translates one HTTP operation into one service call. It is
   package-private. Constructor injection.
2. The service owns `@Transactional(rollbackFor = Exception.class)` and
   domain exceptions. The controller does not use `@Transactional`,
   persistence types, `try`/`catch`, or `HttpServletRequest` past the HTTP
   boundary.
3. Exception advice is `@RestControllerAdvice` that extends
   `ResponseEntityExceptionHandler`. Throw from the service. Whether the body
   is `ProblemDetail` or an existing envelope is [rest.md](rest.md).
4. Return the payload type. Create does not set a `Location` header.

## Validation

- Add `spring-boot-starter-validation`; let Boot dependency management provide a compatible version when available;
  otherwise follow the project's dependency convention.
- Put request-shape constraints on `*Request` DTOs. Use Jakarta Bean Validation
  annotations such as `@NotBlank`, `@Size`, and `@Email`; use `@Valid` for
  nested request objects. Keep business rules in the application service.
- Apply `@Valid` to request bodies and model attributes. For constrained
  controller method parameters, use Spring MVC's built-in method validation;
  do not add class-level `@Validated` to controllers just to activate it.
- The advice must handle both `MethodArgumentNotValidException` and
  `HandlerMethodValidationException`. Keep their response shape consistent
  with [rest.md](rest.md).

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
	return orderService.place(request);
}

@GetMapping("/search")
List<OrderResponse> search(
		@RequestParam @NotBlank @Size(max = 80) String query) {
	return orderService.search(query);
}
```

## Pagination

- Expose pagination as query parameters. Use zero-based `page`, default `0`,
  and `size`, default `20`, with a maximum size of `100` unless the API has a
  documented reason to differ. This matches Spring Data's `Pageable` contract.
- Return `org.springframework.data.domain.Page<T>` containing API response
  DTOs. Do not create a parallel `PageResponse` type. If using MyBatis-Plus,
  translate Spring Data's zero-based page index to MyBatis-Plus's one-based
  current page (`page + 1`), map its `IPage` records to response DTOs, and
  adapt the result to Spring Data's `PageImpl<T>` with the original zero-based
  `PageRequest` and total count. Test first and subsequent pages to catch
  off-by-one errors. Never return MyBatis-Plus page types or persistence
  entities from a controller.
  Add `spring-data-commons` directly if it is not on the classpath; use Boot's
  dependency management for its version and do not add Spring Data JPA just
  for pagination.
- Validate page and size at the HTTP boundary. If sorting is supported, map
  public sort keys through an allowlist and append a stable tie-breaker.
- Read [mybatis-plus.md](mybatis-plus.md) for the database pagination plugin
  and bounded query implementation. Read [rest.md](rest.md) for the HTTP
  response contract. Treat the serialized Spring Data `Page<T>` representation
  as API surface and assert it in controller or integration tests.

```java
@GetMapping
Page<OrderResponse> list(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
	return orderService.list(page, size);
}
```

## Internationalization

- Put the default bundle at `src/main/resources/messages.properties`; add
  locale bundles such as `messages_en.properties` and
  `messages_zh_CN.properties` beside it. Boot does not auto-configure a
  `MessageSource` when the default bundle is missing.
- Use message keys for validation and user-facing API messages. Resolve the
  request locale from `Accept-Language`; set
  `spring.messages.fallback-to-system-locale: false` for deterministic
  fallback behavior.
- Keep API field names, error codes, and status semantics stable across
  locales. Do not translate machine-readable identifiers.

```yaml
spring:
  messages:
    basename: messages
    fallback-to-system-locale: false
```

```properties
# messages.properties
request.name.required=Name is required.
```

```properties
# messages_zh_CN.properties
request.name.required=名称不能为空。
```

```java
@RestController
@RequestMapping("/api/orders")
class OrderController {

	private final OrderService orderService;

	OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
		return orderService.place(request);
	}

	@GetMapping("/{id}")
	OrderResponse getById(@PathVariable Long id) {
		return orderService.findById(id);
	}
}
```

```java
@Service
class OrderService {

	@Transactional(rollbackFor = Exception.class)
	OrderResponse place(CreateOrderRequest request) {
		throw new UnsupportedOperationException();
	}

	@Transactional(readOnly = true, rollbackFor = Exception.class)
	OrderResponse findById(Long id) {
		throw new UnsupportedOperationException();
	}
}
```

```java
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {}
```

Replace `orders` and `OrderService` with the feature. Path and type names
follow [rest.md](rest.md). The `place` / `findById` bodies are not this
file's job. Keep the handler empty until a domain exception needs a mapping.

The template's [controller](../assets/template-maven/src/main/java/com/example/templatemaven/user/api/UserController.java),
[service](../assets/template-maven/src/main/java/com/example/templatemaven/user/application/UserService.java),
and [exception advice](../assets/template-maven/src/main/java/com/example/templatemaven/user/api/UserExceptionHandler.java)
show validation, paginated reads, transactional writes, and consistent
`ProblemDetail` responses. Request-validation messages and locale behavior are
demonstrated in the template's
[message bundles](../assets/template-maven/src/main/resources/messages.properties)
and [integration tests](../assets/template-maven/src/test/java/com/example/templatemaven/user/UserIntegrationTests.java).

## Verify

For a new web feature, check every rule. For an existing feature, check rules
relevant to the requested behavior. Open the controller, service, and
exception advice as needed; do not infer. Report applicable failures.
