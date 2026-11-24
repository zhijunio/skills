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
2. Put `@MapperScan` on the `Application` class, scanning the application
   package.
3. Mapper interfaces extend `BaseMapper<T>` and declare no SQL. Use a fresh
   `LambdaQueryWrapper` / `LambdaUpdateWrapper` and entity method references.
   Do not cache a Wrapper. Do not take column names or SQL fragments from the
   request.
4. The persistence model matches the Flyway table: `@TableName` is singular
   `snake_case`. Field columns use descriptive `snake_case` names without a
   table-name prefix; map a reserved-word identifier explicitly and quote it
   consistently according to [postgresql.md](postgresql.md). `@TableId` is
   `Long` for the `bigint` primary key. The service injects the Mapper. The
   controller does not.
5. Every table has audit time fields named `xxxAt` in Java and `xxx_at` in
   SQL. Type is a timestamp (`Instant` / PostgreSQL `timestamptz`):
   `createdAt` and `updatedAt`. Fill them with a `MetaObjectHandler`. Do not
   take these values from a request. Use `@TableField(fill = FieldFill.INSERT)`
   on `createdAt` and `FieldFill.INSERT_UPDATE` on `updatedAt`. Do not put
   `@JsonFormat` on these fields. JSON timestamp conventions are in the skill's
   Core Rules.
6. Load collections in a batch (`in(...)` or `selectBatchIds`). Do not call
   `selectById` inside a loop.
7. Register one `MybatisPlusInterceptor` bean. For PostgreSQL pagination, add
   `PaginationInnerInterceptor(DbType.POSTGRE_SQL)` to it. Pass a bounded
   `Page<T>` to mapper page methods; validate page number and size at the API
   boundary, set a sensible maximum size, and do not fetch all rows to paginate
   in memory. For MyBatis-Plus 3.5.9 and later, add
   `mybatis-plus-jsqlparser` when using pagination or, if optional
   multi-tenancy is enabled, tenant interception; its version must match the
   MyBatis-Plus version. See the dependency example below.
8. Use optimistic locking only for rows that can be concurrently edited and
   where rejecting a stale write is part of the contract. Add
   `OptimisticLockerInnerInterceptor` to the same interceptor and mark an
   integer or long entity field with `@Version`. Initialize the version in the
   migration, typically `NOT NULL DEFAULT 0`. Use supported update methods
   (`updateById` or a supported entity update); a stale update affects zero
   rows and must be surfaced as a conflict or retry according to the domain.
   Do not assume custom SQL or every wrapper update participates in locking.
9. **Multi-tenancy is optional and disabled by default.** Add row-level tenant
   isolation only when the product requires multiple tenants to share the same
   schema and tables. Store a non-null `tenant_id` on every tenant-owned row.
   Get the tenant ID from the authenticated principal or a trusted tenant context;
   never use an unchecked request parameter or header as the tenant identity.
   Fail closed when no authorized tenant is present.
10. When optional multi-tenancy is enabled and using
    `TenantLineInnerInterceptor`, implement a `TenantLineHandler`
    and register it on the shared `MybatisPlusInterceptor`. Keep
    `ignoreTable` false by default and allowlist only genuinely global tables.
    Populate `tenant_id` on inserts with a `MetaObjectHandler`; the tenant
    interceptor does not fill the inserted column. Scope unique constraints
    and indexes by `tenant_id` when uniqueness is per tenant.
11. When row-level multi-tenancy is enabled, write custom SQL with aliases
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

```java
@SpringBootApplication
@MapperScan("com.example.app")
public class AppApplication {}
```

Register database interceptors once in configuration, not per Mapper:

```java
@Configuration
class MybatisPlusConfiguration {

	@Bean
	MybatisPlusInterceptor mybatisPlusInterceptor() {
		var interceptor = new MybatisPlusInterceptor();
		interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
		interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
		return interceptor;
	}
}
```

Add only the interceptors the application uses. Keep pagination last when
combining it with other SQL-rewriting inner interceptors. If tenant isolation
is enabled, add the tenant interceptor before pagination and optimistic
locking.

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

Every numbered Rule in this file must hold on disk. Inspect the active build
configuration, `Application` class, Mapper interfaces, persistence models, and
`MetaObjectHandler`. When optional multi-tenancy is enabled, also inspect its
tenant handler and tenant-sensitive services. Do not infer; report each Rule
that failed.
