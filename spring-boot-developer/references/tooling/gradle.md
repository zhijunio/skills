# Gradle

The Gradle equivalent of [maven.md](maven.md). Read that file for the reasoning; this one is the syntax and the Gradle-specific traps.

Use the **Kotlin DSL** (`build.gradle.kts`) for new projects - it gives type-safe accessors, real IDE completion and errors at configuration time rather than at task execution.

## Plugins and dependency management

```kotlin
plugins {
    java
    id("org.springframework.boot") version "<current 4.x>"
    id("io.spring.dependency-management") version "<current>"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}
```

The **toolchain** is the important line. It pins the JDK that compiles the code, downloading it if necessary, so every machine and CI agent builds identically. `sourceCompatibility` only sets the bytecode level and leaves the compiling JDK to chance.

`io.spring.dependency-management` applies the Boot BOM so versions are omitted below. Alternatively use Gradle's native platform support:

```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:<current 4.x>"))
}
```

Either works. Do not use both.

## Dependencies

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

The starter table in [maven.md](maven.md) applies unchanged - including that **Flyway is not transitive on Boot 4**.

Use the right configuration; this is where Gradle differs most from Maven:

| Configuration | Meaning |
|---|---|
| `implementation` | Needed to compile and run. Not exposed to consumers |
| `api` | Needed to compile and **exposed** to consumers (java-library plugin only) |
| `runtimeOnly` | Needed at runtime only - JDBC drivers, Flyway database modules |
| `compileOnly` | Needed to compile only |
| `annotationProcessor` | Runs at compile time |
| `testImplementation` / `testRuntimeOnly` | The test equivalents |

Putting everything on `implementation` works but slows incremental builds; `api` in a library leaks your dependencies into every consumer's compile classpath, so reach for it deliberately.

## Tests

```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}
```

The Java plugin supplies the `test` task; there is no separate Surefire plugin to add. Configure `useJUnitPlatform()` so Gradle discovers JUnit Platform tests. Spring Initializr already applies this setting to generated Spring projects. The Maven equivalent is Surefire, which Maven binds to its `test` lifecycle phase.

`exceptionFormat = FULL` gives you the whole stack trace in CI output, which is the difference between diagnosing a failure from the log and having to reproduce it.

### Integration tests

For Docker-backed tests that should run separately from `test`, create a dedicated source set and `Test` task. This is the Gradle equivalent of Maven Failsafe; Gradle has no built-in Failsafe plugin. Wire the task into `check` so CI runs it:

```kotlin
import org.gradle.api.tasks.testing.Test

val integrationTestSourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[integrationTestSourceSet.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[integrationTestSourceSet.compileOnlyConfigurationName]
    .extendsFrom(configurations.testCompileOnly.get())
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(integrationTestTask)
}
```

Put the tests under `src/integrationTest/java`, name them `*IT`, and add Testcontainers dependencies to the `integrationTest` configurations when needed. `./gradlew test` stays fast; `./gradlew check` runs both test tasks.

## The Boot plugin

```kotlin
springBoot {
    buildInfo()
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName = "${System.getenv("DOCKER_REGISTRY")}/${project.name}:${project.version}"
}
```

`buildInfo()` generates the build metadata Actuator exposes at `/actuator/info` ([observability.md](../observability/observability.md)).

`bootBuildImage` builds an OCI image with buildpacks - no Dockerfile ([docker.md](../deployment/docker.md)).

## Coverage and formatting

The generator pins Spotless Gradle Plugin `8.10.4` and Palantir Java Format `2.102.0` in `versions.json`. Spotless automatically attaches `spotlessCheck` to Gradle's `check` task.

```kotlin
plugins {
    jacoco
    id("com.diffplug.spotless") version "8.10.4"
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        formatAnnotations()
        palantirJavaFormat("2.102.0")
    }
}
```

Run `./gradlew spotlessApply` (or `task format`) to apply formatting and `./gradlew spotlessCheck` to check it directly. JaCoCo verification still needs an explicit dependency from `check`; unlike Spotless, applying JaCoCo alone does not create a coverage rule or wire that rule into the lifecycle.

The coverage-as-a-floor argument from [maven.md](maven.md) applies.

### Git build provenance

Spring Boot's `buildInfo()` supplies the application version and build timestamp. If the application also needs the Git branch and commit in Actuator's `/actuator/info`, the community [Gradle Git Properties plugin](https://github.com/n0mer/gradle-git-properties) generates `git.properties`:

```kotlin
plugins {
    id("com.gorylenko.gradle-git-properties") version "4.0.1"
}

gitProperties {
    keys = listOf("git.branch", "git.commit.id.abbrev", "git.commit.time")
    failOnNoGitDirectory = false
}
```

Limit the keys: the plugin can also emit author names, email addresses, hostnames, and remote URLs. Spring Boot exposes Git metadata by default when the file is on the classpath; avoid enabling full metadata unless it is appropriate for the endpoint.

## Multi-project

```kotlin
// settings.gradle.kts
rootProject.name = "my-app"
include("domain", "application", "infrastructure", "web")
```

```kotlin
// build.gradle.kts (root)
plugins {
    id("org.springframework.boot") version "<current 4.x>" apply false
    id("io.spring.dependency-management") version "<current>" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    the<DependencyManagementExtension>().imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}
```

```kotlin
// domain/build.gradle.kts - pure Java, no Spring
dependencies { }

// web/build.gradle.kts - the runnable application
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
}
```

`apply false` at the root then applying the Boot plugin only in the runnable module is the Gradle equivalent of Maven's "boot plugin in the runnable module only". Applying it to every subproject makes Gradle try to build an executable jar from your library modules.

Gradle enforces the dependency direction the build file declares, but not the design. Use ArchUnit ([archunit.md](../architecture/archunit.md)).

## Build performance

Gradle's advantage over Maven is incremental builds and caching. Turn them on:

```properties
# gradle.properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configuration-cache=true
```

The configuration cache is strict about build scripts reading system state at configuration time - the migration is usually small and the payoff is large on a multi-project build.

**Commit the Gradle wrapper** (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) so everyone builds with the same Gradle version. Check `gradle/wrapper/gradle-wrapper.properties` uses an HTTPS distribution URL.

## Version catalogs

For a multi-project build, keep versions in one place:

```toml
# gradle/libs.versions.toml
[versions]
springBoot = "<current 4.x>"
testcontainers = "<managed by the BOM - omit>"

[libraries]
postgresql = { module = "org.postgresql:postgresql" }

[plugins]
springBoot = { id = "org.springframework.boot", version.ref = "springBoot" }
spotless = { id = "com.diffplug.spotless", version = "8.10.4" }
```

```kotlin
dependencies {
    runtimeOnly(libs.postgresql)
}
```

Type-safe, refactorable, and one place to look. Do not add versions for anything the Boot BOM manages - a catalog entry with an explicit version silently overrides it, which is exactly the failure the BOM exists to prevent.

## If on Boot 3.5.x

Structure is identical. The differences are the same as Maven's: starter names, Flyway arriving transitively, `spring-boot-starter-test` rather than per-technology `-test` starters, and Testcontainers 1.x coordinates ([testcontainers.md](../testing/testcontainers.md)).

## Gotchas

- Agent omits `useJUnitPlatform()` - every test is silently skipped and the build passes
- Agent configures JaCoCo verification without wiring it into `check` - the rule never runs
- Agent applies the Boot plugin to every subproject - library modules cannot be repackaged as executables
- Agent uses `sourceCompatibility` instead of a toolchain - the compiling JDK is then whatever the machine has
- Agent puts a JDBC driver on `implementation` - `runtimeOnly`
- Agent puts everything on `api` in a library - leaks dependencies into every consumer's compile classpath
- Agent adds a version for a BOM-managed dependency, including in a version catalog - the BOM decides
- Agent applies both `io.spring.dependency-management` and a `platform(...)` BOM - pick one
- Agent omits the Gradle wrapper - builds differ between machines
- Agent adds `spring-boot-starter-test` next to a Boot 4 `-test` starter
- Agent forgets `spring-boot-starter-flyway` on Boot 4 - migrations silently never run
- Agent creates a separate integration-test source set but does not wire its task into `check` - CI skips those tests
- Agent adds Spotless but assumes it formats code automatically - run `spotlessApply`; `check` only verifies formatting

## Related

- [maven.md](maven.md) · [spring-boot-migration.md](../spring-boot-migration.md) · [docker.md](../deployment/docker.md) · [testcontainers.md](../testing/testcontainers.md) · [archunit.md](../architecture/archunit.md)

## References

- [Spotless Gradle guide](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md)
- [Gradle Git Properties plugin](https://github.com/n0mer/gradle-git-properties)
