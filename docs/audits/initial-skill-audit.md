# Initial Skill Audit

Status: Static baseline completed; conformance and runtime verification pending  
Baseline: [4a88580](https://github.com/zhijunio/skills/tree/4a88580d704630aff939e5c986c144d97856df60)  
Reviewed: 2026-11-28  
Plan: [Phase 01](../plans/phase-01-repository-foundations.md)

## Method and limitations

Read all 59 SKILL.md files at the pinned revision. Extract top-level metadata with a lightweight text scan, compare names to directories, inspect descriptions for responsibility boundaries, and compare simple relative Markdown link destinations to the recursive Git tree. Inspect collection indexes, license paths, representative invocation policies and core workflow contracts.

This is not a complete YAML parser or Markdown parser, nor an execution of skills-ref. Reference files, scripts, external links, technical API claims, upstream licenses and actual agent behavior have not been exhaustively verified. Retain means retain provisionally, not certified correct. All entries share these limitations unless noted. No skill changes are made by this audit.

## Summary

| Collection | Reviewed | Name matches directory | Manual-only frontmatter |
| --- | ---: | ---: | ---: |
| Engineering | 13 | 13 | 6 |
| Spring Boot | 39 | 39 | 0 |
| Collected | 7 | 7 | 4 |
| Total | 59 | 59 | 10 |

All 59 have a recognizable name and description; no duplicate names were observed. One missing destination was identified by the simple inline-link scan. Nine of the ten manual-only entries have an agents/openai.yaml file with allow_implicit_invocation: false, verified by reading those nine files. The remaining entry, show-me, has no such file in the tree. Two skills use argument-hint. No complete schema conformance or behavioral pass is claimed.

## Findings

| ID | Priority | Evidence | Recommended action |
| --- | --- | --- | --- |
| A01 | P1 | spring-boot-skills/multi-module-maven/SKILL.md links to ../../spring-boot-4/versions.json, absent from tree | Identify authoritative replacement version data or remove stale dependency; verify templates |
| A02 | P1 | collected-skills/show-me declares disable-model-invocation: true but lacks agents/openai.yaml | Confirm intended policy and align Codex metadata if manual-only is required |
| A03 | P1 | Spring Boot entry routes to sibling skills; single-skill installation may omit dependencies | Establish dependency closure and test installation in F09 |
| A04 | P1 | Collection indexes contain upstream links but do not map every skill to a pinned source and license; tree contains root LICENSE and spring-boot-skills/LICENSE | Establish per-skill provenance and verify redistribution rights; do not infer license solely from root LICENSE |
| A05 | P1 | Maven plugin 0.0.7 scans configured repository root; nested coverage untested | Compare source, discovery, packaged and extracted inventories in F09 |
| A06 | P1 | web-access description requires all networking to use that skill | Narrow activation and respect host capabilities, user consent and security boundaries; inspect dependencies |
| A07 | P2 | create-spec describes detailed use-case entries in requirements.md while impl-spec treats it as a concise status index and forbids copying behavior details into it | Reconcile canonical artifact contract before expanding SDD |
| A08 | P2 | progen, project-foundation and spring-boot-scaffolding cover overlapping project initialization decisions | Define routing and delegation; avoid duplicate generators and conflicting defaults |
| A09 | P2 | domain-modeling, grill-with-docs, grilling and product-brainstorming overlap in interviews and documentation | Clarify when each applies; compose rather than merge prematurely |
| A10 | P2 | spring-boot, feature/layered/hexagonal architecture, modulith and ArchUnit have intersecting triggers | Preserve router and focused ownership; test representative routing |
| A11 | P2 | spring-data-jpa (354 lines) and spring-boot-testing (304 lines) are relatively large entry points | Review progressive disclosure with real usage evidence; length alone is not a defect |
| A12 | P1 | Platform extensions and agent-specific policy are not portable schema fields | Audit with selected validator; preserve behavior and document export policy |

Priorities are proposed: P1 near-term correctness or distribution risk; P2 maintainability and workflow refinement. No security vulnerability or runtime failure is inferred from static evidence alone.

## Per-skill inventory

Paths are relative to the repository root. M = explicit-only intent declared in frontmatter; A = no such declaration observed (not proof of implicit activation). C = Codex manual policy confirmed; N = not applicable or not checked; missing = no Codex policy file found for a manual skill. All entries require source/license and technical/runtime verification, including the Spring Boot collection whose index identifies upstream revision b90c3db. The relative links navigate the current repository; the baseline commit above preserves the audited revision.

### Collected skills

| Skill | Responsibility | Activation / Codex | Disposition / finding |
| --- | --- | --- | --- |
| [eli5](../../collected-skills/eli5/SKILL.md) | Simple explanation | A / N | Retain; distinguish from teach and show-me |
| [handoff](../../collected-skills/handoff/SKILL.md) | Agent handoff | M / C | Retain; argument-hint portability |
| [retro](../../collected-skills/retro/SKILL.md) | Session retrospective | M / C | Retain |
| [show-me](../../collected-skills/show-me/SKILL.md) | Visual explanation | M / missing | Improve A02 |
| [teach](../../collected-skills/teach/SKILL.md) | Guided learning | M / C | Retain; argument-hint portability |
| [web-access](../../collected-skills/web-access/SKILL.md) | Network/browser access | A / N | Improve A06; license and metadata present, verify values and tools |
| [writing-for-agents](../../collected-skills/writing-for-agents/SKILL.md) | Agent-facing writing | A / N | Retain; reuse in authoring guide |

### Engineering

| Skill | Responsibility | Activation / Codex | Disposition / finding |
| --- | --- | --- | --- |
| [create-spec](../../engineering/behavior-specification/SKILL.md) | Behavior and acceptance specification | M / C | Improve A07 |
| [diagnosing-bugs](../../engineering/diagnosing-bugs/SKILL.md) | Evidence-led diagnosis | A / N | Retain; delegate JVM diagnosis when relevant |
| [domain-modeling](../../engineering/domain-modeling/SKILL.md) | Glossary and domain decisions | A / N | Improve routing A09 |
| [grill-with-docs](../../engineering/grill-with-docs/SKILL.md) | Interview with ADR/glossary updates | M / C | Improve routing A09 |
| [grilling](../../engineering/grilling/SKILL.md) | Challenge assumptions | A / N | Improve routing A09 |
| [html-plan](../../engineering/html-plan/SKILL.md) | Interactive implementation plan | A / N | Retain; distinguish behavior spec from plan |
| [impl-spec](../../engineering/implement-use-case/SKILL.md) | Implement one approved use case | M / C | Improve A07 |
| [java-code-review](../../engineering/java-code-review/SKILL.md) | Java review | A / N | Retain; compose with focused technical skills |
| [prd-to-requirements](../../engineering/prd-to-use-cases/SKILL.md) | PRD to use-case index | M / C | Retain; reconcile contract A07 |
| [product-brainstorming](../../engineering/product-requirements/SKILL.md) | Product discovery and PRD | A / N | Improve routing A09 |
| [progen](../../engineering/progen/SKILL.md) | Spring Boot generator | M / C | Investigate A08 and generator availability |
| [project-foundation](../../engineering/project-foundation/SKILL.md) | Technical choices and baseline | M / C | Improve routing A08 |
| [prototype](../../engineering/prototype/SKILL.md) | Throwaway design experiment | A / N | Retain; distinguish from production implementation |

### Spring Boot

All entries below have A / N activation status as defined above.

| Skill | Responsibility | Disposition / finding |
| --- | --- | --- |
| [agent-qa-browser-regression](../../spring-boot-skills/agent-qa-browser-regression/SKILL.md) | Browser journey verification | Investigate external runner and evidence provenance |
| [archunit](../../spring-boot-skills/archunit/SKILL.md) | Static architecture tests | Retain; A10 |
| [bootui](../../spring-boot-skills/bootui/SKILL.md) | Local diagnostic console | Retain; verify production exclusion |
| [event-driven-messaging](../../spring-boot-skills/event-driven-messaging/SKILL.md) | Broker contracts and delivery | Retain; compose with idempotency |
| [feature-architecture](../../spring-boot-skills/feature-architecture/SKILL.md) | Feature package boundaries | Retain; A10 |
| [flyway-migrations](../../spring-boot-skills/flyway-migrations/SKILL.md) | Schema evolution | Retain |
| [hexagonal-architecture](../../spring-boot-skills/hexagonal-architecture/SKILL.md) | Ports and adapters | Retain; A10 |
| [http-interface-clients](../../spring-boot-skills/http-interface-clients/SKILL.md) | Typed outbound HTTP | Retain |
| [idempotency-patterns](../../spring-boot-skills/idempotency-patterns/SKILL.md) | Duplicate-effect prevention | Retain |
| [jvm-diagnostics](../../spring-boot-skills/jvm-diagnostics/SKILL.md) | Runtime evidence and diagnosis | Retain |
| [layered-architecture](../../spring-boot-skills/layered-architecture/SKILL.md) | Controller/service/data boundaries | Retain; A10 |
| [mise](../../spring-boot-skills/mise/SKILL.md) | Tool versions and tasks | Retain; avoid unrequested tool migration |
| [multi-module-maven](../../spring-boot-skills/multi-module-maven/SKILL.md) | Maven reactor structure | Improve A01 |
| [multi-tenancy](../../spring-boot-skills/multi-tenancy/SKILL.md) | Tenant isolation | Retain; verify security and data contracts |
| [null-safety](../../spring-boot-skills/null-safety/SKILL.md) | Nullability contracts | Retain |
| [postgresql](../../spring-boot-skills/postgresql/SKILL.md) | Application database practices | Retain |
| [resilience-retry](../../spring-boot-skills/resilience-retry/SKILL.md) | Bounded retries and concurrency | Retain; compose with idempotency |
| [spring-ai](../../spring-boot-skills/spring-ai/SKILL.md) | AI integration | Retain; verify version-sensitive APIs |
| [spring-boot-api-versioning](../../spring-boot-skills/spring-boot-api-versioning/SKILL.md) | API version resolution | Retain |
| [spring-boot-cicd](../../spring-boot-skills/spring-boot-cicd/SKILL.md) | Build/release automation | Retain |
| [spring-boot-configuration](../../spring-boot-skills/spring-boot-configuration/SKILL.md) | Typed configuration and profiles | Retain |
| [spring-boot-docker](../../spring-boot-skills/spring-boot-docker/SKILL.md) | Containers and images | Retain |
| [spring-boot-email](../../spring-boot-skills/spring-boot-email/SKILL.md) | Email sending | Retain |
| [spring-boot-frontend-integration](../../spring-boot-skills/spring-boot-frontend-integration/SKILL.md) | Frontend/backend integration | Retain |
| [spring-boot-gradle](../../spring-boot-skills/spring-boot-gradle/SKILL.md) | Gradle builds | Retain |
| [spring-boot-kubernetes](../../spring-boot-skills/spring-boot-kubernetes/SKILL.md) | Kubernetes deployment | Retain |
| [spring-boot-maven](../../spring-boot-skills/spring-boot-maven/SKILL.md) | Maven builds | Retain; delegate reactor structure |
| [spring-boot-mybatis-plus](../../spring-boot-skills/spring-boot-mybatis-plus/SKILL.md) | MyBatis-Plus persistence | Retain |
| [spring-boot-observability](../../spring-boot-skills/spring-boot-observability/SKILL.md) | Service telemetry | Retain |
| [spring-boot-openapi](../../spring-boot-skills/spring-boot-openapi/SKILL.md) | API contract documentation | Retain |
| [spring-boot-scaffolding](../../spring-boot-skills/spring-boot-scaffolding/SKILL.md) | Initializr-based skeleton | Improve routing A08 |
| [spring-boot-security](../../spring-boot-skills/spring-boot-security/SKILL.md) | Servlet API security | Retain; verify version-sensitive controls |
| [spring-boot-testing](../../spring-boot-skills/spring-boot-testing/SKILL.md) | Application verification | Review A11 |
| [spring-boot-webmvc](../../spring-boot-skills/spring-boot-webmvc/SKILL.md) | HTTP runtime and service transactions | Retain |
| [spring-boot](../../spring-boot-skills/spring-boot/SKILL.md) | Focused skill router | Improve distribution A03 and routing A10 |
| [spring-data-jpa](../../spring-boot-skills/spring-data-jpa/SKILL.md) | JPA persistence | Review A11 |
| [spring-data-redis](../../spring-boot-skills/spring-data-redis/SKILL.md) | Redis integration | Retain |
| [spring-modulith](../../spring-boot-skills/spring-modulith/SKILL.md) | Modular monolith | Retain; A10 |
| [spring-task-execution](../../spring-boot-skills/spring-task-execution/SKILL.md) | Async and scheduled work | Retain |

## Follow-up

1. F03/F04: establish scope, provenance, reuse and portability rules from A04/A06/A07/A12.
2. F07: generate the catalog from actual metadata, not this manually maintained audit.
3. F08: reconcile artifact contracts and routing (A07–A10).
4. F09: test dependency closure, manual activation, packaging and extraction (A02/A03/A05).
5. F10: run real schema and link validators, including supporting resources and negative fixtures (A01/A12).
6. Before declaring F06 fully accepted, complete per-skill provenance and license mapping, inspect supporting scripts and references, and record technical verification gaps explicitly. Do not mark unresolved verification as passed.

No external tools were installed, no scripts or builds run, and no existing skills modified.
