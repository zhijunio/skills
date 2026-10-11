# 架构测试

布局和依赖规则会逐渐松动：新同事把 Repository 注入 Controller，审查时没发现，半年后所谓的“边界”只剩装饰作用。ArchUnit 可以把这些规则变成失败的测试。

它负责落实 [code-organization.zh-CN.md](code-organization.zh-CN.md) 中的布局规则，也能确保 [hexagonal-architecture.zh-CN.md](hexagonal-architecture.zh-CN.md) 中不依赖框架的领域层继续保持纯净。

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

依赖仅用于测试，不会进入生产环境。Boot 不管理其版本，因此需要明确指定并定期检查。

## 值得建立的规则

```java
@AnalyzeClasses(packages = "com.example.app", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllersDoNotUseRepositories =
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .because("controllers go through a service");

    @ArchTest
    static final ArchRule controllersDoNotReturnEntities =
            noMethods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .should().haveRawReturnType(assignableTo(Object.class)
                            .and(annotatedWith(Entity.class)))
                    .because("entities leak the schema and trigger lazy loads outside the transaction");

    @ArchTest
    static final ArchRule transactionalOnlyOnServices =
            methodsThatAreAnnotatedWith(Transactional.class)
                    .should().beDeclaredInClassesThat().areAnnotatedWith(Service.class)
                    .because("@Transactional belongs on the service layer");

    @ArchTest
    static final ArchRule noFieldInjection =
            noFields().should().beAnnotatedWith(Autowired.class)
                    .because("constructor injection only");

    @ArchTest
    static final ArchRule noCycles =
            slices().matching("com.example.app.(*)..").should().beFreeOfCycles();
}
```

`noCycles` 是本例中价值最高的规则。功能之间的依赖环会让代码库难以修改；它可能每次只多一个 import，审查时不容易察觉。

`because(...)` 不是装饰，而是失败信息的一部分。“规则被违反”会让下一位开发者去翻这份测试；“Controller 应通过 Service 调用”则直接解释了原因。

## 落实布局规则

```java
@ArchTest
static final ArchRule featuresDoNotReachIntoEachOther =
        slices().matching("com.example.app.(*)..")
                .namingSlices("Feature $1")
                .should().notDependOnEachOther()
                .ignoreDependency(resideInAPackage("com.example.app.."),
                                  resideInAPackage("com.example.app.shared.."))
                .ignoreDependency(resideInAPackage("com.example.app.."),
                                  resideInAPackage("com.example.app.config.."));

@ArchTest
static final ArchRule entitiesStayInsideTheirFeature =
        classes().that().areAnnotatedWith(Entity.class)
                .should().onlyBeAccessed().byClassesThat()
                .resideInTheSamePackageAs(Entity.class)
                .because("an entity is a feature's internal detail");
```

`shared` 和 `config` 的例外，与 Spring Modulith 用 `@ApplicationModule(type = OPEN)` 表达的例外相同（见 [spring-modulith.zh-CN.md](spring-modulith.zh-CN.md)）。如果已用 Modulith 做验证，它的 `ApplicationModules.verify()` 已覆盖这部分，无需重复编写相同的 ArchUnit slice 规则；可以保留 Modulith 表达不了的规则，例如 `@Transactional` 的位置和字段注入检查。

## 保持领域层纯净

采用端口与适配器的功能，可以用下面的规则守住整个模式的基础：

```java
@ArchTest
static final ArchRule domainIsFrameworkFree =
        noClasses().that().resideInAPackage("..payment.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "tools.jackson..")
                .because("the domain must be testable with no framework on the classpath");
```

没有这条规则，第一个领域类上出现 `@Entity` 也可能通过审查，六边形架构就此失守。截止日期面前，单靠自律并不可靠；让构建失败更有效。

## 命名约定

```java
@ArchTest
static final ArchRule servicesAreNamedService =
        classes().that().areAnnotatedWith(Service.class)
                .should().haveSimpleNameEndingWith("Service");

@ArchTest
static final ArchRule configurationIsNamedConfig =
        classes().that().areAnnotatedWith(Configuration.class)
                .should().haveSimpleNameEndingWith("Config");
```

命名规则有用，但应保持精简。它们最容易制造噪声、收益最低；几十条命名规则最终会让人到处添加 `@ArchIgnore`。

## 成本

ArchUnit 会导入并分析字节码，因此每个测试类的第一条规则比较慢；中型代码库通常需要几秒。`@AnalyzeClasses` 会在同一个类的多个 `@ArchTest` 规则间缓存已导入的类，所以**把规则集中在一个测试类中**，不要拆散到多个类。

`ImportOption.DoNotIncludeTests.class` 很重要：如果不排除测试类，规则也会分析测试代码，并对那些合理突破生产代码约束的测试报错。

## 在现有代码库中启用

在成熟代码库里直接启用 `noCycles`，通常会一次发现几十个违规，团队可能因此删掉规则。

可以先冻结已有问题：

```java
@ArchTest
static final ArchRule noCycles = FreezingArchRule.freeze(
        slices().matching("com.example.app.(*)..").should().beFreeOfCycles());
```

第一次运行会把已有违规写入存储；之后只会对**新增**违规失败。修复旧违规后，该违规会永久从冻结基线中移除。这样可以立即启用规则，并让技术债只减不增。

## 可选：预置规则集

[Taikai](https://github.com/enofex/taikai) 基于 ArchUnit，以流式 API 封装大量 Spring 预置规则。它会增加一个第三方依赖，而上面的规则已用大约四十行覆盖多数关键问题；只有确实需要它的广度时再采用。

## Boot 3.5.x 说明

ArchUnit 与 Boot 版本无关，上述规则都可以照用。唯一相关差异是应用自己的包名；若编写针对 Jackson 的规则，3.5.x 使用 `com.fasterxml.jackson`，而不是 `tools.jackson`。

## 常见误区

- 写架构规则时省略 `because(...)`，失败信息无法解释违规原因。
- 忘记设置 `ImportOption.DoNotIncludeTests`，测试代码触发本可接受的违规。
- 把 `@ArchTest` 分散在多个类中，每个 `@AnalyzeClasses` 都会重新导入字节码。
- 在现有项目启用规则后发现大量违规，就删掉规则；应先使用 `FreezingArchRule`。
- 写几十条命名规则，却没有依赖边界规则；真正难处理的是循环依赖和层次越界。
- 同时运行 Modulith 验证和重复的 ArchUnit slice 边界规则；边界检查选择一个即可。
- 依靠审查维持领域层无框架依赖；添加构建规则。
- 不指定 ArchUnit 版本；Boot 不会管理它。

## 相关文档

- [code-organization.zh-CN.md](code-organization.zh-CN.md) · [spring-modulith.zh-CN.md](spring-modulith.zh-CN.md) · [hexagonal-architecture.zh-CN.md](hexagonal-architecture.zh-CN.md) · [testing-strategy.zh-CN.md](testing-strategy.zh-CN.md)
