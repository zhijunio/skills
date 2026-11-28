# Engineering Capability Audit (E01)

Status: static contract review; proposed dispositions, not approved migrations.
Scope: the 13 engineering skills and relevant routing/testing skills. Evidence: repository entry points reviewed through the Phase 01 workflow and subsequent E01 source inspection. See [workflow](../engineering-workflow.md), [initial audit](initial-skill-audit.md) and [reuse research](../research/engineering-skill-reuse.md).

## Responsibility matrix

| Existing skill | Trigger / input | Owned output | Approval, dependency or failure boundary | Proposed disposition |
| --- | --- | --- | --- | --- |
| product-brainstorming | Product idea, MVP scope | Confirmed product summary and draft PRD | Summary approval permits drafting, not automatic PRD approval; preserve existing PRD paths | Adapt; separate exploration and approval semantics before deciding whether to split |
| prd-to-requirements | Authoritative PRD | Use-case index | Explicit invocation; preserve IDs/status; reconcile removals and ambiguity | Refactor; consider prd-to-use-cases for precise naming |
| project-foundation | Product constraints and target repo | Stack record and verified skeleton | Separate stack/initializer approval; exactly one generator; stop at baseline | Refactor decision and execution boundaries; split only if independently useful |
| create-spec | Feature or one UC ID | Behavior spec; new index row if required | One question per turn; human confirms behavior; reconcile existing conflicts | Refactor; candidate behavior-specification |
| impl-spec | One approved UC | Code, tests and synchronized status | Explicit invocation; blocked dependencies and failures must not become DONE | Refactor; candidate implement-use-case |
| domain-modeling | Domain terminology and significant decisions | Glossary and ADR | Confirm material decisions; no product scope ownership | Retain; avoid duplicate architectural authority |
| grilling | Requested decision stress test | Resolved decision tree | No file writes unless authorized; frontier rounds distinct from create-spec interview | Retain |
| grill-with-docs | Explicit request for documented interview | Grilling plus glossary/ADR updates | Requires both installed dependencies; report missing skill | Compose; retain unless evidence warrants consolidation |
| prototype | Bounded design question | Disposable experiment and verdict | Do not treat prototype as production; authorized scratch scope | Retain |
| html-plan | Planning request or explicit HTML | Interactive review artifact | Wait for user response; runtime packer and supporting references | Refactor activation to HTML review use; generic plan remains separate candidate |
| diagnosing-bugs | Hard defect/regression | Reproduction, root cause, correction and regression evidence | Stop when red-capable loop unavailable; redact secrets | Retain |
| java-code-review | Java diff or requested scope | Review report | No unauthorized fix or overwrite; report validation limits | Retain language-specific review |
| progen | Explicit generator selection | Approved project skeleton | Verify capabilities; do not switch or overwrite silently | Retain specialized generator |

Relevant adjacent owners: spring-boot router (select technical guidance), spring-boot-scaffolding (Initializr), spring-boot-testing (Spring test implementation and readiness), spring-boot-cicd (requested CI configuration), handoff (session context), and the feature/layered/hexagonal/Modulith/ArchUnit skills (framework-specific architecture). They do not own product intent or general system design.

## Concrete gaps and proposed contracts

| Gap | Evidence / example | Proposed ownership and disposition |
| --- | --- | --- |
| G01 PRD lifecycle | product-brainstorming writes draft/pending PRD after summary confirmation; conversion does not explicitly gate approval | E02/E03: adapt current product owner; distinguish drafting, approval and change; do not introduce duplicate PRD |
| G02 Use-case naming and lifecycle | prd-to-requirements outputs index; create-spec owns behavior; impl-spec has no complete DONE reopening protocol | E02: align roles and status transitions; propose renames after semantics settled |
| G03 System design | Foundation chooses stack and generates baseline; domain-modeling owns vocabulary and ADR; Spring architecture skills cover implementation patterns | E04: assess standalone system-design contract for boundaries, integrations, data ownership, quality attributes and alternatives; use existing ADRs |
| G04 Planning | html-plan triggers on generic multi-file planning but requires interactive HTML, STE and runtime; impl-spec decomposes only during implementation | E05: adapt format-neutral implementation planning; narrow HTML skill to presentation/review when appropriate |
| G05 Test strategy | Spring Boot testing includes risk-based test mix, slice/integration examples and readiness; impl-spec verifies acceptance; diagnosing-bugs establishes feedback loops | E06: general risk-to-evidence planning contract, compose stack-specific test guidance and optional TDD; do not duplicate test execution |
| G06 Generator routing | spring-boot router introduces scaffolding for new projects even when generator choice unresolved, later says exactly one | E02/E04: clarify routing before initializer selection; foundation remains decision owner |
| G07 Cross-artifact validation | SDD skills stop on conflicts individually; no dedicated read-only coverage review across PRD/index/spec/plan/test evidence | E02/E05: adapt consistency checklist before adding standalone analyzer |
| G08 Review and completion | Java review is language-specific, Spring testing readiness is framework-specific | Defer broad review/release capabilities unless E07 exposes material gap |

## Proposed target architecture

Product discovery and product requirements establish scope; use-case decomposition establishes stable identity. System design and domain modeling resolve different design questions. Foundation establishes a runnable technical baseline when necessary. Behavior specification owns observable feature rules. Implementation planning owns execution decomposition; test strategy owns risk and evidence selection. Implementation owns code and verified status, diagnosis owns defect feedback, review owns findings. These are capability boundaries, not a mandatory sequence or a commitment to one new skill per row.

## Migration policy

Candidate names (prd-to-use-cases, behavior-specification, implement-use-case) are proposals only. Determine semantic contracts first, then check all callers, paths, frontmatter, invocation config, catalog, provenance and docs. Keep applicable notices. The maintainer explicitly permits deletion of unsuitable skills. Remove only with a concrete rationale and dependency/coverage assessment; preserve historical provenance and applicable notices for retained material. Material merges and replacements remain subject to review. No skill is selected for immediate deletion solely by this policy. Avoid permanent aliases unless justified by actual consumers.

## Next steps

E02 must settle product approval and reopening semantics before changing status behavior or renaming the three SDD skills. E03–E06 use the research decisions and inspect remaining alternatives before implementation. No behavioral exercises have been executed; this audit does not close A07–A10.
