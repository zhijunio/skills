# Skill Evaluation Baseline

Status: Static baseline recorded; behavioral evaluation not run.
Related finding: A11 in the [remediation tracker](../audits/remediation-plan.md).

## Static measurements

Measurements count physical lines (excluding a final empty line) and Unicode code points of the complete SKILL.md, including metadata. These are not token counts, context cost, or evidence of poor performance.

| Skill | Blob SHA | Lines | Code points |
| --- | --- | ---: | ---: |
| spring-data-jpa | 7a16b4290ee3b05935d4fceba75a8e28de82c239 | 353 | 12,402 |
| spring-boot-testing | 67abdecf66d489ae62f9f23e7282c51b5390de0b | 303 | 13,527 |
| spring-boot | fa5a6ec19eed13dd0a09fda7de5019dfc447ba91 | 95 | 8,202 |

The first two skills cover several distinct task types. This makes progressive disclosure worth evaluating, not automatically necessary. The Spring Boot entry point is a router and must not load its entire collection for an ordinary task.

## Proposed behavioral cases

| Case | Task | Expected behavior |
| --- | --- | --- |
| JPA-01 | Diagnose an N+1 query | Load relevant JPA guidance; verify query behavior without unrelated redesign |
| JPA-02 | Add a paginated repository query | Preserve pagination semantics and existing persistence conventions |
| TEST-01 | Test one MVC endpoint | Select suitable focused tests; avoid unnecessary infrastructure |
| TEST-02 | Review release readiness with failing tests | Report actual failures rather than declaring readiness |
| ROUTE-01 | Implement an ordinary REST endpoint | Load only applicable focused Spring Boot skills |
| ROUTE-02 | Implement a cross-cutting persistence and security change | Load relevant skills and reconcile their contracts |

## Evaluation approach

Reuse existing skill-creator or equivalent evaluation tooling rather than building a new runner. Evaluate the unchanged baseline, a candidate progressive-disclosure refactor, and a no-skill control using the same model version, task, repository state, tool access, and evaluation rubric. Use fresh sessions and multiple trials.

Record correctness, acceptance criteria, scope violations, loaded files, input/output token usage when available, elapsed time, and tool calls. If a metric is unavailable, mark it unavailable rather than deriving it from line count. Compare quality and context efficiency jointly; do not accept a shorter skill if it reduces correctness or hides necessary constraints.

Do not change the two larger skills solely based on static length. A11 remains open for behavioral evidence and a justified decision in Phase 3.
