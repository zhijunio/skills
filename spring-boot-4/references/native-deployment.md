# Native Image, Spring AOT, and CRaC

Read this when startup time, idle memory, or checkpoint restore time is a
measured deployment constraint. Compare each option with the JVM image under
the target workload before choosing one.

## GraalVM Native Image

Spring Boot 4 requires GraalVM 25 or later. Native Build Tools versions must
match the Spring Boot line. The current Boot 4.2 baseline supports Native Build
Tools 1.1.18. See the [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html).

Native Image performs closed-world analysis at build time. Reflection,
resources, serialization, JNI, and dynamic proxies may need runtime hints.
Spring AOT generates many hints, but each dependency and important application
path still needs verification. Profiles and conditional bean registration are
constrained because the bean graph is fixed during the build. See [Spring Boot
Native Image](https://docs.spring.io/spring-boot/reference/packaging/native-image/introducing-graalvm-native-images.html).

For Maven projects using `spring-boot-starter-parent`, start with the parent
`native` profile:

```bash
./mvnw -Pnative native:compile
./target/<artifactId>
```

Run a native executable on the same operating system, architecture, and libc
family used to build it. A container build avoids copying a host-specific
executable into a different runtime. The
[`Dockerfile-native`](../assets/Dockerfile-native) asset assumes a single
Maven module and GraalVM 25. Match its GraalVM and Native Build Tools versions
to the project's Java and Boot versions. Spring Boot buildpacks are another
container option with `./mvnw -Pnative spring-boot:build-image`.

Test AOT output on the JVM before native compilation:

```bash
./mvnw -Pnative -DskipTests package
java -Dspring.aot.enabled=true -jar target/<artifactId>.jar
```

Then run focused tests against the native executable for reflection, resource,
serialization, and third-party integration paths. Keep the regular unit suite
on the JVM. Add `RuntimeHintsRegistrar` entries only after reproducing a
missing hint, and register the narrowest required type or resource.

## Spring AOT on the JVM

Spring AOT generates application-context code at build time while keeping a
JVM runtime. It can reduce startup work without GraalVM compilation. Build
time configuration choices can affect the generated bean graph, so test the
profiles and conditional configuration used in production.

The [`Dockerfile-aot`](../assets/Dockerfile-aot) is an optional Maven image
example. It targets Java 21 and expects a compatible AOT Maven profile. Check
that profile and the project wrapper before using the asset. Do not use the
Java 25 compact-header flag in the Java 21 image.

## CRaC

CRaC restores a checkpointed JVM process. Use it only when the Linux runtime,
CRaC-enabled JDK, CRIU privileges, and checkpoint storage are available. The
checkpoint captures process state, so open database or network connections
must be closed with CRaC resource handlers or avoided while creating the
checkpoint. Revalidate secrets, credentials, DNS, and external connections
after restore.

The [`Dockerfile-crac`](../assets/Dockerfile-crac) and
[`checkpoint-and-run.sh`](../assets/checkpoint-and-run.sh) assets use Java 21
and a CRaC-enabled Liberica runtime. The optional
[`compose.crac.yaml`](../assets/compose.crac.yaml) illustrates the required
Linux capabilities. Its elevated privileges require review against the target
cluster's security policy. Do not deploy this Compose configuration as a
production Kubernetes workload template.

## Verify the choice

Measure cold startup, restore time, memory, image size, throughput, and build
time for the same application and workload. Include dependency connections,
runtime profiles, and shutdown behavior in compatibility checks. Keep a JVM
deployment path available until the optimized artifact passes its own startup,
health, and representative request checks.
