# Spring Data JPA

Spring Boot 4 管理 **Jakarta Persistence 3.2** 和 **Hibernate ORM 7**。使用 `jakarta.persistence.*`。不要声明 Hibernate 或 JPA 版本；由 BOM 管理。

## 实体是 class，不是 record

record 不能作为实体：JPA 需要无参构造器、可变字段和可代理的非 final 类。record 适用于 DTO、命令、事件、值对象和 embeddable，参见 [Spring 中的 Java](java-in-spring.zh-CN.md)。

```java
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_customer", columnList = "customer_id"),
        @Index(name = "idx_orders_status_placed", columnList = "status, placed_at")
})
class Order extends BaseEntity {

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "id", nullable = false, updatable = false))
    private OrderId id;

    @AttributeOverride(name = "value", column = @Column(name = "customer_id", nullable = false, updatable = false))
    private CustomerId customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "total_currency", nullable = false, length = 3))
    })
    private Money total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "placed_at")
    private Instant placedAt;

    protected Order() {
        // 仅供 JPA 使用
    }

    private Order(OrderId id, CustomerId customerId) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.status = OrderStatus.DRAFT;
        this.total = Money.zero("GBP");
    }

    static Order create(CustomerId customerId) {
        return new Order(OrderId.newId(), customerId);
    }

    void addItem(ProductId productId, int quantity, Money unitPrice) {
        if (status != OrderStatus.DRAFT) {
            throw new OrderNotModifiableException(id, status);
        }
        items.add(OrderItem.create(this, productId, quantity, unitPrice));
        recalculateTotal();
    }

    void place() {
        if (items.isEmpty()) {
            throw new EmptyOrderException(id);
        }
        this.status = OrderStatus.PLACED;
        this.placedAt = Instant.now();
    }

    private void recalculateTotal() {
        this.total = items.stream().map(OrderItem::subtotal).reduce(Money.zero("GBP"), Money::add);
    }

    OrderId id() { return id; }
    OrderStatus status() { return status; }
    Money total() { return total; }
    List<OrderItem> items() { return List.copyOf(items); }
}
```

上例体现以下规则：

- **功能包内使用包级实体类。** 功能外部不直接访问实体。参见[代码组织](code-organization.zh-CN.md)。
- **JPA 使用 `protected` 无参构造器。** 其他调用方使用真实构造器，静态工厂是唯一公开的创建路径。
- **不提供 Setter。** 使用 `order.place()` 而非 `order.setStatus(PLACED)`，通过操作名称表达发生的行为，并在同一处维护不变量。
- **仅提供调用方需要的访问器。** 不使用 `get` 前缀；集合返回防御性副本。返回实时列表会让调用方绕过 `addItem` 和它维护的不变量。
- **显式声明表名、列名和索引名。** 依赖命名策略时，名称变更可能悄悄改变 Schema。
- **枚举使用带长度的 `@Enumerated(EnumType.STRING)`。** `ORDINAL` 保存位置，重排枚举会悄悄破坏所有已存行。
- **集合在声明时初始化且永不为 null。**

## 身份：值对象 ID 与时间有序 UUID

有两个彼此独立的选择，而默认做法都不理想。

### 类型：值对象

`orderRepository.findById(customerId)` 会成为编译错误，而不是运行时静默查找失败。Jakarta Persistence 3.2 允许 record 不标注 `@Embeddable` 也作为 embeddable，但建议保留注解：它更明确，并且兼容 3.5.x。

对外传输时添加 `@JsonValue` / `@JsonCreator`（参见[JSON 与 Jackson](json-and-jackson.zh-CN.md)），并为路径变量添加 Spring `Converter`（参见[REST Controller](rest-controllers.zh-CN.md)）。

```java
@Embeddable
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value");
    }

    public static OrderId newId() {
        return new OrderId(UuidV7.generate());
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

### 值：UUIDv7，不使用 v4

`@GeneratedValue(strategy = GenerationType.UUID)` 生成随机 UUIDv4。每次插入都会落在主键 B-Tree 的随机位置，导致页分裂并破坏缓存局部性；对于大型、高写入量的表，这会造成可测量的吞吐损失。

UUIDv7 按时间排序，前 48 位是毫秒时间戳，因此具有更好的索引局部性。它保持 128 位 UUID 格式且无需外部协调，但会暴露大致创建时间和顺序。绝不能将 ID 本身用作授权凭据；当标识符本身参与安全敏感操作时，应使用 UUIDv4（参见 [RFC 9562 安全注意事项](https://www.rfc-editor.org/rfc/rfc9562.html#section-8)）。

`@EmbeddedId` 是**赋值型** ID，`@GeneratedValue` 和 `@UuidGenerator` 都不适用；应在工厂中生成。UUIDv7 由 RFC 9562 完整定义，无需额外依赖：

```java
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    /// 生成符合 RFC 9562 的版本 7 UUID：48 位大端 Unix 毫秒时间戳，后接 74 位随机数。
    public static UUID generate() {
        var random = new byte[10];
        RANDOM.nextBytes(random);

        long timestamp = System.currentTimeMillis() & 0xFFFF_FFFF_FFFFL;
        long msb = (timestamp << 16)
                | 0x7000L                                          // 版本 7
                | ((random[0] & 0x0FL) << 8) | (random[1] & 0xFFL); // 12 位 rand_a

        long lsb = 0;
        for (int i = 2; i < 10; i++) {
            lsb = (lsb << 8) | (random[i] & 0xFFL);
        }
        lsb = (lsb & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L; // 变体 10

        return new UUID(msb, lsb);
    }
}
```

同一毫秒内生成的 UUIDv7 值彼此不保证有序。这符合 RFC 9562 的规定，也不影响依赖毫秒前缀的索引局部性。

在 PostgreSQL 中使用原生 `uuid` 列，而不是体积翻倍、比较更慢的 `varchar(36)`。

### 备用方案

如果认为 `@AttributeOverride` 的写法不值得使用，可直接使用 UUID ID，但仍应采用 Hibernate 原生生成的 v7：

```java
@Id
@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
@Column(name = "id", nullable = false, updatable = false)
private UUID id;
```

`UuidGenerator.Style` 还包括 `RANDOM`（v4，默认值）、`TIME`（**v1**，不是 v7，容易被误读）和 `VERSION_6`。应使用 `VERSION_7`。

这种方式会失去编译期类型保护，但不能放弃时间有序性：高写入表使用普通 `GenerationType.UUID` 是错误选择。

## 基类

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

```java
@Configuration
@EnableJpaAuditing
class JpaConfig {
}
```

此处的 `@Version` 有两个用途：

1. **乐观锁。** 并发更新会抛出 `ObjectOptimisticLockingFailureException`，并映射为 HTTP 409；参见[错误处理](error-handling.zh-CN.md)。
2. **检测新实体。** 对赋值 ID（包括普通 UUID `@Id` 或 `@EmbeddedId`），Spring Data 无法通过“ID 是否为 null”来判断应调用 `persist` 还是 `merge`。它会先检查可空的 `@Version`：null 表示新实体。包装类型 `Long` 因而很重要；原始 `long` 默认值为 0，JPA 会把它视作已有的首个版本，因此每次 `save()` 都会先执行 `SELECT`，再走 `merge`。

没有 `@Version` 时，赋值 ID 的实体需要实现 `Persistable`，并使用在 `@PostPersist` 和 `@PostLoad` 中清除的 `isNew` 标记。通常 `@Version` 更简单，而且通常本来也需要乐观锁。

当 API 响应需要包含新版本号时，应在映射响应前 flush。Hibernate 执行更新时才会递增托管实体的版本；flush 前内存中的版本可能仍是旧值。flush 也会在 Service 构造响应前暴露乐观锁冲突：

```java
@Transactional
public TaskResponse changeStatus(UUID id, TaskStatus status) {
    var task = tasks.findById(id).orElseThrow();
    task.changeStatus(status);
    tasks.flush();
    return TaskResponse.from(task);
}
```

Flush 不等于 commit：事务之后仍可能失败。只有响应需要更新版本，或操作要求在 Service 返回前检测更新冲突时才使用它。

优先使用 Spring Data auditing 的 `@CreatedDate`，而不是 Hibernate 的 `@CreationTimestamp`：它还可通过 `AuditorAware` Bean 提供 `@CreatedBy` / `@LastModifiedBy`，且不依赖特定 ORM。

## equals 与 hashCode

规则很简单：**不要在实体上编写这两个方法。**

在持久化上下文范围内，默认的对象身份是正确的；同一行的两个引用只会在这个范围内共存。自定义实现会引入以下问题：

- 将 ID 纳入比较会在实体尚未持久化时出错：赋值 ID 可能还未对应数据库记录，生成 ID 则可能仍为 null。
- 将可变字段纳入比较时，对象放入 `HashSet` 后若字段变化，`hashCode` 也会变化，之后将无法在集合中找到该对象。
- 将关联纳入比较会在 `equals` 中触发懒加载，甚至加载整个对象图。

有两种例外：

- **稳定的自然键**有唯一约束保护时，只使用该键作为相等性依据。
- 类型判断使用 **`instanceof`，绝不使用 `getClass()`**：Hibernate 代理是子类，`getClass()` 会导致代理与延迟加载引用比较失败。

```java
@Override
public boolean equals(Object other) {
    return other instanceof Product that && sku != null && sku.equals(that.sku);
}

@Override
public int hashCode() {
    return Objects.hashCode(sku);
}
```

出于同样的懒加载原因，`toString()` 也不要包含集合或关联。

## 关系

```java
@Entity
@Table(name = "order_items")
class OrderItem {

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "id", nullable = false, updatable = false))
    private OrderItemId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_order_items_order"))
    private Order order;

    private int quantity;

    @Embedded
    private Money unitPrice;

    protected OrderItem() {
    }

    Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
```

- 每个 `@ManyToOne` 和 `@OneToOne` 都显式使用 `fetch = FetchType.LAZY`。这两种关系默认是 EAGER，会让每个查询都额外加载其他表。
- 除非确实需要双向导航，否则只映射一个方向。无用的反向引用会增加维护成本，也可能引发 N+1。
- 仅当父对象真正拥有子对象生命周期时使用 `orphanRemoval = true`；对共享引用使用会删除其他聚合仍在引用的行。
- 有数量、日期或状态等属性的关系不要使用 `@ManyToMany`；应将关联行建模为实体。以后再转换会成为 Schema 迁移。
- 不要将关联直接序列化为 JSON；应在事务内映射为 DTO。

## Repository

每个**聚合根**对应一个 Repository，而不是每张表一个；例如 `OrderItem` 没有自己的 Repository，应通过 `Order` 访问。参见[领域建模](domain-modelling.zh-CN.md)。

```java
interface OrderRepository extends JpaRepository<Order, OrderId> {

    Optional<Order> findByReference(String reference);

    boolean existsByCustomerIdAndStatus(CustomerId customerId, OrderStatus status);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(OrderId id);

    @Query("""
            select new com.example.app.order.OrderSummary(o.id, o.status, o.total.amount, o.placedAt)
            from Order o
            where o.customerId = :customerId
            order by o.placedAt desc
            """)
    List<OrderSummary> findSummariesByCustomer(CustomerId customerId, Limit limit);

    default Order getById(OrderId id) {
        return findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
```

- Repository 与实体一样使用包级可见性。
- `getById` 默认方法可放在 `findById` 旁边：必需记录缺失时抛出映射为 404 的领域异常；可选记录仍返回 `Optional`。
- 简单过滤使用派生查询；join 和非平凡查询使用 `@Query`。超过约四个条件的派生方法通常难以阅读；例如 `findByStatusAndCustomerIdAndPlacedAtBetweenOrderByPlacedAtDesc` 应改用 `@Query`。
- 判断是否存在时使用 `exists…`，而不是 `findById(id).isPresent()`；前者只查询布尔结果，不加载实体。
- Endpoint 中不要使用无条件 `findAll()`；要求传入 `Pageable`、`Limit` 或过滤查询。

## Projection

只读视图应避免加载实体。

```java
// Interface Projection 由 Spring Data 实现
interface OrderSummaryView {
    OrderId getId();
    OrderStatus getStatus();
    Instant getPlacedAt();
}

List<OrderSummaryView> findByCustomerId(CustomerId customerId);
```

```java
// 通过构造表达式显式映射到真正的 DTO record
public record OrderSummary(OrderId id, OrderStatus status, BigDecimal total, Instant placedAt) {}
```

Projection 只选择映射列，不进入持久化上下文，也不会触发懒加载。对于列表 Endpoint，这通常是最显著的单项收益。

## N+1

N+1 的表现是一条列表查询加上每行的一条查询。懒关联在循环中被访问，或带关联的实体被序列化时，都可能触发它。

```java
@EntityGraph(attributePaths = {"items", "items.product"})
Optional<Order> findWithItemsAndProductsById(OrderId id);
```

- 使用明确且有界的 `@EntityGraph` 加载确定需要的对象图。
- 只需要标量字段时使用 Projection；它更好，因为完全不加载对象图。
- 不要在一条查询中 Fetch Join 两个集合，这会产生笛卡尔积；Hibernate 7 对 Bag 会抛出 `MultipleBagFetchException`。只 Fetch Join 一个集合，或使用 `@BatchSize`。
- 在测试中启用 `spring.jpa.properties.hibernate.generate_statistics` 统计查询，或直接断言查询次数；否则即使测试通过，N+1 仍可能被忽略。

保持 `spring.jpa.open-in-view=false`。开启时，视图渲染期间的懒加载会让 N+1 静默转移到序列化阶段。

## 分页

`Page<T>` 会额外执行 count 查询；不需要总数时使用 `Slice<T>`。

深页的 Offset 分页会逐行扫描并丢弃之前的结果，性能会下降。此时使用 Keyset Pagination：

```java
@Query("""
        select o
        from Order o
        where o.status = :status
          and (o.placedAt < :lastPlacedAt
               or (o.placedAt = :lastPlacedAt and o.id.value < :lastId))
        order by o.placedAt desc, o.id.value desc
        """)
List<Order> findNextPage(OrderStatus status, Instant lastPlacedAt, UUID lastId, Limit limit);
```

Keyset 示例使用 `(placedAt, id)` 作为游标，即使时间戳相同也能稳定翻页。应建立与排序一致的 `(status, placed_at desc, id desc)` 索引。

## 批量写入

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 50
        order_inserts: true
        order_updates: true
```

`GenerationType.IDENTITY` **会悄悄禁用插入批处理**：Hibernate 必须为每个生成键往返数据库。上文推荐的赋值 UUIDv7 不存在此问题，这也是选择 UUIDv7 的另一个独立理由。

## 配置

```yaml
spring:
  jpa:
    open-in-view: false
    show-sql: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc.batch_size: 50
        order_inserts: true
        order_updates: true
        connection.provider_disables_autocommit: true
        query.fail_on_pagination_over_collection_fetch: true
  datasource:
    hikari:
      auto-commit: false
```

| 设置 | 原因 |
|---|---|
| `open-in-view: false` | 最重要的设置。默认开启会让持久化上下文一直保持到视图渲染，使 Service 外的懒加载成功，N+1 直到生产才暴露。关闭后可在开发时通过 `LazyInitializationException` 尽早发现边界错误。 |
| `ddl-auto: validate` | 临时环境之外不要使用 `update` 或 `create`。Schema 归 [Flyway](flyway.zh-CN.md) 管理；`validate` 可在启动时发现偏差。 |
| `fail_on_pagination_over_collection_fetch` | 当集合 Fetch Join 导致内存分页时直接失败，而不是静默地在内存中分页。 |
| `auto-commit: false` 和 `provider_disables_autocommit` | 让 Hibernate 延迟到确实需要时再获取连接。 |
| `show-sql: false` | 需要 SQL 时设置 `logging.level.org.hibernate.SQL=DEBUG`；`show-sql` 会向 stdout 输出未格式化 SQL。 |

## 使用 Spring Boot 3.5.x 时

- Hibernate **6.x** 和 Jakarta Persistence **3.1**；以上做法均适用。
- record 作为 `@Embeddable` 时必须显式标注 `@Embeddable`；隐式支持仅属于 Boot 4 所用的 Persistence 3.2。保留注解可同时兼容两条版本线。
- `@UuidGenerator` 的 `Style.VERSION_7` 要求 Hibernate 6.6+。较旧的 Hibernate 6.x 应像上例一样在工厂中生成 UUID。
- `Limit` 作为查询参数要求 Spring Data 3.2+（Boot 3.2+）。

## 常见问题

- 将实体写成 record：实体需要无参构造器和可变字段，无法使用 record。
- 在实体上使用 Lombok `@Data`：它会生成 Setter，并基于可变字段和关联生成 `equals`。
- 添加通用 Setter：改用命名操作，并在操作中维护不变量。
- 将实体或构造器声明为 `final` / `private`：Hibernate 无法代理或实例化它们。
- 使用 `@GeneratedValue(strategy = GenerationType.UUID)`：它生成随机 v4，应使用 UUIDv7。
- 将 `UuidGenerator.Style.TIME` 误认为 UUIDv7：`TIME` 是 v1，应使用 `VERSION_7`。
- 在 `@EmbeddedId` 上添加 `@GeneratedValue`：嵌入式 ID 是赋值型，应在工厂中生成。
- `@Version` 使用原始类型 `long`：必须使用可空的 `Long`，否则新实体检测失败，每次保存都会先 SELECT。
- 赋值 ID 的实体没有 `@Version`：Spring Data 无法区分新实体和已有实体。
- flush 前返回带版本号的 DTO：响应可能包含旧版本；API 需要新版本时应在映射前 flush。
- 为实体编写 `equals` / `hashCode`：除非使用稳定自然键，否则不要实现。
- 在实体 `equals` 中调用 `getClass()`：Hibernate 代理会导致比较失败，应使用 `instanceof`。
- 在 `equals`、`hashCode` 或 `toString` 中包含关联：会触发懒加载或无限递归。
- 保留 `@ManyToOne` 默认 EAGER：应显式设为 LAZY。
- 使用不带 `STRING` 的 `@Enumerated`：默认 `ORDINAL`，重排枚举值会破坏数据。
- 一次 Fetch Join 两个集合：会产生笛卡尔积并可能抛出 `MultipleBagFetchException`。
- 列表 Endpoint 返回实体：改用 Projection。
- 在 Controller 中调用 `findAll()`：要求 `Pageable`、`Limit` 或过滤条件。
- 使用 `findById(id).isPresent()`：改用 `existsBy…`。
- 保持默认 `open-in-view`：关闭它，让开发时尽早暴露 N+1。
- 设置 `ddl-auto: update`：Schema 由 Flyway 管理，应使用 `validate`。
- 每张表建立一个 Repository：应按聚合根建立。
- 深页使用 Offset 分页：改为 Keyset Pagination。
- 使用 `GenerationType.IDENTITY` 同时启用 Batch：Hibernate 会静默关闭插入批处理。
- 访问器返回可变集合：返回 `List.copyOf(...)`，避免绕过聚合不变量。
- 将 UUID 保存为 `varchar(36)`：使用数据库原生 `uuid` 类型。

## 相关文档

- [MyBatis-Plus](mybatis-plus.zh-CN.md) · [领域建模](domain-modelling.zh-CN.md) · [事务](transactions.zh-CN.md) · [Flyway](flyway.zh-CN.md) · [持久化切片测试](testing-slices-persistence.zh-CN.md) · [JSON 与 Jackson](json-and-jackson.zh-CN.md)
