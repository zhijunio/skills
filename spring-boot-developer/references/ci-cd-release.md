# CI/CD and Safe Releases

Treat CI as the reproducible path from a reviewed commit to the exact artifact running in production. Keep build, verification, packaging and deployment stages explicit; use the same wrapper and checks locally and in CI. See [taskfile.md](taskfile.md) for discoverable project commands.

## Build once and verify the artifact

Run formatting/lint, compilation, unit and slice tests, integration tests against required real services, migration validation, and dependency/security checks before packaging. Keep credentials in the CI secret store or use federated workload identity. Pin actions/plugins and container base images to reviewed versions or digests according to the platform policy.

Build one immutable JAR or image from a commit, attach commit/build metadata, and promote that exact artifact through environments. Do not rebuild separately for staging and production. Record the source commit, artifact digest, build result and deployment environment so an incident can identify the running version. Generate an SBOM and provenance when the supply-chain policy requires them.

## Coordinate schema and application changes

Deploy backward-compatible schema changes before code that needs them. Use expand and contract: add nullable columns/tables or compatible indexes; deploy code that can work with old and new forms; backfill in bounded, observable batches; switch reads/writes; remove obsolete schema only after every old application version is gone and recovery windows have passed. See [flyway.md](flyway.md).

A database migration is not automatically rolled back when an application deployment rolls back. Prefer a forward fix for destructive data changes, take verified backups before irreversible work, and define which application versions remain compatible with each schema state.

## Release and recovery

Use the deployment platform's rolling, canary or blue/green mechanism according to service capacity and risk. Readiness must become healthy before traffic shifts; liveness must not depend on remote services. Drain traffic and allow graceful shutdown before terminating instances. Keep health checks, smoke requests and rollback criteria explicit.

For a canary, compare error rate, latency percentiles, saturation and key business outcomes against the stable cohort. Halt promotion on agreed thresholds. A rollback restores the prior artifact only when the current schema and external side effects remain compatible; otherwise use a forward repair. Feature flags can decouple code deployment from activation, but assign an owner and removal date to temporary flags.

## Pipeline permissions and environments

Give build and deployment jobs separate least-privilege identities. Production credentials should be short-lived where supported, scoped to the required environment, and unavailable to untrusted pull-request code. Protect production promotion with the repository's review and environment controls; do not embed a long-lived production credential in a workflow file.

Use isolated test data and services. Do not point CI at production databases, mailboxes, buckets or payment accounts. Keep deployment configuration external to the artifact, validate required settings before rollout, and ensure diagnostics do not print secrets. See [configuration.md](configuration.md) and [observability.md](observability.md).

## Evidence and tests

The pipeline should leave inspectable evidence: source revision, test and scan results, artifact digest, migration outcome, deployment status and rollback decision. Test the packaged artifact with a startup smoke check; test migration paths against the production database engine. For high-risk releases, rehearse rollback and backup restoration rather than treating a successful deploy as proof of recoverability.

## If on Spring Boot 3.5.x

The release practices are Boot-line independent. Keep the Java runtime, Boot plugin/parent, dependency BOM and container image aligned with the project's supported Boot line; use its managed versions rather than pinning framework dependencies independently. See [boot-versions.md](boot-versions.md).

## Gotchas

- Agent builds a new artifact for each environment - promote the same digest that passed verification.
- Agent deploys code before its compatible schema change - use an expand-and-contract sequence.
- Agent assumes rolling back the JAR rolls back the database - plan schema compatibility and forward recovery.
- Agent deploys without waiting for readiness or watching error and business signals - gate traffic shifts and promotion.
- Agent exposes production secrets to pull-request workflows - use scoped identities and protected environments.
- Agent calls a successful deployment recoverable without testing restore - rehearse backup restoration and rollback.

## Related

- [taskfile.md](taskfile.md) · [flyway.md](flyway.md) · [testcontainers.md](testcontainers.md) · [configuration.md](configuration.md) · [observability.md](observability.md) · [containerization-and-native.md](containerization-and-native.md)
