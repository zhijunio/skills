# OpenAPI

Read this when creating a web project, or when the task adds API docs,
springdoc, or Swagger UI. Swagger UI and the OpenAPI endpoints are enabled in
development and disabled in production. API descriptions come from JavaDoc
comments. The skeleton script does not add springdoc. Add dependencies and version
constraints using the project's build tool. For Maven, see [maven.md](maven.md).

## When to use

- A new web project, unless the user explicitly skipped OpenAPI
- Expose a code-first OpenAPI document or Swagger UI
- Add or pin `springdoc-openapi-starter-webmvc-ui`

## When not to use

- The user asked for no OpenAPI, no Swagger UI, or no springdoc
- Spec-first generators, unless the task names one
- HTTP paths, `*Request` / `*Response`, or `ProblemDetail` → [rest.md](rest.md)

## Rules

1. Read the effective Spring Boot version from the project's build. Find its
   major and minor line in `springdoc.bySpringBootLine` in `versions.json`.
   Preserve an existing springdoc version if it matches that entry's
   `compatible` value. Use the entry's exact `recommended` version when adding
   springdoc or replacing an incompatible version. If the Boot line is absent,
   use the official compatibility matrix linked in `versions.json`.
2. Declare that version using the project's dependency-management convention.
3. Use `springdoc-openapi-starter-webmvc-ui`. Do not add a second OpenAPI
   starter.
4. Disable both OpenAPI documents and Swagger UI in the production profile.
   Keep them enabled in development unless the user requests otherwise.
5. Write JavaDoc on controller handler methods and response types. The method
   comment describes the operation and `@return` describes the successful
   response. Therapi does not expose record-component or explicit record
   accessor JavaDoc as OpenAPI property descriptions. When a property needs a
   generated description, model that DTO as a JavaBean class and document its
   field. Do not add `@Schema` annotations by default; use the JavaDoc
   integration for operation and model descriptions. Add annotations only
   when the task requires metadata JavaDoc cannot express. Verify the
   description in the generated OpenAPI document.

Maven dependency example (use the equivalent notation for the selected build tool):

```xml
<properties>
	<springdoc.version>SELECTED_SPRINGDOC_VERSION</springdoc.version>
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

Use Therapi to make JavaDoc available to springdoc at runtime. Pin
`therapiRuntimeJavadoc.default` from `versions.json` using the project's
version-management convention. Add both artifacts as dependencies and configure
the scribe annotation processor. Maven example follows; other build tools use
their annotation-processor configuration. Merge the processor path into any existing compiler configuration.
Confirm the generated OpenAPI document contains the comments; missing JavaDoc
metadata can fail silently.

```xml
<properties>
	<therapi.version>COPY_FROM_VERSIONS_JSON</therapi.version>
</properties>

<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>com.github.therapi</groupId>
			<artifactId>therapi-runtime-javadoc</artifactId>
			<version>${therapi.version}</version>
		</dependency>
		<dependency>
			<groupId>com.github.therapi</groupId>
			<artifactId>therapi-runtime-javadoc-scribe</artifactId>
			<version>${therapi.version}</version>
		</dependency>
	</dependencies>
</dependencyManagement>

<dependency>
	<groupId>com.github.therapi</groupId>
	<artifactId>therapi-runtime-javadoc</artifactId>
</dependency>

<build>
	<plugins>
		<plugin>
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-compiler-plugin</artifactId>
			<configuration>
				<annotationProcessorPaths>
					<path>
						<groupId>com.github.therapi</groupId>
						<artifactId>therapi-runtime-javadoc-scribe</artifactId>
						<version>${therapi.version}</version>
					</path>
				</annotationProcessorPaths>
			</configuration>
		</plugin>
	</plugins>
</build>
```

Keep docs available outside production:

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

In `application-prod.yaml`, disable both endpoints:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

Do not rely on production network isolation as the only control.

The template demonstrates controller JavaDoc, Therapi's compiler processor,
and production endpoint disabling in its
[controller](../assets/template-maven/src/main/java/com/example/templatemaven/user/api/UserController.java),
[Maven compiler configuration](../assets/template-maven/pom.xml),
[profiles](../assets/template-maven/src/main/resources/application-prod.yaml),
and [OpenAPI integration test](../assets/template-maven/src/test/java/com/example/templatemaven/user/UserIntegrationTests.java).

## Verify

For a new web project, check every rule. For an existing project, check rules
relevant to the OpenAPI change. Open the active build configuration, profile
configuration, controller JavaDoc, and generated OpenAPI document as needed;
do not infer. Report applicable failures.
