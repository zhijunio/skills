# Phase 03 — Quality Engineering

Status: preparation started on the review branch. Phase 02 behavioral acceptance remains open. Baseline: b8bc25ad387452158693738a2f279b49cbdeb9e5.

## Objective

Measure whether skills improve actual agent performance while preserving correctness, scope, authorization, compatibility and reasonable context cost. Reuse existing evaluation tooling and repository CI. Do not create new skills solely to fill a roadmap category.

## Work packages

| ID | Priority | Deliverable and acceptance |
| --- | --- | --- |
| Q01 | P0 | Evaluation standard separating static, distribution, fixture, activation, behavior and efficiency evidence; explicit verdicts and matched comparison protocol. |
| Q02 | P0 | Verify disposable fixtures deterministically, including intended failing baselines, repeatability and destination protection; existing helper-test CI discovers tests. |
| Q03 | P0 | Inspect pinned upstream skill-creator schemas, execution assumptions, license and host compatibility; document permitted reuse and a minimal verified invocation or blocker. |
| Q04 | P0 | Create representative positive, negative and boundary trigger prompts; separate explicit-only cases and held-out data. |
| Q05 | P0 | Execute core engineering workflow cases in isolated authenticated sessions; retain real evidence and fix material regressions. |
| Q06 | P1 | Evaluate representative Spring Boot router, persistence, testing and security cases with realistic fixtures. |
| Q07 | P1 | Compare measured context, tokens, time and tool activity for matched tasks; optimize only with correctness preserved. |
| Q08 | P1 | Compare actual Codex and Claude Code behavior and invocation policies, distinct from installation compatibility. |
| Q09 | P1 | Extend deterministic PR gates where justified; separately design opt-in bounded model evaluation with explicit credentials and spend controls. |
| Q10 | P1 | Retain regression cases, review upstream drift and optimize based on observed failures. |
| Q11 | P0 | Review evidence, unresolved blockers, maintainer acceptance and publication separation. |

## Sequence and constraints

Q01/Q02 establish reliable inputs. Q03 precedes runtime tooling integration. Q04/Q05 target the highest-risk workflow and activation boundaries first; Q06–Q10 expand only after evidence is available. No custom agent runner unless an evidenced gap and maintainer review justify it. Reuse the pinned Anthropic skill-creator evaluation workflow where compatible, not its host-specific instructions as universal requirements. Inspect its license before copying any resources.

Use the [evaluation standard](../skill-evaluation-standard.md). Keep fixtures and evaluation workspaces outside packaged skills. Ordinary PR CI must not require model credentials or incur model charges. Paid evaluation requires a chosen host/model, explicit budget, authorized tools and isolated targets. Do not use real project credentials or publish experiments.

## Progress

Q01: evaluation standard drafted. Q02: fixture helper tests added to existing discovery path; execution evidence pending. Q03–Q11: pending. Phase 02 W01–W18 remain NOT RUN in independent agents.

## Exit criteria

- Evaluation standard reviewed and applicable cases reproducible.
- Deterministic fixture and distribution checks pass.
- Representative activation and behavior cases actually executed, compared and recorded, with missing evidence visible.
- Critical authorization/correctness regressions resolved or explicitly blocked; no missing required check counted as passing.
- Cross-host and efficiency claims limited to actual observations.
- Review and release decisions remain separate; A04 and A13 remain open until resolved.
