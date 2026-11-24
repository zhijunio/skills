# Maven

Read this when creating a Maven project, or when the task edits `pom.xml`, the
wrapper, or Maven plugins. The skeleton script does not add Surefire, Spotless,
JaCoCo, Boot `build-info`, or Enforcer. This reference applies to Maven builds;
follow existing conventions for Gradle. Persistence and JSON defaults are in
the skill's Core Rules and their matching stack references.

## When to use

- A **new** Maven project; omit only plugins the user explicitly declined
- Add, remove, or pin a Maven dependency on a task that already edits the pom
- Change `java.version`, the Boot parent, or a plugin
- Diagnose a wrapper or unit-test failure that starts in the pom
- The user asked to align an existing project with house plugins
- Add or run the Maven Release Plugin when the user requests Maven releases

## When not to use

- An existing project that is not a new Create and the user did not ask to
  align house plugins
- A task that does not change or need Maven-specific build configuration
- Gradle builds; this reference covers Maven only
- Java language or JDK work with no pom change → [java-developer](../../java-developer/SKILL.md)

## Rules

1. Run the committed wrapper, `./mvnw`. The wrapper is part of the project.
2. Keep `spring-boot-starter-parent`. Read its version from the pom. Change it
   only to `springBoot.maven` in `versions.json`; `springBoot.default` is the
   generic Boot version for non-Maven build tools. Import
   `spring-boot-dependencies` only when the task names a corporate parent.
3. Put every third-party version in `<properties>` and
   `<dependencyManagement>`. Dependencies omit `<version>`. Copy property
   values from `versions.json`.
4. Omit `<version>` on artifacts the parent already manages. A second version
   is how you get a `NoSuchMethodError` the build never reported.
5. Keep Boot 4 starter names. Web is `spring-boot-starter-webmvc`. A matching
   `-test` starter is self-contained. Use `spring-boot-starter-webmvc-test`
   alone for web tests.
6. Keep `maven-surefire-plugin` with `failIfNoTests` true. The parent manages
   its version. Do not add Failsafe. [testing.md](testing.md) does not cover it.
7. Keep `spotless-maven-plugin`. Pin `spotless.mavenPlugin` and
   `spotless.palantirJavaFormat` from `versions.json` through `<properties>`
   and `<pluginManagement>`. `verify` runs `spotless:check`; format in this
   file's Verify, not from other stack files.
8. Keep `spring-boot-maven-plugin` from Initializr. Add a `build-info`
   execution. Do not add a second Boot plugin. The parent manages its version.
9. Keep `maven-enforcer-plugin`. The parent manages its version. Require a JDK
   at or above `<java.version>` and a Maven at or above `mavenMinimum` from
   `versions.json`. Ban duplicate dependency versions in the pom. Do not add
   `dependencyConvergence` unless the task names it.
10. Maven release is opt-in. Do not add or run the Maven Release Plugin unless
    the user asks for release support or a release. Pin its version from
    `versions.json` in `<pluginManagement>` when it is needed.
11. New Maven projects include JaCoCo for test coverage reports. Pin
    `jacoco.mavenPlugin` from `versions.json` in `<pluginManagement>`. Attach
    the agent before tests and generate the report during `verify`. Do not
    impose a minimum coverage threshold unless the user or repository policy
    defines one. Add it to an existing project only when requested.

## Add a dependency

Boot-managed starter. No version.

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Third-party library. Add the property and the `dependencyManagement` entry
first. Then add the dependency with no version.

```xml
<properties>
	<library.version>COPY_FROM_VERSIONS_JSON</library.version>
</properties>

<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>io.example</groupId>
			<artifactId>example-library</artifactId>
			<version>${library.version}</version>
		</dependency>
	</dependencies>
</dependencyManagement>

<dependency>
	<groupId>io.example</groupId>
	<artifactId>example-library</artifactId>
</dependency>
```

OpenAPI, MyBatis-Plus, and Flyway coordinates belong in those stack files,
not here.

Insert under the existing `<dependencies>`. Do not add a second
`<dependencies>` block. Do not add `build.gradle` or `build.gradle.kts`.

## Plugins

Boot plugin. No version. The parent manages it. Keep the Initializr plugin and
add the `build-info` execution. Do not declare a second
`spring-boot-maven-plugin`.

```xml
<plugin>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-maven-plugin</artifactId>
	<executions>
		<execution>
			<goals>
				<goal>build-info</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

Enforcer. No version. The parent manages it. Copy `mavenMinimum` from
`versions.json` into the Maven version range.

```xml
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-enforcer-plugin</artifactId>
	<executions>
		<execution>
			<id>enforce</id>
			<goals>
				<goal>enforce</goal>
			</goals>
			<configuration>
				<rules>
					<requireJavaVersion>
						<version>[${java.version},)</version>
					</requireJavaVersion>
					<requireMavenVersion>
						<version>[COPY_FROM_VERSIONS_JSON,)</version>
					</requireMavenVersion>
					<banDuplicatePomDependencyVersions/>
				</rules>
			</configuration>
		</execution>
	</executions>
</plugin>
```

Surefire. No version. The parent manages it.

```xml
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-surefire-plugin</artifactId>
	<configuration>
		<failIfNoTests>true</failIfNoTests>
	</configuration>
</plugin>
```

JaCoCo. Copy `jacoco.mavenPlugin` from `versions.json` through a property and
`pluginManagement`. `prepare-agent` instruments Surefire tests; `report`
generates `target/site/jacoco/index.html` during `verify`.

```xml
<properties>
	<jacoco.version>COPY_FROM_VERSIONS_JSON</jacoco.version>
</properties>

<pluginManagement>
	<plugins>
		<plugin>
			<groupId>org.jacoco</groupId>
			<artifactId>jacoco-maven-plugin</artifactId>
			<version>${jacoco.version}</version>
		</plugin>
	</plugins>
</pluginManagement>

<plugin>
	<groupId>org.jacoco</groupId>
	<artifactId>jacoco-maven-plugin</artifactId>
	<executions>
		<execution>
			<goals>
				<goal>prepare-agent</goal>
			</goals>
		</execution>
		<execution>
			<id>report</id>
			<phase>verify</phase>
			<goals>
				<goal>report</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

Keep Surefire's forked test JVM enabled; JaCoCo cannot collect coverage when
Surefire runs with `forkCount=0`. If Surefire already sets `argLine`, preserve
JaCoCo's late-bound `@{argLine}` property in that configuration.

Spotless. Copy `spotless.mavenPlugin` and `spotless.palantirJavaFormat` from
`versions.json`. Version lives in `pluginManagement`. The plugin declaration
omits `<version>`.

```xml
<properties>
	<spotless.version>COPY_FROM_VERSIONS_JSON</spotless.version>
	<palantir-java-format.version>COPY_FROM_VERSIONS_JSON</palantir-java-format.version>
</properties>

<pluginManagement>
	<plugins>
		<plugin>
			<groupId>com.diffplug.spotless</groupId>
			<artifactId>spotless-maven-plugin</artifactId>
			<version>${spotless.version}</version>
		</plugin>
	</plugins>
</pluginManagement>

<plugin>
	<groupId>com.diffplug.spotless</groupId>
	<artifactId>spotless-maven-plugin</artifactId>
	<configuration>
		<java>
			<shortenFullyQualifiedTypes/>
			<importOrder/>
			<removeUnusedImports/>
			<expandWildcardImports/>
			<forbidWildcardImports/>
			<formatAnnotations/>
			<palantirJavaFormat>
				<version>${palantir-java-format.version}</version>
			</palantirJavaFormat>
		</java>
	</configuration>
	<executions>
		<execution>
			<goals>
				<goal>check</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

## Maven Release

Use Apache Maven Release Plugin when the project intentionally publishes
versioned releases through SCM. It operates on Git state: `release:prepare`
updates POM versions, commits, and creates a tag; `release:perform` checks out
that tag and performs the release build/deploy. These operations can change
the checkout and remote repository whether invoked locally or from CI.

Before enabling it, confirm the project has a valid SCM `developerConnection`,
the release and next development versions are agreed, credentials are provided
through Maven `settings.xml` or the execution environment, and the working tree
is clean. Do not store SCM or deployment credentials in the POM. Run a dry
run and review the generated changes before a real release. Do not run a real
release without the user's explicit request.

The plugin is not part of the default Maven setup. If requested, copy
`mavenRelease.plugin` from `versions.json` into a Maven property and pin the
plugin in `pluginManagement`:

```xml
<properties>
	<maven-release-plugin.version>COPY_FROM_VERSIONS_JSON</maven-release-plugin.version>
</properties>

<build>
	<pluginManagement>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-release-plugin</artifactId>
				<version>${maven-release-plugin.version}</version>
			</plugin>
		</plugins>
	</pluginManagement>
</build>

<scm>
	<developerConnection>scm:git:https://github.com/ORG/REPOSITORY.git</developerConnection>
</scm>
```

Validate the workflow without committing or tagging:

```bash
./mvnw --batch-mode release:prepare -DdryRun=true
./mvnw release:clean
```

After review and explicit authorization to release, run the actual goals:

```bash
./mvnw --batch-mode release:prepare
./mvnw --batch-mode release:perform
```

Treat prepare/perform as separate steps. `release:prepare` commits and tags;
`release:perform` may publish artifacts according to the project's
distribution configuration. Confirm remote effects and repository state
before reporting completion. A CI workflow may call these goals, but CI is
optional and is not required for Maven Release.

## Verify

**Rules.** Every numbered Rule in this file must hold on disk. Open `pom.xml`;
do not infer. Report each Rule that failed.

**Unit tests.** SKILL Workflow's last step requires unit tests to pass once,
after every applied reference's Rules hold. This file is how Maven does that.
Do not copy these commands into other stack files. Gradle projects follow
their existing build conventions; these Maven commands do not apply. A green
run only means unit tests (and Spotless check) passed. It does not prove the
Rules. If a datasource is on the classpath, its service must be available.

After Spotless is in the pom, format then verify:

```bash
./mvnw spotless:apply
./mvnw verify
```

If `Taskfile.yml` exists, `task verify` is the same lifecycle. Run one of
`./mvnw verify` or `task verify`, not both.

Report the commands actually run.
