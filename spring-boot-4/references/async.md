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

1. For a new Java 21+ application with blocking request or I/O workloads,
   recommend Spring Boot virtual threads by default with
   `spring.threads.virtual.enabled=true`. Reconsider this for CPU-bound,
   tightly concurrency-limited, or incompatible workloads, and measure under
   representative load. Java 24+ improves virtual-thread runtime behavior;
   on Java 21–23, be alert to monitor-related pinning and use JFR when
   diagnosing throughput. Existing applications keep their current threading
   model unless migration is requested.
2. Virtual threads change the execution cost model, not downstream capacity.
   Keep database connection pools, remote-service limits, timeouts, and
   backpressure aligned with actual resource limits. They do not make
   sequential fan-out concurrent or make blocking remote work durable.
3. Boot-managed task execution and scheduling can use virtual threads when
   enabled; scheduler pool properties do not size the virtual-thread scheduler.
   An explicitly declared executor remains governed by its own configuration.
   Use a bounded executor when the workload needs a bulkhead or bounded queue.
   Virtual threads are daemon threads; set `spring.main.keep-alive=true` when
   scheduled or background work must keep an otherwise idle process alive.
4. Virtual threads do not automatically propagate arbitrary ThreadLocal,
   tracing, or security context. Keep the context rules below and configure
   explicit propagation at executor boundaries.
5. Use in-process asynchronous execution only for work that may be lost on
   process termination and does not require durable delivery. Use an outbox,
   broker, or job system when the business contract requires retries after
   restart, delivery tracking, or cross-instance coordination.
6. Enable `@Async` explicitly. Configure a bounded `ThreadPoolTaskExecutor`
   with named threads, an explicit core/max size, queue capacity, and a
   rejection policy. Size it from workload and resource limits; do not rely
   on an unbounded queue or silently discard work.
7. Inject an executor by qualifier for each distinct workload. Keep blocking
   I/O and CPU-bound work on appropriately sized executors. Do not run
   unbounded tasks on the web request executor or a shared common pool.
8. Put `@Async` on a public method of a Spring-managed bean and call it through
   the Spring proxy. Calls from one method to another method on the same
   instance do not pass through the proxy and therefore are not asynchronous.
9. Return `CompletableFuture<T>` when the caller needs a result or completion
   signal; return `void` only for true fire-and-forget work with an explicit
   failure handling path. Do not block immediately on the returned future,
   which removes the concurrency benefit.
10. Handle failures explicitly. For `CompletableFuture`, propagate and handle
   failures at the composition boundary. For `void` methods, configure an
   `AsyncUncaughtExceptionHandler` and log enough context to diagnose the
   failed operation. Do not swallow exceptions.
11. Treat the worker as a child task, not as a continuation of the caller
   thread. Thread-bound state is not automatically inherited, and executor
   threads are reused. Propagate only the context the worker needs, restore it
   for the task, and clear it afterward so one task cannot observe another
   task's state.
12. Handle each context explicitly:
   - **Business/request data:** pass immutable values such as tenant ID,
     correlation ID, locale, and actor ID as method arguments when practical.
   - **MDC and observation context:** use Spring's
     `ContextPropagatingTaskDecorator` when Micrometer Context Propagation is
     available and the required context accessors are registered. It adds
     overhead, so avoid it for very small, high-volume tasks unless needed.
   - **Spring Security:** use Spring Security's concurrency support, such as
     `DelegatingSecurityContextAsyncTaskExecutor`, only when the worker must
     run under the caller's identity. The wrapper must install the submitting
     context for that task and restore/clear it before the executor thread is
     reused. Prefer passing the minimal actor or tenant identity when
     authorization is not needed in the worker. For delayed or durable jobs,
     store actor/tenant identifiers as explicit job data and revalidate policy
     when executing; never persist a live `SecurityContext`.
   - **Transactions:** do not propagate the caller's transaction. Start a new
     transaction in the worker when it writes to the database.
   - **Servlet request/session:** do not propagate or retain these objects.
     Extract the required immutable data before scheduling the task.
13. Never enable inheritable thread-local mode as a general executor solution;
   pooled threads outlive parent requests and can retain stale or another
   request's context. Use a task decorator or security-aware executor that
   installs and reliably clears the intended context.
14. Start database transactions inside the asynchronous worker when the worker
   performs persistence. The caller's transaction does not cross the thread
   boundary. Keep transactions short and exclude remote calls and other slow
   side effects.
15. Test the observable result and failure behavior. Avoid tests that depend on
   arbitrary sleeps; use futures, latches, or a controllable executor with
   time-bounded waits. When context propagation is configured, verify the
   worker sees the intended values and that they are cleared/restored before a
   later task reuses the same thread. Keep tests deterministic and shut down
   custom executors cleanly.

## In-process Spring events

Use Spring's `ApplicationEventPublisher` when one feature needs to announce a
fact to another in-process listener without coupling the publisher to that
listener. Keep the event payload immutable and owned by the publishing feature.
Publish an application/domain event as part of the use case, and listen with
`@EventListener` for work that should run synchronously in the publishing
thread.

When a listener must observe committed database state, use
`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. This
avoids running the listener for a transaction that later rolls back. It is
still an in-process callback and is not durable. Add `@Async("<executor>")`
only when the work has an acceptable loss policy and is isolated from the
request transaction. Apply the executor sizing, error handling, and context
rules above. Use an outbox or durable messaging system when a committed event
must be retried or delivered across process boundaries.

The template's user registration use case publishes an immutable
`UserRegistered` event. Its notification listener runs after commit and sends
through the bounded application executor. This example intentionally accepts
possible delivery loss after commit. For durable email delivery, persist an
outbox record in the same transaction and dispatch it with retry tracking.
Keep event listeners idempotent when retries are possible. See the actual
[publisher](../assets/template-maven/src/main/java/com/example/templatemaven/user/application/UserService.java),
[event](../assets/template-maven/src/main/java/com/example/templatemaven/user/domain/UserRegistered.java),
and [listener](../assets/template-maven/src/main/java/com/example/templatemaven/notification/application/UserRegisteredListener.java)
in the template. The template's [executor configuration](../assets/template-maven/src/main/java/com/example/templatemaven/config/AsyncConfiguration.java)
uses `ContextPropagatingTaskDecorator`, a bounded queue, explicit rejection,
shutdown draining, and an uncaught-exception logger. Its
[context propagation test](../assets/template-maven/src/test/java/com/example/templatemaven/config/AsyncContextPropagationTests.java)
proves the submitted value is visible in the worker and that a reused worker
thread does not retain it for the next task.
The template declares Micrometer's `context-propagation` artifact directly in
its [Maven build](../assets/template-maven/pom.xml), because it exercises the
`ContextRegistry` API in that test.

## Example

```java
package com.example.templatemaven.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfiguration.class);

    @Bean
    ThreadPoolTaskExecutor applicationTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("app-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (exception, method, params) -> logger.error(
                "Asynchronous method {} failed with {} parameters", method.toGenericString(), params.length, exception);
    }
}
```

The concrete template listener calls `EmailNotificationService` through a
Spring bean after transaction commit. Its `void` async method relies on the
configured `AsyncUncaughtExceptionHandler`; methods that return a future
should propagate failures through that future instead.

The template uses `ContextPropagatingTaskDecorator` on the application
executor. It propagates only values whose `ThreadLocalAccessor` is registered
with Micrometer Context Propagation. Register accessors for application-owned
MDC, tracing, or tenant context; the template test registers a test accessor
and proves propagation and cleanup on a reused worker thread. For another
executor, attach the same decorator only when the needed accessors are
registered:

```java
executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
```

For authenticated work, configure the executor with Spring Security's
`DelegatingSecurityContextAsyncTaskExecutor`. Do not use the general context
decorator as a substitute for security context handling.

```java
@Bean("securityAwareExecutor")
AsyncTaskExecutor securityAwareExecutor(
		@Qualifier("notificationExecutor") AsyncTaskExecutor delegate) {
	return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
}
```

Use this wrapper only for work that must execute with the submitting user's
security context. The wrapper captures the context when work is submitted and
clears/restores it when the work finishes. Work scheduled for later or stored
for retry should carry explicit actor and tenant IDs instead and re-check
authorization at execution time. See [security.md](security.md).

## Verify

Inspect the changed async call sites and executor configuration. Confirm the
affected tasks have an acceptable loss policy, bounded queues and pools,
observable failures, worker-thread transactions, and explicit propagation of
needed caller context. Verify thread-local state is cleared before executor
threads are reused. Report any caller context the implementation assumes will
propagate implicitly.
