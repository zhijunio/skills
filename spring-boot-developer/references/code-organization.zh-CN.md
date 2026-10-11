# 代码组织

按**功能**组织包，不要按层组织。这是 [Spring Boot 参考文档](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html) 推荐的方式，也是唯一能让代码真正隐藏在边界内的布局。

## 为什么不按层优先

```
com.example.app/
├── controller/      ← OrderController, CustomerController, InvoiceController…
├── service/         ← OrderService, CustomerService, InvoiceService…
├── repository/
└── dto/
```

这种布局有两种代价，其中一种会破坏边界：

- 新增一个功能要同时改四个包，却没有任何一个包能说明应用提供什么能力。
- **类无法设为包级可见。**`OrderController` 和 `OrderService` 位于不同包，Service 必须是 `public`。最终应用里的每个类型都公开了，既没有可执行的边界，也无法安全地更改功能内部实现。

只有小到不需要考虑这些问题的应用才适合按层组织。不要从这种结构起步。

## 默认选择：扁平的功能包

```
com.example.app/
├── Application.java
├── order/
│   ├── Order.java                 (包级可见 @Entity)
│   ├── OrderRepository.java       (包级可见)
│   ├── OrderService.java          (public - 功能边界)
│   ├── OrderController.java
│   └── CreateOrderRequest.java, OrderResponse.java
├── customer/
└── config/
```

Entity 和 Repository 设为包级可见，只有 Service 是 `public`，因此其他功能不能绕过 Service 访问持久化模型。这是真正由编译器执行、且没有额外成本的边界。

把 `Application.java` 放在所有功能包的上层根包中。`@SpringBootApplication` 会从它所在包推导组件、实体和 Repository 的扫描范围。

## 逐级提升边界

功能会逐渐变大。每次只提升一级，不要一开始就采用最重的结构。

| 功能规模 | 结构 | 由什么强制执行 |
|---|---|---|
| 少量类型 | 扁平功能包 | javac 的包级可见性 |
| 超出单个包的规模 | 功能基础包作为 API，嵌套包作为内部实现 | Spring Modulith `verify()` |
| 包含可分离的子部分 | 在功能内部使用嵌套 `@ApplicationModule` 包 | Spring Modulith `verify()` |
| 需要独立部署或强团队边界 | Maven 或 Gradle 模块 | 构建系统 |

**第 2 级开始，javac 就无法继续帮你。**Java 包在访问控制上不是嵌套关系：`order.internal.OrderEntity` 无法既设为包级可见，又让 `order.web.OrderController` 看见它。一旦拆分子包，包级可见性就不再能充当边界，需要 Spring Modulith 或 ArchUnit 执行编译器无法执行的规则。这不是不拆子包的理由，而是 Modulith 存在的原因。读 [spring-modulith.zh-CN.md](spring-modulith.zh-CN.md)。

**第 3 级留给真正庞大的功能**，例如有十个以上 Service 或多个子领域。在采用前先做下面的判断。

**第 4 级成本最高**，换来强制边界和独立版本管理。读 [maven.zh-CN.md](maven.zh-CN.md) 或 [gradle.zh-CN.md](gradle.zh-CN.md)。

### 这是一个大功能，还是几个被放在一起的功能？

在一个功能内嵌套模块前先确认：

- 这些部分是否共享必须共同成立的不变量？如果没有，它们可能只是兄弟功能。
- 依赖方向是否一致，还是有两个部分互相依赖？循环依赖说明拆分方式有问题，而不只是功能很大。
- 这些部分是否总是一起部署或扩缩容？

如果答案表明它们是“多个功能”，应拆成并列功能。把第 3 级结构用于本质上是三个功能的代码，只会把混乱固定下来。

## 横切代码应该放在哪里

有两种横切代码；混为一谈就会让 `SecurityConfig` 变成万能对象。

### 真正全局的基础设施：`config/`

`SecurityConfig`、`WebMvcConfig`、`JacksonConfig`、`AsyncConfig`、`OpenApiConfig` 和应用级 `@RestControllerAdvice` 都不属于特定功能。把它们放在与 `Application.java` 同级的顶层 `config/` 包。

使用 Spring Modulith 时，顶层 `config/` 也会被识别为应用模块；功能依赖它时会报告违规。将其标记为开放模块：

```java
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.example.app.config;

import org.springframework.modulith.ApplicationModule;
```

### 真正共享的领域类型：`shared/`

多个功能共同使用的值对象和工具类可放在这里，也要标记为 `OPEN`。**如果不加约束，`shared/` 就会成为杂物间**：只有两个或更多功能确实需要的类型才属于这里；不能因为暂时不知道放在哪里就塞进来。仅被一个功能使用的类型应留在该功能内部。

### 看起来全局、实际属于功能的配置

`/api/orders/**` 的授权规则属于订单功能，而不是平台。按职责拆分：

| 关注点 | 归属 |
|---|---|
| 认证：过滤器链骨架、密码编码器、JWT decoder、401/403 入口点 | 全局 `SecurityConfig` |
| 授权：谁可以做什么 | 所属功能，可在 Service 方法上使用 `@PreAuthorize`，或为整段 URL 空间配置带 `@Order` 的专用 `SecurityFilterChain` bean |
| 框架和通用异常 | 全局 `@RestControllerAdvice` |
| 领域异常 | 功能自己的 `@RestControllerAdvice(basePackageClasses = OrderService.class)` |

不这样拆分，`SecurityConfig` 就会列出应用中每个 URL，每次功能变化都要改它。见 [security-fundamentals.zh-CN.md](security-fundamentals.zh-CN.md) 和 [error-handling.zh-CN.md](error-handling.zh-CN.md)。

## 命名约定

| 类型 | 约定 | 示例 |
|---|---|---|
| Entity | 领域名词；若已有同名领域模型则使用 `*Entity` | `Order`、`OrderEntity` |
| 值对象 | 使用领域名称和 record | `OrderId`、`Money`、`EmailAddress` |
| 命令 | `*Cmd` | `CreateOrderCmd` |
| 查询 | `*Query` | `FindOrdersQuery` |
| 命令结果 | `*Result` | `RegistrationResult` |
| HTTP 请求负载 | `*Request` | `CreateOrderRequest` |
| HTTP 响应负载 | `*Response` | `OrderResponse` |
| Repository | `*Repository` | `OrderRepository` |
| Service | `*Service` | `OrderService` |
| 领域异常 | `*Exception` | `OrderNotModifiableException` |
| 配置类 | `*Config` | `SecurityConfig` |
| 模块门面（第 2 级以上） | `*Api` | `OrdersApi` |

让 `*Request` / `*Response` 与领域模型保持独立：不要把请求负载直接绑定到 Entity，也不要把 Entity 作为响应返回。见 [rest-controllers.zh-CN.md](rest-controllers.zh-CN.md)。

## Boot 3.5.x 说明

布局与版本无关：包结构、可见性和逐级提升边界的方式都相同。唯一受版本影响的是 Modulith 本身：Boot 3.5.x 使用 1.4.x，Boot 4 使用 2.x（见 [spring-modulith.zh-CN.md](spring-modulith.zh-CN.md)）。

## 常见误区

- 创建 `controller/`、`service/`、`repository/` 横向包：按功能组织；按层组织会迫使所有类型公开，失去唯一零成本的边界。
- 习惯性将所有类设为 `public`：扁平功能包中的 Entity 和 Repository 应设为包级可见。
- 把 `Application.java` 放在某个功能包中：它应在所有功能包上方的根包，否则组件扫描可能漏掉功能。
- 把功能自己的 `SecurityFilterChain` 授权规则放进全局 `SecurityConfig`：认证全局统一，授权属于功能。
- 创建 `util/`、`common/`、`helper/` 包：给能力命名；只有多个功能真正共用的类型才放入 `shared/`。
- 给功能增加子包后仍以为包级可见性有效：Java 包的访问控制不嵌套；增加子包时同时引入 Modulith 或 ArchUnit。
- 在一个本质上包含三个功能的代码里嵌套模块：先检查是否共享不变量和单向依赖。
- 创建跨功能的 `dto/` 包：请求和响应负载应与提供它们的功能放在一起。

## 相关文档

[spring-modulith.zh-CN.md](spring-modulith.zh-CN.md) · [archunit.zh-CN.md](archunit.zh-CN.md) · [hexagonal-architecture.zh-CN.md](hexagonal-architecture.zh-CN.md) · [domain-modelling.zh-CN.md](domain-modelling.zh-CN.md)
