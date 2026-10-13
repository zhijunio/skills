# Database Schema and SQL

Design the relational schema around durable business invariants and the queries the application must serve. Keep database-specific behavior explicit and verify it against the production engine. This guide covers shared schema and SQL decisions; use the persistence-specific references for JPA or MyBatis-Plus mappings, [Flyway](flyway.md) for migration workflow, and [database-concurrency.md](database-concurrency.md) for concurrent writes and locking.

## Design tables and columns

- Give tables and columns stable, descriptive names. Use singular `snake_case` table names (for example, `task`, `order_item`) and `snake_case` columns, with Java properties mapped from `camelCase`. Keep API resource paths independent of database table names; plural paths such as `/api/tasks` do not imply a plural table. Reserved words are allowed when required by the domain or an existing schema, but quote them using the target database's identifier syntax consistently in DDL, SQL, and ORM mappings (for example, PostgreSQL uses `"user"`, while MySQL commonly uses `` `user` ``). Avoid abbreviations without domain meaning and names that encode a temporary implementation. Do not repeat the table name in every column: for a `role` table, prefer `name` over `role_name`; qualify it as `r.name` in SQL when a join makes the column ambiguous.
- Choose a primary key that fits access patterns and growth. Keep identifiers immutable. A surrogate key does not replace unique constraints on business keys. Consider index locality and information exposure when choosing time-ordered IDs; identifiers are never authorization checks. Snowflake IDs are not guaranteed to be globally monotonically increasing across nodes or clock skew, so do not use ID order as business chronology; store an explicit timestamp or sequence when order matters. See [spring-data-jpa.md](spring-data-jpa.md) for UUIDv7 and entity identity guidance.
- Choose the narrowest database type that preserves the domain value and expected range. Use exact numeric types such as `DECIMAL` / `NUMERIC` for money and quantities requiring exact arithmetic; floating point is approximate. Specify precision and scale deliberately.
- Represent instants and local calendar values according to their meaning. Persist event timestamps consistently (commonly UTC) and keep the application/JDBC/database timezone assumptions explicit. A local business date or local wall-clock time is not interchangeable with an instant.
- Make nullability intentional. `NOT NULL` is a data invariant; do not use `NULL` to encode many unrelated states. Use a documented status or separate relation when the state has business meaning. Add defaults only when the database is the intended owner of that default.
- Avoid overloading one column with multiple meanings or serialized blobs that must be filtered, joined, constrained, or updated independently. Use JSON only when the data is genuinely document-shaped and the chosen database's indexing and validation behavior is understood.

## Put invariants in database constraints

The database is the final authority for invariants that must hold across every writer, including application instances, jobs, scripts, and future services:

- `PRIMARY KEY` for row identity.
- `NOT NULL` for required values.
- `UNIQUE` for business keys that must not duplicate.
- `FOREIGN KEY` for relationships whose referenced row must exist; choose `ON DELETE` behavior deliberately rather than relying on defaults.
- `CHECK` for row-local rules when the target database enforces the expression as expected.

Application validation gives useful errors, but it cannot replace a database constraint under concurrent writes. Translate constraint violations at the application boundary to a stable domain or HTTP error; do not expose SQL text or constraint names. See [database-concurrency.md](database-concurrency.md).

Unique constraints, `NULL` semantics, collations, case sensitivity, and deferred constraints differ across engines. State the intended behavior and verify it on the production database. If soft deletion changes uniqueness, design the unique key or a database-specific partial/filtered index together with the restore behavior; see [mybatis-plus.md](mybatis-plus.md) for logical-delete boundaries.

## Design indexes from query shapes

An index is a write, storage, and maintenance cost. Add it to serve a real query or constraint, then verify that the target database can use it:

- Start from representative `WHERE`, join, and `ORDER BY` predicates. For a composite B-tree index, column order matters; put columns in an order that matches the query shapes and selectivity, and verify the chosen plan rather than applying a universal recipe.
- Add indexes for important foreign-key lookup and join paths where the database does not create them automatically. In PostgreSQL, declaring a foreign key does not automatically index the referencing columns.
- Include a stable tie-breaker in paginated ordering. A matching composite index can support keyset pagination; see [spring-data-jpa.md](spring-data-jpa.md).
- Do not create a separate index for every column by default. Check whether an existing composite index already serves the leading predicates, and account for the extra work on inserts, updates, and deletes.
- Inspect `EXPLAIN` / `EXPLAIN ANALYZE` output on representative data. `EXPLAIN ANALYZE` executes the statement; use it carefully for writes and expensive production queries. Keep statistics representative and compare actual row counts and timing with expectations. See the PostgreSQL [Using EXPLAIN](https://www.postgresql.org/docs/current/using-explain.html) guide for database-specific behavior.

## Write SQL safely and deliberately

- Bind every value through prepared parameters or the persistence framework's parameter-binding API. Never concatenate request data into SQL. Parameters bind values, not identifiers or keywords; map public sort/filter names to a fixed allowlist of columns and directions. See OWASP's [SQL Injection Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html).
- Treat raw SQL fragments, dynamic table/column names, and vendor-specific expressions as reviewed code. Keep them static or choose them from a strict server-side allowlist.
- Select explicit columns rather than `SELECT *`, especially in application-facing queries. This keeps result mappings intentional and avoids pulling large or sensitive columns unnecessarily.
- Make ordering deterministic whenever the caller expects stable pages or repeatable results. `LIMIT` without a complete `ORDER BY` does not define which rows are returned.
- Make destructive statements visibly bounded. For an update or delete, review the predicate and expected affected-row count; use a transaction and an operationally safe batch size for large changes. A missing predicate must not silently become an all-row mutation.
- Prefer set-based SQL for bulk work when its lifecycle and concurrency effects are understood. Bulk JPQL/native SQL and MyBatis custom SQL may bypass entity callbacks, optimistic version checks, or logical-delete behavior; implement equivalent rules explicitly and test them. See [spring-data-jpa.md](spring-data-jpa.md) and [mybatis-plus.md](mybatis-plus.md).

For multiline JPQL, native SQL, or `JdbcClient` queries in Java, use text blocks so the query remains readable. Bind every dynamic value through named or positional parameters; never interpolate it into the text block:

```java
@Query("""
        select o
        from Order o
        where o.status = :status
        order by o.placedAt desc, o.id desc
        """)
List<Order> findRecentByStatus(OrderStatus status, Limit limit);
```

## Bound query cost and verify behavior

Require pagination or a deliberate bounded result size for collection endpoints and internal scans. Set maximum page sizes, use deterministic ordering, and prefer keyset pagination for deep sequential traversal. Bound `IN` lists, report ranges, and batch sizes so a valid request cannot produce unbounded database work. See [rate-limiting.md](../integration/rate-limiting.md) for request-level resource protection.

Test DDL, constraints, types, generated/custom SQL, collation behavior, and query results against the same database family as production. H2 compatibility does not prove PostgreSQL, MySQL, or another engine will behave the same. Use Testcontainers where practical; see [testing-slices-persistence.md](../testing/testing-slices-persistence.md) and [testcontainers.md](../testing/testcontainers.md). Keep fixtures representative enough to expose query-plan and index problems; tiny tables often make a sequential scan the correct plan.

## Evolve the schema safely

Keep production schema changes in versioned migration files. Do not use ORM startup DDL as the production change process. Prefer additive changes that work with both the currently deployed application and the next version; use expand-and-contract for renames, type changes, and removals. Backfill large tables in bounded batches when one migration transaction would hold locks or create unacceptable load. See [flyway.md](flyway.md) for migration naming, ordering, and rollout details.

Keep application behavior in the application layer. Do not introduce custom stored functions or procedures, triggers, database extensions, or other engine-specific programmable objects: they couple behavior to a database engine and complicate local setup, migration, rollback, and testing. Prefer application code and portable SQL. If a legacy schema already contains such an object, document its engine/version dependency, manage its DDL through versioned migrations, and verify upgrade, rollback, backup, and restore paths against that engine.

## If on Boot 3.5.x

The relational design and SQL principles in this guide are independent of the Boot line. Provider behavior, SQL generation, and supported Java mappings depend on the managed Spring Data / Hibernate / MyBatis and JDBC driver versions; verify generated SQL against the exact production database and dependency versions. Boot 4 changes some persistence starter and library versions; see [spring-boot-migration.md](../spring-boot-migration.md), [spring-data-jpa.md](spring-data-jpa.md), and [mybatis-plus.md](mybatis-plus.md).

## Gotchas

- Agent validates uniqueness with `SELECT` and inserts afterward - enforce uniqueness with a database constraint and translate a conflict.
- Agent treats `NULL` as a business state without defining its meaning - model the state explicitly and verify unique/null behavior on the target engine.
- Agent adds indexes without matching them to queries - inspect real query plans and account for write cost.
- Agent assumes a foreign key creates an index on the referencing columns - verify the target database behavior and add the needed index.
- Agent uses a floating-point type for exact money - use a suitable exact numeric type and explicit precision/scale.
- Agent concatenates request data or accepts arbitrary sort identifiers in SQL - bind values and allowlist identifiers.
- Agent expects `LIMIT` without a complete order to return a stable page - add deterministic ordering and a tie-breaker.
- Agent runs a bulk mutation without checking its predicate or affected-row count - bound the statement and verify the expected rows changed.
- Agent tests production SQL only on H2 - test against the production database family.
- Agent edits the schema through ORM startup DDL - use versioned migrations and rollout-compatible changes.
- Agent introduces a custom stored function, trigger, database extension, or other engine-specific programmable object - keep the behavior in the application layer and use portable SQL.

## Related

- [Flyway](flyway.md) · [Database Concurrency](database-concurrency.md) · [Spring Data JPA](spring-data-jpa.md) · [MyBatis-Plus](mybatis-plus.md) · [Persistence Slice Testing](../testing/testing-slices-persistence.md) · [Testcontainers](../testing/testcontainers.md) · [Rate Limiting and Resource Protection](../integration/rate-limiting.md)

## References

- [Using EXPLAIN](https://www.postgresql.org/docs/current/using-explain.html)
- [SQL Injection Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html)
