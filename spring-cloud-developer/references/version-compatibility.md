# Spring Cloud Version Compatibility

Spring Cloud releases coordinated component versions in a **release train**. Select a train that
supports the project's Spring Boot generation, import its BOM, and let it manage Spring Cloud
components. Do not select a component version from a different train to satisfy a dependency conflict.

## Compatibility checked 2026-10-13

| Spring Boot line | Compatible Spring Cloud train | Open-source support note |
| ---------------- | ----------------------------- | ------------------------- |
| 4.1.x | 2025.1.x, starting with 2025.1.2 | Oakwood 2025.1.2 was the latest GA train verified for this guide. |
| 4.0.x | 2025.1.x | Verify the latest service release and its Boot patch compatibility before upgrading. |
| 3.5.x | 2025.0.x | Northfields 2025.0.3 was the final open-source release; Spring announced OSS support ended 2026-06-30. Use only with an applicable commercial support arrangement or plan a Boot upgrade. |

This table records a point-in-time compatibility check, not a promise that a train remains supported.
Spring Cloud 2025.1.2 announced compatibility with Boot 4.0.7 and added Boot 4.1.0 compatibility;
the Spring Cloud project matrix says 4.1.x starts at 2025.1.2. The 2025.0.3 release announcement
identifies it as the final open-source release of that train. Recheck the [official compatibility
matrix](https://spring.io/projects/spring-cloud#overview), [support policy](https://spring.io/projects/spring-cloud#support),
and release announcements before making a production upgrade decision.

## Maven BOM

Import one release train BOM, then omit versions from Spring Cloud dependencies:

```xml
<properties>
    <spring-cloud.version>2025.1.2</spring-cloud.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

If the project uses the Spring Boot parent or dependency-management plugin, retain that model and
import the Cloud BOM in its dependency-management section. For Gradle, use the compatible Cloud BOM
with the project's existing dependency-management setup. Do not also override individual Cloud
module versions without a documented compatibility reason.

## Upgrade procedure

1. Record the exact Boot, Java, Cloud train, and resolved Cloud component versions.
2. Check both compatibility and support status; compatibility alone does not mean the line receives
   open-source fixes.
3. Update the Boot patch and Cloud train together when the compatibility matrix requires it.
4. Inspect release notes for component changes, security fixes, renamed properties, and removed APIs.
5. Run dependency convergence checks and integration tests across the real network or broker boundary.
6. Roll out with health checks and a rollback path; do not upgrade the whole train and change service
   topology in the same unreviewable step.

## Sources

- [Spring Cloud release-train compatibility matrix](https://spring.io/projects/spring-cloud#overview)
- [Spring Cloud 2025.1.2 release announcement](https://spring.io/blog/2026/06/11/spring-cloud-2025-1-2-aka-oakwood-has-been-released/)
- [Spring Cloud 2025.0.3 release and end-of-OSS-support announcement](https://spring.io/blog/2026/06/11/spring-cloud-2025-0-3-aka-northfields-has-been-released/)
