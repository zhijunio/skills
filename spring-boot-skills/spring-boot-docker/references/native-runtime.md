# Spring AOT, Native Image, and CRaC in Containers

Read this when startup time, idle memory, or checkpoint restore time is a measured deployment
constraint. Compare each option with the JVM image under the target workload before choosing one.

## GraalVM Native Image

Spring Boot 4 requires GraalVM 25 or later. Match Native Build Tools to the Spring Boot line and
the project's Java version. Native Image performs closed-world analysis at build time. Reflection,
resources, serialization, JNI, and dynamic proxies may need runtime hints. Spring AOT generates
many hints, but verify important application paths and third-party integrations. Profiles and
conditional bean registration are constrained because the bean graph is fixed during the build.

For Maven projects using `spring-boot-starter-parent`, start with the parent's `native` profile:

```bash
./mvnw -Pnative native:compile
./target/<artifactId>
```

Run the executable on the same operating system, architecture, and libc family used to build it.
A container build avoids copying a host-specific executable into a different runtime. The
[`Dockerfile-native`](../templates/Dockerfile-native) assumes a single Maven module and GraalVM 25.
Match its GraalVM and Native Build Tools versions to the project. Spring Boot buildpacks are another
container option with `./mvnw -Pnative spring-boot:build-image`.

Test AOT output on the JVM before native compilation:

```bash
./mvnw -Pnative -DskipTests package
java -Dspring.aot.enabled=true -jar target/<artifactId>.jar
```

Then run focused tests against the native executable for reflection, resource, serialization, and
third-party integration paths. Keep the regular unit suite on the JVM. Add runtime hints only after
reproducing a missing hint, and register the narrowest required type or resource.

## Spring AOT on the JVM

Spring AOT generates application-context code at build time while keeping a JVM runtime. It can
reduce startup work without GraalVM compilation. Build-time configuration choices can affect the
generated bean graph, so test the profiles and conditional configuration used in production.

The [`Dockerfile-aot`](../templates/Dockerfile-aot) is an optional Maven image example. It targets
Java 21 and expects a compatible AOT Maven profile. Check that profile and the project wrapper
before using the asset. Do not use Java 25 compact-header flags in this Java 21 image.

## CRaC

CRaC restores a checkpointed JVM process. Use it only when the Linux runtime, CRaC-enabled JDK,
CRIU privileges, and checkpoint storage are available. The checkpoint captures process state, so
open database or network connections must be closed with CRaC resource handlers or avoided while
creating the checkpoint. Revalidate secrets, credentials, DNS, and external connections after
restore.

The [`Dockerfile-crac`](../templates/Dockerfile-crac) and
[`checkpoint-and-run.sh`](../templates/checkpoint-and-run.sh) use Java 21 and a CRaC-enabled Liberica
runtime. The optional [`compose.crac.yaml`](../templates/compose.crac.yaml) illustrates the required
Linux capabilities. Its elevated privileges require review against the target cluster's security
policy. Do not deploy this local Compose configuration as a production Kubernetes template.

## Verify the choice

Measure cold startup, restore time, memory, image size, throughput, and build time for the same
application and workload. Include dependency connections, runtime profiles, and shutdown behavior
in compatibility checks. Keep a JVM deployment path available until the optimized artifact passes
its own startup, health, and representative request checks.
