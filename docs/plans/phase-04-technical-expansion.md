# Phase 04 — Technical Expansion

Status: proposed preparation only. Phase 02 and Phase 03 behavioral exit criteria remain OPEN. See [Phase 03 review](../reviews/phase-03-review.md).

## Objective

Extend reusable technical capability only where the existing 63 skills leave a demonstrated task gap. Avoid duplicating Spring-specific knowledge and preserve existing engineering workflow contracts. Do not create collections merely to match a target directory tree.

## Entry conditions

Discovery and source comparison may overlap with pending runtime work. Before importing or implementing a new capability, establish a representative task, existing coverage and gap, intended activation/exclusions, authoritative version constraints, candidate upstream revision and license, focused evaluation cases and a viable verification method. Material splits, replacements or collection changes require review. Do not silently accept unresolved rights. Keep no-skill/old-skill comparison available; report missing execution explicitly. Full Phase 03 acceptance and release remain separate.

## Proposed sequence

| ID | Work | Acceptance evidence |
| --- | --- | --- |
| T01 | Inventory Java and Spring overlap, generic engineering coverage, and realistic cross-framework tasks | Capability matrix with current owners and gaps, no new skills |
| T02 | Source discovery for first justified Java or framework-neutral capability | Actual upstream content, pinned revision, license and adopt/adapt/compose/build rationale |
| T03 | Resolve collection ownership and compatibility | Review of packaging, catalog, installation, dependencies and migration implications |
| T04 | Implement one approved focused capability or improve existing owner | Scoped changes, source notices, regression cases and deterministic verification |
| T05 | Evaluate real technical behavior | Reproducible fixture, actual tests and matched agent evidence or explicit blocker |
| T06 | Reprioritize subsequent frontend/data/distributed/security/testing/delivery topics | Evidence-driven backlog; no automatic bulk imports |

Candidate Java language/concurrency/JVM guidance is a discovery priority, not a decision to create java-skills. Existing Spring JVM diagnostics, null-safety, PostgreSQL, idempotency, testing and architecture guidance may already cover parts of the proposed scope. Prefer composition before extraction. A new collection would require explicit update of all inventory, packaging, installation and measurement helpers; inspect those paths before proposing it.

## Parallel blockers

Run the Phase 03 pilot when an authorized runtime is available. Resolve A04 notices and source evidence before redistribution; A13 Central authentication requires maintainer-side review. No merge, publication, global installation or paid evaluation authorized by this plan.

## Spring Boot collection follow-up

The [Spring Boot collection audit](../audits/spring-boot-skill-audit.md) reviews all 39 entry points and identifies focused optimization candidates before new Java or technology collections. Treat its proposed sequence as T01 input, not authorization for broad restructuring or proof of behavioral quality.
