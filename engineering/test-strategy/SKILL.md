---
name: test-strategy
description: Design or audit a risk-driven test strategy from requirements, acceptance criteria, architecture and defects. Use to select test techniques, fidelity, dependencies, data, environments and required evidence; not framework-specific test implementation, test execution or CI configuration.
license: MIT
---

# Test Strategy

Choose the smallest reliable set of checks that can detect material failures. Adapted from L-GEVITY test-strategy; see [source](SOURCE.md).

## Scope and inputs

Use for a bounded feature, subsystem, contract, change or test portfolio. Read approved requirements and acceptance criteria, relevant architecture and contracts, existing tests, historical defects, and environment constraints. For a clear small change, a short risk-to-check mapping is enough. Do not force a separate strategy document.

Own verification obligations and technique selection, not product requirements, architecture, implementation or pipeline configuration. If architecture is unsettled, identify the required observations first and defer architecture-dependent test choices until the relevant decision is accepted. Existing tests do not establish intended behavior.

## Workflow

1. Select design or audit mode. Identify the subject, scope, canonical criteria where available, material risks, failure modes, impact, exposure and evidence confidence. Unknown exposure is not low risk.
2. For each obligation, define the authoritative expected outcome, observable pass/fail conditions and required observations. Resolve ambiguous behavior with its owner. Do not invent targets or treat a test that merely executes code as proof.
3. Select the technique and smallest sufficient test scope using [technique selection](references/technique-selection.md). Specify stimulus, exercised dependencies, environment and expected result before labeling a test unit, integration or end-to-end.
4. Choose dependency realism, fixtures, isolation, repeatability and relevant broader backstops using [evidence reliability](references/evidence-reliability.md). Use real compatible dependencies where semantics determine correctness. Do not claim mocks prove transactions, protocol behavior, security wiring or deployment compatibility.
5. Scan relevant quality risks, including negative paths, permissions, data integrity, concurrency, compatibility, recovery, performance and accessibility. Include only applicable obligations; obtain authoritative targets and authorization before intrusive tests or paid/external execution.
6. For an audit, map existing assertions and actual evidence to risks. Identify missing or hollow assertions, obsolete tests, duplication, flaky behavior and unverified doubles. Do not remove tests merely to improve speed or coverage numbers; retain equivalent or stronger evidence.
7. Identify required completion evidence, owners, remaining gaps and checks that cannot yet run. Map obligations to implementation tasks with implementation-planning and stack-specific test guidance, including spring-boot-testing when relevant. Use [strategy outline](references/strategy-outline.md) if a document is useful.

## Deliverable and boundaries

Return a proportionate risk-to-evidence matrix, technique rationale, data/environment assumptions, required checks, residual gaps and handoff. Use existing conventions or docs/test-strategy.md when a document is authorized. Label planned checks separately from executed PASS, FAIL and NOT RUN evidence. A well-designed strategy is not proof that a system passed its tests.

Do not set universal coverage percentages, fixed test-layer ratios or arbitrary test counts. Do not implement tests, change CI, run scans, mutate production or authorize release solely because a strategy was requested. Required missing or failed checks block verified completion under implement-use-case. The user or applicable authority owns changes to required obligations and risk acceptance; do not waive mandatory safety, security or acceptance requirements.
