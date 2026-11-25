# Flyway

Read this when the task adds schema migrations or Flyway.
The generator does not add Flyway. Add the Boot-managed starter using the
project's build tool and dependency conventions. For Maven, see
[maven.md](maven.md). Mapper work lives in
[mybatis-plus.md](mybatis-plus.md). Context-load tests go in
[testing.md](testing.md).

## When to use

- Create or change SQL schema
- Add `spring-boot-starter-flyway` or `flyway-database-postgresql`

## When not to use

- Hibernate `ddl-auto` as the migration strategy

## Rules

1. Declare `spring-boot-starter-flyway` as a project dependency. Boot 4 does
   not pull Flyway transitively. Without the starter, migrations never run.
2. With PostgreSQL, also declare `flyway-database-postgresql`; manage versions
   according to the selected build tool and Boot compatibility.
3. Put migrations in `src/main/resources/db/migration`. Follow
   [postgresql.md](postgresql.md) for table, column, constraint, index, and
   default conventions. Use `bigint` primary keys. Time columns are
   `timestamptz`, named `xxx_at` (`created_at`, `updated_at`). Java names are
   [mybatis-plus.md](mybatis-plus.md).

Maven dependency example (use the equivalent notation for the selected
build tool):

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
	<groupId>org.flywaydb</groupId>
	<artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

## Verify

The template's migration is
[`V1__create_user_account.sql`](../assets/template-maven/src/main/resources/db/migration/V1__create_user_account.sql).

For a new migration setup, check every rule. For an existing project, check
rules relevant to the schema change. Open the active build configuration and
`src/main/resources/db/migration`; do not infer. Report applicable failures.
