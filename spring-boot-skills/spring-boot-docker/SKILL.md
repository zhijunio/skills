---
name: spring-boot-docker
description: >
  Use when building or deploying a Spring Boot application with Docker, Docker Compose, OCI images,
  JVM layers, Spring AOT, GraalVM Native Image, or CRaC. Covers local Compose services, image
  construction, runtime behavior, security, and verification.
---

# Spring Boot Docker

Use the JVM image as the baseline. Choose Spring AOT, GraalVM Native Image, or CRaC only when
measurements show that startup time, memory, or restore time justifies their added constraints.

For Docker Compose services, the Spring Boot Docker Compose integration, Dockerfiles, or image
verification, read [Docker and Compose guidance](references/docker-compose.md). Use the matching
file under `templates/` as a project-specific template, not a drop-in universal configuration.
For AOT, Native Image, or CRaC choices and constraints, read
[advanced runtime guidance](references/native-runtime.md).

## Container image

- Prefer Boot build-image tasks with Cloud Native Buildpacks for standard services.
- Preserve dependency, loader, snapshot, and application layers for cache reuse.
- Pin compatible builder/run image families and promote immutable digests.
- Run as non-root and use a read-only filesystem where possible.
- Inject secrets at runtime; never copy credentials into a build context or image layer.
- Configure graceful shutdown and platform termination timing together.

## Runtime behavior

- Verify JVM memory and CPU ergonomics under real container limits.
- Expose Actuator liveness and readiness probes separately.
- Use a startup probe for expensive initialization.
- Keep writable paths explicit and bounded.

## Native image and AOT

- Use GraalVM 25 or newer with Boot 4 native build support.
- Register narrow `RuntimeHints` for reflection, resources, serialization, JNI, or proxies.
- Respect the closed-world model: runtime bean topology and classpath cannot change.
- Review profile and conditional-bean behavior during AOT processing.
- Run native integration tests against the produced executable.

## Supply chain

- Generate an SBOM, scan the final image, and regularly rebuild on patched run images.
- Sign and attest release images where the delivery platform supports it.

Scan the actual final image by immutable digest, including OS packages and bundled Java
dependencies; record the scanner database timestamp and findings that need manual triage.
Verify signatures against the expected signer identity and issuer or trusted key, and check
that provenance refers to the intended source commit and build workflow. A signature from
an arbitrary identity is not a release policy. Retain SBOM/provenance evidence for the promoted
digest; rebuilding the same tag produces a different artifact and requires new verification.
Test untrusted-signer and digest-mismatch rejection before enforcing a deployment policy.

## Examples

- Compare the [image example](examples/Dockerfile) with the
  [Dockerfile that exposes a build secret](examples/dockerfile-with-secret).
- Ready-to-adapt templates are in `templates/`: `Dockerfile`, `Dockerfile-aot`, `Dockerfile-native`,
  `Dockerfile-crac`, Compose files, and `.dockerignore`.

The good example uses Boot's `jarmode=tools` extraction in a builder stage. A normal Maven package
does not create `target/dependencies/` or `target/application/` directories by itself.

## Official sources

- Container images: https://docs.spring.io/spring-boot/reference/packaging/container-images/
- Dockerfiles and layer extraction: https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html
- Native images: https://docs.spring.io/spring-boot/reference/packaging/native-image/

## Gotchas

- Agent assumes a passing JVM suite proves native compatibility - run the actual native executable.
- Agent adds broad reflection metadata - use the smallest runtime hints possible.
- Agent bakes secrets into layers - inject them only at runtime.
- Agent makes liveness depend on remote systems - use readiness for traffic dependencies.
- Agent selects native only for fashion - benchmark startup, memory, throughput, and build time.
- Agent copies nonexistent `target/dependencies` directories - extract the packaged jar in a builder stage.
