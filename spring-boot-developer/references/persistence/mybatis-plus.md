# MyBatis-Plus

Use MyBatis-Plus when the feature benefits from explicit SQL, mapper interfaces, or incremental adoption over an existing MyBatis schema. It enhances MyBatis; it does not provide JPA-style aggregate persistence or remove the need to design and review SQL. Read [spring-data-jpa.md](spring-data-jpa.md) when the feature is better served by entity lifecycle and repository semantics.

## Choose one persistence model per feature

- Prefer **Spring Data JPA** when aggregate changes should be expressed through domain behavior and entity state, and repository operations fit the query needs.
- Prefer **MyBatis-Plus** when query shape and SQL need to stay explicit, the schema is legacy or database-specific, or the feature is read-heavy and uses custom projections.
- Keep one persistence owner for a table or aggregate. If JPA and MyBatis-Plus must both access the same tables, document which owns writes and how reads observe them; account for JPA flush/clear and first-level cache behavior. Verify the combination with the real database.

## Match the starter to Spring Boot

Use the artifact for the project's Boot line and keep all MyBatis-Plus modules on one release. The official guide currently documents `3.5.17`; Boot 4 support starts with `3.5.13`.

```xml
<!-- Spring Boot 4.x -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>

<!-- Spring Boot 3.5.x: use this artifact instead -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
```

Do not add `mybatis-spring-boot-starter`, `mybatis-spring`, or a separate MyBatis dependency beside the MyBatis-Plus starter unless the official compatibility guidance for the selected release explicitly requires it. Duplicate dependency lines can introduce mismatched MyBatis versions. Manage the MyBatis-Plus version centrally and check the dependency tree after upgrades.

Register mapper packages once, in a narrow feature-owned package:

```java
@SpringBootApplication
@MapperScan("com.example.orders.persistence.mapper")
class Application {}
```

## Keep SQL and domain behavior visible

Use `BaseMapper<T>` for routine single-table operations. Name custom mapper methods after the query intent and keep nontrivial joins, aggregates, locking clauses, and database-specific SQL in reviewed mapper SQL or XML. A generic CRUD method is not a substitute for a use-case boundary.

Keep mapper interfaces and persistence models inside the feature's persistence package. The service coordinates transactions and domain behavior; the mapper executes persistence operations. Map persistence models or query projections to response DTOs at the application boundary. Do not expose mapper models, `Page`, or Wrapper objects as API contracts.

Use explicit table/column mappings where naming conventions do not match the schema. Let Flyway or Liquibase own production schema changes; do not use application startup DDL or code generation as the production migration path. See [flyway.md](flyway.md).

## Build conditions safely

Prefer `LambdaQueryWrapper` / `LambdaUpdateWrapper` for compile-time property references and Wrapper methods such as `eq`, `in`, and `between` for bound values. A wrapper helps bind values; it does not make raw SQL fragments safe.

- Never accept SQL fragments, column names, or `ORDER BY` expressions directly from a request. Map public sort keys through a fixed allowlist to known columns.
- Treat `last`, raw `apply` fragments, `inSql`, and string column overloads as SQL construction boundaries. Keep their contents static or validate them against a strict allowlist; never concatenate request values.
- Create a fresh Wrapper per operation. Wrapper instances are mutable and not thread-safe, and must not be cached or shared across requests.
- Do not accept a serialized Wrapper from a remote caller. Accept business-level filters and build the query on the server.

These are security boundaries, not just style preferences. See the official [Wrapper guide](https://baomidou.com/en/guides/wrapper/) and [SQL injection guidance](https://baomidou.com/en/guides/security/).

## Auto-fill audit fields with `MetaObjectHandler`

Use MyBatis-Plus auto-fill for persistence-level fields such as creation and modification timestamps or trusted actor IDs. Mark entity properties with `@TableField(fill = FieldFill.INSERT)` or `FieldFill.INSERT_UPDATE`, and register a Spring-managed `MetaObjectHandler`:

```java
@Component
class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdAt", Instant.class, Instant.now());
        strictInsertFill(metaObject, "updatedAt", Instant.class, Instant.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
    }
}
```

Use the exact Java property name and type in the fill call. Resolve actor IDs from trusted server-side authentication context, never from client-controlled DTO fields. Strict fill normally leaves a non-null entity value unchanged; avoid letting clients construct persistence entities with audit values. Auto-fill runs only on supported MyBatis-Plus operations: custom SQL, direct JDBC, and some update forms may need explicit assignment. Test insert and update paths, including wrapper-only updates, with the selected release and database.

## Logical deletion

Use `@TableLogic` on a field or configure the global logical-delete property when records must remain stored but hidden from normal generated operations. MyBatis-Plus changes supported delete operations into updates and adds the not-deleted predicate to supported selects and updates. It is not physical deletion, retention policy, or an authorization boundary.

Review custom XML/annotation SQL, joins, reports, and direct JDBC statements: they may need an explicit logical-delete predicate, and administrative queries may intentionally include deleted rows. Decide how unique constraints behave when a row is deleted and whether restore is supported; database-specific partial indexes or a delete marker in the unique key may be needed. Test generated and custom SQL, including duplicate-create and restore behavior. See the official [logical delete guide](https://baomidou.com/en/guides/logic-delete/).

## Data permission is separate from tenant isolation

Use `DataPermissionInterceptor` with a `MultiDataPermissionHandler` (or the handler API provided by the selected MyBatis-Plus release) when row visibility depends on trusted user, role, or organizational scope. This is distinct from a tenant-line plugin: tenant isolation partitions records by tenant, while data permission narrows access within the tenant. Build predicates from server-derived authorization data and bind values safely; do not concatenate request input into SQL or accept a client-supplied permission fragment.

Treat the interceptor as defense in depth. Audit custom SQL and joins, mapped statements excluded through interceptor-ignore annotations/configuration, batch and background work, and every query shape supported by the selected parser. A missing or failed permission rule must not silently become unrestricted access; define explicit deny/allow behavior and fail closed. Verify the emitted SQL and cross-user, cross-role, and cross-organization cases against the production database. See the official [data permission plugin guide](https://baomidou.com/en/plugins/data-permission/) and [multi-tenancy.md](multi-tenancy.md).

## Custom JDBC mappings with `BaseTypeHandler`

Use a custom MyBatis `BaseTypeHandler<T>` only when a domain value needs a JDBC representation that built-in handlers do not provide, such as a database-specific value object or encoding. Implement `setNonNullParameter` and all three `getNullableResult` overloads (by column name, column index, and callable-statement index). Map the handler with `@MappedTypes` / `@MappedJdbcTypes`, configuration, or an explicit result mapping / `@TableField(typeHandler = ...)` as appropriate; for result mapping, confirm the mapper enables the required result-map behavior for the field.

Keep conversion deterministic and handle SQL `NULL` and JDBC type differences deliberately. Prefer built-in enum or JSON support when it matches the schema. Test both parameter binding and result reads against the production database driver, including nulls and legacy/malformed stored values; do not let a handler hide invalid persisted data.

```java
@MappedTypes(Money.class)
@MappedJdbcTypes(JdbcType.DECIMAL)
final class MoneyTypeHandler extends BaseTypeHandler<Money> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Money value, JdbcType jdbcType)
            throws SQLException {
        ps.setBigDecimal(i, value.amount());
    }

    @Override
    public Money getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return moneyOrNull(rs.getBigDecimal(columnName));
    }

    @Override
    public Money getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return moneyOrNull(rs.getBigDecimal(columnIndex));
    }

    @Override
    public Money getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return moneyOrNull(cs.getBigDecimal(columnIndex));
    }

    private static Money moneyOrNull(BigDecimal amount) {
        return amount == null ? null : new Money(amount);
    }
}
```

This example assumes `Money` validates its scale and currency elsewhere; a real mapping must also preserve every value required by the domain type. See the MyBatis [type-handler configuration guide](https://mybatis.org/mybatis-3/configuration.html) and [`BaseTypeHandler` API](https://mybatis.org/mybatis-3/apidocs/org/apache/ibatis/type/BaseTypeHandler.html).

## Bound pagination and order interceptors

The `PaginationInnerInterceptor` applies database pagination, but request limits and sort choices remain application responsibilities. Clamp page size to an explicit maximum, validate page values, allowlist sort fields and directions, and add a deterministic tie-breaker to prevent records shifting between pages. Configure `maxLimit` as a second guard.

Since MyBatis-Plus 3.5.9, the pagination parser is a separate dependency. Add `mybatis-plus-jsqlparser` at the same version when pagination is used; choose the JDK-compatible parser artifact described by the official installation guide when targeting an older Java baseline.

Register plugins through one `MybatisPlusInterceptor` bean. Follow the documented order: tenant and dynamic-table transformations first; pagination and optimistic locking next; SQL checks and full-table update/delete guards last. Add only plugins the application needs. See the official [pagination](https://baomidou.com/en/plugins/pagination/) and [plugin ordering](https://baomidou.com/en/plugins/) guides.

## Transactions, concurrency, and tenant isolation

Put `@Transactional` at the application service/use-case boundary. Mapper calls using the same Spring-managed `DataSource` participate in that transaction. Do not catch and suppress a failed mapper operation inside a transaction and then continue as though the write succeeded; follow [transactions.md](transactions.md) and [database-concurrency.md](database-concurrency.md).

For optimistic locking, configure `OptimisticLockerInnerInterceptor`, map a version column with `@Version`, and check the affected-row outcome as part of the use case. A zero-row update is a conflict to resolve or report, not a successful write. Custom SQL must include and test the equivalent version predicate itself; do not assume every statement is rewritten by the plugin. See the official [optimistic-lock plugin guide](https://baomidou.com/en/plugins/optimistic-locker/).

For multi-tenant data, derive tenant identity from authenticated server-side context. A tenant-line plugin is defense in depth, not authorization: audit custom SQL, excluded tables, joins, batch jobs, and background tasks, and prove cross-tenant isolation with integration tests. A scoped `ThreadLocal` tenant context and a post-authentication filter are optional context-carrier mechanisms, not requirements; use them only when the tenant handler cannot resolve a trusted principal directly. See [multi-tenancy.md](multi-tenancy.md) for context lifecycle, async boundaries, plugin bypasses, and test cases, and the official [tenant plugin guide](https://baomidou.com/plugins/tenant/).

## Test against the production database engine

Test custom SQL, result mappings, plugin behavior, optimistic locking, tenant isolation, and pagination with Testcontainers using the same database family as production. MyBatis-Plus documents `@MybatisPlusTest` in `mybatis-plus-boot-starter-test`; keep its version aligned with MyBatis-Plus and verify compatibility with the project's Boot line. Make sure the slice does not replace the Testcontainers `DataSource` with embedded H2. Assert SQL-visible behavior and affected-row outcomes; do not spend tests re-proving `BaseMapper` CRUD. See the official [testing guide](https://baomidou.com/en/getting-started/test/), [testing-slices-persistence.md](../testing/testing-slices-persistence.md), and [testcontainers.md](../testing/testcontainers.md).

## If on Boot 3.5.x

Use `mybatis-plus-spring-boot3-starter` and its matching test starter. Boot 4 uses the distinct `mybatis-plus-spring-boot4-starter`; do not substitute one artifact for the other. MyBatis-Plus's Boot 4 support begins at 3.5.13. Resolve the current compatible release and its dependency constraints from the official [installation guide](https://baomidou.com/en/getting-started/install/) before upgrading.

## Gotchas

- Adding the Boot 2 artifact `mybatis-plus-boot-starter` to Boot 3.5 or 4 instead of the Boot-specific starter.
- Adding MyBatis or MyBatis-Spring dependencies beside the MyBatis-Plus starter and creating version conflicts.
- Treating `BaseMapper` convenience CRUD as a domain repository or returning persistence models from controllers.
- Sending a Wrapper, raw sort expression, or SQL fragment from a client.
- Assuming an optimistic-lock or tenant plugin protects custom SQL without testing its exact behavior.
- Using an unbounded page size or allowing client-controlled columns in `ORDER BY`.
- Testing production SQL on H2 or allowing a persistence test slice to replace the container database.
- Letting JPA and MyBatis-Plus write the same aggregate without an explicit owner and cache/flush policy.

## Related

- [Spring Data JPA](spring-data-jpa.md) · [Transactions](transactions.md) · [Database Concurrency](database-concurrency.md) · [Multi-Tenancy](multi-tenancy.md) · [Persistence Slice Testing](../testing/testing-slices-persistence.md) · [Flyway](flyway.md)

## References

- [Wrapper guide](https://baomidou.com/en/guides/wrapper/)
- [SQL injection guidance](https://baomidou.com/en/guides/security/)
- [logical delete guide](https://baomidou.com/en/guides/logic-delete/)
- [data permission plugin guide](https://baomidou.com/en/plugins/data-permission/)
- [type-handler configuration guide](https://mybatis.org/mybatis-3/configuration.html)
- [`BaseTypeHandler` API](https://mybatis.org/mybatis-3/apidocs/org/apache/ibatis/type/BaseTypeHandler.html)
- [pagination](https://baomidou.com/en/plugins/pagination/)
- [plugin ordering](https://baomidou.com/en/plugins/)
- [optimistic-lock plugin guide](https://baomidou.com/en/plugins/optimistic-locker/)
- [tenant plugin guide](https://baomidou.com/plugins/tenant/)
- [testing guide](https://baomidou.com/en/getting-started/test/)
- [installation guide](https://baomidou.com/en/getting-started/install/)
