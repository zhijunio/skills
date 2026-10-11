---
name: java-developer
description: Generates modern Java code and provides architectural guidance for Java 8 through 25. Trigger when writing or modernising Java, or for best practices on immutability, records, sealed types, pattern matching, switch expressions, Optional vs null, JSpecify nullness annotations, var, text blocks, instance main methods, unnamed variables, module imports, Javadoc and Markdown doc comments, exceptions and resource management, JPMS modules, bean generation, data-oriented programming, virtual threads and carrier pinning, structured concurrency with StructuredTaskScope and Joiner, ScopedValue versus ThreadLocal, static analysis and formatting with javac lint, Error Prone, NullAway and google-java-format, Google Java Style, or version migration.
license: MIT
metadata:
  author: Avinay Basnet
  version: '1.0'
---

# Java Developer Guidelines

Covers Java 8 through 25. Use this skill for language and standard-library guidance; route JVM performance diagnosis and tuning to [java-performance-developer](../java-performance-developer/SKILL.md).

## Workflow

1. **Establish the target Java release and build tool.** Read Maven or Gradle configuration, version files, and CI. The build target is authoritative; `java -version` tells you which JDK runs locally. If the installed JDK is older than the target, report the mismatch.
2. **Load only the relevant references below.** Check their version notes and gotchas before applying a rule. The complete feature-floor table is in [java-versions.md](references/java-versions.md).
3. **Follow the project's existing conventions.** Apply the defaults in this skill where they fit; when the codebase differs, match its style and explain a meaningful conflict. A newer feature is not automatically a better fit.
4. **Use tools to enforce rules where possible.** Check [enforcement.md](references/enforcement.md) before recommending build or static-analysis changes.
5. **Compile the changes and run the project's tests** with its build tool. Use the repository's documented commands.

The version floors in the references were checked by compiling the idioms with `javac --release`; see [java-versions.md](references/java-versions.md). For new projects, target Java 25 and use `release` or a Gradle toolchain. For migration, use OpenRewrite recipes as a starting point and review the resulting diff.

## Choose a topic

### Modelling data and state

- **Immutability and collections**: final fields, contained mutability, collection factories, defensive copies, and iteration-order pitfalls. Read [immutability.md](references/immutability.md) and [immutable-collections.md](references/immutable-collections.md).
- **Records, beans, and sealed types**: choose records for data, beans when framework or representation constraints require them, and sealed hierarchies for closed alternatives. Read [records.md](references/records.md), [beans-vs-records.md](references/beans-vs-records.md), and [sealed-types.md](references/sealed-types.md).
- **Data-oriented programming**: combine records, sealed types, patterns, and enums across system boundaries; this is a system-level design choice. Read [data-oriented-programming.md](references/data-oriented-programming.md).

### Nulls and control flow

- **Absence and nullness**: use `Optional` on public return types, and use nullness annotations plus a checker for nullable parameters, fields, and type uses. Read [optional-and-null.md](references/optional-and-null.md) and [nullness.md](references/nullness.md).
- **Pattern matching**: replace type checks plus casts, destructure records, and account for `null` behavior. Read [patterns.md](references/patterns.md) and [record-patterns.md](references/record-patterns.md).
- **Switch**: choose the arrow expression form where it fits, and make exhaustiveness and `null` handling explicit. Read [switch.md](references/switch.md).
- **Local inference**: use `var` consistently for locals; name the data rather than the type, and suffix `Optional` locals with `Opt`. Read [var.md](references/var.md).

### APIs, failures, and documentation

- **Exceptions and resources**: preserve causes when wrapping, restore interrupt status, model expected absence as data, and use try-with-resources. Read [exceptions-and-resources.md](references/exceptions-and-resources.md).
- **Modern APIs**: assess Stream Gatherers, virtual threads, and the Foreign Function & Memory API against the actual use case. Read [modern-apis.md](references/modern-apis.md).
- **Structured concurrency**: use the Java 25 `StructuredTaskScope` shape and `ScopedValue` with their version and preview constraints in mind. Read [structured-concurrency.md](references/structured-concurrency.md).
- **Smaller language features**: check each feature's verdict and release floor, including text blocks, unnamed variables, compact source files, and module import declarations. Read [smaller-features.md](references/smaller-features.md).
- **Javadoc**: follow the summary, tag, snippet, and Markdown-comment rules; several features depend on the Javadoc tool version rather than `--release`. Read [javadoc.md](references/javadoc.md).
- **JPMS**: understand module-path enforcement and publication costs before modularising an application. Read [modules.md](references/modules.md).

### Style, enforcement, and review

- **Enforcement**: map rules to `javac -Xlint`, Error Prone, NullAway, and formatters; note which checks require explicit configuration. Read [enforcement.md](references/enforcement.md).
- **Google Java Style differences**: use the Google guide as the baseline and consult the documented exceptions. Read [google-style-deltas.md](references/google-style-deltas.md).
- **Review checklist**: use [checklist.md](references/checklist.md) for review passes. Establish the Java release first; many items are version-gated, and the checklist is not a generation template.

## Performance boundary

This skill covers writing correct Java; it does not diagnose a slow application, choose a collector, size a heap, or recommend JVM flags. For symptoms, profiling, GC, startup, benchmarking, or measured code-level costs, use [java-performance-developer](../java-performance-developer/SKILL.md). Never recommend a JVM flag without measurement; keep safe code improvements separate from tuning decisions that need evidence.
