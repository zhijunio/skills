# Async Execution

## Choose the execution boundary

First decide whether losing work on process exit is acceptable. In-memory async work is not
a durable queue. Required notifications, payments and accepted background jobs need persisted
intent plus recovery, not just an AFTER_COMMIT listener or an HTTP 202 response.
Keep an existing broker or job framework when it already owns this contract.

Enable async interception once and call a separate Spring bean through its proxy. A same-class
call does not become asynchronous. Use a named executor and return CompletableFuture when the
caller must observe failure. A void method needs an AsyncUncaughtExceptionHandler; the caller
cannot catch its later failure. Rejection can happen synchronously during submission.

See the [executor](../templates/ReportExecutorConfiguration.java), [worker](../templates/ReportTasks.java),
[submission flow](../examples/async-submission.java), and [proxy bypass](../examples/proxy-bypass.java).
Executor sizes are example budgets, not universal defaults.

## Capacity and lifecycle

Bound threads and queued work against downstream capacity and latency targets. A large queue
can hide overload and delay growth beyond the core pool. Define a visible rejection outcome;
do not silently discard required work. CallerRunsPolicy can move blocking work onto an HTTP
or event-loop thread, so it is not a universal backpressure fix.

A custom `Executor` can change Boot auto-configuration. For a task-specific executor,
preserve Boot's default `applicationTaskExecutor` and MVC/WebFlux integration; on Boot 4,
mark the named executor `defaultCandidate = false` and select it explicitly with `@Async`.
The sample does this for `reportExecutor` and only covers that worker. Check the effective
Boot minor's behavior before changing global executor configuration.
Virtual threads do not increase database connections or remote quotas: bound outstanding work
even without a platform-thread pool. Configure I/O timeouts and bounded shutdown separately.
Future cancellation/timeout is not proof that an external operation stopped.

## State and transactions

Pass immutable IDs or snapshots, not request objects, managed entities or lazy collections.
The caller's thread-bound transaction does not move with the task. Put any worker transaction
on a separate proxied service and load its state there; see
[application transactions](../../spring-boot-webmvc/references/transactions.md). Starting async work before commit can
observe missing data; after-commit dispatch still has a crash-loss window.

Tenant, MDC and security state need deliberate propagation and cleanup. Capture only required,
trusted context; restore prior worker state in finally. Use Spring Security's delegating
executor support when the operation must run with the submitting security context. Do not
use inheritable thread locals as a pool-wide tenant or authentication strategy.

## Verification

Test via the actual proxy: worker thread selection, failed futures, saturation rejection and
shutdown. Use latches with deadlines, not sleep-based assumptions. Test context cleanup with
successive users on the same worker and database visibility at the commit boundary. Report
which tests were actually run; these checks do not certify durable delivery.

## Official sources

- [Boot task execution](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html)
- [Framework scheduling and async interception](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Async return types and executor selection](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Async.html)
- [ThreadPoolTaskExecutor capacity](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/concurrent/ThreadPoolTaskExecutor.html)
- [Spring Security concurrency support](https://docs.spring.io/spring-security/reference/features/integrations/concurrency.html)
- [Transaction-bound events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
