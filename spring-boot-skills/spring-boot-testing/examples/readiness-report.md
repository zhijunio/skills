# Readiness report examples

These synthetic reports illustrate how to state verification results without
claiming more than the recorded evidence supports.

## Evidence-backed report

Scope: local checkout at commit `abc123`; working-tree changes listed; wrapper,
JDK, and Spring Boot versions recorded.

- Build and unit tests: **PASS**. The exact wrapper command exited with code 0;
  24 tests ran and the run-specific reports were reviewed.
- Integration tests: **NOT RUN** (blocked). The disposable PostgreSQL service is
  unavailable; no production service was used as a fallback.
- Combined coverage gate: **NOT RUN**. It depends on the incomplete
  integration-test suite.
- Dependency scan: **NOT RUN**. The configured advisory source was unavailable.

Verdict: release readiness is not established. Restore the test infrastructure
and rerun the affected gates.

## Unsupported claim

> The build returned zero, so everything is production-ready.

The command used `-DskipTests`, scanned no dependencies, and read a coverage
report from an earlier run. Missing integration-test infrastructure was
silently ignored, so the claim is not supported by the recorded evidence.
