# Flyway

数据库结构应通过迁移脚本进行版本控制、只向前演进，并由迁移脚本负责管理；不要交给 `ddl-auto`。设置 `spring.jpa.hibernate.ddl-auto=validate`，让 Hibernate 检查 Schema 与实体是否一致，不一致时拒绝启动。

## 依赖

在 **Boot 4** 中，JDBC 或 JPA Starter 不再传递引入 Flyway。缺少 Starter 时迁移会静默停止，应用可能使用任意已有 Schema 启动：

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

第二个依赖也不可少。Flyway 10 将数据库支持拆分成各数据库模块；classpath 中只有 `flyway-core` 时，启动会报 `Unsupported Database`。

## 命名

```text
src/main/resources/db/migration/
    V1__create_customers.sql
    V2__create_orders.sql
    V2_1__add_orders_status_index.sql
    V3__add_orders_cancelled_at.sql
    R__order_summary_view.sql
```

| 前缀 | 含义 |
|---|---|
| `V` | 版本迁移——按版本顺序执行一次 |
| `R` | 可重复迁移——校验和变化时执行，排在所有版本迁移之后 |
| `U` | 回滚迁移——仅 Flyway Teams 提供，应视为不可用 |

版本和描述之间必须使用**两个下划线**，描述内部使用单下划线。`V1_create_customers.sql` 不是有效迁移，会被静默忽略。

需要时，可重复迁移可用于可替换的视图，并应使用幂等语句（`CREATE OR REPLACE`）。业务行为应放在应用层；不要引入自定义存储函数 / 存储过程、触发器、数据库扩展或其他数据库引擎专属的可编程对象，因为它们会将应用与单一数据库耦合，并增加本地开发、迁移、回滚和测试的复杂度。如果遗留 Schema 中已经存在此类对象，应记录依赖，通过版本化迁移管理其 DDL，并在目标数据库引擎上验证其生命周期。

## 最重要的规则

**绝不要修改任何环境中已执行过的迁移。** Flyway 会记录每个迁移的校验和；修改文件会让所有已执行环境的 `validate` 失败，并使本地数据库历史与生产不一致。

生产中发现问题？编写 `V4__fix_the_thing.sql`。迁移历史只追加，不改写。

唯一例外是迁移仅存在于你自己的工作副本中且尚未提交。

## Expand / Contract

旧版应用仍在提供流量时就会运行数据库迁移。任何破坏运行中代码的更改都会造成中断。

```sql
-- ❌ 提交后立即破坏正在运行的应用
ALTER TABLE orders RENAME COLUMN user_id TO customer_id;
```

应将更改拆分到多个发布中：

```sql
-- V5：扩展阶段——添加新列，允许为空
ALTER TABLE orders ADD COLUMN customer_id UUID;
UPDATE orders SET customer_id = user_id WHERE customer_id IS NULL;
ALTER TABLE orders ALTER COLUMN customer_id SET NOT NULL;
```

先部署同时写入**两个**列、读取新列的代码。等后续发布确认没有代码再引用旧列后，再执行：

```sql
-- V6：收缩阶段
ALTER TABLE orders DROP COLUMN user_id;
```

缩窄类型、拆分字段或把字段改为必填也遵循相同模式。至少三步、两个部署，并确保每个中间状态都能被当前运行的代码接受。

## 安全与不安全的操作

```sql
-- ✅ 安全
ALTER TABLE orders ADD COLUMN notes TEXT;                       -- 可为空
ALTER TABLE orders ADD COLUMN priority INT NOT NULL DEFAULT 0;  -- NOT NULL 且有默认值
CREATE TABLE order_notes (…);
```

```sql
-- ❌ 有数据时会失败
ALTER TABLE orders ADD COLUMN priority INT NOT NULL;

-- ❌ 大多数数据库会重写整张表并持有排他锁
ALTER TABLE orders ALTER COLUMN description TYPE VARCHAR(100);
```

PostgreSQL 11+ 为列添加带常量默认值的 `NOT NULL` 列时，不再重写表；但**易变**默认值仍会重写。

### `CREATE INDEX CONCURRENTLY`

`CONCURRENTLY` 可以避免锁住写入，但不能在事务中执行。Flyway 默认会把每个迁移放入事务，因此会失败。通过同名配置文件关闭该脚本的事务：

```text
V7__add_orders_email_index.sql
V7__add_orders_email_index.sql.conf     → executeInTransaction=false
```

```sql
CREATE INDEX CONCURRENTLY idx_orders_email ON orders (customer_email);
```

`CONCURRENTLY` 语句应单独放入一个迁移。无事务意味着没有回滚；失败会留下无效的部分构建索引，重试前必须手动删除。

## 索引

每个外键，以及大型表中用于 `WHERE` 或 `ORDER BY` 的列都应建立索引。PostgreSQL 会自动索引主键，**不会自动索引外键**；未索引的外键会导致删除父记录时扫描整个子表。

索引应匹配查询，包括排序方向和 Keyset 分页需要的列：

```sql
CREATE INDEX idx_orders_status_placed ON orders (status, placed_at DESC, id DESC);
```

参见[Spring Data JPA](spring-data-jpa.zh-CN.md)。

## 配置

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
    out-of-order: false
    baseline-on-migrate: false
    clean-disabled: true
  jpa:
    hibernate:
      ddl-auto: validate
```

| 设置 | 原因 |
|---|---|
| `clean-disabled: true` | `flyway:clean` 会**删除 Schema 中所有对象**。Flyway 当前默认禁用它，仍应显式保持，避免有人重新启用 |
| `out-of-order: false` | 拒绝低于当前版本号的迁移；启用它会掩盖合并错误 |
| `baseline-on-migrate` | 仅在已有数据库引入 Flyway 时使用；完成基线后应关闭，否则会掩盖空数据库错误 |
| `validate-on-migrate: true` | 在启动时验证校验和 |

## 种子数据

每个环境都需要的参考数据（国家、币种、角色）属于 Schema，应放在迁移中。

开发和演示数据不属于 Schema。应使用限定 Profile 的 Runner：

```java
@Component
@Profile("local")
class DevDataSeeder implements ApplicationRunner {

    private final CustomerRepository customers;

    DevDataSeeder(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (customers.count() == 0) {
            customers.save(Customer.create(EmailAddress.of("dev@example.com")));
        }
    }
}
```

迁移中的种子数据会进入生产，之后只能再通过新迁移修改。

## 团队协作

两个开发者同时编写 `V8__` 是常见问题，会在后合并的分支中触发校验和或顺序错误。

- 使用时间戳编号，例如 `V20260810_1430__add_orders_index.sql`，几乎可以消除冲突。
- 在 CI 中将数据库恢复到上一发布状态后运行 `flyway validate`。这能在迁移进入环境前发现已修改的迁移。
- 像审查生产代码一样审查迁移，因为迁移本身就是生产变更。

## 批处理元数据

使用 Spring Batch 时，`BATCH_*` 表也属于 Schema。设置 `spring.batch.jdbc.initialize-schema=never`，并将 DDL 作为迁移发布；规范脚本位于 `spring-batch-core`。Batch 6 将 `BATCH_JOB_SEQ` 改名为 `BATCH_JOB_INSTANCE_SEQ`，现有 Boot 3 数据库需要迁移该名称。参见[Spring Batch](spring-batch.zh-CN.md)。

Spring Modulith 的事件发布表也应如此管理，参见[Spring Modulith](spring-modulith.zh-CN.md)。

## 测试迁移

测试应在真实数据库中运行迁移，因此与其他测试共用 Testcontainers 配置即可。能启动的持久化切片测试已经证明迁移可执行，且 Hibernate `validate` 通过。这也是不使用 H2 的有力理由：PostgreSQL 方言的迁移无法在 H2 上运行。参见[持久化切片测试](testing-slices-persistence.zh-CN.md)。

破坏性迁移要测试**数据**，不只是 Schema：先插入旧结构数据，执行迁移，再断言新结构。

## 使用 Spring Boot 3.5.x 时

Flyway 会由 JDBC 和 JPA Starter 传递引入，因此不需要 `spring-boot-starter-flyway`；但 Flyway 10+ 仍需 `flyway-database-postgresql`。其他行为相同。

## 常见问题

- Boot 4 只添加 `spring-boot-starter-data-jpa`，却期望自动运行迁移——还应显式加入 `spring-boot-starter-flyway`。
- 只添加 `flyway-core`——数据库支持位于 `flyway-database-<db>`，否则启动报 `Unsupported Database`。
- 修改已执行过的迁移——校验和会在所有执行过的环境失败，应新增迁移。
- 版本号后只用单下划线——`V1_create_users.sql` 会被静默忽略。
- 一次迁移中重命名列——会破坏运行中的应用，应使用 Expand / Contract。
- 对已有数据的表添加无默认值的 `NOT NULL` 列——迁移会失败。
- 普通迁移中执行 `CREATE INDEX CONCURRENTLY`——它不能在事务中运行；应为其单独创建脚本并使用 `.sql.conf` 设置 `executeInTransaction=false`。
- 在迁移中加入演示数据——使用 `@Profile` Runner；迁移会进入生产。
- Flyway 旁边仍使用 `ddl-auto: update`——Schema 有两个管理者，迟早不一致。
- 为解决合并冲突启用 `out-of-order`——这会掩盖迁移顺序错误，应重新编号。
- 引入 Flyway 后仍保持 `baseline-on-migrate: true`——空数据库会被静默设为基线。
- 外键没有索引——删除父行时会扫描子表。
- 让 Spring Batch 或 Modulith 在生产环境自动创建表——应将表结构作为迁移发布。

## 相关文档

- [Spring Data JPA](spring-data-jpa.zh-CN.md) · [持久化切片测试](testing-slices-persistence.zh-CN.md) · [Spring Batch](spring-batch.zh-CN.md) · [配置](configuration.zh-CN.md)
