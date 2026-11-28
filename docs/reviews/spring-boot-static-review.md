# Spring Boot Skills: S01–S07 Static Review

Reviewed evidence revision: b520e924f02714380bc7d90e6c50668d29003640 (2026-11-28).

## Scope and evidence

The review branch retains 39 Spring Boot entry points. S01–S06 improved architecture, testing, JPA, Redis, transaction boundaries and version resolution. S07.1–S07.7 reviewed specialist guidance and selected examples; these changes are not exhaustive API or runtime verification.

- [CI #145](https://github.com/zhijunio/skills/actions/runs/46150145214): success.
- [Skill verification #110](https://github.com/zhijunio/skills/actions/runs/46150144620): all four jobs succeeded.
- [Deterministic evidence artifact](https://github.com/zhijunio/skills/actions/runs/46150144620/artifacts/15265481571): inspected revision-bound reference inventory and helper-test log; 39 helper tests passed. Artifact retention is limited.
- Package, extraction, full Codex/Claude Code installation, explicit engineering dependency group, metadata, generated catalog, context measurement and offline link checks passed at this revision.

## Review areas

| Area | Changes reviewed | Remaining evidence |
| --- | --- | --- |
| S01–S06 | Architecture choices, test fidelity, persistence, transactions, shared version resolution | Compile and execute representative local examples |
| S07.1 | Security, AI, Modulith, HTTP interfaces, MyBatis-Plus | Release-matched dependency and runtime checks |
| S07.2 | Messaging, idempotency, retry, task execution | Real broker, concurrent database and worker tests |
| S07.3 | PostgreSQL, Flyway, tenant isolation | Migration rehearsal and isolation tests |
| S07.4 | API contracts, versioning, configuration | Generator compilation, compatibility and binding tests |
| S07.5 | Observability, JVM diagnostics | Export, cardinality and isolated diagnostic tests |
| S07.6 | Container, Kubernetes and CI/CD guidance | Image build, rollout and supply-chain evidence |
| S07.7 | Scaffolding, nullness, architecture tests, mail, frontend, developer tools and build conventions | Compilation, template validation and targeted runtime checks |
| S07.8 | Catalog, resource links and reference inventory | Selected-install scenarios and runtime routing |

## Reference coupling

The inspected CI JSON reports 63 skills, 153 distinct directed inter-skill reference edges and no detected existing repository resources outside skill directories. The Spring Boot router directly references 38 sibling skills. 35 of 39 Spring Boot skills have a transitive reference closure of 39 (including themselves); the four with no outgoing references are hexagonal-architecture, jvm-diagnostics, null-safety and spring-boot-scaffolding.

This graph records simple inline Markdown links, not mandatory runtime dependencies. The shared version-resolution reference in the router contributes to graph expansion. Cross-references can be legitimate navigation and should not be eliminated merely to produce a DAG. Full collection installation provides resource availability; load only task-relevant skills. Selected installation requires explicit dependencies or an adequate documented fallback. The graph does not detect prose-only dependencies, dynamic references, external URL validity or runtime activation.

## Next verification gates

S08: classify executable templates versus illustrative fragments and anti-patterns; use pinned compatible build fixtures and record source revision, toolchain, commands and results. S09: exercise component-specific behavior using real isolated services. S10: evaluate routing and task completion with actual agents, including missing-sibling behavior and unnecessary context loading.

Do not treat Maven packaging of skill text as compilation of Java samples. Historical upstream fixtures do not prove local changes. Redistribution permissions and publication authorization remain open; do not merge or publish on the basis of this report.
