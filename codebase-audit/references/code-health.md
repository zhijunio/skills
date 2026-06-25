# Code Health Dimensions

Apply when the **code health**, **security**, **tests**, or **deps/docs** pillars are in scope. Findings need **file/symbol evidence** and **impact** — not style-only nits unless they signal a class of debt.

## D1 — Maintainability & complexity

- Hotspots: files >500–800 LOC, deep nesting, high cyclomatic complexity clusters
- Duplicated logic across modules without shared owner
- Inconsistent patterns for the same concern (three error wrappers, parallel config loaders)
- Dead code, unused exports, commented-out blocks masquerading as features
- Missing or misleading names on public APIs

**Evidence:** `rg`, churn stats, duplicate snippet samples, unreachable branch hints.

## D2 — Error handling & observability

- Swallowed exceptions, empty catch blocks, log-and-continue on critical paths
- Missing context in logs (request id, user id type only — no PII values)
- Metrics/traces absent on failure-prone IO or payment/auth paths
- Panics/asserts in library code without contract documentation

**Evidence:** catch blocks, `console.log` in prod paths, missing error returns.

## Security

- Hardcoded secrets, tokens, or private keys in repo or defaults
- SQL/command/template injection surfaces without parameterization
- AuthZ checks missing on mutating endpoints or admin routes
- Unsafe defaults: debug mode on, CORS `*`, disabled TLS verify, overly broad file permissions
- Dependency with known critical CVE on hot path (note version + usage site)

**Evidence:** `rg` for secret patterns, route handlers without auth middleware, raw string concat in queries.

**Not in scope:** full penetration test, compliance certification, secret rotation execution.

## Tests

- Core domain or contract owners without unit/integration tests
- CI runs lint but not tests (or vice versa) without documented reason
- Tests that mock internal modules instead of IO boundaries
- Flaky patterns: time.sleep, race-prone shared state, network without retry policy in test
- E2E-only coverage for logic that could run headless

**Evidence:** test file layout vs `src/` owners, CI workflow YAML, skipped test annotations.

## Deps & docs

- Critical runtime deps many major versions behind with breaking security notes
- License conflicts on distributed artifacts (when detectable from manifests)
- README missing: install, run, test commands for new contributor
- No runbook or env var doc for production-adjacent services
- ADR/decision docs stale vs obvious code reality (flag as doc debt, not auto-finding)

**Evidence:** lockfile/manifest dates, README sections, `docs/` tree vs entrypoints.

---

## Scoring hints

| Signal | Weight |
|--------|--------|
| Auth/payment path without tests | High |
| Secret pattern in committed file | Critical until vetted false positive |
| Churn hotspot + no tests | audit deeper |
| Documented security tradeoff in ADR | vet as by-design |

Stack-specific search patterns: `references/stack-signals.md`.
