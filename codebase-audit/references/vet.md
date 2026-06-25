# Vet (Phase 4)

Re-read every **High** and **Critical** finding before the report. Spot-check ≥30% of **Medium**.

## Reject or downgrade when

| Class | Example |
|-------|---------|
| **By-design** | ADR explicitly accepts sync-over-async; documented fallback |
| **Proxy/env convention** | `https_proxy` honored — not architectural flaw |
| **Mis-attributed** | Real issue, wrong file or wrong layer |
| **Duplicate root cause** | Keep richest row; note confirmation in impact |
| **Non-structural** | Single bug, no pattern — remove from this report |

Record rejections in report appendix **已排除项** with reason.

## Verify inference

For `evidence_type=inferred`: trace call path yourself or downgrade confidence / severity.

## Counter-evidence search

Before confirming High+: look for tests, feature flags, or comments explaining intentional design.
