# Architecture Tests

Layout and dependency rules erode. A junior developer injects a repository into a controller, a reviewer misses it, and six months later the "boundary" is decorative. ArchUnit turns those rules into failing tests.

This is the enforcement rung for the layout in [code-organization.md](code-organization.md), and it is how the framework-free domain in [hexagonal-architecture.md](hexagonal-architecture.md) stays framework-free.

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

Test scope only - nothing reaches production. Boot does not manage the version, so set it and check periodically.

## The rules worth having

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

`noCycles` is the highest-value rule in the file. A dependency cycle between features is the failure that makes a codebase hard to change, it accumulates one import at a time, and nobody notices it in review.

`because(...)` is not decoration - it is what the failure message says. "Rule violated" sends the next developer to this file; "controllers go through a service" does not.

## Enforcing the layout

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

The exceptions for `shared` and `config` are the same carve-outs Spring Modulith expresses as `@ApplicationModule(type = OPEN)` ([spring-modulith.md](spring-modulith.md)). If you already use Modulith, its `ApplicationModules.verify()` covers this ground and you do not need both - keep ArchUnit for the rules Modulith does not express, such as `@Transactional` placement or field injection.

## Keeping the domain pure

For a feature using ports and adapters, this is the rule the whole pattern rests on:

```java
@ArchTest
static final ArchRule domainIsFrameworkFree =
        noClasses().that().resideInAPackage("..payment.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "tools.jackson..")
                .because("the domain must be testable with no framework on the classpath");
```

Without it, the first `@Entity` on a domain class passes review and the hexagon is over. Discipline does not survive contact with a deadline; a failing build does.

## Naming conventions

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

Useful, but keep them few. Naming rules are the ones that generate the most noise for the least benefit, and a file of thirty of them trains people to add `@ArchIgnore`.

## Cost

ArchUnit imports and analyses bytecode, so the first rule in a class is slow - usually a few seconds for a medium codebase. `@AnalyzeClasses` caches the imported classes across `@ArchTest` rules in the same class, so **keep the rules together in one test class** rather than spreading them across several.

`ImportOption.DoNotIncludeTests.class` matters: without it, test classes are analysed too and rules fire on test code that legitimately breaks them.

## Adopting on an existing codebase

Turning on `noCycles` in a mature codebase usually produces dozens of violations at once, and the response is to delete the rule.

Use a freeze instead:

```java
@ArchTest
static final ArchRule noCycles = FreezingArchRule.freeze(
        slices().matching("com.example.app.(*)..").should().beFreeOfCycles());
```

The first run records existing violations to a store; from then on the rule fails only on **new** ones, and removing an old violation permanently retires it. That makes the rule adoptable today and the debt monotonically decreasing, which a rule everyone deleted does not.

## Taikai baseline

[Taikai](https://github.com/enofex/taikai) wraps ArchUnit with a fluent builder over predefined Java, Spring, JPA and test rules. New projects generated by this skill include it as a test-scoped dependency and add a conservative `TaikaiArchitectureTest`. The current generator pin is **Taikai version:** `2.8.0`; keep it in sync with [`versions.json`](../../versions.json) by running `node scripts/sync-versions-in-docs.mjs --check`.

The generated baseline checks deprecated API usage, generic exceptions, import cycles, field injection, self-invocation of proxied methods, application package placement, controller-to-controller dependencies, service naming and repository-to-service dependencies. It deliberately does not require a REST controller for every controller or impose a persistence ID strategy, because this skill also supports Thymeleaf, MyBatis-Plus and projects with no persistence layer.

For an existing Maven project, add the test dependency with the version managed by the project or this skill's [`versions.json`](../../versions.json):

```xml
<dependency>
    <groupId>com.enofex</groupId>
    <artifactId>taikai</artifactId>
    <version>2.8.0</version>
    <scope>test</scope>
</dependency>
```

For Gradle, use the equivalent `testImplementation("com.enofex:taikai:2.8.0")`. Put the rules in one architecture test and keep the namespace equal to the application's base package. Use `dynamicTests()` when individual rule failures need separate IDE and CI test results; use `checkAll()` for a compact baseline.

Taikai does not replace Modulith. Use Taikai for cross-cutting Java/Spring conventions and custom ArchUnit rules; use `ApplicationModules.verify()` for application-module boundaries, cycles and named interfaces. Avoid enabling overlapping rules twice, and do not weaken a rule merely to make a feature pass. When a rule is a project-specific policy, document its reason beside the rule and test the intended behavior.

## Gotchas

- Agent writes architecture rules without `because(...)` - the failure message then sends the next developer digging
- Agent omits `ImportOption.DoNotIncludeTests` - rules fire on test classes that legitimately break them
- Agent spreads `@ArchTest` rules across many classes - each `@AnalyzeClasses` re-imports the bytecode
- Agent adds a rule to an existing codebase, gets dozens of failures, and deletes the rule - use `FreezingArchRule`
- Agent writes thirty naming rules and no dependency rules - cycles and layer violations are what actually hurt
- Agent runs Taikai, Modulith and duplicate ArchUnit slice rules for the same boundary - keep one owner for each constraint
- Agent relies on review to keep the domain framework-free - add the rule; discipline does not survive deadlines
- Agent pins no ArchUnit version - Boot does not manage it
- Agent enables Taikai's REST-only controller rules in a Thymeleaf application - keep the baseline compatible with the selected web style

## Related

- [code-organization.md](code-organization.md) · [spring-modulith.md](spring-modulith.md) · [hexagonal-architecture.md](hexagonal-architecture.md) · [testing-strategy.md](../testing/testing-strategy.md)

## References

- [Taikai](https://github.com/enofex/taikai)
