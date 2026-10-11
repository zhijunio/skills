# Spring 代理与依赖注入

造成 Spring 代码静默失效的两个常见原因是字段注入，以及通过 `this` 调用带注解的方法。本文集中说明一次，技能中的其他文档会链接到这里。

## 始终使用构造器注入

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    OrderService(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }
}
```

不要使用 Lombok，也不需要 `@Autowired`。只有一个构造器的 Bean 会自动使用该构造器注入依赖；从 Spring 4.3 起，这个注解就不是必需的。

这不只是风格偏好：

- **字段可以是 `final`。** 依赖不能被重新赋值，编译器也能验证每个字段都已设置。
- **不会出现半构造对象。** 字段注入先构造 Bean，再注入字段；中间运行的代码可能看到 `null`。
- **无需容器即可做单元测试。** `new OrderService(mockRepo, mockInventory)` 不需要反射或 `@SpringBootTest`。这正是[单元测试](testing-unit.zh-CN.md)可行的原因。
- **参数太多是信号，不是麻烦。** 九个依赖的构造器说明类承担九项工作。字段注入会掩盖这一点。

在扁平功能包中可将构造器设为包级可见。Spring 不要求它是 `public`，功能包外的代码也不应手动创建 Service。

### 绝不要这样写

```java
@Autowired private OrderRepository orderRepository;   // 字段注入
@Autowired public void setRepo(OrderRepository r) { } // Setter 注入
```

只有确实可选且会重新配置的依赖才有理由使用 Setter 注入；这种情况很少，使用前应先确认。

### 可选依赖与多个依赖

```java
// 可选协作者；缺失时允许
OrderService(OrderRepository repo, Optional<AuditSink> auditSink) { … }

// 注入接口的所有实现
OrderService(List<OrderValidator> validators) { … }

// 区分同类型的两个 Bean
OrderService(@Qualifier("primaryClock") Clock clock) { … }
```

`Optional<T>` 作为*构造器参数*是 java-developer“参数不使用 Optional”规则的唯一例外：Spring 用它表示可选依赖。另一种做法是在字段上使用 `@Autowired(required = false)`，不推荐。

## 代理与同类自调用陷阱

Spring 通过把 Bean 包装成**代理**来实现 `@Transactional`、`@Async`、`@Cacheable`、`@Retryable`、`@ConcurrencyLimit`、`@PreAuthorize` 和 `@Validated`。调用方拿到代理；代理执行增强行为后再委托给原对象。

因此，注解只在调用**从外部进入**时生效。`this.method()` 会直接调用原对象，完全绕过代理。

```java
@Service
public class OrderService {

    @Transactional
    public void processAll(List<UUID> ids) {
        ids.forEach(id -> this.processSingle(id));   // ⚠ 直接调用，没有新事务
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingle(UUID id) { … }         // 注解静默失效
}
```

代码能编译、能运行，却会做错事，而且没有警告。

### 正确修复：跨越 Bean 边界

```java
@Service
public class OrderService {

    private final OrderItemProcessor processor;      // 独立 Bean

    OrderService(OrderItemProcessor processor) {
        this.processor = processor;
    }

    @Transactional
    public void processAll(List<UUID> ids) {
        ids.forEach(processor::processSingle);       // 经过代理
    }
}
```

几乎所有场景中，正确修复都是把方法提取到独立 Bean 中：两个方法需要不同事务语义，通常也代表不同职责。自注入（`@Lazy OrderService self`）虽然有效，但属于代码异味。

### 哪些注解会受影响

| 注解 | 自调用时的表现 |
|---|---|
| `@Transactional` | 没有事务或传播行为错误；写入可能意外提交或回滚 |
| `@Async` | 在调用方线程同步运行 |
| `@Cacheable` | 不查询或填充缓存；每次都访问数据库 |
| `@Retryable` | 不重试，首次失败直接传播 |
| `@PreAuthorize` | **完全跳过授权检查** |
| `@Validated` | 不校验方法参数 |

`@PreAuthorize` 这一项是安全漏洞，不只是性能问题。

### 另外两条代理规则

- **`private`、`static` 和 `final` 方法无法被增强。** CGLIB 代理通过继承并覆盖方法实现增强，而这些方法无法覆盖。`private` 方法上的 `@Transactional` 不起作用。
- **`final` 类根本无法代理**——创建 Bean 时会失败，或增强逻辑被丢弃。带注解的 `@Service` 不要设为 `final`。这是 java-developer“优先使用 final 类”规则必须服从框架的场景；实体和 Spring Bean 是例外，DTO 和值对象不是。

## 配置类

```java
@Configuration
public class HttpClientConfig {

    @Bean
    RestClient inventoryRestClient(RestClient.Builder builder,
                                   InventoryProperties properties) {
        return builder.baseUrl(properties.baseUrl()).build();
    }
}
```

- `@Configuration` 放在 `config/` 包中；若 Bean 只属于某个功能，也可放进该功能包。参见[代码组织](code-organization.zh-CN.md)。
- **装配基础设施，不编排业务逻辑。** `@Configuration` 注入 `@Service` 表示依赖方向反了。
- `@Configuration` 中的 `@Bean` 方法本身会被代理，因此重复调用只返回同一个 Bean。方法之间不互相调用时，可设置 `@Configuration(proxyBeanMethods = false)`：性能更好，Spring Boot 自身自动配置也使用此做法。
- 多个相关配置项应使用 `@ConfigurationProperties`，而非 `@Value`。参见[配置](configuration.zh-CN.md)。

### 条件 Bean

```java
@Bean
@ConditionalOnMissingBean            // 允许应用覆盖默认实现
OrderNumberGenerator orderNumberGenerator() { … }

@Bean
@ConditionalOnProperty(name = "app.features.recommendations", havingValue = "true")
RecommendationClient recommendationClient() { … }
```

`@ConditionalOnMissingBean` 适用于库和自动配置作者，用来提供可覆盖的默认实现。应用代码中优先使用 `@ConditionalOnProperty` 或 Profile；应用 Bean 若因其他地方定义了同类型 Bean 而静默消失，会很难排查。

## Bean 生命周期

| 需求 | 方式 |
|---|---|
| 依赖注入后执行工作 | `@PostConstruct`，或直接在构造器中处理 |
| 关闭时清理 | `@PreDestroy` |
| 整个上下文就绪后执行 | `ApplicationRunner` / `CommandLineRunner`，或 `@EventListener(ApplicationReadyEvent.class)` |

不要在构造器或 `@PostConstruct` 中启动线程、打开连接或调用远程服务——上下文尚未完全构建，失败会表现为难以理解的启动错误。应使用 `ApplicationReadyEvent`。

## 使用 Spring Boot 3.5.x 时

与版本无关。构造器注入、代理、自调用陷阱、`@Configuration` 语义和 Bean 生命周期在两个版本线中均相同，本文可直接适用。

## 常见问题

- 使用 `@Autowired` 字段——只用构造器注入，以获得 `final` 字段并避免半构造对象。
- 给唯一构造器添加 `@Autowired`——Spring 4.3 起不需要。
- 通过 `this` 调用带注解的方法——代理被绕过，注解静默失效。
- 在 `private` 方法上使用 `@Transactional` 或其他增强注解——代理无法覆盖它。
- 将被增强的 `@Service` 标为 `final`——无法代理。
- 同一方法同时使用 `@Retryable` 和 `@Transactional`——重试在已标记回滚的事务中运行；应将重试放到调用方 Bean。参见[弹性模式](resilience.zh-CN.md)。
- `@Configuration` 注入 `@Service`——配置类应装配基础设施，而非业务逻辑。
- 应用代码使用 `@ConditionalOnMissingBean`——这是自动配置惯用法，应改用属性或 Profile。
- 在 `@PostConstruct` 中打开连接——使用 `ApplicationReadyEvent`；此时上下文才构建完成。
- 注入 `ApplicationContext` 手工查找 Bean——直接注入依赖，或注入 `List<T>` / `Map<String, T>`。

## 相关文档

- [事务](transactions.zh-CN.md) · [异步与定时任务](async-and-scheduling.zh-CN.md) · [缓存](caching.zh-CN.md) · [弹性模式](resilience.zh-CN.md) · [安全基础](security-fundamentals.zh-CN.md) · [Spring 中的 Java](java-in-spring.zh-CN.md)
