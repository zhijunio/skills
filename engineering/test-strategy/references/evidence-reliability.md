# Evidence reliability

## Dependencies and scope

Select a test boundary that contains the mechanism responsible for the risk. Prefer deterministic owned collaborators; stub unrelated nondeterministic side effects. Mock an interaction when that interaction is the required behavior. Use production-compatible databases, brokers, framework wiring or other dependencies when their actual semantics matter. Document which assumptions a double makes and how contract drift is checked.

## Data and environments

Use synthetic or appropriately sanitized data, minimal fixtures, controlled time/randomness, independent reset and explicit cleanup. Record schema/version, seed or fixture identity, isolation and material differences from the target environment. Use isolated local or ephemeral resources by default; no production access, destructive scenarios, active scans or paid services without appropriate authorization. Do not copy sensitive data merely to increase realism.

## Audit and reliability

Examine whether each assertion would fail for the named wrong behavior. Identify tests coupled to incidental implementation details, redundant checks, stale contracts, missing negative paths, order dependence and uncontrolled external state. Coverage reports indicate exercised code, not correctness. Mutation testing may help assess assertion strength when appropriate.

Preserve initial failures and retry results separately. Treat nondeterministic checks as an evidence problem. If quarantine is authorized, record reason, owner, expiry, repair plan and replacement evidence; a quarantined or skipped required check does not establish completion. Remove redundant tests only after demonstrating equivalent reliable evidence and preserving distinct integration checks.

## Evidence

For each required check record expected result, actual execution result and applicable revision/environment when available. PASS, FAIL and NOT RUN are execution states, not strategy design ratings. Report residual uncertainty and unresolved obligations. Do not invent risk acceptance or claim that green CI establishes untested requirements.
