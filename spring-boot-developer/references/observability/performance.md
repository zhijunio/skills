# Performance

Measure the application under representative load before tuning it. Start with request latency, throughput, error rates, allocation, database time, and downstream calls; see [observability](observability.md) for metrics and tracing.

## Virtual threads

Virtual threads require Java 21 or later. They are **opt-in**; enable them only after checking the workload and its dependencies:

```yaml
spring:
  threads:
    virtual:
      enabled: true
  main:
    keep-alive: true
```

When virtual threads are active, Boot's thread-pool sizing properties no longer control request scheduling. Avoid blocking operations inside `synchronized` sections on hot paths, because they can pin carrier threads. Use JFR or `jcmd` to investigate pinning and compare throughput before and after enabling them. The keep-alive setting matters for applications whose only active work uses daemon virtual threads, such as some scheduled workloads.

## HTTP response compression

Enable compression when the application or its ingress proxy is responsible for it. Do not compress a response twice:

```yaml
server:
  compression:
    enabled: true
    min-response-size: 1KB
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/css
      - text/plain
      - application/javascript
```

Spring Boot's embedded Tomcat, Jetty, and Reactor Netty support response compression. Keep the default MIME types unless a measured need requires changing them; `mime-types` replaces the defaults, while `additional-mime-types` extends them.

## HTTP/2

```yaml
server:
  http2:
    enabled: true
```

With TLS enabled, this configures HTTP/2 over TLS (`h2`). Without TLS, Spring Boot uses cleartext HTTP/2 (`h2c`) where the chosen server supports it. Confirm that the reverse proxy and deployment platform preserve the intended protocol.

## Tomcat thread and connection limits

Only tune Tomcat's platform-thread pool when virtual threads are disabled and measurements show that the defaults are a bottleneck:

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 20
    accept-count: 100
    max-connections: 10000
```

These are capacity limits, not universal targets. Size them with request duration, downstream capacity, memory, and the deployment's connection limits in mind. Increasing concurrency can move overload to a database or remote service.

## Gotchas

- Agent enables virtual threads because the JDK supports them - they are opt-in; benchmark the workload and check library behavior first.
- Agent sets a larger Tomcat pool while virtual threads are enabled - those pool settings no longer control request scheduling.
- Agent turns on compression behind an ingress that already compresses responses - avoid duplicate work.
- Agent increases connection limits without checking database and downstream capacity - bound concurrency end to end.

## Related

- [Observability](observability.md) · [Async and scheduling](../integration/async-and-scheduling.md) · [Static resource caching](../web/static-resources.md) · [Docker](../deployment/docker.md)

## References

- [Spring Boot virtual threads](https://docs.spring.io/spring-boot/4.1/reference/features/spring-application.html#features.spring-application.virtual-threads)
- [Spring Boot embedded web servers](https://docs.spring.io/spring-boot/4.1/how-to/webserver.html)
- [Java virtual threads](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html)
