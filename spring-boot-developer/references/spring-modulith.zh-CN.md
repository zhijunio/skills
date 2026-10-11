# Spring Modulith

Spring Modulith 是[代码组织](code-organization.zh-CN.md)分级体系中的第二级和第三级：当功能拆分为子包后，javac 无法再检查边界时，它可以强制验证模块边界。它是一个**测试时验证工具**，同时也提供事件基础设施；除非使用事件功能，否则不会改变运行时行为。

当功能超出单一平铺包的规模时再引入它，不要过早采用。

## 约定

| 规则 | 含义 |
|---|---|
| 主应用包的每个**直接子包**都是一个应用模块 | `com.example.app.orders` 是模块；`com.example.app.orders.internal` 不是 |
| 模块的**基础包就是它的 API** | 其他模块唯一可以依赖的包 |
| **嵌套包属于内部实现** | 其他模块引用 `orders.internal.*` 会导致验证失败 |
| `@NamedInterface` 可以重新公开嵌套包 | 仅在基础包不足以表达 API 时使用 |

在 Modulith 中，嵌套包用于**声明**内部实现。这与平铺包结构相反：平铺结构中嵌套包会破坏边界；Modulith 用它替代失去的 `package-private` 强制能力。

```
com.example.app/
├── Application.java
├── orders/
│   ├── OrdersApi.java             ← API：对其他模块可见
│   ├── OrderResponse.java         ← API
│   └── internal/                  ← 对其他模块不可见
│       ├── Order.java
│       ├── OrderRepository.java
│       └── OrderService.java
├── inventory/
├── shared/                        ← OPEN
└── config/                        ← OPEN
```

## 配置

```xml
<properties>
    <spring-modulith.version>2.1.0</spring-modulith.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>${spring-modulith.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Boot 4 使用 **Modulith 2.x**，Boot 3.5.x 使用 1.4.x。不要直接照抄上面的版本；应确认当前兼容分支中的最新补丁版本。

## 结构验证测试

没有这个测试，Modulith 不会执行任何边界验证。只需在根包中编写一次：

```java
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
```

`verify()` 会在模块之间存在循环依赖或某模块引用另一个模块的内部实现时令构建失败。`Documenter` 会将 PlantUML 组件图和 AsciiDoc 模块画布写到 `target/spring-modulith-docs`，方便代码审查，而且无需额外成本。

## 开放 shared 和 config 模块

`shared/` 和 `config/` 会被识别为模块，因此依赖它们的每个功能都会被报告为违规。将它们标记为开放模块：

```java
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.example.app.shared;

import org.springframework.modulith.ApplicationModule;
```

`OPEN` 表示“这里没有内部结构，任何模块都可以依赖其中任何内容”。只对真正横切多个功能的包使用。一个开放的功能模块自相矛盾：它主动放弃了引入 Modulith 的目的。

## 公开基础包以外的 API

```java
@NamedInterface("events")
package com.example.app.orders.events;

import org.springframework.modulith.NamedInterface;
```

之后，其他模块除了基础包，还可以依赖 `orders.events`。可将它用于模块对外发布的事件类型，或由其他模块实现的 SPI。每个命名接口都是对外的长期承诺，应有意识地添加，不能只为消除验证失败。

## 嵌套模块：大型功能

从 Modulith 1.3 起，模块内部的包本身也可以成为模块。对于包含十个 Service 和多个子领域的大型功能，这是受支持的组织方式：

```java
@ApplicationModule
package com.example.app.orders.pricing;

import org.springframework.modulith.ApplicationModule;
```

```
orders/
├── OrdersApi.java              ← 顶层模块的 API
├── pricing/                    ← 嵌套模块，对 inventory/、shipping/… 不可见
│   └── internal/
├── fulfilment/                 ← 嵌套模块
│   └── internal/
└── internal/
```

嵌套模块对同级的**顶层模块**不可见，但可以使用顶层模块公开的内容。这样大型功能能保留内部结构，而不向外泄漏。

采用这种结构前，先应用[代码组织](code-organization.zh-CN.md)中的判断标准：如果这些部分没有共享不变量且依赖方向各异，它们可能本就是被放在一起的多个功能；嵌套只会将错误结构固定下来。

## 使用事件进行模块通信

跨模块直接注入会产生耦合。对于不需要同步返回结果的操作，发布事件：

```java
// 在 orders 模块中，事件类型属于该模块 API
public record OrderPlaced(OrderId orderId, CustomerId customerId, Money total) {}

// 在 orders/internal 中
@Transactional
public OrderId place(PlaceOrderCmd cmd) {
    var order = Order.place(cmd);
    orderRepository.save(order);
    events.publishEvent(new OrderPlaced(order.id(), order.customerId(), order.total()));
    return order.id();
}
```

```java
// 在 inventory 模块中：不编译依赖 orders 的内部实现
@Component
class OrderPlacedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedListener.class);

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        inventoryService.reserve(event.orderId());
    }
}
```

应使用 `@ApplicationModuleListener`。它是一个组合注解，包含：

- `@TransactionalEventListener(phase = AFTER_COMMIT)`：只有发布者事务提交后才运行监听器。
- `@Async`：在独立线程执行，发布者不必等待。
- `@Transactional(propagation = REQUIRES_NEW)`：监听器使用自己的事务。

这几项组合通常正是所需行为，但手动编写时很容易遗漏。为什么 `AFTER_COMMIT` 很重要，参见[事务](transactions.zh-CN.md)。

### 让事件在崩溃后仍可恢复

默认情况下，异步监听器失败后事件会丢失。事件发布注册表会持久化每次发布，并在监听器成功后标记完成：

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jdbc</artifactId>
</dependency>
```

```properties
spring.modulith.events.jdbc.schema-initialization.enabled=true
spring.modulith.events.completion-mode=update
spring.modulith.events.republish-outstanding-events-on-restart=true
```

未完成的发布会在重启时重试。生产环境应通过 [Flyway](flyway.zh-CN.md) 管理事件发布表，而不是让 Modulith 创建它；`schema-initialization.enabled=true` 只适合开发便利场景。

这是用于进程内模块的事务 Outbox。跨进程边界时参见[消息](messaging.zh-CN.md)。

## 模块测试

```java
@ApplicationModuleTest
class OrdersModuleTests {

    @Test
    void placesOrder(Scenario scenario) {
        scenario.stimulate(() -> ordersApi.place(cmd))
                .andWaitForEventOfType(OrderPlaced.class)
                .toArriveAndVerify(event -> assertThat(event.total()).isEqualTo(expected));
    }
}
```

`@ApplicationModuleTest` 只启动被测模块，并在它访问其他模块内部实现时失败。`Scenario` 会等待异步事件送达；不要用 `Thread.sleep`。

## 使用 Boot 3.5.x 时

使用 Modulith **1.4.x**；2.x 面向 Boot 4。嵌套模块（1.3+）、`@ApplicationModuleListener`、`@NamedInterface` 和事件发布注册表在 1.4.x 中都可用。Modulith 2.1 与 Boot 切片测试支持的集成仅存在于 2.x。

## 常见问题

- Agent 添加 Modulith 依赖后没有编写结构验证测试：测试中没有 `modules.verify()` 就不会强制任何边界。
- Agent 期待 Modulith 在运行时强制模块边界：它是构建时检查工具，另带事件库。
- Agent 未标记 `shared/` 和 `config/`：它们会被识别为模块，依赖它们会被报告违规；应标记 `type = OPEN`。
- Agent 为消除错误将功能模块标记为 `OPEN`：这会放弃引入 Modulith 的唯一目的。
- Agent 添加 `@NamedInterface` 来消除违规：违规通常是真实问题；应移动类型或发布事件。
- Agent 直接注入另一个模块的 `@Service`：应依赖其 API 类型或通过事件通信。
- Agent 在模块间使用普通 `@EventListener`：它在发布者事务中运行，后续回滚时副作用已经发生；应使用 `@ApplicationModuleListener`。
- Agent 认为异步事件默认能在崩溃后恢复：应添加 `spring-modulith-starter-jdbc`。
- Agent 在生产环境让 Modulith 创建事件表：应交给 Flyway 管理。
- Agent 将 Entity 和 Repository 放在模块基础包中：这会将它们公开；应放入嵌套内部包。
- Agent 在一个实际包含多个功能的模块内继续嵌套：先检查这些部分是否共享不变量。

## 相关文档

- [代码组织](code-organization.zh-CN.md) · [领域建模](domain-modelling.zh-CN.md) · [事务](transactions.zh-CN.md) · [消息](messaging.zh-CN.md) · [架构测试](archunit.zh-CN.md)
