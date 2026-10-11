# 测试策略

阅读本文，决定应该编写**哪些层次**的测试，以及如何让大型 Spring Boot 测试套件保持快速。具体做法见各专题文档。

## 各层测试互为补充，而非相互替代

切片测试和端到端测试不是二选一，应同时编写，因为它们能发现彼此从结构上无法发现的问题。

- **纯单元测试**即使覆盖率很高，也可能在应用已损坏时仍然通过。它不执行持久化映射、请求映射、序列化、安全配置或注解逻辑。缺少 `@RestController` 会导致生产环境端点没有响应，但单元测试察觉不到。
- **完整上下文冒烟测试**可以发现 Bean 定义错误、映射无法解析等装配问题，但它是黑盒，难以定位行为问题，而且运行慢。
- **Spring 切片测试**使用真实框架启动被测组件，因此能捕获单元测试遗漏的注解、映射和 SQL 问题，同时仍然较快。

常见误区是认为单元测试通过，就可以跳过触及框架的测试。

## 测试数量分布

大致应当这样安排：

- **主要使用 Spring 切片测试。** `@SpringBootTest(classes = { … })` 配合切片注解，可以仅启动列出的组件，同时使用真实框架、注解和映射。对缓慢或昂贵的外部依赖使用 Mock，其余部分保持真实。参见[集成测试](testing-integration.zh-CN.md)、[Web 切片测试](testing-slices-web.zh-CN.md)和[持久化切片测试](testing-slices-persistence.zh-CN.md)。
- **至少一个冒烟测试和一个请求级端到端测试。** 几乎不带配置的 `@SpringBootTest` 用于证明上下文能启动；通过完整调用栈发送一个请求，用于证明装配后确实能返回响应。参见[集成测试](testing-integration.zh-CN.md)和 [REST API 测试](testing-rest-api.zh-CN.md)。
- **适量单元测试**用于复杂且不依赖框架的逻辑。这类测试快且有价值，但很少能单独满足需求。参见[单元测试](testing-unit.zh-CN.md)。

本文有意不采用 70/20/10 金字塔。该金字塔形成于集成测试依赖共享环境、只能夜间运行的时代；使用缓存上下文的 Spring 切片测试只需毫秒，并且可以捕获单元测试无法发现的问题。将测试资源过度偏向单元测试，是在优化一个已不复存在的成本。

关于“我要测什么，应查哪份文档”，请使用 [SKILL.md](../SKILL.zh-CN.md) 中的决策表。本文说明了背后的原因。

## 切片测试为何是主力：上下文成本

完整 `@SpringBootTest` 的启动成本很高：中型应用需要数秒，大型应用还会因数据初始化和缓存预热而更久。因此默认使用**切片** Spring 测试，把完整上下文测试限制在少量冒烟和端到端用例中。原因是完整上下文启动慢，不是切片与端到端测试互相替代。

## 上下文缓存

Spring 会在配置相同的测试类之间复用已加载的 `ApplicationContext`：第一个测试承担启动成本，后续测试复用上下文。正确使用缓存，是测试套件只需几分钟还是要几十分钟的区别。

缓存键是 **`MergedContextConfiguration`**。以下各项必须全部相同，Spring 才会复用上下文：

- 配置类 / `classes`
- 活跃 Profile（`@ActiveProfiles`）
- 属性（`@TestPropertySource` 和内联 `properties`）
- **Mock Bean**（`@MockitoBean` / `@MockitoSpyBean`）
- 测试初始化器和定制器

其中任意一项不同，Spring 就会重新构建上下文，也就是再启动一次完整环境。**增加一个 `@MockitoBean` 就会拆分缓存。** 因此：

- 将通用 Mock 集中到抽象的 `BaseIntegrationTest` 中，不要在每个类中散布 `@MockitoBean`。
- 在 `application-test.yml` 中定义共享配置，并使用 `@ActiveProfiles("test")`，不要到处覆盖属性。
- **避免使用 `@DirtiesContext`。** 它会丢弃缓存上下文；十次使用可能浪费数分钟。应修复隔离问题，通常是某个测试修改共享状态后没有清理。
- 使用 `logging.level.org.springframework.test.context.cache=DEBUG` 观察缓存情况。

测试套件悄悄积累二十种不同上下文配置，就会启动二十次。值得统计一下。

## 整个套件共用一个容器

只有复用容器，真实基础设施测试的成本才低。**整个套件启动一个容器**，例如使用 `static` 容器或共享的 `@ServiceConnection` Bean；不要每个类都启动一个。参见 [Testcontainers](testcontainers.zh-CN.md)。

这与上下文缓存配合后，只需一个容器和一个缓存上下文，就能在较大规模下始终针对真实数据库测试。同一套件中的两种容器配置意味着两套数据库和两组上下文缓存，通常是意外造成的。

## Mock 是边界测试工具

在架构边界处使用 Mock，例如外部服务、时钟和随机源。其他依赖保持真实。详细理由和过度 Mock 的问题见[单元测试](testing-unit.zh-CN.md)。

## 隔离与执行顺序

测试不能依赖执行顺序，也不能依赖其他测试留下的状态。

- 切片测试带事务并会回滚。完整上下文的 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 测试**不会回滚**——请求在线程中执行，因此测试事务不会覆盖它。应显式清理，或使用 `@Sql` 重置数据。
- 在生产代码中注入固定的 `Clock`，不要直接调用 `Instant.now()`，这样依赖时间的断言才稳定。午夜或切换时区后失败，说明测试本身有问题。
- 并行执行会放大上述影响。先做好隔离。

## GraalVM Native Image 不支持 Mock

如果以 Native Image 运行测试来验证兼容性，**Mockito 不受支持**：`@Mock`、`@MockitoBean` 和 `@MockitoSpyBean` 都会失败，因为 Mockito 需要运行时生成类，而闭世界 Native Image 不允许这样做。

- 使用 JUnit 的 `@DisabledInNativeImage` 在 Native Image 测试中跳过基于 Mock 的测试。
- 如果需要 Native Image 兼容的 Bean 替换，使用 **`@TestBean`** 和返回真实实现或手写 Stub 的静态工厂方法。Bean Override 支持 Native Image；Mockito 不支持。
- 这也是优先使用真实组件和 Testcontainers 测试的理由：它们不使用 Mock，可以在原生模式下执行，真正验证原生兼容性。参见[容器化与 Native Image](containerization-and-native.zh-CN.md)。

## 使用 Spring Boot 3.5.x 时

- 上下文缓存及缓存键的行为相同。
- **仅 4.x：上下文暂停**（Spring Framework 7）可在缓存上下文闲置期间暂停 `@Scheduled` 任务和监听器，并在下次使用时立即恢复，从而减少对 `@DirtiesContext` 的需求。4.x 还改进了 `@TestConfiguration` Bean 覆盖体验。
- 两个版本线都使用 `@MockitoBean` / `@MockitoSpyBean`。`@MockBean` 在 3.5.x 中已弃用，在 4.x 中已移除。

## 常见问题

- 只编写单元测试——应用可能已损坏但测试仍通过；没有测试映射、序列化和配置。
- 所有测试都使用 `@SpringBootTest`——完整上下文才是耗时部分，应使用切片测试。
- 套用 70/20/10 金字塔——这种权重优化的是缓存切片测试已不存在的成本。
- 在各测试类中散布 `@MockitoBean`——每组不同 Mock 都会拆分上下文缓存并增加启动次数。
- 散布 `@TestPropertySource`，而不是使用一个 `application-test.yml` 和 `@ActiveProfiles`——同样会拆分缓存。
- 用 `@DirtiesContext` 解决不稳定测试——它会丢弃缓存；应修复隔离问题。
- 每个测试类都启动一个容器——整个套件应共用一个。
- 一个套件定义两种 Testcontainers 配置——会产生两套数据库和缓存上下文。
- 认为 `RANDOM_PORT` 测试会回滚——不会，请求运行在另一个线程。
- 生产代码调用 `Instant.now()` 并直接断言时间——应注入 `Clock`。
- 在 `nativeTest` 中运行基于 Mock 的测试——Native Image 不支持 Mockito；使用 `@TestBean` 或 `@DisabledInNativeImage`。

## 相关文档

- [单元测试](testing-unit.zh-CN.md) · [Web 切片测试](testing-slices-web.zh-CN.md) · [持久化切片测试](testing-slices-persistence.zh-CN.md) · [集成测试](testing-integration.zh-CN.md) · [REST API 测试](testing-rest-api.zh-CN.md) · [Testcontainers](testcontainers.zh-CN.md)

---

*分层测试模型和优先使用切片测试的分布归功于 **Paul Bakker**（Netflix）的 [Testing Spring Boot the Netflix Way](https://github.com/paulbakker/testing-spring-boot-presentation)。上下文缓存成本模型归功于 **Philip Riecks** 的 Testing Spring Boot Applications Demystified。*
