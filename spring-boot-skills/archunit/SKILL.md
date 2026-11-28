---
name: archunit
description: >
  Add or review static architecture tests in a Spring Boot codebase. Use existing architecture checks, ArchUnit for package and dependency constraints, or Taikai when its ready-made conventions are selected.
---

# Architecture Rules with Taikai and ArchUnit

Preserve the project's existing architecture-testing tool. For new checks Taikai is an optional convention library, not a mandatory dependency. Taikai provides
ready-made Java and Spring rules on top of ArchUnit and runs as a normal test. Add an ArchUnit rule
when a project-specific package or dependency boundary is not covered by Taikai. Both analyze
compiled classes; neither proves runtime behavior or that a rule matches the business intent.
For Maven, use the Taikai pin in
[Maven versions](../spring-boot-maven/references/maven-versions.json); for Gradle, use the pin in
[Gradle versions](../spring-boot-gradle/references/gradle-versions.json).

## Choose the right boundary tool

- Use existing ArchUnit rules or selected Taikai conventions when they fit the approved boundaries.
- Use ArchUnit for class and package dependencies, layer access, package cycles, and feature slices
  that the selected Taikai rules do not express.
- Use [feature architecture](../feature-architecture/SKILL.md) to choose business package ownership.
- Use [Spring Modulith](../spring-modulith/SKILL.md) for Spring module APIs, module cycles,
  application events, and module-level integration tests. Keep static class rules in ArchUnit and
  Spring module verification in Spring Modulith, as the HelpDesk example does.
- Check the build for an existing architecture-testing setup before adding dependencies. Taikai
  brings ArchUnit core transitively. For tests that import ArchUnit APIs directly, either rely on
  that transitive core or declare the test-scoped ArchUnit JUnit artifact as an explicit dependency,
  as HelpDesk does. Use `@AnalyzeClasses` / `@ArchTest` when its runner and class caching help; do
  not add a JUnit adapter solely to call ArchRule methods from ordinary JUnit tests. If Taikai and an
  ArchUnit artifact are both declared, align their ArchUnit versions through dependency management.

## Configure Taikai

Add Taikai with test scope and use the version selected by the project's dependency catalog or
version policy. Run a focused JUnit test from the normal test task. Start with rules that protect
conventions the project actually follows; avoid adopting every available rule without review.

```java
class ArchUnitTests {
    private static final String BASE_PACKAGE = ArchUnitTests.class.getPackageName();
    private static JavaClasses importedClasses;

    @BeforeAll
    static void importApplicationClasses() {
        importedClasses = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages(BASE_PACKAGE);
    }

    @Test
    void commonConventionsShouldHold() {
        Taikai.builder()
            .namespace(BASE_PACKAGE)
            .java(java -> java.noUsageOfDeprecatedAPIs()
                .imports(ImportsConfigurer::shouldHaveNoCycles))
            .spring(spring -> spring.noAutowiredFields())
            .build().checkAll();
    }
}
```

Use the actual application base package; the example requires ArchUnit's ImportOption import. Assert the intended production classes were imported and verify that custom rules are not vacuously satisfied by an empty selection. Import classes once when multiple custom rules inspect the
same package tree, then reuse that `JavaClasses` value in focused tests. Inspect Taikai rules and
configure only those that fit the project's conventions. The [HelpDesk architecture test](https://github.com/sivaprasadreddy/HelpDesk/blob/main/src/test/java/dev/sivalabs/helpdesk/ArchUnitTests.java)
shows this split between one Taikai convention test and separate custom rules.

The HelpDesk POM declares both Taikai and `archunit-junit5`, while its direct rules use
`ClassFileImporter` and `JavaClasses` inside ordinary JUnit methods rather than ArchUnit's
`@ArchTest` runner. Treat that explicit adapter dependency as a project-specific choice; inspect
the dependency graph before adding it to another project.

## Add custom ArchUnit constraints

Keep project-specific rules in focused JUnit tests when they describe a boundary Taikai does not
cover. Simple package constraints can use ArchUnit's rule DSL:

```java
@Test
void domainShouldNotAccessWebAdapters() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().accessClassesThat().resideInAPackage("..web..")
        .check(importedClasses);
}
```

Use a custom `ArchCondition` only when a rule needs logic beyond package predicates, such as
checking that a service is referenced only from its owning feature. Keep ownership extraction
aligned with a stable package convention; do not copy positional package parsing unless that
convention is enforced. Taikai also supports `addRule(TaikaiRule.of(rule))` when keeping a custom
rule inside its aggregated check is useful.

Use Spring Modulith's `ApplicationModules.verify()` for module dependency and internal-access
checks. Keep ArchUnit for Java-level rules that Modulith does not express, and avoid duplicate
assertions with different failure output.

## Make rules useful

- Scope analysis to the application's production base package. Use Taikai's default production-only
  import behavior unless test classes are part of the intended rule.
- Prefer one rule for a real dependency boundary over a broad rule that forbids all cross-package use.
- For feature-first packages, add slice cycle checks only when top-level packages represent real
  features. Keep the slice pattern aligned with the root package.
- Keep checks in normal test execution. Add a small invalid fixture when a custom rule's behavior is
  ambiguous, and verify that the rule rejects it before relying on the rule as a guardrail.
- When adopting rules in a codebase with existing violations, report the baseline and tighten it
  deliberately. Do not weaken the rule until it passes without documenting the boundary lost.
- Treat reflection, string-based Spring configuration, runtime bean conditions, and external
  behavior as outside the evidence ArchUnit provides.

## Verify and report

Run the project's architecture test or normal test task after changing a rule. Report the Taikai
rule or custom ArchUnit rule that failed and the dependency path it identified. Separate a real
boundary violation from an incorrect package pattern, generated code, or an intentional exception.
A passing check proves only the encoded static constraints for the analyzed classes.

## Gotchas

- Agent adds the ArchUnit core artifact beside Taikai — reuse Taikai's transitive core; declare a
  JUnit adapter only for the adapter API the project actually uses.
- Agent adopts every Taikai default without checking project conventions — select and configure the
  rules that encode intended boundaries.
- Agent imports the same package tree separately in each rule — share one imported `JavaClasses`
  value when multiple direct rules analyze the same production package.
- Agent writes package patterns that do not match the project's packages — inspect matched classes
  and use a focused fixture to prove custom rules catch a violation.
- Agent treats a passing ArchUnit test as proof of module behavior — use Spring Modulith or runtime
  tests for those guarantees.
- Agent bans all cross-feature dependencies — model the intended public API or event boundary.

## Official sources

- [Taikai documentation](https://enofex.github.io/taikai/documentation/)
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)
- [HelpDesk `ArchUnitTests`](https://github.com/sivaprasadreddy/HelpDesk/blob/main/src/test/java/dev/sivalabs/helpdesk/ArchUnitTests.java)
- [HelpDesk `ModularityTests`](https://github.com/sivaprasadreddy/HelpDesk/blob/main/src/test/java/dev/sivalabs/helpdesk/ModularityTests.java)
- [HelpDesk test dependencies](https://github.com/sivaprasadreddy/HelpDesk/blob/main/pom.xml)
