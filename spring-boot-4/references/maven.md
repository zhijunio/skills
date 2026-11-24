# Maven

Read this when the task edits `pom.xml`, the wrapper, or Maven plugins.
Gradle waits for `references/gradle.md`. Forbidden persistence and JSON
artifacts stay in Core Rules and `doctor-project.mjs`.

## When to use

- Add, remove, or pin a Maven dependency
- Change `java.version`, the Boot parent, or `spring-boot-maven-plugin`
- Diagnose a wrapper or `./mvnw verify` failure that starts in the pom

## When not to use

- Gradle. Stay on Maven until `references/gradle.md` exists
- Java language or JDK work with no pom change → [java-developer](../java-developer/SKILL.md)

## Rules

1. Run the committed wrapper, `./mvnw`. The wrapper is part of the project.
2. Keep `spring-boot-starter-parent`. Read its version from the pom. Change it
   only to a Boot 4.x pin from `versions.json`. Import
   `spring-boot-dependencies` only when the task names a corporate parent.
3. Omit `<version>` on artifacts the parent manages. A second version is how
   you get a `NoSuchMethodError` the build never reported.
4. Pin MyBatis-Plus and springdoc from `versions.json`. They are not in the
   Boot BOM.
5. Keep `spring-boot-starter-flyway` (and `flyway-database-postgresql` with
   PostgreSQL) in the pom. Boot 4 does not pull Flyway transitively. Without
   the starter, migrations never run.
6. Keep Boot 4 starter names. Web is `spring-boot-starter-webmvc`. A matching
   `-test` starter is self-contained. Use `spring-boot-starter-webmvc-test`
   alone for web tests.

## Add a dependency

Boot-managed starter. No version.

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
```

Third-party pin. Copy the version from `versions.json`.

```xml
<dependency>
	<groupId>com.baomidou</groupId>
	<artifactId>mybatis-plus-spring-boot4-starter</artifactId>
	<version>COPY_FROM_VERSIONS_JSON</version>
</dependency>
```

Insert under `<dependencies>`. Do not add a second `<dependencies>` block.
Do not add `build.gradle` or `build.gradle.kts`.

## Verify

From the app directory:

```bash
./mvnw verify
```

From this skill directory, after a pom change:

```bash
node scripts/doctor-project.mjs --strict /absolute/path/to/app
```

Report the commands actually run.
