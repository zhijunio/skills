# MyBatis-Plus

Read this when the task adds persistence, mappers, or MyBatis-Plus.
The generator does not add MyBatis-Plus. Add its dependency using the
project's existing build tool and version-management convention. For Maven,
see [maven.md](maven.md). Schema SQL is
[flyway.md](flyway.md). Controllers and `@Transactional` are
[webmvc.md](webmvc.md). Do not return persistence types from HTTP;
that contract is [rest.md](rest.md). Context-load tests are
[testing.md](testing.md).

## When to use

- Add a Mapper, persistence model, or database access
- Add or pin `mybatis-plus-spring-boot4-starter`

## When not to use

- Spring Data JPA, `spring-boot-starter-jdbc`, `JdbcTemplate`, or `JdbcClient`
- SQL in `@Select` / `@Insert` / `@Update` / `@Delete`, providers, or mapper XML
- Schema files → [flyway.md](flyway.md)
- HTTP types or paths → [rest.md](rest.md)

## Rules

1. Copy `mybatisPlus.default` from `versions.json` into the project's normal
   version catalog or property mechanism. Declare
   `mybatis-plus-spring-boot4-starter` using the selected build tool. Do not
   add `mybatis-spring-boot-starter` or a second MyBatis artifact beside it.
2. Put `@MapperScan` on the application's MyBatis-Plus configuration class,
   scanning the application package with `annotationClass = Mapper.class`.
   Annotate each Mapper interface with `@Mapper` so ordinary application
   interfaces are not registered as persistence adapters. Keep this wiring in
   the root `config` package as shown in the template.
3. Put shared MyBatis-Plus beans such as `MybatisPlusInterceptor` and
   `MetaObjectHandler` in the application's root `config` package, following
   [architecture.md](architecture.md). Put these under a feature's
   `persistence` package only when they are truly feature-specific and do not
   become accidental global configuration. These are ordinary `@Configuration`
   classes, not Boot starter `@AutoConfiguration` classes.
4. Mapper interfaces extend `BaseMapper<T>` and declare no SQL. Use a fresh
   `LambdaQueryWrapper` / `LambdaUpdateWrapper` and entity method references.
   Do not cache a Wrapper. Do not take column names or SQL fragments from the
   request.
5. The persistence model matches the Flyway table: `@TableName` is singular
   `snake_case`. Field columns use descriptive `snake_case` names without a
   table-name prefix; map a reserved-word identifier explicitly and quote it
   consistently according to [postgresql.md](postgresql.md). `@TableId` is
   `Long` for the `bigint` primary key. The service injects the Mapper. The
   controller does not.
6. Every table has audit time fields named `xxxAt` in Java and `xxx_at` in
   SQL. Type is a timestamp (`Instant` / PostgreSQL `timestamptz`):
   `createdAt` and `updatedAt`. Fill them with a `MetaObjectHandler`. Do not
   take these values from a request. Use `@TableField(fill = FieldFill.INSERT)`
   on `createdAt` and `FieldFill.INSERT_UPDATE` on `updatedAt`. Do not put
   `@JsonFormat` on these fields. JSON timestamp conventions are in the skill's
   Core Rules.
7. Load collections in a batch (`in(...)` or `selectBatchIds`). Do not call
   `selectById` inside a loop.
8. Register one `MybatisPlusInterceptor` bean. For PostgreSQL pagination, add
   `PaginationInnerInterceptor(DbType.POSTGRE_SQL)` to it. Pass a bounded
   `Page<T>` to mapper page methods; validate page number and size at the API
   boundary, set a sensible maximum size, and do not fetch all rows to paginate
   in memory. For MyBatis-Plus 3.5.9 and later, add
   `mybatis-plus-jsqlparser` when using pagination or, if optional
   multi-tenancy is enabled, tenant interception; its version must match the
   MyBatis-Plus version. See the dependency example below.
9. Use optimistic locking only for rows that can be concurrently edited and
   where rejecting a stale write is part of the contract. Add
   `OptimisticLockerInnerInterceptor` to the same interceptor and mark an
   integer or long entity field with `@Version`. Initialize the version in the
   migration, typically `NOT NULL DEFAULT 0`. Use supported update methods
   (`updateById` or a supported entity update); a stale update affects zero
   rows and must be surfaced as a conflict or retry according to the domain.
   Do not assume custom SQL or every wrapper update participates in locking.
10. **Multi-tenancy is optional and disabled by default.** Add row-level tenant
   isolation only when the product requires multiple tenants to share the same
   schema and tables. Store a non-null `tenant_id` on every tenant-owned row.
   Get the tenant ID from the authenticated principal or a trusted tenant context;
   never use an unchecked request parameter or header as the tenant identity.
   Fail closed when no authorized tenant is present.
11. When optional multi-tenancy is enabled and using
    `TenantLineInnerInterceptor`, implement a `TenantLineHandler`
    and register it on the shared `MybatisPlusInterceptor`. Keep
    `ignoreTable` false by default and allowlist only genuinely global tables.
    Populate `tenant_id` on inserts with a `MetaObjectHandler`; the tenant
    interceptor does not fill the inserted column. Scope unique constraints
    and indexes by `tenant_id` when uniqueness is per tenant.
12. When row-level multi-tenancy is enabled, write custom SQL with aliases
    for every table and verify each join is tenant-filtered. Restrict
    `@InterceptorIgnore(tenantLine = "1")` to reviewed internal operations
    with explicit authorization. Test cross-tenant reads/writes and tenant ID
    assignment on inserts.

Maven example (use the equivalent dependency declaration for other build
tools):

```xml
<properties>
	<mybatis-plus.version>COPY_FROM_VERSIONS_JSON</mybatis-plus.version>
</properties>

<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>com.baomidou</groupId>
			<artifactId>mybatis-plus-spring-boot4-starter</artifactId>
			<version>${mybatis-plus.version}</version>
		</dependency>
	</dependencies>
</dependencyManagement>

<dependency>
	<groupId>com.baomidou</groupId>
	<artifactId>mybatis-plus-spring-boot4-starter</artifactId>
</dependency>
```

For Maven, merge this entry into the existing `<dependencyManagement>` block;
do not add a second block. Add the dependency only when using
`PaginationInnerInterceptor` or, when multi-tenancy is enabled,
`TenantLineInnerInterceptor`:

```xml
<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>com.baomidou</groupId>
			<artifactId>mybatis-plus-jsqlparser</artifactId>
			<version>${mybatis-plus.version}</version>
		</dependency>
	</dependencies>
</dependencyManagement>

<dependency>
	<groupId>com.baomidou</groupId>
	<artifactId>mybatis-plus-jsqlparser</artifactId>
</dependency>
```

Use MyBatis-Plus's `MetaObjectHandler` for application-owned audit timestamps.
Mark `createdAt` with `FieldFill.INSERT` and `updatedAt` with
`FieldFill.INSERT_UPDATE`; migrations use `created_at` and `updated_at` as
`timestamptz`. Do not accept audit timestamps from API requests. Follow the
working configuration and entity mapping linked below.

The template demonstrates pagination and optimistic locking together. Keep the
configuration and persistence model in sync with the actual template files:
[`MybatisPlusConfiguration.java`](../assets/template-maven/src/main/java/com/example/templatemaven/config/MybatisPlusConfiguration.java),
[`UserEntity.java`](../assets/template-maven/src/main/java/com/example/templatemaven/user/persistence/UserEntity.java),
and [Flyway migration](../assets/template-maven/src/main/resources/db/migration/V1__create_user_account.sql).
Because PostgreSQL reserves `USER`, the template quotes the required `"user"`
table name in both the entity mapping and migration.
The following configuration is the template implementation:

```java
package com.example.templatemaven.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import java.time.Instant;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(value = "com.example.templatemaven", annotationClass = org.apache.ibatis.annotations.Mapper.class)
class MybatisPlusConfiguration {

    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    @Bean
    MetaObjectHandler auditMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                var now = Instant.now();
                strictInsertFill(metaObject, "createdAt", Instant.class, now);
                strictInsertFill(metaObject, "updatedAt", Instant.class, now);
            }

            @Override
            public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
            }
        };
    }
}
```

`UserEntity.version` uses `@Version`; the Flyway migration initializes the
non-null integer column to `0`. The API returns the version and requires the
client's version on `PUT /api/users/{id}`. `UserService.update` uses
`updateById`; zero affected rows become `404` when the row is gone or `409`
when the row still exists with a newer version. The exact request, service,
and HTTP error mapping are in the [template API](../assets/template-maven/src/main/java/com/example/templatemaven/user/api/UpdateUserRequest.java),
[service](../assets/template-maven/src/main/java/com/example/templatemaven/user/application/UserService.java),
and [exception handler](../assets/template-maven/src/main/java/com/example/templatemaven/user/api/UserExceptionHandler.java).
The unit test covers conflict and missing-row outcomes; the integration test
checks the version increment and stale-write HTTP response:
[UserServiceTests](../assets/template-maven/src/test/java/com/example/templatemaven/user/application/UserServiceTests.java),
[UserIntegrationTests](../assets/template-maven/src/test/java/com/example/templatemaven/user/UserIntegrationTests.java).
The template's list endpoint demonstrates bounded zero-based pagination by
adapting MyBatis-Plus `Page<UserEntity>` to Spring Data `Page<UserResponse>`;
see its [controller](../assets/template-maven/src/main/java/com/example/templatemaven/user/api/UserController.java)
and [service](../assets/template-maven/src/main/java/com/example/templatemaven/user/application/UserService.java).
Keep pagination last when combining it with SQL-rewriting inner interceptors.
If tenant isolation is enabled, add the tenant interceptor before pagination
and optimistic locking.

The following is optional and applies only when shared-schema row-level
multi-tenancy is a product requirement. Register a tenant handler before the
other interceptors. Resolve its ID from the trusted security context, and fail
the operation when the context has no authorized tenant:

```java
@Configuration
class TenantMybatisConfiguration {

	@Bean
	MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineHandler tenantLineHandler) {
		var interceptor = new MybatisPlusInterceptor();
		var tenantInterceptor = new TenantLineInnerInterceptor();
		tenantInterceptor.setTenantLineHandler(tenantLineHandler);
		interceptor.addInnerInterceptor(tenantInterceptor);
		interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
		interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
		return interceptor;
	}
}
```

Omit this configuration, tenant handler, tenant column and tenant-fill logic
when the application does not use shared-schema row-level tenancy. Implement
`TenantLineHandler` and fill tenant-owned entities' `tenantId` using the same trusted tenant
context; never accept the insert value from a client. A tenant-owned entity
uses a fillable field:

```java
@TableField(fill = FieldFill.INSERT)
private Long tenantId;
```

The insert-fill handler obtains that value from the trusted context:

```java
strictInsertFill(metaObject, "tenantId", Long.class, tenantContext.requireTenantId());
```

```java
@TableName("purchase_order")
class PurchaseOrder {

	@TableId
	private Long id;

	private String status;

	@TableField(fill = FieldFill.INSERT)
	private Instant createdAt;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Instant updatedAt;
}
```

```java
interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

	default PurchaseOrder selectByStatus(String status) {
		return selectOne(new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getStatus, status));
	}
}
```

```java
@Service
class PurchaseOrderService {

	private final PurchaseOrderMapper purchaseOrderMapper;

	PurchaseOrderService(PurchaseOrderMapper purchaseOrderMapper) {
		this.purchaseOrderMapper = purchaseOrderMapper;
	}

	@Transactional(readOnly = true, rollbackFor = Exception.class)
	PurchaseOrderResponse findById(Long id) {
		purchaseOrderMapper.selectById(id);
		throw new UnsupportedOperationException();
	}
}
```

```java
@Component
class AuditMetaObjectHandler implements MetaObjectHandler {

	@Override
	public void insertFill(MetaObject metaObject) {
		var now = Instant.now();
		strictInsertFill(metaObject, "createdAt", Instant.class, now);
		strictInsertFill(metaObject, "updatedAt", Instant.class, now);
	}

	@Override
	public void updateFill(MetaObject metaObject) {
		strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
	}
}
```

Replace `com.example.app` with the project's package. Map `PurchaseOrder` to
`*Response` in the service; do not return the persistence model from a
controller. Schema for `purchase_order` is [flyway.md](flyway.md). The fill
property names are `createdAt` and `updatedAt`, not `created_at`.

## Verify

For a new persistence feature, check every rule. For an existing feature,
check rules relevant to the requested persistence change. Inspect its active
build configuration, `Application` class, Mapper interfaces, persistence
models, and `MetaObjectHandler`. When tenancy is in scope, inspect its handler
and tenant-sensitive services. Do not infer; report applicable failures.
