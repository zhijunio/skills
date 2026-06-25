# Stack-Specific Search Patterns

Quick reference for each agent to adapt its searches based on the detected tech stack. The "Agent" column uses the current dimension names from SKILL.md (Frontend-Backend Contract / Data Integrity & Flow / Error Handling & Security / Architecture & Code Quality / Config & Persistence).

## Python + Pydantic + FastAPI

| Agent | Key Search Patterns |
|-------|-------------------|
| Frontend-Backend Contract | `Field(alias=...)` values, `model_dump(by_alias=True)` output keys |
| Data Integrity & Flow | `model_validate()`, `model_dump(exclude_none=True)`, `extra="ignore"`, `@app.get/post`, `Depends()`, `include_router()` |
| Error Handling & Security | `except Exception: pass`, `logger.debug` for errors, `logger.warning + return` |
| Architecture & Code Quality | files >800 lines, classes >15 methods; tests: `pytest.mark.skip`, bare `assertTrue`, missing `test_*.py` for logic modules |
| Config & Persistence | `os.getenv()`, `.env` files, `Settings(BaseSettings)`, `hashlib` cache keys, `pickle.dumps/loads` |

## TypeScript + React

| Agent | Key Search Patterns |
|-------|-------------------|
| Frontend-Backend Contract | `interface X`, `type X =`, compare with backend model fields; Zod `.strict()` vs `.passthrough()`, `as` type assertions |
| Data Integrity & Flow | Component registry, route definitions, `switch(type)` exhaustiveness, `props?.fieldName` routing chains |
| Error Handling & Security | `catch(e) {}`, `.catch(() => {})`, unhandled promise rejections |
| Architecture & Code Quality | God components >300 lines; tests: `test.skip`/`it.skip`, `toBeTruthy()` wrapping, missing `*.test.tsx` |
| Config & Persistence | env vars not in `.env.example`, conflicting tsconfig/bundler settings |

## Rust + serde + axum/actix

| Agent | Key Search Patterns |
|-------|-------------------|
| Frontend-Backend Contract | `#[serde(rename=...)]`, `#[serde(rename_all=...)]` |
| Data Integrity & Flow | `#[serde(skip_serializing_if)]`, `#[serde(default)]`, `#[serde(deny_unknown_fields)]` absence, `serde_json::from_str`, `impl Trait for X`, `Router::new().route()`; concurrency: `std::sync::Mutex` guard across `.await`, `tokio::spawn(` with discarded handle, `unbounded_channel`, `reqwest::blocking`/`std::fs` in async fn |
| Error Handling & Security | `let _ = expr`, `unwrap()`, `.ok()` discarding errors |
| Architecture & Code Quality | `impl X { }` with >15 methods, files >800 lines; tests: `#[ignore]`, `assert!(true)`, modules without `#[cfg(test)]` |
| Config & Persistence | config structs never `load()`ed (only `Default::default()`), cache key construction |

## Go + gin/echo

| Agent | Key Search Patterns |
|-------|-------------------|
| Frontend-Backend Contract | `json:"field_name"` struct tags, compare with frontend types; missing `json` tags (Go exports uppercase but JSON uses lowercase) |
| Data Integrity & Flow | `json.Marshal/Unmarshal`, `omitempty` tags, struct embedding, `r.GET/POST()` route registrations, interface implementations; concurrency: `go func(` without exit path, concurrent map writes, channels without close |
| Error Handling & Security | `if err != nil { return }` without logging, `_ = expr` |
| Architecture & Code Quality | files >800 lines; tests: `t.Skip(` without reason, missing `_test.go` for logic packages |
| Config & Persistence | `os.Getenv` outside config package, hardcoded defaults |

## Java + Spring Boot (Jackson, JPA)

Typical signals: `pom.xml` or `build.gradle*`, `@SpringBootApplication`,
`spring-boot-starter-web` / `spring-boot-starter-webflux`, `application.yml`,
`@RestController`, `@Entity`, MapStruct mappers.

| Agent | Key Search Patterns |
|-------|-------------------|
| Frontend-Backend Contract | `@JsonProperty`, `@JsonAlias`, `@JsonNaming`, `@JsonInclude(NON_NULL)`; compare DTO/record fields with frontend/OpenAPI; `@Schema` vs actual JSON; enum `@JsonValue` / `@JsonCreator`; missing `@JsonIgnore` on lazy JPA relations |
| Data Integrity & Flow | `@RequestBody` / `@ResponseBody`, `ObjectMapper` custom modules, `FAIL_ON_UNKNOWN_PROPERTIES` disabled globally; MapStruct `@Mapping`; JPA `@OneToMany(fetch=EAGER)` / open session in view; `@Transactional` boundary vs controller; `@Cacheable` key SpEL; registry maps (`Map<String, Handler>`), `@ConditionalOnProperty` beans declared but never wired; concurrency: `@Async` without `AsyncUncaughtExceptionHandler`, `CompletableFuture` fire-and-forget, blocking JDBC/`RestTemplate` inside WebFlux/Reactor chain, `synchronized` on hot `@Service`, virtual threads with thread-local assumptions |
| Error Handling & Security | `@ControllerAdvice` returning default/empty body on exception; `catch (Exception e) { return fallback; }`; `log.debug` for failures affecting user output; `@ExceptionHandler` swallowing root cause; hardcoded secrets in `application.yml`; default Spring Security user; native SQL string concat; `ObjectInputStream` / unsafe Jackson polymorphic types; `@CrossOrigin(origins="*")`; Actuator endpoints exposed without auth |
| Architecture & Code Quality | `@RestController` with business logic; domain importing `spring-*` / `jakarta.persistence`; god classes >800 lines; `@Service` classes >15 public methods; duplicate DTO/entity/VO triplets; extension cost for new enum/type (controller + service + repo + mapper + test); tests: `@Disabled` / `@Ignore`, `@SpringBootTest` overuse, missing slice tests (`@WebMvcTest`, `@DataJpaTest`), Mockito `any()` weakening assertions |
| Config & Persistence | `@Value` / `Environment` reads bypassing `@ConfigurationProperties`; properties in yaml never bound; conflicting defaults in `application.yml` + profile; Flyway/Liquibase migration errors swallowed; `@Cacheable` without version dimension in key; `@Scheduled` without lock/idempotency; temp files without `deleteOnExit`/cleanup in `finally`; hardcoded relative paths depending on cwd |

Also scan: Lombok `@Data` on JPA entities (equals/hashCode on relations), `@Builder` with required invariants missing, `@Slf4j` + swallowed exceptions.

## Java + Micronaut / Quarkus

| Agent | Key Search Patterns |
|-------|-------------------|
| Frontend-Backend Contract | Micronaut `@SerdeProperty` / `@Introspected`; Quarkus REST `@RegisterForReflection`; compare generated OpenAPI with frontend types |
| Data Integrity & Flow | `@Singleton` startup `@PostConstruct` side effects; reactive `Multi`/`Flux` chains with blocking calls; Panache active record leaking into API layer |
| Error Handling & Security | `@Error` handlers returning generic 200; default dev/test security disabled in prod profile |
| Architecture & Code Quality | CDI bean scope misuse (`@ApplicationScoped` mutable state); duplicate config in `application.properties` + env |
| Config & Persistence | Native-image reflection config drift; missing `@ConfigMapping` for env vars referenced in code |

## Full-Stack Projects

When both frontend and backend are detected:

- The **Frontend-Backend Contract agent** is the most critical — it must compare BOTH sides of every shared type and owns the full rendering-pipeline check.
- The **Data Integrity & Flow agent** should trace data across the API boundary, not stop at the serialization layer.
- The **Architecture & Code Quality agent** should specifically check for cross-stack duplication (same constants, enums, validation rules defined in both Python/Go/Java and TypeScript).
