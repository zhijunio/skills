# Prototype Capability Contract

Status: product and behavior modes implemented as product-prototyping; technical PoC implemented as technical-spike on review branch. Runtime verification pending.
Related: [Phase 02 plan](../plans/phase-02-engineering-workflow.md), [existing prototype](../../engineering/product-prototyping/SKILL.md), [UI reference](../../engineering/product-prototyping/references/ui-prototype.md), [logic reference](../../engineering/product-prototyping/references/behavior-prototype.md), [SDD contract](sdd-lifecycle.md).

## 1. Select the question, not the output format

| Mode | Decision question | Typical evidence | Decision owner |
| --- | --- | --- | --- |
| Product | Can users understand and complete the intended journey? | Information architecture, wireframe, interactive mock, alternatives and feedback | PRD / product owner |
| Behavior | Are rules, invariants and state transitions correct? | State model, scenario walkthrough, rejected action and resulting state | Behavior spec / domain owner |
| Technical | Does a candidate mechanism meet an explicit constraint? | Isolated integration, benchmark, experiment logs and limitations | System design / ADR owner |

Choose only the necessary mode and fidelity; modes may iterate or compose. A headless API may need no graphical prototype. A straightforward fix may need none. A visual prototype and an implementation plan are different artifacts.

## 2. Common contract

Establish a bounded question, existing decisions, intended audience, alternatives or hypothesis, success criteria, and authorized scope. Use synthetic or sanitized fixtures and safe isolated paths by default. Preserve existing routes, auth and production behavior; do not make real mutations, access production systems, create remote branches, deploy or publish without authorization. Use existing project conventions and available tools. Report how to run or review, observed findings, open questions and limitations. Mark artifacts non-production; retain or clean up only within authorized scope. Feed decisions back through the relevant authority, not by silently updating requirements or status.

Product exploration: select fidelity by uncertainty; cover primary journeys and relevant empty, loading, error, accessibility and responsive states where meaningful; avoid speculative product decisions. Behavior exploration: show initial state, legal/illegal transitions, outcomes and resettable scenarios; a simulation does not certify real dependency behavior. Technical feasibility: record environment, versions, workload, repeatable procedure, pass/fail threshold when authorized, observed results and uncertainty. Do not invent scale or thresholds; use NOT RUN when evidence is unavailable. Tests and instrumentation may be required. No blanket no-tests policy for PoCs.

## 3. Historical implementation gaps (before migration)

The current UI reference prefers modifying an existing route and only explicitly hides its switcher in production. This is insufficient evidence that variants cannot ship. Prefer isolated dev-only routes or standalone artifacts; protect all variant code and data paths. The current logic reference is a useful standalone simulator with free play and guided cases; do not automatically lift experimental logic into production. Existing instructions require branch/issue capture and folding winners into real code; separate those actions from prototype authorization. The technical mode is not covered by the two current branches. These findings are source review, not runtime defects observed in a real project.

## 4. Proposed reuse decision

Compare existing Matt Pocock prototype guidance with maintained product design/prototyping and technical spike workflows. Prefer a single focused skill plus progressive references if activation, authorization and outputs remain coherent; split only when independent invocation and distinct evidence justify it. Do not assume three abilities require three skills. Record license, fixed revision, dependencies and gaps before importing. The [prototype extension research](../research/engineering-skill-reuse.md) supports retaining product and behavior exploration together, with a separate candidate technical-spike capability for reproducible feasibility experiments. Product and behavior modes now use product-prototyping, preserving its original MIT notice; technical-spike implements the separate technical capability via selective adaptation of Hermes spike, with both Hermes and GSD MIT notices retained. The exact GSD revision used by Hermes is unknown; no GSD workflow source is imported.

## 5. Proposed evaluation cases (NOT RUN)

| ID | Scenario | Observable result |
| --- | --- | --- |
| P01 | New product with unclear navigation | Reviewable journey alternatives, assumptions and PRD feedback; no production implementation |
| P02 | Existing UI with real mutation endpoint | Isolated mock/stub, no live mutation or unapproved route change |
| P03 | Ambiguous state transition | Resettable example covers allowed and forbidden action; feedback routed to spec/domain |
| P04 | Technical compatibility uncertainty | Reproducible isolated evidence, version and limitation recorded |
| P05 | Required performance environment unavailable | NOT RUN, no invented result or feasibility claim |
| P06 | Approved prototype but no implementation authorization | No production edits, status transition or publication |
| P07 | Clear bounded defect | No compulsory prototype |

All cases remain pending actual runtime verification; both skills are implemented on the review branch.
