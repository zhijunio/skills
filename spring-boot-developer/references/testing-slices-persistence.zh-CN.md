# 持久化切片测试

持久化层应使用切片测试，而非完整上下文，并通过 Testcontainers 连接**真实数据库**，不要使用内存 H2。

## H2 陷阱

每种持久化切片（`@DataJpaTest`、`@JdbcTest`、`@DataJdbcTest`、`@JooqTest`）默认都会自动配置**内嵌 H2 DataSource**。简单派生查询在 H2 上能通过，于是测试套件看起来全绿，实际却悄悄偏离生产环境：

- PostgreSQL 原生查询使用 `to_tsvector`、`ON CONFLICT`、数组或 JSON 列时，在生产环境会报 `Function "TO_TSVECTOR" not found`，而 H2 测试此前已通过。
- 类型转换、`NULL` 排序、字符串比较和日期计算的行为都有差异。
- **Flyway 迁移不会运行**，因为迁移使用 PostgreSQL 方言（参见[Flyway](flyway.zh-CN.md)）。H2 Schema 是 Hibernate 生成的，不是实际迁移结果；两者可能无限期漂移而测试毫无失败。

最后一点是决定性原因。只有针对部署使用的数据库测试，持久化测试才有意义。

**四种切片都遵循同一规则：** 添加 `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`，避免 DataSource 被替换为 H2，并提供 Testcontainers 数据库。参见[Testcontainers](testcontainers.zh-CN.md)。

## `@DataJpaTest`

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class OrderRepositoryTest {

    @Autowired OrderRepository orderRepository;
    @Autowired TestEntityManager entityManager;

    @Test
    void findsRecentOrdersByStatus() {
        orderRepository.saveAll(List.of(placedOrder(), shippedOrder(), placedOrder()));
        entityManager.flush();
        entityManager.clear();          // 强制真实读取，而非命中一级缓存

        var found = orderRepository.findRecentByStatus(OrderStatus.PLACED, Limit.of(10));

        assertThat(found).hasSize(2);
    }
}
```

导入**共享**容器配置，让整个测试套件共用一个容器并复用上下文缓存（参见[测试策略](testing-strategy.zh-CN.md)）。

`entityManager.flush()` 后接 `clear()` 的重要性容易被忽略。若不这样做，`findAll` 可能直接从一级缓存返回，测试并未执行目标 SQL 却照样通过。

切片测试带有 `@Transactional`，每个测试结束后都会回滚。可使用 `@Sql("/test-data.sql")` 初始化数据。

### 应测与不应测内容

**应测：** 自定义 `@Query`（JPQL 和原生 SQL）、数据库特定功能、包含边界情况的复杂派生查询、自定义 Repository 实现、`@EntityGraph` 抓取行为、约束违规和乐观锁。

**不应测：** `findById`、`save`、基础 CRUD 和简单派生查询。这些是 Spring Data 的代码，它本身已有测试。如果 Repository 测试套件大多在断言 `save` 和 `findById`，那就是纯粹的额外成本。

```java
@Test
void enforcesUniqueReference() {
    orderRepository.saveAndFlush(orderWithReference("ORD-1"));

    assertThatThrownBy(() -> orderRepository.saveAndFlush(orderWithReference("ORD-1")))
            .isInstanceOf(DataIntegrityViolationException.class);
}
```

必须使用 `saveAndFlush`：普通 `save` 会把 INSERT 延迟到提交时，提交发生在测试方法返回后，断言无法捕获异常。

### 捕捉 N+1

查询计数是唯一可靠办法；功能测试通过并不能说明不存在 N+1。

```java
@Test
void loadsItemsInOneQuery() {
    var statistics = entityManager.getEntityManager()
            .unwrap(Session.class).getSessionFactory().getStatistics();
    statistics.clear();

    var order = orderRepository.findWithItemsById(orderId).orElseThrow();
    assertThat(order.items()).hasSize(3);

    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
}
```

测试 Profile 需要设置 `spring.jpa.properties.hibernate.generate_statistics=true`。对重要读取路径值得这样测试（参见[Spring Data JPA](spring-data-jpa.zh-CN.md)）。

## `@JdbcTest`——`JdbcClient` 和 `JdbcTemplate`

该切片会配置 DataSource 及这两个客户端，同样要遵循 `replace = NONE` 并使用 Testcontainers：

```java
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class OrderSummaryQueryTest {

    @Autowired JdbcClient jdbcClient;

    @Test
    void aggregatesByStatus() {
        jdbcClient.sql("insert into orders (id, status, total_amount) values (?, ?, ?)")
                .params(UUID.randomUUID(), "PLACED", new BigDecimal("10.00")).update();

        var count = jdbcClient.sql("select count(*) from orders where status = :status")
                .param("status", "PLACED")
                .query(Long.class).single();

        assertThat(count).isEqualTo(1);
    }
}
```

## `@DataJdbcTest` 与 `@JooqTest`

Spring Data JDBC Repository 和 jOOQ `DSLContext` 也遵循同一规则。jOOQ 在 H2 上测试，同样会彻底掩盖方言相关 SQL 问题。

## 免费覆盖迁移

真实数据库配合 `spring.jpa.hibernate.ddl-auto=validate` 时，持久化切片只要**能启动**，就已证明 Flyway 迁移可以执行，且结果 Schema 与实体映射匹配。仅靠一个配置就获得了相当多覆盖。

对于破坏性或转换数据的迁移，应明确测试数据：先写入旧结构数据，执行迁移，再断言新结构。

## 使用 Spring Boot 3.5.x 时

切片注解、`@AutoConfigureTestDatabase`、`@ServiceConnection`、`JdbcClient` 和 `TestEntityManager` 都相同。两个区别：

- **Testcontainers 坐标**：3.5.x 使用 1.x，4.x 使用 2.x，参见[Testcontainers](testcontainers.zh-CN.md)。
- **jOOQ**：两个 BOM 管理的主版本不同，应检查 `spring-boot-dependencies`，不要自行锁定版本。

## 常见问题

- 持久化切片使用默认 H2——数据库专属 SQL 会静默偏离，Flyway 迁移也不会执行。
- 忘记 `@AutoConfigureTestDatabase(replace = NONE)`——即使提供了容器，DataSource 仍会被替换为 H2。
- 每个测试类都启动容器——导入共享配置。
- 只断言 `findById` 和 `save`——那是 Spring Data 的代码。
- 忘记 `flush()` / `clear()`——读取命中一级缓存，目标 SQL 不会执行。
- 测约束违规时只调用 `save`——应使用 `saveAndFlush`；普通 `save` 会延迟 INSERT，断言无法捕获异常。
- 通过功能结果测试 N+1——应断言查询次数。
- 测试中将 `ddl-auto` 设为 `update`——Hibernate 会生成 Schema，迁移漂移不可见；应使用 `validate`。
- 持久化切片中对 Repository 使用 `@MockBean`——切片的目的就是真实数据库。

## 相关文档

- [Testcontainers](testcontainers.zh-CN.md) · [Spring Data JPA](spring-data-jpa.zh-CN.md) · [Flyway](flyway.zh-CN.md) · [测试策略](testing-strategy.zh-CN.md)

---

*持久化测试模式参考 **Philip Riecks** 的 Testing Spring Boot Applications Demystified；Testcontainers 模式参考 **Siva Prasad Reddy Katamreddy**。*
