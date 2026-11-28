# Evidence and verdict

For each question record expected outcome, actual environment and commands, observed output or artifact, repeatability, deviations, and checks that were not run. Retain sanitized logs or measurements where authorized. Distinguish observations from interpretation.

## Check results

- PASS: executed check meets its stated criterion.
- FAIL: executed check contradicts its stated criterion.
- NOT RUN: execution did not occur or could not complete meaningfully; record reason.

## Hypothesis verdict

- VALIDATED: evidence meets the agreed criteria within stated conditions.
- PARTIAL: some criteria are supported but material limitations or gaps remain.
- INVALIDATED: observed evidence contradicts the hypothesis under the stated conditions.
- INCONCLUSIVE: missing, insufficient or conflicting evidence prevents a reliable conclusion.

Do not use INVALIDATED merely because the environment could not run. A positive compatibility check does not establish security, scalability or production readiness. If no criterion is agreed, report findings and uncertainty instead of inventing a pass threshold.

Compare candidates using observed results and explicit tradeoffs, not fabricated scores. Recommend a decision, further experiment or rejection, with the affected design/ADR and owner. State which risks remain and whether the result is transferable to the target environment. Keep status and implementation approval separate.
