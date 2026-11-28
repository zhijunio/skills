# Build verification

## Build and release verification

When asked to assess readiness, inspect the repository's build files, wrapper,
CI workflow, profiles, and contribution instructions before choosing commands.
Use the project's configured wrapper and quality gates. Preserve module
boundaries, thresholds, and scanner configuration. A verification request does
not authorize production access, credential uploads, deployment, or unrelated
code changes.

- Confirm the effective Boot/JDK/toolchain versions and the JVM used by the build.
- For Maven, use the configured wrapper lifecycle. The verify lifecycle includes
  integration-test verification only when Failsafe is bound. Do not stop at
  integration-test.
- For Gradle, inspect task dependencies. The check task does not guarantee a
  separately registered integration-test task runs.
- Run configured lint, static analysis, coverage, secret, and dependency checks.
  A generated coverage report is not a gate unless a threshold is enforced.
- Compare discovered tests and run-specific reports with expected suites. Expose
  zero tests, disabled tests, skipped suites, stale reports, scan failures, and
  exclusions even when the build exits successfully.
- Record the source revision and dirty state, versions, exact commands, exit
  status, test counts, report locations, and remaining risks. Mark each gate
  PASS, FAIL, or NOT RUN. Record the blocking reason separately when a check
  cannot run. A blocked required check means readiness is unestablished.
- After a deterministic failure, stop dependent checks. Retry a proven transient
  infrastructure failure only within the authorized retry budget and retain every outcome. Do not weaken
  gates or modify thresholds to get a green result.

Use the [readiness report examples](../examples/readiness-report.md) to keep claims tied to evidence.

Use the configured Maven/Gradle wrapper and actual task graph. A release readiness review does
not itself authorize release. Distinguish checks that were executed and failed from missing checks.
