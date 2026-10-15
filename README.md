# Agent Skills

A collection of Agent Skills for agentic coding tools, covering Java, PostgreSQL, Spring Boot, Spring Cloud, SSO, Java performance, and CI/CD, alongside an SDD workflow and reusable engineering prompts. Spring Boot references include frontend integration guidance where it belongs, and runnable examples live under `samples/`. Every skill has a `SKILL.md` entry point. Skills with larger guidance sets use focused reference files; Spring Cloud currently provides a version-aware entry point, with component references added incrementally. Version-sensitive guidance states its supported versions, and focused guides record common implementation mistakes.

## Available skills

| Skill | What it does |
| ----- | ------------ |
| [`postgresql-developer`](postgresql-developer/SKILL.md) | Designs PostgreSQL schemas from requirements, makes database architecture decisions, writes SQL, and diagnoses slow queries, for **PostgreSQL 14-18**. Covers schema design from a spec, multi-tenancy (shared schema + RLS, schema-per-tenant, database-per-tenant), data types and keys, constraints, SQL authoring (joins, aggregation, window functions, CTEs, upserts, keyset pagination, full-text search, JSONB), indexing and `EXPLAIN`, statistics and the planner, transactions and isolation, locking, MVCC and vacuum, lock-aware migrations, roles and row-level security, connection pooling, configuration, backups, replication, and testing with Testcontainers. Pairs with [`java-developer`](java-developer/SKILL.md) and [`spring-boot-developer`](spring-boot-developer/SKILL.md). |
| [`java-developer`](java-developer/SKILL.md) | Generates modern Java code and provides architectural guidance for Java 8-27: immutability, records, sealed types, pattern matching, `switch` expressions, `Optional` vs `null` **plus JSpecify nullness annotations**, `var`, text blocks, Javadoc (`///`, `{@return}`, `{@snippet}`), exceptions and resource management, JPMS modules, bean generation, data-oriented programming, **structured concurrency and scoped values** (the Java 25 `StructuredTaskScope` preview shape, later preview API changes through 27, `ScopedValue` against `ThreadLocal`, and the measured cost of `synchronized` pinning before Java 24), and version migration. Calls out preview/incubator stability and migration risks. Carries an **enforcement** map from every rule to the `javac -Xlint` key, Error Prone check or formatter that actually checks it, and records every departure from the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) in [google-style-deltas.md](java-developer/references/google-style-deltas.md). Java 8-25 feature floors were compiled at each release; Java 26/27 coverage is sourced from release notes and spot-checked on JDK 27, not exhaustively compiled. |
| [`spring-boot-developer`](spring-boot-developer/SKILL.md) | Creates and maintains Spring Boot 4.x and 3.5.x applications: project generation, architecture, REST APIs, persistence, security, testing, integrations, observability, frontend applications, optional local BootUI diagnostics, Docker, GraalVM, and Azure deployment. Includes a version-aware reference library, environment checker, generator, and full-stack workshop. Pairs with [`java-developer`](java-developer/SKILL.md), [`spring-cloud-developer`](spring-cloud-developer/SKILL.md), and [`spring-sso-developer`](spring-sso-developer/SKILL.md). |
| [`spring-cloud-developer`](spring-cloud-developer/SKILL.md) | Guides Spring Cloud version selection and distributed-system design. Includes references for Boot/Cloud compatibility and support windows, Gateway, Config, service discovery, client-side load balancing, and resilience. Spring Cloud Stream remains an indexed topic pending a focused guide. |
| [`spring-sso-developer`](spring-sso-developer/SKILL.md) | Implements **Single Sign-On** in Java and Spring, for internal organisational SSO and for SSO-as-a-feature in multi-tenant SaaS. Spring Security as an **OIDC/OAuth2 client and resource server**, **SAML 2.0** service provider, **Spring Authorization Server**, and **SCIM provisioning** from RFC 7644. A full attack catalogue split by protocol - PKCE, `state`, `nonce`, redirect-URI matching, algorithm confusion, mix-up, XML Signature Wrapping, XXE, assertion replay, session fixation, logout propagation, deprovisioning and multi-tenant isolation - cross-checked against **OWASP ASVS 5.0**. Plus **13 provider files** (Entra ID, Okta, Keycloak, Auth0, Google Workspace, Cognito, AD FS, Ping, OneLogin, Shibboleth, WorkOS, Entra External ID and a procedure for the rest), each verified against that vendor's own documentation, indexed by cross-vendor tables showing **where role information actually lives**, which issuer strings surprise you, and what to use as a primary key. Also **SCIM provisioning** from RFC 7644, **frontend integration** for SPA-plus-BFF shapes, and both logout directions including **OIDC back-channel logout**. Covers **Boot 4.x with Spring Security 7 and Boot 3.5.x with 6.5**. As of this repository's Boot 4.1.1 baseline, Boot manages Security 7.1.1; implementation-level checks in the references are pinned to exact source versions and call out when to re-check. Spring identifiers and defaults use the Spring Security 7.1.0 reference or source at its version tag; the guides call out verification limits where they affect the recommendation. Targeted Spring CVEs and their affected feature configurations are documented by component; check resolved dependencies against current vendor advisories. The skill was then put through an **independent adversarial review**, whose 15 findings were each re-verified against primary sources before being applied; the 106-rule review checklist and the ASVS V9 and V10 cross-check came out of that pass. |
| [`java-performance-developer`](java-performance-developer/SKILL.md) | Diagnoses and fixes Java performance problems, and writes Java that does not create them, for **Java 8 through 25**. Built as **triage-first**: a symptom-to-cause decision tree routes from what the user observed to the measurement that identifies the cause, and only then to the fix. Covers benchmarking and the statistics of regression testing, the tool surface (`jcmd`, `jstat`, JFR, JMC, async-profiler, NMT, MAT) including the **safepoint bias** that makes some profilers systematically wrong, all six collectors with a per-version availability matrix, heap and G1 tuning, the JIT and code cache, startup via CDS and the **Java 24/25 AOT cache**, native memory and containers, then the code-level half - allocation and object layout, object lifecycle and indefinite references, heap analysis and the full out-of-memory taxonomy, collections, strings, buffered I/O and serialization, exceptions and logging, lambdas and streams, and concurrency. Owns **proving** where the bottleneck is for database and server layers, then defers to [`postgresql-developer`](postgresql-developer/SKILL.md) and [`spring-boot-developer`](spring-boot-developer/SKILL.md). Its [checklist](java-performance-developer/references/checklist.md) is deliberately **split into apply-blind and measure-first halves**, because a performance audit that recommends a flag with no measurement behind it is the failure mode the skill exists to prevent. **Version-specific behaviour was verified by running it** on Temurin 8u504, 11.0.32.1, 17.0.20.1, 21.0.12 and 25.0.4.1: every flag's existence, default and removal behaviour - and crucially whether a removed flag merely warns or **stops the JVM starting**, which most do. The flag tables keep *accepted* and *effective* in separate columns, because launching a JVM proves only the first; container and cgroup flags are marked as **cited rather than measured**, since they do not exist on the Windows hosts used. Timings are **JMH with a control benchmark**: buffering unbuffered I/O at **143x read and 763x write**, a buffer above a deflater at **2.7x where the same buffer below it does nothing**, `HashMap` pre-sizing at **2.3x at 100,000 entries** while `ArrayList` pre-sizing is **not measurable at that size**, loop concatenation as a **quadratic curve** (1.3x at 10 iterations, **320x at 10,000**) rather than one multiplier, uncontended `synchronized` at **16.7ns against a 1.3ns floor**, a throw at **1,300ns from one frame down and 6,716ns from sixty** with stack-trace capture scaling **sublinearly**, object headers at **16 to 8 bytes** under Java 25 compact headers, and - re-measured with a **paired, interleaved** harness after the first method proved to be the noise source - `-Xshare:off` at **~32% of startup** and the AOT cache at **~18% on top of CDS**, both reproducing across independent passes to within half a point. Derived from both editions of Scott Oaks' *Java Performance*, which turn out to be **one book twice** - so the 1st edition is version-scoped to the Java 8 column rather than discarded. [book-deltas.md](java-performance-developer/references/book-deltas.md) records every departure, **three errata found by running the book's own flags**, and - after an adversarial audit - the figures this skill **withdrew from its own earlier drafts**, including a stream-versus-loop result that re-measurement showed was noise. |
| [`ci-cd-developer`](ci-cd-developer/SKILL.md) | Designs, reviews and fixes **CI/CD pipelines** and the release path to production, across **GitHub Actions, Azure DevOps and GitLab CI**. Structured so the reasoning is written once and translated: a platform-neutral spine (pipeline design, gates, promotion, trunk-based delivery) plus one reference per platform and a **translation matrix** mapping every concept to its spelling on each. Six deployment targets each answer the same four questions - how an artifact becomes running code, how config is injected, what the rollback primitive is, what the zero-downtime primitive is: **Kubernetes**, **Azure App Service and Container Apps**, **AWS ECS and Fargate**, **VM and on-prem**, **Cloudflare Workers** and **Cloudflare Containers**. The security half covers action **SHA pinning** and script injection, **OIDC federation** so no cloud key is stored, **SBOM** (CycloneDX/ECMA-424 and SPDX/ISO-IEC-5962), **SLSA provenance and signing**, dependency updates and scanner placement. Verified by running it: Maven reproducibility proven **with a control** (identical hashes with `project.build.outputTimestamp`, two different hashes without), syft measured emitting **CycloneDX 1.7 but SPDX 2.3 by default** while the standard is 3.0.1, `cosign` v3 offline signing with a **tampering control** plus three v2 recipes that now fail, and action tags resolved to SHAs (`actions/checkout` is at **v7**, not the v3 every tutorial shows). Its [checklist](ci-cd-developer/references/checklist.md) splits **adopt-anywhere** from **depends-on-context**, and [book-deltas.md](ci-cd-developer/references/book-deltas.md) records what could not be verified - every cloud target is cited rather than measured, and no DORA figures are quoted because the source was not available. |

## SDD workflow skills

Use these skills for a spec-driven delivery flow. The main path is `sdd-spec` → `sdd-plan` → `sdd-build` → `sdd-review`; use `sdd-improve` separately for a read-only, broad-scope health scan.

| Skill | Purpose |
| ----- | ------- |
| [`sdd-spec`](spec-skills/sdd-spec/SKILL.md) | Define scope, requirements, and testable acceptance criteria. |
| [`sdd-plan`](spec-skills/sdd-plan/SKILL.md) | Turn an approved spec into verifiable vertical slices. |
| [`sdd-build`](spec-skills/sdd-build/SKILL.md) | Implement an approved plan test-first, or fix listed review findings. |
| [`sdd-review`](spec-skills/sdd-review/SKILL.md) | Review a fixed increment against documented standards and its spec. |
| [`sdd-improve`](spec-skills/sdd-improve/SKILL.md) | Find improvement candidates through a read-only repository scan. |

The workflow includes [spec and plan templates](spec-skills/) and shared [review baselines](spec-skills/sdd-review/references/).

## Runnable samples

The [`samples/`](samples/README.md) directory catalogs runnable Spring Boot examples and [planned Spring Security samples](samples/ROADMAP.md).

## Reusable prompts

Prompts for common tasks are in [`docs/prompts/`](docs/prompts/): [create or revise `AGENTS.md`](docs/prompts/create-agentsmd.prompt.md), [create or revise a `README.md`](docs/prompts/create-readme.prompt.md), [explain code](docs/prompts/explain-code.prompt.md), [prepare a contributor onboarding plan](docs/prompts/onboarding-plan.prompt.md), and [review code](docs/prompts/review-code.prompt.md).

## Using these skills

Agent Skills are designed to be used with agentic coding tools like Claude Code, Codex, Gemini CLI, Antigravity and more. Activating a skill loads the specific instructions and resources needed for that task.

To use these skills in your own environment you may follow the instructions for your specific tool or use a community tool like [skills.sh](https://skills.sh).

## SkillsJars packaging

This repository can also package its top-level skills into a Maven JAR with [SkillsJars](https://skillsjars.com). The build scans immediate child directories of the repository root and packages each directory that contains a `SKILL.md`; nested skills under `spec-skills/` are not included by this configuration and should be installed from their directories separately.

```bash
./mvnw clean verify
jar tf target/skills-0.1.0-SNAPSHOT.jar | grep '^META-INF/skills/'
```

To consume an installed snapshot from another Maven project, first install it locally:

```bash
./mvnw install
```

Add the artifact as a dependency in the consuming project:

```xml
<dependency>
  <groupId>io.github.zhijunio</groupId>
  <artifactId>skills</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Then extract it into a project-local Codex skills directory:

```bash
mvn com.skillsjars:maven-plugin:extract \
  -Ddir=.codex/skills \
  -DuseSkillsNameAsDirectory=true
```

The artifact is a packaging format; it does not replace the existing skills CLI or direct-copy installation options. SkillsJars packaging is documented here; publishing coordinates to Maven Central requires a separate release configuration and namespace verification.

<details open>
<summary><b>pnpm</b></summary>

```bash
pnpm dlx skills add https://github.com/zhijunio/skills
```

</details>

<details open>
<summary><b>npm</b></summary>

```bash
npx skills add https://github.com/zhijunio/skills
```

</details>

## License

[MIT](LICENSE) © 2026 Zhijunio.
