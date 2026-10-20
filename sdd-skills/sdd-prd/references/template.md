# Product Requirements Document: <product or initiative>

**Status:** Draft (approval pending) / Approved by <person> on <date><br>
**Version:** 0.1<br>
**Date:** <date><br>
**Information sources:** <user input, interviews, data, existing documents; note when none were provided>

## Overview

### Problem statement

<Who has what problem, in which context, why it matters, and why now. Link to supporting evidence when available.>

### Solution summary

<Proposed product-level approach and user-facing capabilities. Keep architecture, API, and implementation choices in canonical design documents.>

### Target users

| User / segment | Need and context | Evidence / status |
| --- | --- | --- |
| <user> | <need> | <source; Confirmed / Proposed / Unknown> |

### Evidence and assumptions

- **Evidence:** <Direct user input, interview findings, data, repository facts, or none supplied>
- **Assumptions:** <Important assumptions and how they will be checked>

## Goals & Success Metrics

### Goals

1. **G-1:** <Primary user or business outcome>
2. **G-2:** <Secondary outcome, if relevant>

### Success metrics

| Goal | Metric | Current baseline | Target | Timeline / status |
| --- | --- | --- | --- | --- |
| G-1 | <How success is measured> | <Known value or TBD> | <Target or TBD> | <Date or TBD; Proposed / Confirmed> |

### Non-goals

- <Problem, user, or capability this initiative will not address>

## User Stories (when useful)

<Include stories when they clarify distinct user outcomes. Omit for simple changes where goals and requirements already explain the users. Detailed acceptance criteria belong in `sdd-spec`.>

| ID | User story | Priority | Goal / source |
| --- | --- | --- | --- |
| US-1 | As a <user>, I want <capability>, so that <outcome>. | <Must / Should / Could / TBD> | <G-1; source> |

## Scope

### In scope / product direction

- <Capability in the proposed or approved product direction; Draft PRD items remain proposals>

### Out of scope

- <Capability excluded from this product scope>

### Future considerations (optional)

- <Capability and reason for deferral>

<When a release batch is defined, mark which in-scope capabilities it excludes; do not label all deferred capabilities as permanent product non-goals.>

### Release phases and delivery batches (optional)

Use this table as a short roadmap summary. Detailed product behavior belongs in Solution Design. A phase describes capability order or dependency; a delivery batch describes what ships together. A Draft PRD's batch is proposed, not committed.

| Phase / batch | User outcome | Included requirement IDs | Prerequisite requirement IDs / external dependencies | Scope status |
| --- | --- | --- | --- | --- |
| <Phase or batch> | <Short outcome> | <FR-1, FR-2> | <FR-1; external dependency or None / TBD> | <Proposed / Approved for release / Deferred> |

**Current release / delivery batch** (optional; use TBD when not decided): <requirement IDs selected for the current release>

The requirements register below is authoritative. Phases and batches organize delivery, reference requirement IDs, and do not repeat requirement statements. A requirement may be unassigned or appear in a proposed future batch. Distinguish a true prerequisite from planned sequence; use `None` when no dependency is known and `TBD` when unresolved.

## Solution Design

### Functional requirements

Maintain one requirements register as the source of truth, independent of phases or delivery batches. Requirement priority and decision status are separate: a confirmed requirement does not by itself approve this PRD. Use stable IDs and observable statements; record dependencies explicitly, including `None` or `TBD`. Include a Source / evidence column when traceability clarifies why a requirement exists; omit it when it only repeats the information sources or decision status.

| ID | Requirement | Priority | Related goal / story | Depends on | Delivery phase / batch | Source / evidence | Decision status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| FR-1 | <Observable product behavior> | <Must / Should / Could / TBD> | <G-1 / US-1> | <FR-2 / external prerequisite / None / TBD> | <Phase or batch ID / Unassigned> | <Source or TBD> | <Proposed / Confirmed / Deferred> |

<Add rows to this single register; do not create a separate copy under each phase.>

### User experience (when useful)

<Key interaction, workflow, design reference, or usability constraint. Do not prescribe implementation details.>

### Edge cases (product-significant only)

| Scenario | Expected product behavior | Related requirement |
| --- | --- | --- |
| <Important exception> | <User-visible or operational behavior> | FR-1 |

Keep detailed feature flows and exhaustive edge cases in `sdd-spec`.

### AI behavior and evaluation (conditional)

<Include only when a model produces user-visible or otherwise consequential output; omit this section for deterministic behavior. Define release-required behavior and the evidence that will demonstrate it. Do not claim a quality rate from a curated evaluation set.>

#### Behavior requirements and evidence

| ID | Required model behavior | Evaluation evidence | Pass rule (fixed before evaluation) |
| --- | --- | --- | --- |
| AB-1 | <Normal-case behavior> | <Cases, rubric, or check> | <Case-level pass rule> |
| AB-2 | <Refusal behavior: when it must not answer and the safe response> | <Refusal cases> | <Case-level pass rule> |
| AB-3 | <Abstention behavior: when it lacks sufficient grounding> | <Insufficient-evidence cases> | <Case-level pass rule> |
| AB-4 (when applicable) | Treat instructions embedded in untrusted content as data, not instructions to follow. | <Cases with embedded instructions> | <Case-level pass rule> |
| AB-5 (when applicable) | Do not expose source information to an audience with less access than the source permits. | <Cases with differing source and audience permissions> | <Case-level pass rule> |

<For multi-step model features, define and evaluate each consequential component separately so an end-to-end result does not hide where failures occur.>

#### Evaluation set and execution

- **Case location:** <Path or system>
- **Required slices:** <Derive from the behavior requirements and costly failure modes; name the consequence each slice covers>
- **Coverage floor per slice:** <Minimum cases and rationale; state what failure modes the cases deliberately search for, not an assumed failure rate>
- **Runs per case:** <Number of production-setting runs and whether every run, or a predeclared number of runs, must pass>
- **Held-out cases:** <Cases withheld from tuning, or state why none are held out and how overfitting is limited>
- **Separate slice reporting:** <Slices reported individually so aggregate results cannot conceal a failing group>
- **Rate claims (if any):** <Define a separate designed sample, sampling frame, sample size, and threshold before making a population-rate claim. If these are not established, mark rate verification unresolved.>

## Technical Considerations

Surface product constraints and dependencies for engineering without specifying the architecture. Keep canonical decisions in design/API documents.

### Constraints and non-functional requirements

| ID | Quality area / constraint | Requirement or measure | Priority | Depends on | Delivery phase / batch | Status / source |
| --- | --- | --- | --- | --- | --- | --- |
| NFR-1 | <Security / performance / availability / privacy / accessibility / compatibility> | <Observable need; use TBD if unmeasured> | <Must / Should / Could / TBD> | <FR-1 / external prerequisite / None / TBD> | <Phase or batch ID / Unassigned> | <Confirmed / Proposed; source> |

### Integration points

- **<System or service>:** <Product dependency or contract reference>

### Data requirements

- <Information, privacy, retention, export, or data-quality need; link canonical contracts>

## Agent Execution Contract (conditional)

<Include only when a coding agent or other executor will implement from this PRD without the authoring context and the downstream spec/plan does not already carry these instructions.>

### Authoritative sources

| Source | Path or link | Definitive for |
| --- | --- | --- |
| <Source> | <Path or link> | <Decision or scope it settles> |

### Do not touch (optional)

| Path or system | Reason |
| --- | --- |
| <Path or system> | <Why it is outside this work> |

### Requirement verification map (optional)

| Requirement | Verification evidence | Verifier |
| --- | --- | --- |
| FR-1 | <Test, check, or observation> | <Role or team> |

### Stop and escalate (optional)

| Condition | Escalate to |
| --- | --- |
| <Decision the executor must not make unilaterally> | <Role or person> |

## Dependencies & Risks

### Dependencies

| Dependency | Owner | Status | Impact if delayed |
| --- | --- | --- | --- |
| <Dependency> | <Role or team> | <Status> | <Impact> |

### Risks and assumptions

| Item | Likelihood / impact | Mitigation or validation |
| --- | --- | --- |
| <Risk or assumption> | <Known level or TBD> | <Action, evidence, or owner> |

## Timeline & Milestones (optional)

Use for dated checkpoints or decision gates. Do not repeat the capability phase roadmap from Scope.

| Milestone | Product outcome or decision | Target date / status |
| --- | --- | --- |
| <Checkpoint or launch> | <Outcome or decision> | <Date or TBD; Proposed / Approved> |

## Open Questions

| Question | Affected requirement(s) | Impact | Owner / needed before |
| --- | --- | --- | --- |
| <Unresolved decision> | <FR-1 / NFR-1> | <What changes if unresolved> | <Role and milestone> |

## Appendix

### Related documents

- User research / inputs: <paths or none>
- Domain design / API: <paths or none>
- Feature specs: <paths or none>

### Supporting material (optional)

- <Additional evidence or reference needed to understand this PRD; omit when none>

### Revision history

| Version | Date | Changes | Approval |
| --- | --- | --- | --- |
| 0.1 | <date> | Initial PRD draft | Pending |

<!-- Keep Status as Draft until the user explicitly approves the product scope. -->
