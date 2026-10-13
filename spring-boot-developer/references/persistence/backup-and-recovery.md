# Database Backup and Recovery

A backup is useful only when it can restore the required data within the service's recovery objectives. Define the
recovery point objective (RPO: acceptable data loss) and recovery time objective (RTO: acceptable restore duration)
with service owners before choosing backup frequency or retention.

## Design the recovery path

Choose database-native snapshots, continuous archiving/point-in-time recovery, logical exports, or a combination
according to the RPO/RTO and database size. Keep copies outside the primary failure domain; protect them with
encryption, least-privilege access, retention locks or equivalent controls where supported. Separate backup
administration from application credentials, and alert on missed backups, failed jobs and retention-policy drift.

Document the restore order and dependencies: provision an isolated target, restore the selected point, apply required
configuration and keys, validate schema and critical data, then route traffic only after application checks pass.
Record the exact commands or automation, required approvals, expected duration and rollback point. Keep recovery
credentials available through a separately controlled emergency process.

## Coordinate data and application state

Database backups may not include object storage, uploaded files, encryption keys, search indexes, or messages already
delivered to external systems. Decide which state is authoritative and how each dependency is restored or reconciled.
A file/database backup taken at different times can leave broken references; use a coordinated snapshot, immutable
object versions, or an explicit consistency/reconciliation procedure.

Schema migrations are not backups. A migration that drops or rewrites data may be irreversible even when the
application binary can be rolled back. Prefer expand/contract changes, take a recovery point before destructive steps,
and ensure the restored schema can run a compatible application version. See [Flyway](flyway.md) and
[CI/CD and releases](../deployment/ci-cd-release.md).

## Rehearse and verify

Restore backups regularly into an isolated environment using the production database engine and realistic data
volume. Measure actual RPO/RTO, verify migration and application startup, compare critical row counts or checksums, and
exercise access controls. A successful backup job is not evidence that recovery works.

Keep test restores isolated from production integrations: disable outbound email, payment calls and scheduled jobs,
and use distinct credentials and endpoints. Apply production-grade access control to restored data; masking may be
required before broader testing. Record the exercise result, data point, duration, gaps and owner for remediation.

## Boot 3.5 and Boot 4

Backup and restore are database/platform responsibilities, not a Spring Boot feature. Keep Boot's migration tooling
for schema evolution, but use the database provider's supported backup and point-in-time recovery mechanism. Verify
restore behavior against the exact database engine and version used in production.

## Gotchas

- Agent equates a green backup job with recoverability; restore and validate on a schedule.
- Agent stores every backup in the same account or region as the database; protect against shared failure and
  compromised credentials.
- Agent tests restore using a different database engine; production-specific backup and migration behavior may
  differ.
- Agent restores production data and lets scheduled jobs or external integrations run; isolate the target and disable
  side effects.
- Agent treats schema rollback as data recovery; destructive data changes need a recoverable point and a compatible
  application plan.
- Agent backs up the database but omits files or keys; define consistency and recovery for every authoritative data
  store.

## Related

- [Flyway](flyway.md) · [CI/CD and releases](../deployment/ci-cd-release.md) ·
  [Observability](../observability/observability.md) · [File storage](../integration/file-storage.md) ·
  [Configuration](../configuration/configuration.md)
