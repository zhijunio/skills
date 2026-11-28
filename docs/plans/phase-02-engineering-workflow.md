# Phase 02 — Engineering Workflow

Status: E01 in progress on review branch; static audit and initial reuse research documented.
Prerequisites: [Phase 01 review](../reviews/phase-01-review.md), [engineering workflow](../engineering-workflow.md), [design principles](../skill-design-principles.md), [authoring guide](../skill-authoring-guide.md), [skill catalog](../skill-catalog.md).

## 1. Objective

Establish a coherent, reusable software engineering workflow for new products, complex features, bounded changes and defect repair. Close material handoff gaps with the smallest justified skill changes and demonstrate representative behavior. Improve existing capabilities before adding new ones. The maintainer explicitly permits refactoring and renaming existing skills when their design is unsuitable. Existing names and responsibility boundaries are not immutable.

## 2. Scope

In scope: responsibility and artifact contracts, product requirements, system design, implementation planning, test strategy, change control, and representative workflow exercises. Review domain modeling and release readiness boundaries where needed.

Out of scope: application-specific implementations, indiscriminate collection restructuring without evidence, mandatory heavyweight SDD for small changes, bulk technical skill expansion, custom evaluation infrastructure, and systematic multi-model benchmarks (Phase 03). Publication remains independently blocked by unresolved provenance and authorization.

## 3. Work items

| ID | Task | Priority | Dependencies | Expected deliverable |
| --- | --- | --- | --- | --- |
| E01 | Detailed responsibility and handoff review | P0 | Phase 01 | Contract matrix and gap decisions |
| E02 | SDD lifecycle and change control | P0 | E01 | Clarified PRD approval, stable identities, reopen semantics and conflict handling |
| E03 | Product requirements capability | P0 | E01, E02 | Reuse decision and focused improvement if justified |
| E04 | System design capability | P0 | E01 | Reuse decision and scoped design contract |
| E05 | Implementation planning capability | P0 | E01, E02, E04 | Reuse decision and plan-to-implementation contract |
| E06 | Test strategy capability | P0 | E01, E02 | Reuse decision and risk-based verification contract |
| E07 | Compose representative workflows | P0 | E02–E06 | Updated workflow and actual exercise evidence |
| E08 | Phase review and next priorities | P0 | E07 | Acceptance report and remaining risks |

## 4. Execution details

### E01 — Responsibilities and reuse research

Inspect existing entry points and supporting resources, including collected skills and relevant Spring Boot skills. Record for each core capability: activation, exclusions, input, output owner, approval gates, dependencies, failure behavior and adjacent owners. Identify concrete examples of missing or overlapping behavior.

Research maintained upstream skills and tools against those examples. Start with the already identified Siva, Matt Pocock, official skill-creator and Agent Skills ecosystem sources; expand discovery when useful. Do not preselect any import solely by popularity. Record source revision, license evidence, maintenance, compatibility, actual capability and verification limitations. Choose adopt, adapt, compose or build with rationale for each gap.

Acceptance: every proposed change has a concrete gap, an owner, alternatives and a stated reuse decision. No new skill is necessary if an existing one satisfies the contract.

### Existing skill restructuring policy

Maintainer direction (2026-11-28): refactor or rename existing skills when their design is unreasonable. Evaluate retain, refactor, rename, split, merge, replace, remove or build as candidate dispositions, with adopt/adapt/compose/build as reuse decisions. Splits, merges, replacements and removals that change public contracts or third-party content require explicit review before execution; permission to refactor or rename is not blanket publication or deletion authorization.

For each restructuring proposal, document the concrete defect, intended responsibility and activation boundary, alternatives, before/after mapping, downstream callers and migration impact. For an approved rename, update the directory, frontmatter name, references, invocation configuration where applicable, provenance register, generated catalog, README, workflow documentation and tests. Preserve applicable upstream notices and history. Run deterministic checks and record behavioral verification limitations. Avoid duplicate alias skills unless a justified migration strategy requires them.

### E02 — SDD lifecycle and change control

Review product-brainstorming, prd-to-requirements, create-spec and impl-spec together. Specify the difference between summary confirmation and PRD approval; preserve legacy document conventions, IDs, status and dependencies. Resolve how a DONE use case is reopened for approved changes or repairs, what evidence invalidates prior completion, and how to handle affected dependents. Obtain maintainer approval for material status semantics before editing skills.

Acceptance: testable rules cover new use case, regeneration, legacy migration, conflicting metadata, unapproved scope changes, unmet dependencies, failed tests and repair of DONE work. No silent deletion, renumbering or status reset.

### E03 — Product requirements

Assess whether the existing brainstorming and PRD decomposition contracts already meet the need. Candidate product-requirements capability should refine an agreed product intent into reviewable product-level scope, constraints, priorities, success measures and open decisions without duplicating feature specifications or the use-case index. Establish the owner of PRD approval and change management.

Acceptance: a representative ambiguous product requirement is clarified without invented business decisions; existing approved scope is preserved. Prefer a focused change over another competing skill.

### E04 — System design

Evaluate existing domain-modeling, grilling, project-foundation and architecture-focused skills. A missing system design capability, if confirmed, should own system boundaries, responsibilities, integration contracts, data ownership, quality attributes, trade-offs and significant risks. It must not silently choose a technology stack, redefine business behavior, generate a project or prescribe microservices. Use existing ADR and glossary mechanisms rather than creating duplicate decision records.

Acceptance: given an approved product scope and material constraints, produce a reviewable design with alternatives, assumptions, decisions and unresolved risks; respect an existing architecture. Keep diagrams or artifacts proportional to complexity.

### E05 — Implementation planning

Assess html-plan and existing impl-spec decomposition, plus maintained external planning skills. Define a format-neutral plan contract only if a gap remains: approved behavior, affected components, dependencies, implementation tasks, test coverage, integration order, migration/rollback considerations where relevant and completion criteria. Planning does not authorize implementation. Prefer existing project conventions and avoid plans for trivial changes.

Acceptance: a complex approved use case yields independently verifiable tasks, no conflicting file ownership, explicit prerequisites and a verification path; a simple fix bypasses unnecessary ceremony.

### E06 — Test strategy

Evaluate spring-boot-testing, impl-spec and diagnosing-bugs before adding a technology-neutral capability. Define risk-based unit, integration, contract, end-to-end and non-functional coverage as applicable; map acceptance criteria to evidence, prioritize failure paths, and distinguish missing tests from executed results. Reuse the project's actual tools.

Acceptance: strategy covers material risks and negative scenarios without mandating irrelevant test types or inventing execution evidence. Failed or unavailable required checks prevent verified completion.

### E07 — Composition and representative verification

Use isolated, synthetic or explicitly authorized target repositories. Exercise at least the following scenario families, selecting concrete prompts and observable assertions before execution:

| Family | Required observation |
| --- | --- |
| New product / complex feature | Product approval, consistent artifacts, approved foundation/design, one verified use case |
| Lightweight change | Existing conventions preserved; no unnecessary PRD, initializer or architecture migration |
| Scope change | Stable IDs, explicit reconciliation, correct affected-spec and status handling |
| Failure / blocked dependency | No false DONE; no unapproved expansion |
| Defect repair | Reproduction, correction, regression evidence, approved reopening policy |
| Routing / composition | Relevant skills only, explicit-only policy respected, honest missing-dependency handling |

Compare a baseline and changed skill where practical using the same task, model and tools. Reuse the upstream skill-creator evaluation workflow; no custom runner is required. Record actual outputs, commands and limitations. Use deterministic CI for every material skill change and regenerate the catalog when inputs change.

If runtime execution is unavailable, record NOT RUN and the blocker, keep the corresponding acceptance item open, and do not claim the phase's behavioral exit criterion passed.

### E08 — Review

Review changed contracts, verified cases, outstanding blockers and follow-up priorities. Update roadmap, workflow, catalog, provenance and remediation tracker as appropriate. Maintain the separate publication gate. Recommend Phase 03 only with transparent evidence and explicitly recorded outstanding behavior work.

## E01 progress

See the [capability audit](../audits/engineering-capability-audit.md) and [reuse research](../research/engineering-skill-reuse.md). Static responsibilities, candidate dispositions and pinned source comparisons are documented. Dedicated system-design and test-strategy candidate research remains open; no migration or runtime exercise has been performed.

## 5. Execution order

E01 then E02; E03 and E04/E06 may proceed after their stated dependencies and approved scope, E05 follows design and lifecycle contracts, then E07 and E08. Each material skill change should be a focused reviewable unit. Do not import external content before verifying applicable rights.

## 6. Approval and exit criteria

- [ ] Core skill responsibilities and artifact contracts reviewed against actual source.
- [ ] Reuse decisions recorded before new skills are introduced.
- [ ] Material lifecycle changes reviewed by maintainer and implemented consistently.
- [ ] Demonstrated P0 gaps addressed or explicitly deferred with rationale.
- [ ] Representative end-to-end cases executed and results recorded; limitations visible.
- [ ] Deterministic validation, packaging and catalog checks pass for changed skills.
- [ ] Documentation, provenance and remediation tracker updated.
- [ ] Remaining release blockers preserved; no publication or merge inferred from phase completion.

No skill creation, runtime evaluation or publication is claimed by this plan.
