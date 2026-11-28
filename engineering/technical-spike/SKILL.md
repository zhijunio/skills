---
name: technical-spike
description: Run bounded technical feasibility experiments or comparative proofs of concept to resolve engineering uncertainty with reproducible evidence. Use for explicit spike or PoC requests, integration compatibility, mechanism or performance hypotheses; not product UI prototypes, routine research, defect repair or production implementation.
license: MIT
disable-model-invocation: true
---

# Technical Spike

Resolve a technical unknown with the smallest experiment that can distinguish competing explanations or approaches. Experimental code is disposable; the evidence and resulting decision are the durable outputs. Adapted from the Hermes spike workflow; see [source](SOURCE.md).

## Scope

Use when the user explicitly requests a feasibility experiment. If existing documentation or code inspection can answer the question, report that evidence instead of building an experiment. Route product journeys and business simulations to product-prototyping, established defects to diagnosing-bugs, architecture decisions to system-design, and production implementation to its approved owner.

## Workflow

1. Read the relevant scope, constraints, design decisions and existing evidence. State the technical hypothesis, observable outcome, decision it affects and limits of the claim. Split independent questions only when useful; prioritize uncertainties that could invalidate the proposed approach.
2. Research viable approaches and relevant dependency versions using primary sources. Distinguish documentation claims from observed behavior. Choose the smallest suitable method using [experiment design](references/experiment-design.md).
3. Establish the comparison conditions, success criteria, authorized environment, resource limits and stop conditions before running. Do not invent numerical targets. If the user has authorized only exploration or planning, do not execute. Ask about consequential unresolved setup, cost or access decisions.
4. Build in an isolated scratch location, using synthetic data and scoped test credentials where necessary. Prefer existing tools and pinned dependencies. Keep production code, data, routes and configuration unchanged. Do not install global tooling, access production, make external mutations, incur charges or publish without separate authorization.
5. Execute the relevant experiment and negative or boundary cases. Record commands, environment, dependency versions, workload, observations, failures and repeatability. Compare alternatives under equivalent conditions where practical. If execution is unavailable, report NOT RUN; do not invent results or substitute a mock for real dependency semantics.
6. Use [evidence and verdict](references/evidence-and-verdict.md) to assess the hypothesis against the agreed criteria. Distinguish limited positive evidence, contrary evidence, uncertainty and missing checks. A negative result can be valuable.
7. Return the evidence, recommendation and remaining unknowns to the architecture or technical decision owner. Do not change approved design, requirements, status, production implementation or ADRs without the appropriate authorization.

## Output

Honor the requested format and repository conventions. If files are authorized, keep experiment code and evidence together in a clearly isolated project-selected location (for example, spikes/<name>/README.md). Use the [report template](assets/spike-report-template.md) when useful. Include actual run instructions and evidence locations, verdict, limitations, decision implications and cleanup needs. Stop at the agreed learning goal or resource boundary. Do not treat a spike verdict as production readiness or implementation approval.
