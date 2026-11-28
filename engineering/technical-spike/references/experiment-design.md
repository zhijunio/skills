# Experiment design

## Define a falsifiable question

Specify the candidate mechanism, conditions, observable result and decision affected. A compatibility experiment might establish that two specified versions exchange a defined payload; a benchmark needs a defined workload, environment and measurement method. If a threshold is not approved, report observations without declaring that the target was met.

## Select the smallest adequate experiment

Use documentation or source inspection when sufficient. Use a focused executable assertion for a narrow semantic claim, a minimal runnable harness for integration or failure behavior, or a controlled measurement harness for performance. Simulations may validate an assumption about logic but cannot prove real integration or deployment properties. Use real dependencies only in an authorized isolated environment when their semantics are essential.

For comparisons, hold relevant versions, fixtures, workload and measurement procedure constant; describe unavoidable differences. Include relevant edge and failure cases, reset conditions and repeat runs when variability matters. For performance, consider warmup, concurrency, resource limits, sample size and measurement overhead where relevant. Do not extrapolate scratch results to production capacity.

## Execution boundary

Identify prerequisites, scratch path, permissions, network use, dependencies, cost/resource limits, timeout and cleanup. Use synthetic or sanitized fixtures, avoid secrets in logs and code, prefer pinned versions and scoped credentials. Stop if the experiment would exceed authorized access or resources. Do not silently install tools, call live endpoints, run destructive commands or modify production files. Report any prerequisite that cannot be satisfied.
