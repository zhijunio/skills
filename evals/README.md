# Evaluation datasets

Status: Q04 seed data, not executed. See the [evaluation standard](../docs/skill-evaluation-standard.md) and [integration review](../docs/verification/skill-creator-integration.md).

## Description trigger datasets

Seven unrestricted skills each have eight development prompts and four holdout prompts, balanced between positive and negative expected selections. These 84 authored seed prompts cover neighboring responsibilities, existing-project changes and both English and Chinese. The JSON arrays use the upstream run_eval query/should_trigger interface directly. They are single-description hypotheses, not exclusive routing labels: another relevant skill may also be appropriate. A positive label does not authorize execution.

Use development queries for iterative description work. Holdout queries must not be supplied to the optimization process or used to choose revisions; evaluate them only after freezing a candidate. Because these seeds are committed openly and the authors can see them, they are not a secret independent test set. If used for tuning, retire the affected holdout and create fresh reviewer-controlled cases. The small sample cannot establish generalization. Review realistic prompts and labels before a paid run.

## Invocation policy

The separate explicit-only.json contains paired implicit and explicit prompts for all eleven currently configured explicit-only skills. Run in fresh actual host sessions, observing skill loading and the relevant host policy. Use native explicit invocation syntax for the host where necessary and record the exact submitted prompt. A natural-language mention is not necessarily native explicit invocation. Explicit eligibility does not prove successful activation and does not waive scope, approvals or missing dependencies. Do not submit these policy cases to the temporary-command description runner.

## Execution

Use the pinned external skill-creator only after runtime, permission and budget approval. Do not combine development and holdout datasets for optimization. Record host, model, revision, actual prompt, observation, verdict and missing evidence. A timeout or execution error is not a negative trigger observation. For cross-skill routing, install the relevant actual skills in an isolated project and record observed selection separately.

All trigger and invocation trials are NOT RUN. No description has been tuned against these data. No model calls are part of ordinary PR CI.

## Behavior cases

See [core workflow cases](behavior/README.md) for the structured registry and selected offline filesystem assertions. All independent behavior trials are NOT RUN.

## Spring Boot cases

See [Spring Boot evaluation cases](spring-boot/README.md) for fourteen version-aware technical and routing scenarios. Manual fixture setup and actual execution remain pending.

## Cross-host cases

See [native host verification](../docs/verification/cross-host-verification.md) and host-cases.json under compatibility. These cases require actual host sessions and are not installation or description-trigger tests.
