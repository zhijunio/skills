# Spring Cloud Contract Testing

Use contract tests to check compatibility at a service boundary without starting every collaborating service. Spring
Cloud Contract supports provider- and consumer-driven workflows for HTTP and messaging. It complements component and
end-to-end tests; it does not prove production availability, deployment topology, or every consumer's behavior.

## Define ownership and scope

- A contract describes an agreed interaction: request/message shape and relevant response/output behavior. Keep it
  focused on externally observable behavior rather than provider internals.
- Decide who may propose and approve changes, how contracts are versioned, and how published stubs are retained and
  retrieved.
- A producer-owned contract can verify what the provider promises. Consumer-driven contracts help coordinate multiple
  consumers, but require an explicit workflow for consumer contributions and provider compatibility decisions.
- Use OpenAPI or AsyncAPI for a published descriptive interface when that is the project's contract. Contract tests
  add executable compatibility checks; they do not automatically replace API documentation or schema governance.

## Producer and consumer workflow

On the producer side, maintain contract definitions with the provider or in an agreed contract repository. Generate
provider verification tests and run them against the real application boundary with controlled state. Publish generated
stubs only after verification passes.

On the consumer side, use Stub Runner or the project's equivalent to start the matching provider stubs in tests. Verify
the consumer's client and behavior against those stubs. Pin or resolve a deliberate stub version; avoid silently using
an arbitrary latest artifact that can make builds nondeterministic.

For messaging, define the event direction, payload, relevant headers, and triggering behavior. Test message production
or consumption against the agreed contract, then retain broker integration tests for binder-specific behavior such as
partitioning, acknowledgments, and dead-letter handling.

## Keep contracts trustworthy

- Test observable HTTP semantics such as method, path, headers, body, status, and response shape. Avoid over-constraining
  incidental headers or implementation details.
- Make provider test data deterministic and isolate state per contract. A passing generated test should represent a real
  provider guarantee, not a permissive mock.
- Run contract verification in the provider build and consumer stub tests in the consumer build. Publish artifacts
  atomically with the corresponding version and make rollback possible.
- Treat a breaking contract change as a coordinated rollout: deploy compatible consumers/providers before removing the
  old behavior.
- Include authentication-relevant behavior in the contract only when it is part of the service boundary; never publish
  real credentials or sensitive production payloads in fixtures.

## Verification strategy

Test that a valid producer satisfies its contracts, that an intentionally incompatible change fails verification, and
that consumers pass against the correct stub version. Keep a small number of real HTTP/broker integration tests for
behavior the stub cannot prove. For external-provider contract tests and safe test doubles, also see
[Spring Boot integration testing](../../spring-boot-developer/references/testing/testing-integration.md).

## Gotchas

- Agent treats a contract test as a complete end-to-end test - retain a small number of real boundary tests.
- Agent writes a contract that asserts internal implementation details - contract only externally observable behavior.
- Agent publishes stubs before provider verification succeeds - make verification gate publication.
- Agent always resolves the latest stub artifact - pin or explicitly select versions to keep builds reproducible.
- Agent adds consumer contracts without a provider ownership/review workflow - define the change and compatibility process.
- Agent assumes message contracts prove broker semantics - add real-broker tests for binder-specific guarantees.

## Sources

- [Spring Cloud Contract reference](https://docs.spring.io/spring-cloud-contract/reference/)
- [Spring Cloud Contract getting started](https://docs.spring.io/spring-cloud-contract/reference/getting-started.html)
- [Spring Cloud Contract consumer-driven workflows](https://docs.spring.io/spring-cloud-contract/reference/using/cdc-producer-side.html)
- [Spring Cloud Contract messaging](https://docs.spring.io/spring-cloud-contract/reference/project-features-messaging.html)
