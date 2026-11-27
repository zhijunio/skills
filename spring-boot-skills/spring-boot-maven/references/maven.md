# Maven

Read this when creating a Maven project, or when the task edits `pom.xml`, the
wrapper, or Maven plugins. Spring Initializr does not add Surefire, Spotless,
JaCoCo, Boot `build-info`, or Enforcer by default. This reference applies to
Maven builds; follow existing conventions for Gradle.

The plugin versions in this reference are recorded in [maven-versions.json](maven-versions.json).

## Build conventions

- Run the committed wrapper, `./mvnw`. The wrapper is part of the project.

- Keep `spring-boot-starter-parent`. Read its version from `pom.xml`. Change it
  only to `springBoot.maven` in `maven-versions.json`; `springBoot.default` is the
  generic Boot version for non-Maven build tools. Import
  `spring-boot-dependencies` only when the task names a corporate parent.

- Put every third-party version in `<properties>` and
  `<dependencyManagement>`. Dependencies omit `<version>`. Copy property
  values from `maven-versions.json`.

- Omit `<version>` on artifacts the parent already manages. A second version
  is how you get a `NoSuchMethodError` the build never reported.

- Keep Boot 4 starter names. Web is `spring-boot-starter-webmvc`. Use
  `spring-boot-starter-webmvc-test` for MVC slices. Add the BOM-managed
  `spring-boot-resttestclient` module only when a test calls a live random-port
  server through `RestTestClient`. Add `spring-boot-starter-restclient-test`
  when testing outbound `RestClient` code with `@RestClientTest`.

- Keep `maven-surefire-plugin` with `failIfNoTests` true. The parent manages
  its version. Do not add Failsafe. For application test strategy, see
  [spring-boot-testing](../../spring-boot-testing/SKILL.md).

- Keep `spotless-maven-plugin`. Pin `spotless.mavenPlugin` and
  `spotless.palantirJavaFormat` from `maven-versions.json` through `<properties>`
  and `<pluginManagement>`. `verify` runs `spotless:check`; format with the commands in Verify below.

- Keep `spring-boot-maven-plugin` from Initializr. Add a `build-info`
  execution. Do not add a second Boot plugin. The parent manages its version.

- Keep `maven-enforcer-plugin`. The parent manages its version. Require a JDK
  at or above `<java.version>` and a Maven at or above `mavenMinimum` from
  `maven-versions.json`. Ban duplicate dependency versions in `pom.xml`. Do not add
  `dependencyConvergence` unless the task names it.

- Maven release is opt-in. Do not add or run the Maven Release Plugin unless
  the user asks for release support or a release. Pin its version from
  `maven-versions.json` in `<pluginManagement>` when it is needed.

- New Maven projects include JaCoCo for test coverage reports. Pin
  `jacoco.mavenPlugin` from `maven-versions.json` in `<pluginManagement>`. Attach
  the agent before tests and generate the report during `verify`. Do not
  impose a minimum coverage threshold unless the user or repository policy
  defines one. Add it to an existing project only when requested.

- New Maven projects include Taikai as a test-scoped architecture baseline. Pin
  `architectureTesting.taikai.default` from `maven-versions.json` through
  `<properties>` and `<dependencyManagement>`. Add focused boundary rules only
  when the project has those package or dependency boundaries; add Taikai to an
  existing project only when architecture checks are requested.
## Dependencies

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
	<library.version>COPY_FROM_MAVEN_VERSIONS_JSON</library.version>
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

Taikai architecture test. The selected version comes from
`architectureTesting.taikai.default` in `maven-versions.json`.

```xml
<properties>
	<taikai.version>COPY_FROM_MAVEN_VERSIONS_JSON</taikai.version>
</properties>

<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>com.enofex</groupId>
			<artifactId>taikai</artifactId>
			<version>${taikai.version}</version>
		</dependency>
	</dependencies>
</dependencyManagement>

<dependency>
	<groupId>com.enofex</groupId>
	<artifactId>taikai</artifactId>
	<scope>test</scope>
</dependency>
```

Keep stack-specific dependency choices in their corresponding skills. This
reference owns Maven configuration only.

Insert under the existing `<dependencies>`. Do not add a second
`<dependencies>` block. Do not add `build.gradle` or `build.gradle.kts`.

## Plugin configuration

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

When `spring-boot:build-image` is part of the project's workflow, set a stable image name:

```xml
<properties>
	<dockerImageName>registry.example/my-application</dockerImageName>
</properties>

<plugin>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-maven-plugin</artifactId>
	<configuration>
		<image>
			<name>${dockerImageName}</name>
		</image>
	</configuration>
</plugin>
```

Do not add image configuration to projects that do not build container images.

Enforcer. No version. The parent manages it. Copy `mavenMinimum` from
`maven-versions.json` into the Maven version range.

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
						<version>[COPY_FROM_MAVEN_VERSIONS_JSON,)</version>
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

JaCoCo. Copy `jacoco.mavenPlugin` from `maven-versions.json` through a property and
`pluginManagement`. `prepare-agent` instruments Surefire tests; `report`
generates `target/site/jacoco/index.html` during `verify`.

```xml
<properties>
	<jacoco.version>COPY_FROM_MAVEN_VERSIONS_JSON</jacoco.version>
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

An optional `check` execution can enforce a repository-approved minimum. HelpDesk uses a bundle
line-coverage threshold of 75%; treat that as a project policy choice, not a universal default.
Its POM spells the property as `75%`, but JaCoCo expects a ratio from `0.0` to `1.0`; use `0.75`
for 75% rather than copying that literal.

Set the threshold property in `<properties>`:

```xml
<properties>
	<jacoco.minimum.coverage>0.75</jacoco.minimum.coverage>
</properties>
```

Append this execution to the existing JaCoCo `<executions>` list:

```xml
<execution>
	<id>check</id>
	<goals>
		<goal>check</goal>
	</goals>
	<phase>verify</phase>
	<configuration>
		<rules>
			<rule>
				<element>BUNDLE</element>
				<limits>
					<limit>
						<counter>LINE</counter>
						<value>COVEREDRATIO</value>
						<minimum>${jacoco.minimum.coverage}</minimum>
					</limit>
				</limits>
			</rule>
		</rules>
	</configuration>
</execution>
```

Keep Surefire's forked test JVM enabled; JaCoCo cannot collect coverage when
Surefire runs with `forkCount=0`. If Surefire already sets `argLine`, preserve
JaCoCo's late-bound `@{argLine}` property in that configuration.

Spotless. Copy `spotless.mavenPlugin` and `spotless.palantirJavaFormat` from
`maven-versions.json`. Version lives in `pluginManagement`. The plugin declaration
omits `<version>`.

```xml
<properties>
	<spotless.version>COPY_FROM_MAVEN_VERSIONS_JSON</spotless.version>
	<palantir-java-format.version>COPY_FROM_MAVEN_VERSIONS_JSON</palantir-java-format.version>
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
		<!-- Optional: normalize pom.xml formatting as part of Spotless. -->
		<pom>
			<sortPom>
				<nrOfIndentSpace>4</nrOfIndentSpace>
				<keepBlankLines>false</keepBlankLines>
				<expandEmptyElements>false</expandEmptyElements>
			</sortPom>
		</pom>
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

Spotless binds `check` to `verify` by default. A project may bind it to `compile` for earlier
feedback, as HelpDesk does; choose one phase deliberately and keep the documented command aligned.

## Optional Git build metadata

When Actuator or an operations endpoint needs source revision metadata, a Git commit ID plugin can
generate `git.properties`. HelpDesk limits the generated fields and allows builds without a Git
directory. Add this only when the application consumes the metadata. Resolve and pin the plugin
version in `maven-versions.json` first; the HelpDesk POM does not declare one and this catalog does
not currently pin it.

```xml
<properties>
	<git-commit-id-maven-plugin.version>ADD_TO_MAVEN_VERSIONS_JSON</git-commit-id-maven-plugin.version>
</properties>

<pluginManagement>
	<plugins>
		<plugin>
			<groupId>io.github.git-commit-id</groupId>
			<artifactId>git-commit-id-maven-plugin</artifactId>
			<version>${git-commit-id-maven-plugin.version}</version>
		</plugin>
	</plugins>
</pluginManagement>

<plugin>
	<groupId>io.github.git-commit-id</groupId>
	<artifactId>git-commit-id-maven-plugin</artifactId>
	<configuration>
		<failOnNoGitDirectory>false</failOnNoGitDirectory>
		<failOnUnableToExtractRepoInfo>false</failOnUnableToExtractRepoInfo>
		<generateGitPropertiesFile>true</generateGitPropertiesFile>
		<includeOnlyProperties>
			<includeOnlyProperty>^git.branch$</includeOnlyProperty>
			<includeOnlyProperty>^git.commit.id.abbrev$</includeOnlyProperty>
		</includeOnlyProperties>
	</configuration>
	<executions>
		<execution>
			<goals>
				<goal>revision</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

Do not include commit messages or author names in generated application metadata unless exposing
them is intentional. If release provenance requires Git metadata, keep missing repository
information a build failure instead of silently producing incomplete metadata.

## Maven releases

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
`mavenRelease.plugin` from `maven-versions.json` into a Maven property and pin the
plugin in `pluginManagement`:

```xml
<properties>
	<maven-release-plugin.version>COPY_FROM_MAVEN_VERSIONS_JSON</maven-release-plugin.version>
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

For a new project or an explicit house-style alignment, inspect every build convention. For other
changes, inspect the conventions relevant to the request. Read `pom.xml`; do not infer. If a
datasource is on the test classpath, make it available before running verification. A passing
`verify` confirms configured Maven checks, not native-image or production deployment behavior.

After Spotless is in `pom.xml`, format then verify:

```bash
./mvnw spotless:apply
./mvnw verify
```

If `mise.toml` defines a `verify` task, [`mise run verify`](../../mise/SKILL.md) may wrap the same lifecycle. Run one verification command, not both.

Report the commands actually run.

## Reference example

See the [HelpDesk POM](https://github.com/sivaprasadreddy/HelpDesk/blob/main/pom.xml) for Boot
`build-info` and Buildpacks image naming, Git revision metadata, JaCoCo reporting and coverage
checks, and Spotless Java plus POM formatting. Adapt its project-specific threshold, image name,
and source metadata fields to the target project.

## Plugin references

- [Spring Boot Maven Plugin: build image](https://docs.spring.io/spring-boot/maven-plugin/build-image.html)
- [Spotless Maven Plugin](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md)
- [JaCoCo `check` goal](https://www.eclemma.org/jacoco/trunk/doc/check-mojo.html)
- [Git commit ID Maven Plugin configuration](https://github.com/git-commit-id/git-commit-id-maven-plugin/blob/master/docs/configuration-and-properties.md)
