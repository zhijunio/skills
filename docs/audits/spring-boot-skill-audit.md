# Spring Boot collection audit

Status: entry-point and resource-inventory review; optimization proposals, not implementation or runtime acceptance.

Baseline: 4075d7565353a9f0919c8d2aaca6a78fa516aaf2. CI 46144252299 and Skill verification 46144251394 passed. Inventory: 39 entry points, 271 collection files, 174458 entry-point bytes. Reviewed all 39 entry points, selected rule excerpts and resource inventories; not a line-by-line validation of all 271 files. No Java example compiled, no independent agent trial executed.

## Findings

| ID | Priority | Observation | Proposed action | Evidence needed |
| --- | --- | --- | --- | --- |
| S01 | P0 | layered-architecture mandates one service per aggregate, one service call per endpoint, no manual loops, no service imports in config, and discourages SQL irrespective of project needs | Distinguish responsibility boundaries from house style; preserve existing project conventions, support use-case orchestration and legitimate SQL | Existing layered project and bounded change; SB01/SB14 plus review of examples |
| S02 | P0 | spring-data-jpa 12402 bytes, 354 lines; detailed examples inline; no references directory at baseline | Retain skill name; move modeling, relationships, queries, pagination and batch details into focused references; preserve invariants and links | SB04/SB05, compile relevant examples, matched loading and correctness |
| S03 | P0 | spring-boot-testing 13869 bytes, 306 lines; large inline examples across unit/slice/integration/security/release checks | Route by risk and test scope; preserve actual verification, negative assertions and security; move detailed examples to references | SB09/SB10/SB06/SB07, real wrapper reports |
| S04 | P0 | spring-data-redis 12392 bytes, 240 lines; caching, serialization and rate limiting inline | Preserve cache failure/transaction/serialization safety; route detailed topics by actual Redis role | Real Redis round-trip, TTL, concurrent limiter and transaction-aware cache tests |
| S05 | P0 | spring-boot-webmvc owns detailed service transactions (references/transactions.md, 11351 bytes), including non-HTTP concerns | Improve routing from non-web skills to the existing source; consider separate spring-transactions only if independently demonstrated trigger gap; no duplicated transaction text | SB06 and non-web consumer/scheduled service case |
| S06 | P0 | Router preserves existing Boot line but examples and references cover different Boot minors | Establish version-aware lookup for concrete APIs and properties; do not force upgrade; verify specific dependency combinations | SB02/SB14 and actual resolved versions; official version-specific sources |
| S07 | P1 | archunit favors Taikai defaults; multi-module-maven examples use a framework-free domain module; both can become overprescriptive | Preserve existing architecture tools/module responsibilities; defaults only for chosen designs | SB11 and existing reactor fixture |
| S08 | P1 | Several skills claim upstream executable fixture coverage; pinned upstream tree request returned 404 in Q06 | Distinguish upstream reported evidence from local observed evidence, retain provenance and independently reproduce where viable | Actual source/rights and local tests; A04 remains open |
| S09 | P1 | event-driven-messaging covers multiple brokers with Kafka-centered examples; docker covers JVM/AOT/native | Use focused references per selected broker/runtime where useful; validate real integration semantics | Broker-specific listener evidence, image/runtime tests |
| S10 | P1 | Some short entries delegate to one large guide (Maven reference 15697 bytes, Gradle 5668 bytes) | Inspect reference selection and distinguish new-project house defaults from existing project changes; do not split based on size alone | Representative build task and actual loading evidence |
| S11 | P1 | API-versioning favors built-in mappings over handwritten path prefixes | Clarify existing contract preservation and intentional migration | Existing path-versioned API regression |
| S12 | P1 | Security/multi-tenancy require actual enforced authorization and persisted state evidence | Keep existing boundaries; prioritize SB07/SB08 real fixture and no security weakening | Real filter chain, tenant-scoped persistence and denied effects |

## All entry points disposition

| Area | Skill | Recommendation |
| --- | --- | --- |
| Build | spring-boot | Refine routing/version check (S06) |
| Build | spring-boot-scaffolding | Retain; verify Initializr coordinates and approvals |
| Build | spring-boot-maven | Review defaults/reference routing (S10) |
| Build | spring-boot-gradle | Retain; respect existing DSL and build defaults (S10) |
| Build | multi-module-maven | Refine module assumptions (S07) |
| Build | mise | Retain; preserve existing tool management |
| Build | spring-boot-configuration | Retain; verify binding, overrides and secrets |
| Java | null-safety | Retain; candidate future generic Java reuse |
| Architecture | feature-architecture | Retain feature ownership |
| Architecture | layered-architecture | Refine overly absolute rules (S01) |
| Architecture | hexagonal-architecture | Retain; avoid forced ports/adapters |
| Architecture | archunit | Refine existing-tool preference (S07) |
| Architecture | spring-modulith | Retain; verify runtime event delivery separately |
| Web | spring-boot-webmvc | Clarify transaction routing (S05) |
| Web | spring-boot-api-versioning | Preserve existing API contracts (S11) |
| Web | spring-boot-openapi | Retain separate contract/code-first paths |
| Web | http-interface-clients | Verify actual minor-specific configuration (S06) |
| Web | spring-boot-frontend-integration | Retain integration scope |
| Web | spring-boot-email | Retain; verify delivery around transaction commit |
| Data | spring-data-jpa | Progressive disclosure (S02) |
| Data | spring-data-redis | Progressive disclosure (S04) |
| Data | spring-boot-mybatis-plus | Retain; avoid JPA substitution |
| Data | postgresql | Retain; candidate future generic data reuse |
| Data | flyway-migrations | Distinguish generic and PostgreSQL-specific patterns |
| Security | spring-boot-security | Retain; actual authorization tests (S12) |
| Security | multi-tenancy | Retain; actual isolation tests (S12) |
| Reliability | idempotency-patterns | Retain; real concurrency tests |
| Reliability | resilience-retry | Preserve compatible existing retry policy (S06) |
| Reliability | event-driven-messaging | Broker-specific detail (S09) |
| Reliability | spring-task-execution | Retain; context and restart semantics |
| Testing | spring-boot-testing | Progressive disclosure (S03) |
| Testing | agent-qa-browser-regression | Retain; verify installed CLI and scoped authorization |
| Operations | spring-boot-docker | Runtime-specific detail (S09) |
| Operations | spring-boot-kubernetes | Retain workload boundary |
| Operations | spring-boot-cicd | Retain opt-in release boundary |
| Operations | spring-boot-observability | Review signal-specific reference selection |
| Operations | jvm-diagnostics | Retain safety; candidate generic Java reuse |
| Operations | bootui | Retain dev-only isolation |
| AI | spring-ai | Retain progressive disclosure; verify actual dependency versions |

## Verified source spot checks

- [Framework resilience reference](https://docs.spring.io/spring-framework/reference/core/resilience.html): maxRetries counts retries beyond initial call.
- [Boot 4.1 testing](https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html): MockitoBean and TestRestTemplate availability.
- [Boot 4.2 snapshot TestRestTemplate API](https://docs.spring.io/spring-boot/4.2-SNAPSHOT/api/java/org/springframework/boot/resttestclient/TestRestTemplate.html): deprecation since 4.2.0; snapshot documentation is not evidence of resolved dependencies.
- [Boot 4.0 properties](https://docs.spring.io/spring-boot/4.0/appendix/application-properties/index.html): pageable serialization-mode property.

These checks are selective, not a full version certification. The [Spring evaluation cases](../../evals/spring-boot/README.md) remain NOT RUN. A04 provenance and A13 release authentication remain open.

## Implementation sequence proposed

1. S01 bounded instruction corrections and related examples; preserve skill identity.
2. S03, S02, S04 sequential progressive-disclosure refactors; measure before/after static files, retain applicable notices, verify links and no loss of core instructions. Actual efficiency remains unverified until matched runs.
3. S05/S06 routing and version policy with targeted technical cases.
4. S07/S09/S10/S11 incremental refinements after source and fixture review.
5. Real Spring fixture pilot and evidence-based acceptance.

No skill merge, extraction, rename, new collection, imported code, or publication authorized by this audit. Review material reorganizations and source/rights before implementation.

## S01 implementation

Revised layered-architecture to distinguish application orchestration, domain invariants, HTTP and data access; preserve existing conventions, permit appropriate SQL and mapping styles, and route transaction, retry and configuration details to existing owners. Removed mandatory service count and aggregate mapping, mandatory Lombok/streams, and absolute configuration restrictions. Updated illustrative service and mixed-boundary examples; positive example maps response within the application transaction and documents the same-transaction assumption for inventory. Removed unused mapper import. Entry description and host metadata unchanged; license retained. Entry size: 7093 to 6307 Unicode code units (not tokens). SB01/SB14 scope assertions extended; actual behavior and Java compilation NOT RUN, new CI pending. No other skill modified.

## S03 finding / second optimization batch: spring-boot-testing

S01 CI 46144578355 and Skill verification 46144577741 passed at 2651fce. Moved detailed testing sections into six task-specific references and kept risk-based selection, version checks, security, real dependency semantics and evidence boundaries in the entry. Original description, skill identity, host metadata and license retained. Clarified illustrative fragments, valid inventory-failure input, no-save assertion, MVC security scope, real-HTTP cleanup, Testcontainers version awareness and PASS/FAIL/NOT RUN with blockers separate. Updated SB06/SB07/SB09/SB10 assertions. Entry changed from 1 to 1 split lines; static size is not token evidence. Java compilation and actual behavior/efficiency NOT RUN; CI pending.
