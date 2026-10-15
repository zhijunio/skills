# GraalVM Native Image

Read this when a Spring Boot application needs native startup or memory characteristics and the team is ready to pay for slower builds and a more constrained runtime. Measure against the JVM image before adopting native compilation; startup gains do not guarantee higher throughput or a smaller on-disk image.

## Check compatibility first

Native Image uses build-time static analysis. Code reached only through reflection, dynamic resource access, serialization, JNI, or runtime-generated proxies may need reachability metadata. Spring AOT generates many hints, but third-party libraries and application code still need verification.

Spring AOT also fixes parts of the application at build time. Bean definitions selected by profiles and conditional properties cannot be changed freely at runtime. Review the [Spring Boot native-image restrictions](https://docs.spring.io/spring-boot/4.1/reference/packaging/native-image/introducing-graalvm-native-images.html) before designing deployment-specific behavior.

Use a GraalVM version compatible with the project's Spring Boot and Java versions. Boot 4 requires GraalVM 25 or later. This skill's Boot 4 baseline uses GraalVM 25. Keep the Native Build Tools plugin aligned with the framework line and check the exact release requirements. See [version compatibility and migration](../spring-boot-migration.md).

## Build a native image

For a Maven project using `spring-boot-starter-parent` and the Native Build Tools plugin, Spring Boot provides the `native` profile:

```bash
./mvnw -Pnative native:compile
./target/<artifactId>
```

The native executable is platform-specific. Build it for the operating system and CPU architecture where it will run, or build inside a compatible container. Do not copy a macOS or Windows executable into a Linux runtime image.

The bundled [`Dockerfile-native`](../../assets/Dockerfile-native) builds with GraalVM and produces a non-root distroless runtime image:

```bash
docker build -f Dockerfile-native -t inventory-api-native:local .
docker run --rm -p 8080:8080 inventory-api-native:local
```

That template assumes a single-module Maven project. If a `frontend/` directory and Maven frontend plugin exist, the build packages their generated static resources; backend-only projects need no frontend placeholder. Review the Maven profile, executable output, libc compatibility, build memory, and `NATIVE_IMAGE_OPTIONS` before adopting it in another project. See [Docker](docker.md) for image and Compose guidance.

Spring Boot buildpacks can also create a native image without a hand-written Dockerfile:

```bash
./mvnw -Pnative spring-boot:build-image
```

The buildpack route still needs the project's native profile and a compatible container daemon. For Gradle, apply the GraalVM Native Build Tools plugin and use the `nativeCompile` task; follow the build-tool version's plugin documentation.

If the project does not inherit from `spring-boot-starter-parent`, configure Spring AOT and Native Build Tools executions explicitly. Do not paste the parent-provided profile into an existing build without checking its current plugin configuration. See [Developing Your First GraalVM Native Application](https://docs.spring.io/spring-boot/4.1/how-to/native-image/developing-your-first-application.html).

## Add hints only for demonstrated gaps

First check whether Spring or a dependency already provides the required hint. Reproduce the failure with the native build, identify the missing reflection, resource, serialization, proxy, or JNI access, then add the narrowest hint. Spring hints can be contributed with `RuntimeHintsRegistrar` and registered through `@ImportRuntimeHints`:

```java
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(OrderRuntimeHints.class)
class NativeConfiguration {}

class OrderRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("prompts/*.st");
        hints.reflection().registerType(
                LegacyPayload.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
}
```

Avoid broad reflection registrations when a specific type, constructor, method, or resource is sufficient. See [Spring AOT](https://docs.spring.io/spring-boot/4.1/reference/packaging/native-image/advanced-topics.html) and [GraalVM reachability metadata](https://www.graalvm.org/latest/reference-manual/native-image/metadata/).

## Test the native execution path

Keep most unit and integration tests on the JVM for a fast development loop. Then add focused native checks for paths that depend on reflection, serialization, resources, proxies, or third-party integrations.

Run the AOT-generated application on the JVM before the slower native compile:

```bash
./mvnw -Pnative -DskipTests package
java -Dspring.aot.enabled=true -jar target/<artifactId>.jar
```

This checks generated AOT code, but it does not replace running the native executable. With Maven and the Spring Boot parent, native tests use the `nativeTest` profile:

```bash
./mvnw -PnativeTest test
```

Native tests can take several minutes and consume substantial memory. Run them when native compatibility is part of the change or in a suitable CI job. See [testing strategy](../testing/testing-strategy.md) and [Testing GraalVM Native Images](https://docs.spring.io/spring-boot/4.1/how-to/native-image/testing-native-applications.html).

## Gotchas

- Native Image is selected only for a headline startup claim; measure startup, memory, throughput, image size, and build time against the JVM image.
- A native executable is built on one platform and copied to another; match target OS, architecture, and libc.
- Runtime profiles or conditional properties are expected to select a different bean graph after compilation; define the supported configuration at build time.
- Missing metadata is addressed with broad reflection registration before reproducing and narrowing the missing access.
- A successful native compile is treated as proof the application works; run AOT and native execution tests on important paths.
- Every test is forced to run natively; keep the regular development suite on the JVM and target native tests to compatibility risks.
- The bundled Dockerfile is applied unchanged to a Gradle or multi-module project; its build stages assume single-module Maven and may copy a frontend directory.

## Related

- [Docker](docker.md) · [Testing strategy](../testing/testing-strategy.md) · [Testcontainers](../testing/testcontainers.md) · [Maven](../tooling/maven.md) · [Version compatibility and migration](../spring-boot-migration.md)

## References

- [Spring Boot: Introducing GraalVM Native Images](https://docs.spring.io/spring-boot/4.1/reference/packaging/native-image/introducing-graalvm-native-images.html)
- [Developing Your First GraalVM Native Application](https://docs.spring.io/spring-boot/4.1/how-to/native-image/developing-your-first-application.html)
- [Testing GraalVM Native Images](https://docs.spring.io/spring-boot/4.1/how-to/native-image/testing-native-applications.html)
- [Spring Boot 4.1 system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [GraalVM Native Image Reachability Metadata](https://www.graalvm.org/latest/reference-manual/native-image/metadata/)
