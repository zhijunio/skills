# MyBatis-Plus

Use this guidance when adding MyBatis-Plus persistence, mappers, or configuration.
Add the dependency using the project's existing build and version-management convention. Use
[spring-boot-maven](../../spring-boot-maven/SKILL.md) for Maven build changes,
[Flyway migrations](../../flyway-migrations/SKILL.md) for schema SQL,
[Spring Boot Web MVC](../../spring-boot-webmvc/SKILL.md) for controller and transaction boundaries,
and [Spring Boot testing](../../spring-boot-testing/SKILL.md) for test strategy. Map persistence
models to API response types; do not expose mapper entities directly over HTTP.

## Dependency and configuration

- Preserve an existing compatible MyBatis-Plus version. For a new Boot 4 project, treat
  `mybatisPlus.default` in [mybatis-versions.json](mybatis-versions.json) as a candidate only;
  confirm the starter and resolved dependencies against the official release documentation
  before adding it to the project's version catalog or property mechanism. Declare
  `mybatis-plus-spring-boot4-starter` using the selected build tool. Do not
  add `mybatis-spring-boot-starter` or a second MyBatis artifact beside it.

- Put `@MapperScan` on the application's MyBatis-Plus configuration class,
  scanning the application package with `annotationClass = Mapper.class`.
  Annotate each Mapper interface with `@Mapper` so ordinary application
  interfaces are not registered as persistence adapters. Keep this wiring in
  the root `config` package.

- Put shared MyBatis-Plus beans such as `MybatisPlusInterceptor` and
  `MetaObjectHandler` in the application's root `config` package, following
  the application's established configuration boundaries. Put these under a feature's
  `persistence` package only when they are truly feature-specific and do not
  become accidental global configuration. These are ordinary `@Configuration`
  classes, not Boot starter `@AutoConfiguration` classes.

## Mappers and entity mapping

- Mapper interfaces extend `BaseMapper<T>` and declare no SQL. Use a fresh
  `LambdaQueryWrapper` / `LambdaUpdateWrapper` and entity method references.
  Do not cache a Wrapper. Do not take column names or SQL fragments from the
  request.

- The persistence model matches the Flyway table: `@TableName` is singular
  `snake_case`. Field columns use descriptive `snake_case` names without a
  table-name prefix; map a reserved-word identifier explicitly and quote it
  consistently according to the selected database's identifier rules.
  `@TableId` is `Long` for a `bigint` primary key. The service injects the Mapper. The
  controller does not.

- Every table has audit time fields named `xxxAt` in Java and `xxx_at` in
  SQL. Type is a timestamp (`Instant` / PostgreSQL `timestamptz`):
  `createdAt` and `updatedAt`. Fill them with a `MetaObjectHandler`. Do not
  take these values from a request. Use `@TableField(fill = FieldFill.INSERT)`
  on `createdAt` and `FieldFill.INSERT_UPDATE` on `updatedAt`. Do not put
  `@JsonFormat` on these fields as a persistence workaround; preserve the API's timestamp
  serialization contract.

- Load collections in a batch (`in(...)` or `selectBatchIds`). Do not call
  `selectById` inside a loop.

## Pagination and concurrency

- Register one `MybatisPlusInterceptor` bean. For PostgreSQL pagination, add
  `PaginationInnerInterceptor(DbType.POSTGRE_SQL)` to it. Pass a bounded
  `Page<T>` to mapper page methods; validate page number and size at the API
  boundary, set a sensible maximum size, and do not fetch all rows to paginate
  in memory. For MyBatis-Plus 3.5.9 and later, add
  `mybatis-plus-jsqlparser` when using pagination or, if optional
  multi-tenancy is enabled, tenant interception; its version must match the
  MyBatis-Plus version. See the dependency example below.

- Use optimistic locking only for rows that can be concurrently edited and
  where rejecting a stale write is part of the contract. Add
  `OptimisticLockerInnerInterceptor` to the same interceptor and mark an
  integer or long entity field with `@Version`. Initialize the version in the
  migration, typically `NOT NULL DEFAULT 0`. Use supported update methods
  (`updateById` or a supported entity update); a stale update affects zero
  rows and must be surfaced as a conflict or retry according to the domain.
  Do not assume custom SQL or every wrapper update participates in locking.

## Optional multi-tenancy

- **Multi-tenancy is optional and disabled by default.** Add row-level tenant
  isolation only when the product requires multiple tenants to share the same
  schema and tables. Store a non-null `tenant_id` on every tenant-owned row.
  Get the tenant ID from the authenticated principal or a trusted tenant context;
  never use an unchecked request parameter or header as the tenant identity.
  Fail closed when no authorized tenant is present.

- When optional multi-tenancy is enabled and using
  `TenantLineInnerInterceptor`, implement a `TenantLineHandler`
  and register it on the shared `MybatisPlusInterceptor`. Keep
  `ignoreTable` false by default and allowlist only genuinely global tables.
  The tenant interceptor can inject a missing tenant column into supported INSERT SQL;
  its default `ignoreInsert` skips injection when the column is already present.
  Never rely on it to overwrite or validate an explicit tenant value. Use a trusted
  application-owned fill policy and reject or replace mismatched explicit values before
  persistence, including custom and batch inserts. See the [official tenant plugin](https://baomidou.com/en/plugins/tenant/). Scope unique constraints
  and indexes by `tenant_id` when uniqueness is per tenant.

- When row-level multi-tenancy is enabled, write custom SQL with aliases
  for every table and verify each join is tenant-filtered. Restrict
  `@InterceptorIgnore(tenantLine = "1")` to reviewed internal operations
  with explicit authorization. Test cross-tenant reads/writes and tenant ID
  assignment on inserts.
## Dependency examples

Use the equivalent dependency declaration for the selected build tool. The Maven example is:

```xml
<properties>
	<mybatis-plus.version>COPY_FROM_MYBATIS_VERSIONS_JSON</mybatis-plus.version>
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

## Audit and interceptors

Use MyBatis-Plus's `MetaObjectHandler` for application-owned audit timestamps.
Mark `createdAt` with `FieldFill.INSERT` and `updatedAt` with
`FieldFill.INSERT_UPDATE`; migrations use `created_at` and `updated_at` as
`timestamptz`. Do not accept audit timestamps from API requests.

Register one `MybatisPlusInterceptor` bean for pagination and any enabled optimistic-locking or
multi-tenant interceptors. The following configuration shows pagination, optimistic locking, and
server-owned audit timestamps together:

```java
package com.example.app.config;

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
@MapperScan(value = "com.example.app", annotationClass = org.apache.ibatis.annotations.Mapper.class)
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

Use `@Version` only when stale writes must be rejected. Initialize the version column in the
migration, use a supported update method such as `updateById`, and surface zero-row updates as a
conflict or missing-resource result according to the domain. Keep pagination bounded at the API
boundary. When combining interceptors, add tenant filtering before pagination and optimistic locking;
keep pagination last among SQL-rewriting interceptors.

## Tenant interception example

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
Long tenantId = tenantContext.requireTenantId();
Object supplied = getFieldValByName("tenantId", metaObject);
if (supplied != null && !tenantId.equals(supplied)) {
    throw new IllegalArgumentException("Tenant mismatch");
}
setFieldValByName("tenantId", tenantId, metaObject);
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





Replace `com.example.app` with the project's package. Map `PurchaseOrder` to
`*Response` in the service; do not return the persistence model from a
controller. Manage the `purchase_order` schema with the Flyway migration skill. The fill property
names are `createdAt` and `updatedAt`, not `created_at`.

## Verify

For a new persistence feature, check every rule. For an existing feature,
check rules relevant to the requested persistence change. Inspect its active
build configuration, `Application` class, Mapper interfaces, persistence
models, and `MetaObjectHandler`. When tenancy is in scope, inspect its handler
and tenant-sensitive services. Test explicitly supplied mismatched tenant IDs, absent tenant
context, custom SQL and batch inserts against the actual database. Do not infer; report applicable failures.
