# Contract-first Generation

Use this workflow when the OpenAPI document is authoritative. Generate the
server boundary and DTOs into the build output, then implement the generated
delegate in handwritten source. Never edit generated files.

## Maven setup

Use OpenAPI Generator 7.20 or later for Spring Boot 4 and Jackson 3 support.
Keep generator versions and package names aligned with the project. This
configuration uses the Spring generator, delegate pattern, and distinct DTO
names:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.26.0</version>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <apiPackage>com.example.api</apiPackage>
                <modelPackage>com.example.api.model</modelPackage>
                <configOptions>
                    <delegatePattern>true</delegatePattern>
                    <interfaceOnly>false</interfaceOnly>
                    <useSpringBoot4>true</useSpringBoot4>
                    <useJackson3>true</useJackson3>
                    <useTags>true</useTags>
                    <dateLibrary>java8</dateLibrary>
                    <openApiNullable>false</openApiNullable>
                    <skipDefaultInterface>false</skipDefaultInterface>
                </configOptions>
                <supportingFilesToGenerate>ApiUtil.java</supportingFilesToGenerate>
                <modelNameSuffix>Dto</modelNameSuffix>
                <output>${project.build.directory}/generated-sources/openapi</output>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Do not enable all supporting files. The generated SpringDoc configuration and
application class can conflict with the application's own configuration and
main class. Ignore generated output and generate it during the build:

```gitignore
target/generated-sources/openapi/
```

The [OpenAPI template](../templates/openapi.yaml) shows resource operations,
request and response schemas, pagination, and Problem Details responses.
For money, avoid `format: double`; keep the generated Java value decimal-safe (typically
`BigDecimal`) and constrain its scale with `multipleOf` when the currency contract requires it.
Use a JSON string only when the API deliberately chooses a decimal-string wire format.

## Implement the delegate

Map generated request and response DTOs at the adapter boundary. Keep domain
models out of the public API and leave generated controllers untouched. The
[delegate implementation example](../examples/delegate-implementation.java) is the canonical
mapping example for the four operations in the OpenAPI template, including the generated `Dto`
suffix, request-to-command mapping, created-resource `Location`, and nested page metadata.

The `Dto` suffix comes from `modelNameSuffix`; keep implementation signatures aligned with the
generated delegate. Map generated request types to application commands instead of passing them
into the service layer. Keep `skipDefaultInterface` false with `delegatePattern` enabled. Generated
controllers instantiate a default delegate, so true can leave operations unimplemented. Cover
every operation through the generated HTTP boundary.

## Verify

Generate from a clean checkout. Confirm generated output stays under the build
directory, every operation has a real delegate, and the generated HTTP
responses match the contract. Include contract checks for errors as well as
success responses.
