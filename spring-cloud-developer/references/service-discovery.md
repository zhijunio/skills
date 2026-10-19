# Spring Cloud Service Discovery

Service discovery answers where a service instance can be reached. Choose it from the deployment
topology; it is not a required part of every microservice stack.

## Decide whether a registry is needed

Use platform-native DNS or service routing when the deployment platform already provides stable
service names and health-aware load balancing. Add a registry such as Eureka or Consul when
applications need dynamic instance lists or routing metadata that the platform does not provide.
Avoid running a second registry without a clear need.

With Eureka, a client starter can register the application and provide discovery to clients. Give
each service a stable `spring.application.name`, secure registry communication, and decide how health,
zones, metadata, and stale instances affect routing. Treat registry responses as routing input, not
as proof that a service is authorized to receive a request.

On Kubernetes, start with Kubernetes Service DNS and platform load balancing. Use Spring Cloud
Kubernetes discovery or configuration only when the application needs its APIs, metadata, or behavior
beyond the platform's service routing. Do not run Eureka alongside Kubernetes discovery without a
specific cross-platform or metadata requirement.

Prefer Spring's `DiscoveryClient` abstraction over a native vendor client unless vendor-specific
behavior is required. Discovery and load balancing are related but distinct: a registry provides
instance information, while the client-side load balancer selects an instance for a request. See
[HTTP clients and load balancing](http-clients.md) for client configuration.

## Verification and operations

Test registration, instance refresh, stale or unhealthy instances, metadata handling, and behavior
when the registry is unavailable. In multi-instance deployments, verify that registry access and
service identity are protected and that routing does not bypass service-level authorization.

## Sources

- [Spring Cloud Netflix reference](https://docs.spring.io/spring-cloud-netflix/reference/)
- [Spring Cloud Kubernetes reference](https://docs.spring.io/spring-cloud-kubernetes/reference/)
- [Spring Cloud Commons: DiscoveryClient](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html)
