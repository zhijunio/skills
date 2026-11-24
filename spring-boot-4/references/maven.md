# Maven

Read this when the task edits `pom.xml`, the wrapper, or Maven plugins.
Gradle waits for `references/gradle.md`. Forbidden persistence and JSON
artifacts stay in Core Rules and `doctor-project.mjs`.

## When to use

- Add, remove, or pin a Maven dependency
- Change `java.version`, the Boot parent, or a plugin
- Diagnose a wrapper or `./mvnw verify` failure that starts in the pom

## When not to use

- Gradle. Stay on Maven until `references/gradle.md` exists
- Java language or JDK work with no pom change → [java-developer](../java-developer/SKILL.md)

## Rules

1. Run the committed wrapper, `./mvnw`. The wrapper is part of the project.
2. Keep `spring-boot-starter-parent`. Read its version from the pom. Change it
   only to a Boot 4.x pin from `versions.json`. Import
   `spring-boot-dependencies` only when the task names a corporate parent.
3. Put every third-party version in `<properties>` and
   `<dependencyManagement>`. Dependencies omit `<version>`. Copy property
   values from `versions.json`.
4. Omit `<version>` on artifacts the parent already manages. A second version
   is how you get a `NoSuchMethodError` the build never reported.
5. Keep `spring-boot-starter-flyway` (and `flyway-database-postgresql` with
   PostgreSQL) in the pom. Boot 4 does not pull Flyway transitively. Without
   the starter, migrations never run.
6. Keep Boot 4 starter names. Web is `spring-boot-starter-webmvc`. A matching
   `-test` starter is self-contained. Use `spring-boot-starter-webmvc-test`
   alone for web tests.
7. Keep `maven-surefire-plugin` with `failIfNoTests` true. The parent manages
   its version. Do not add Failsafe until `references/testing.md` exists.
8. Keep `spotless-maven-plugin`. Pin it from `versions.json` through
   `<properties>` and `<pluginManagement>`. `./mvnw verify` runs `spotless:check`.
   Format with `./mvnw spotless:apply`.

## Add a dependency

Boot-managed starter. No version.

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
```

Third-party library. Add the property and the `dependencyManagement` entry
first. Then add the dependency with no version.

```xml
<properties>
	<springdoc.version>COPY_FROM_VERSIONS_JSON</springdoc.version>
</properties>

<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>${springdoc.version}</version>
		</dependency>
	</dependencies>
</dependencyManagement>

<dependency>
	<groupId>org.springdoc</groupId>
	<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

Insert under the existing `<dependencies>`. Do not add a second
`<dependencies>` block. Do not add `build.gradle` or `build.gradle.kts`.

## Plugins

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

Spotless. Version from `pluginManagement`, Palantir version from properties.

```xml
<plugin>
	<groupId>com.diffplug.spotless</groupId>
	<artifactId>spotless-maven-plugin</artifactId>
	<configuration>
		<java>
			<importOrder/>
			<removeUnusedImports/>
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

## Verify

From the app directory:

```bash
./mvnw spotless:apply
./mvnw verify
```

From this skill directory, after a pom change:

```bash
node scripts/doctor-project.mjs --strict /absolute/path/to/app
```

Report the commands actually run.
