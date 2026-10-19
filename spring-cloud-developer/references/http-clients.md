# Spring Cloud Service-to-Service HTTP Clients

Choose an HTTP client and service-name resolution mechanism from the application's blocking model,
number of remote calls, and deployment topology. For straightforward blocking calls, Spring Boot's
HTTP Service Client backed by `RestClient` is a good default; add Spring Cloud capabilities only
where they solve a concrete need.

## Choose the client

Use Spring Boot's HTTP Service Client backed by `RestClient` for a small number of ordinary blocking
calls. Add Spring Cloud LoadBalancer when the URI should resolve a logical service name to a healthy
instance instead of a fixed host. A load-balanced `RestClient.Builder` can use the service name as
the URI host:

```java
@Configuration
class ClientConfiguration {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }
}
```

This example uses Spring Boot 4's `RestClientBuilderConfigurer` so Boot customizers and observation
support remain applied. Its package and builder APIs differ on older Boot lines; check the project's
Boot documentation before copying it into a Boot 3.5.x application. Service-name resolution requires
a matching discovery or load-balancer configuration; see [service discovery](service-discovery.md).

Use OpenFeign when declarative interfaces materially improve a larger client boundary or existing
services already use it. Do not add Feign only to avoid writing a small `RestClient` call. Keep
per-client Feign configuration isolated: a configuration class intended only for one Feign client
should not be accidentally component-scanned as global application configuration.

## Identity and client configuration

Configure finite connection and response timeouts for every client. Do not put bearer tokens,
credentials, or user-controlled identity headers in static client properties. Authenticate and
authorize at the service that owns the operation. If identity must be propagated, define the trusted
service identity and token-relay behavior explicitly.

Retries and circuit-breaker integration are opt-in or version-dependent for some client choices. Use
the [resilience guidance](resilience.md) to define attempt budgets, deadlines, and failure behavior
across clients, gateways, and the deployment platform.

## Verification

Test request construction, serialization, authentication behavior, service-name resolution, and
instance selection. Use a controllable downstream for timeouts and failures. A mocked client does not
prove DNS, load-balancer, TLS, or network behavior; add an integration test for those boundaries when
they are part of the requirement.

## Sources

- [Spring Cloud LoadBalancer reference](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html)
- [Spring Cloud Commons: Load-balanced RestClient](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html#spring-restclient-as-a-loadbalancer-client)
- [Spring Cloud OpenFeign reference](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html)
- [Spring Boot 4.1 `RestClientBuilderConfigurer` API](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/restclient/autoconfigure/RestClientBuilderConfigurer.html)
