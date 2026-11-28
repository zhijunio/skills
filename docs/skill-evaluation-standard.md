# Skill evaluation standard

Status: Phase 03 evaluation contract for review. Applies to changed and new skills and representative composition cases. It does not authorize model spending, remote execution or release.

## Separate evidence layers

| Layer | Evidence | Does not establish |
| --- | --- | --- |
| Static | Metadata, resources, reference and policy consistency | Actual model behavior |
| Distribution | Package, extract and install equality | Invocation or execution quality |
| Fixture | Reproducible initial state and working test oracle | Agent task completion |
| Activation | Observed host skill selection or explicit invocation | Correct task execution |
| Behavior | Actual outputs, traces, file changes and checks | Universal reliability |
| Efficiency | Measured usage, latency and tool activity | Quality by itself |

A green deterministic CI run is not a behavioral pass. A simulated run or manually repaired fixture is not an agent run.

## Design a case before execution

Assign a stable ID, purpose, target skill or workflow, pinned fixture, exact prompts and permitted approval turns. Record available tools, required and forbidden effects, expected observable results, necessary checks and evaluator method. Include positive, negative, ambiguous, failure and missing-dependency cases as relevant. Do not make user prompts contain hidden expected answers or test-only instructions unless those are part of the intended real task.

Separate automatic selection from explicit invocation. A description being present or a model mentioning a skill is not sufficient activation evidence; use host traces or observable loading where available. Mark unavailable observations NOT RUN rather than guessing. Explicit-only skills must be tested both when requested and when not requested; keep host-specific policy distinctions visible.

## Matched experiments

Compare candidate with unchanged baseline and, where useful, a no-skill control. Use fresh sessions and identical task, fixture, model/version, tools, permissions, budget and approval sequence. Record deviations, including renamed explicit invocation tokens. Keep held-out trigger prompts separate from development prompts. Use repeated independent trials when assessing reliability; set trial count and spending limit before execution, report denominators and variation, and do not generalize a single run.

## Assertions and verdicts

Use executable checks for file content, command results, behavior and protected paths where feasible. Use a declared human or model-review rubric for qualitative artifacts; record grader identity/version, rationale and evidence. A final diff cannot establish operation ordering; require traces when order is an assertion.

Each required assertion is PASS, FAIL or NOT RUN. A case fails if any required assertion fails, including unauthorized effects, fabricated evidence or violation of an approval gate. If no assertion fails but required evidence is missing, case verdict is NOT RUN (incomplete), never PASS. Only all required assertions passing establishes a case PASS. Optional observations are separately reported; they cannot compensate for a critical failure.

Report pass/fail/not-run counts with denominators per case, host, revision and experiment group. Do not collapse critical failures into a weighted average or count missing cases as successful. For trigger tests report true/false positives and negatives only where selection is observable; explicit-only invocation cases are separate.

## Evidence record

Record case and fixture revision/hash, skill revision and installed inventory, host/model/version, exact prompts and approvals, tool permissions, commands and actual outputs, before/after diff, assertion outcomes and limitations. Preserve sanitized evidence outside packaged skill folders; never commit secrets, private data or raw authenticated traces. Record actual input/output tokens, wall time, tool calls and cost only when available; distinguish provider metrics and wall-clock measurements, and do not estimate tokens from line count.

## Acceptance and regression

Set applicable criteria before running. Preserve the required correctness and authorization floor; efficiency improvements cannot justify a regression. Compare matched results and investigate failures before modifying instructions. Keep reproducible regressions and rerun impacted cases after a change. Use a separate approved manual or controlled model-evaluation workflow for paid execution; ordinary PR checks remain credential-free. Missing runtime, cost approval or evidence keeps the applicable criterion open. See [Phase 02 cases](verification/phase-02-workflow-acceptance.md) and [authoring guide](skill-authoring-guide.md).

No universal pass-rate, token or latency threshold is established by this standard. The maintainer must approve relevant decision thresholds, model choices and budget before paid evaluation.
