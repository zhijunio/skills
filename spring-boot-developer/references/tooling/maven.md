# Maven

The rest of this skill is build-tool agnostic. This file and [gradle.md](gradle.md) are the two places build configuration lives.

## The parent

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version><!-- current 4.x --></version>
    <relativePath/>
</parent>

<properties>
    <java.version>21</java.version>
</properties>
```

The parent supplies dependency management, plugin management, sensible compiler settings and resource filtering. If your organisation already has a corporate parent, import the BOM instead:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version><!-- current 4.x --></version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

You then lose the plugin management, so `spring-boot-maven-plugin` needs its version declared.

**Declare no version for anything the BOM manages** - Hibernate, Jackson, Testcontainers, JUnit, Micrometer and several hundred others. Overriding one is how you get a `NoSuchMethodError` at runtime that the build never warned about. See [spring-boot-migration.md](../spring-boot-migration.md).

## Starters

One starter per technology. Boot 4 renamed several; the old names still resolve but are deprecated.

| Need | Starter |
|---|---|
| Web / REST | `spring-boot-starter-webmvc` (was `-web`) |
| JPA | `spring-boot-starter-data-jpa` |
| **Flyway** | `spring-boot-starter-flyway` - **not transitive on Boot 4**; without it migrations silently never run |
| Validation | `spring-boot-starter-validation` - not brought in by the web starter |
| Security | `spring-boot-starter-security` |
| OAuth2 resource server | `spring-boot-starter-security-oauth2-resource-server` (was `-oauth2-resource-server`) |
| AOP | `spring-boot-starter-aspectj` (was `-aop`) |
| Actuator | `spring-boot-starter-actuator` |
| Testing (web) | `spring-boot-starter-webmvc-test` |
| Testing (persistence) | `spring-boot-starter-data-jpa-test` |

The Boot 4 `-test` starters are self-contained - do **not** add `spring-boot-starter-test` alongside one. `spring-boot-starter-classic` exists as a migration aid, not a destination.

## The Boot plugin

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <image>
            <name>${docker.registry}/${project.artifactId}:${project.version}</name>
        </image>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>build-info</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

`build-info` generates `META-INF/build-info.properties`, which Actuator exposes at `/actuator/info` - so the running application can tell you exactly which version it is ([observability.md](../observability/observability.md)). It costs nothing and settles a surprising number of incidents.

In a multi-module build, the plugin belongs **only in the runnable module**. In the parent it tries to repackage every module, including the ones that are libraries.

**Optional dependencies are excluded from the repackaged jar on Boot 4.** If you rely on one at runtime, set `<includeOptional>true</includeOptional>`.

## Plugins worth configuring

None of these are required. Each buys something specific.

### Unit and slice tests with Surefire

Maven's default lifecycle binds Surefire to the `test` phase, so ordinary Maven projects already run unit and slice tests without declaring the plugin in `<build>`. Spring Boot's parent manages its version. When importing only the Boot BOM, configure a Surefire version in `<pluginManagement>`; the BOM does not manage Maven plugins. Add `maven-surefire-plugin` under `<plugins>` only when the project needs non-default behavior.

Use the default class-name patterns (`Test*`, `*Test`, `*Tests`, `*TestCase`) and keep Docker-dependent integration tests under a Failsafe convention such as `*IT`. Surefire selects by filename, not by annotations or words such as `Integration` in the class name: `TaskApiIntegrationTest` matches `*Test` and runs during `./mvnw test`, while `ProductApiIT` is reserved for Failsafe. A Docker-backed class that matches Surefire makes `test` require Docker. Rename it to `*IT` and bind Failsafe when `test` should stay fast and independent of Docker, with `./mvnw verify` running the full suite. Avoid custom include/exclude lists unless test names genuinely require them; stale patterns can silently omit tests.

```xml
<!-- Usually no extra configuration is needed. Declare the plugin only to customize it. -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <failIfNoTests>true</failIfNoTests>
    </configuration>
</plugin>
```

`failIfNoTests` is useful in runnable application modules, but may need a different policy in aggregator or intentionally test-free modules. Surefire runs tests matching its patterns; it does not configure Failsafe or move integration tests to `verify`.

```bash
./mvnw test
./mvnw test -Dtest=OrderServiceTest
./mvnw test -Dtest='OrderServiceTest#rejectsExpiredOrder'
./mvnw verify
```

See the official [Surefire guide](https://maven.apache.org/surefire/maven-surefire-plugin/) and the project's [testing strategy](../testing/testing-strategy.md).

### Integration tests with Failsafe

Use Failsafe when Docker-backed integration tests should run separately from the fast `test` phase. Name them `*IT` (or use one of Failsafe's other default patterns) and bind both goals. The `integration-test` goal runs the tests; `verify` checks the result and reports failures after the integration-test lifecycle has completed.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The Spring Boot parent manages the plugin version. The project generator adds this configuration only when PostgreSQL/Testcontainers is selected. Run `./mvnw test` for fast tests and `./mvnw verify` for the full suite. Do not bind only `integration-test`; doing so can leave test failures unreported or skip cleanup/reporting behavior.

#### Optional JUnit 5 tree output

For per-test console output, `maven-surefire-junit5-tree-reporter` is an optional plugin dependency; it does not enter the application's runtime classpath. Pin a reporter release compatible with the Surefire version managed by the project. The example below uses reporter `1.5.1`; check [Maven Central](https://central.sonatype.com/artifact/me.fabriciorby/maven-surefire-junit5-tree-reporter) before upgrading.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <dependencies>
        <dependency>
            <groupId>me.fabriciorby</groupId>
            <artifactId>maven-surefire-junit5-tree-reporter</artifactId>
            <version>1.5.1</version>
        </dependency>
    </dependencies>
    <configuration>
        <reportFormat>plain</reportFormat>
        <consoleOutputReporter>
            <disable>true</disable>
        </consoleOutputReporter>
        <statelessTestsetInfoReporter
                implementation="org.apache.maven.plugin.surefire.extensions.junit5.JUnit5StatelessTestsetInfoTreeReporter">
            <theme>ASCII</theme>
            <printStacktraceOnError>true</printStacktraceOnError>
            <printStacktraceOnFailure>true</printStacktraceOnFailure>
            <printStdoutOnSuccess>false</printStdoutOnSuccess>
            <printStderrOnSuccess>false</printStderrOnSuccess>
        </statelessTestsetInfoReporter>
    </configuration>
</plugin>
```

Keep `reportFormat=plain`, the tree reporter, and `consoleOutputReporter.disable` together to avoid duplicate or empty output. Set successful-test stdout/stderr to `true` temporarily when diagnosing output-dependent failures. Merge this into the existing Surefire block rather than declaring the plugin twice. Failsafe does not inherit Surefire's plugin configuration; repeat the dependency and configuration there only if integration tests also need tree output. See the official [JUnit Platform / Surefire extension guide](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html#external-extensions-for-the-plugin).

### Configuration metadata

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

Generates IDE autocomplete and documentation for your `@ConfigurationProperties` types ([configuration.md](../configuration/configuration.md)). Cheapest win in this file.

### Test coverage

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution><goals><goal>prepare-agent</goal></goals></execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Coverage is a **floor that stops regression**, not a target. A team optimising for the number writes tests that execute code without asserting anything, and the metric goes up while the suite gets worse. Set it near where you already are and ratchet slowly.

### Formatting

The generator pins Spotless Maven Plugin `3.10.4` and Palantir Java Format `2.102.0` in `versions.json`.

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>3.10.4</version>
    <configuration>
        <java>
            <importOrder/>
            <removeUnusedImports/>
            <formatAnnotations/>
            <palantirJavaFormat>
                <version>2.102.0</version>
            </palantirJavaFormat>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
            <phase>validate</phase>
        </execution>
    </executions>
</plugin>
```

A build-time formatter, not a runtime dependency. The generator adds it to new Maven projects and binds `check` to `validate`, so `./mvnw verify` checks formatting. Run `./mvnw spotless:apply` (or `task format`) to format files locally. Any consistent formatter does the job; the value is in having one, not in which. See the official [Spotless Maven guide](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md).

### Build provenance

```xml
<plugin>
    <groupId>io.github.git-commit-id</groupId>
    <artifactId>git-commit-id-maven-plugin</artifactId>
    <configuration>
        <failOnNoGitDirectory>false</failOnNoGitDirectory>
        <generateGitPropertiesFile>true</generateGitPropertiesFile>
        <includeOnlyProperties>
            <includeOnlyProperty>^git.branch$</includeOnlyProperty>
            <includeOnlyProperty>^git.commit.id.abbrev$</includeOnlyProperty>
            <includeOnlyProperty>^git.commit.time$</includeOnlyProperty>
        </includeOnlyProperties>
    </configuration>
    <executions>
        <execution><goals><goal>revision</goal></goals></execution>
    </executions>
</plugin>
```

Surfaces the exact commit at `/actuator/info`. `failOnNoGitDirectory=false` keeps builds working from a source archive. Restrict the properties - the full set includes the committer's name and email, which you probably do not want on an endpoint.

## Multi-module

```
my-app/
├── pom.xml                 (packaging: pom)
├── my-app-domain/          pure Java
├── my-app-application/     use cases - depends on domain
├── my-app-infrastructure/  JPA, HTTP clients - depends on domain
└── my-app-web/             the runnable Boot application
```

| Module | May depend on |
|---|---|
| `domain` | nothing |
| `application` | `domain` |
| `infrastructure` | `domain`, `application` |
| `web` | all |

- `<dependencyManagement>` in the parent, never `<dependencies>` - the latter puts the dependency on every module's classpath.
- `${project.version}` for inter-module dependencies.
- `spring-boot-maven-plugin` in the runnable module only.
- Maven does not prevent a cycle from being *designed*, only from being built. Enforce direction with ArchUnit ([archunit.md](../architecture/archunit.md)).

**Reach for multi-module deliberately.** It is rung four of the ladder in [code-organization.md](../architecture/code-organization.md) - real enforcement, real ceremony. A single module with feature packages is the right default until a boundary genuinely needs the build to enforce it.

## Reproducibility and hygiene

- **Commit the Maven wrapper** (`mvnw`, `mvnw.cmd`, `.mvn/`). Everyone and CI then build with the same Maven version.
- `mvn verify`, not `mvn install`, in CI - `install` writes to a shared local repository and creates order dependencies between builds.
- `mvn dependency:tree` when a version surprises you; `mvn dependency:analyze` finds declared-unused and used-undeclared dependencies.
- Add `dependency-check` or your platform's scanner to CI. A dependency with a known CVE is the most likely security problem in a typical Spring application.

## Gotchas

- Agent declares a version for a BOM-managed dependency - the BOM decides; overriding causes runtime `NoSuchMethodError`
- Agent adds `spring-boot-starter-data-jpa` and expects Flyway to run on Boot 4 - add `spring-boot-starter-flyway`
- Agent assumes the web starter brings validation - add `spring-boot-starter-validation`
- Agent adds `spring-boot-starter-test` next to a Boot 4 `-test` starter - they are self-contained
- Agent uses `spring-boot-starter-web` / `-aop` / `-oauth2-resource-server` on Boot 4 - renamed (old names deprecated, still resolve)
- Agent puts `spring-boot-maven-plugin` in the parent POM - runnable module only
- Agent puts `<dependencies>` in the parent instead of `<dependencyManagement>` - adds them to every module
- Agent relies on an optional dependency at runtime on Boot 4 - set `<includeOptional>true</includeOptional>`
- Agent sets a coverage target well above current coverage - the team games the metric
- Agent publishes the full git-commit-id property set - it includes committer name and email
- Agent runs `mvn install` in CI - use `verify`
- Agent omits the Maven wrapper - builds differ between machines
- Agent reaches for multi-module before a boundary needs enforcing - feature packages first

## Related

- [gradle.md](gradle.md) · [spring-boot-migration.md](../spring-boot-migration.md) · [code-organization.md](../architecture/code-organization.md) · [docker.md](../deployment/docker.md) · [graalvm.md](../deployment/graalvm.md) · [observability.md](../observability/observability.md) · [configuration.md](../configuration/configuration.md)

## References

- [Surefire guide](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [Failsafe guide](https://maven.apache.org/surefire/maven-failsafe-plugin/)
- [Spotless Maven guide](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md)
- [Maven Central](https://central.sonatype.com/artifact/me.fabriciorby/maven-surefire-junit5-tree-reporter)
- [JUnit Platform / Surefire extension guide](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html#external-extensions-for-the-plugin)
