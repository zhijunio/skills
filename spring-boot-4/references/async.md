# Asynchronous Processing

Read this when moving work off a request thread, adding `@Async`, or
configuring application task executors. Durable jobs and message-driven work
need an explicit broker or job-system design; this reference covers
in-process asynchronous execution.

## When to use

- Run independent, bounded work without blocking the caller
- Add `@Async` or configure a task executor
- Handle asynchronous results or failures

## When not to use

- Work must survive process restarts or deployment → use a durable queue or
  job system
- Cross-service event delivery → use the messaging integration reference
  when one exists
- Outbound HTTP implementation → [webmvc.md](webmvc.md)

## Rules

1. Use in-process asynchronous execution only for work that may be lost on
   process termination and does not require durable delivery. Use an outbox,
   broker, or job system when the business contract requires retries after
   restart, delivery tracking, or cross-instance coordination.
2. Enable `@Async` explicitly. Configure a bounded `ThreadPoolTaskExecutor`
   with named threads, an explicit core/max size, queue capacity, and a
   rejection policy. Size it from workload and resource limits; do not rely
   on an unbounded queue or silently discard work.
3. Inject an executor by qualifier for each distinct workload. Keep blocking
   I/O and CPU-bound work on appropriately sized executors. Do not run
   unbounded tasks on the web request executor or a shared common pool.
4. Put `@Async` on a public method of a Spring-managed bean and call it through
   the Spring proxy. Calls from one method to another method on the same
   instance do not pass through the proxy and therefore are not asynchronous.
5. Return `CompletableFuture<T>` when the caller needs a result or completion
   signal; return `void` only for true fire-and-forget work with an explicit
   failure handling path. Do not block immediately on the returned future,
   which removes the concurrency benefit.
6. Handle failures explicitly. For `CompletableFuture`, propagate and handle
   failures at the composition boundary. For `void` methods, configure an
   `AsyncUncaughtExceptionHandler` and log enough context to diagnose the
   failed operation. Do not swallow exceptions.
7. Treat the worker as a child task, not as a continuation of the caller
   thread. Thread-bound state is not automatically inherited, and executor
   threads are reused. Propagate only the context the worker needs, restore it
   for the task, and clear it afterward so one task cannot observe another
   task's state.
8. Handle each context explicitly:
   - **Business/request data:** pass immutable values such as tenant ID,
     correlation ID, locale, and actor ID as method arguments when practical.
   - **MDC and observation context:** use Spring's
     `ContextPropagatingTaskDecorator` when Micrometer Context Propagation is
     available and the required context accessors are registered. It adds
     overhead, so avoid it for very small, high-volume tasks unless needed.
   - **Spring Security:** use Spring Security's concurrency support, such as
     `DelegatingSecurityContextAsyncTaskExecutor`, only when the worker must
     run under the caller's identity. Prefer passing the minimal actor or
     tenant identity when authorization is not needed in the worker.
   - **Transactions:** do not propagate the caller's transaction. Start a new
     transaction in the worker when it writes to the database.
   - **Servlet request/session:** do not propagate or retain these objects.
     Extract the required immutable data before scheduling the task.
9. Never enable inheritable thread-local mode as a general executor solution;
   pooled threads outlive parent requests and can retain stale or another
   request's context. Use a task decorator or security-aware executor that
   installs and reliably clears the intended context.
10. Start database transactions inside the asynchronous worker when the worker
   performs persistence. The caller's transaction does not cross the thread
   boundary. Keep transactions short and exclude remote calls and other slow
   side effects.
11. Test the observable result and failure behavior. Avoid tests that depend on
   arbitrary sleeps; use futures, latches, or a controllable executor with
   time-bounded waits. When context propagation is configured, verify the
   worker sees the intended values and that they are cleared/restored before a
   later task reuses the same thread. Keep tests deterministic and shut down
   custom executors cleanly.

## Example

```java
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
class AsyncConfiguration {

	@Bean("notificationExecutor")
	ThreadPoolTaskExecutor notificationExecutor() {
		var executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		executor.setThreadNamePrefix("notification-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(10);
		return executor;
	}
}

@Service
class NotificationService {

	@Async("notificationExecutor")
	CompletableFuture<Void> send(Notification notification) {
		// Perform bounded, non-durable work.
		return CompletableFuture.completedFuture(null);
	}
}
```

Call the asynchronous method through the injected `NotificationService` bean,
not through `this`. Replace the example body with the actual operation and
propagate failures through the returned future.

For trace/MDC propagation, attach Spring's decorator to the executor only
when the application has registered the corresponding Micrometer context
accessors:

```java
executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
```

For authenticated work, configure the executor with Spring Security's
`DelegatingSecurityContextAsyncTaskExecutor`. Do not use the general context
decorator as a substitute for security context handling.

## Verify

Inspect async call sites and executor configuration. Confirm each task is
allowed to be lost on process termination, queue and pool sizes are bounded,
proxy invocation is used, failures are observable, and worker transactions are
created on the worker thread. Check which parent-thread values reach each
child task and verify thread-local state is cleared before the executor reuses
the thread. Report any caller context that the implementation assumes will
propagate implicitly.
