# Phase 01 — Repository Foundations Review

Date: 2026-11-28
Disposition: Foundation deliverables implemented and deterministic baseline verified on the review branch; maintainer acceptance and publication clearance remain separate.
Evidence revision: [eff97c1](https://github.com/zhijunio/skills/commit/eff97c18a32886a46e478cab79d845267229a03d)
Review branch: [PR #4](https://github.com/zhijunio/skills/pull/4), not merged.
Plan: [Phase 01](../plans/phase-01-repository-foundations.md)

## 1. Deliverables and acceptance

| Item | Outcome | Evidence and limitation |
| --- | --- | --- |
| F01 | Documented | [Compatibility](../skill-compatibility.md); host behavior and hosted exports not certified |
| F02 | Maintainer confirmed | [AGENTS.md](../../AGENTS.md) |
| F03 | Maintainer confirmed | [Design principles](../skill-design-principles.md) |
| F04 | Documented | [Authoring guide](../skill-authoring-guide.md); maintainer review pending |
| F05 | Documented | [Contribution guide](../../CONTRIBUTING.md); maintainer review pending |
| F06 | Baseline documented | [Initial audit](../audits/initial-skill-audit.md), 59 entries; remediation tracked separately |
| F07 | Verified | [Generated catalog](../skill-catalog.md), 59 entries; freshness enforcement passed |
| F08 | Documented | [Engineering workflow](../engineering-workflow.md); behavioral cases proposed, not executed |
| F09 | Verified within narrowed scope | [Distribution verification](../verification/distribution.md); local installation, packaging and extraction, not hosted export or runtime |
| F10 | Verified within narrowed scope | [Skill verification](../../.github/workflows/skill-verification.yml); metadata, helper tests, catalog, local links, distribution |
| F06-R | In progress | [Remediation tracker](../audits/remediation-plan.md); do not confuse documentation completion with defect closure |

## 2. Reproducible CI evidence

Both [regular CI](https://github.com/zhijunio/skills/actions/runs/46122691952) and [Skill verification](https://github.com/zhijunio/skills/actions/runs/46122691606) succeeded at the evidence revision.

| Job | Observed result |
| --- | --- |
| [Metadata](https://github.com/zhijunio/skills/actions/runs/46122691606/job/140910421522) | Upstream metadata projection and local extension policy passed; 12 helper tests passed; catalog current |
| [Package](https://github.com/zhijunio/skills/actions/runs/46122691606/job/140910421785) | Maven build, 59/59 archive entries and resource checks, 59/59 local extraction |
| [Discovery](https://github.com/zhijunio/skills/actions/runs/46122691606/job/140910421802) | 59/59 in each Codex and Claude Code target layout; explicit three-skill dependency group 3/3 in each |
| [Local links](https://github.com/zhijunio/skills/actions/runs/46122691606/job/140910421691) | Lychee offline: 951 total, 692 successful, 259 excluded, zero errors; exit 0 |

The pre-fix audit found two stale version-registry links; both were corrected and the blocking offline check passed. The original packaging defect skipped all three collections; the corrected build and extraction compare actual inventory and bytes. Earlier CI confirmed skill-local notice files survive tested distribution methods. See the distribution report for exact commands and versions.

No local assistant build is claimed. No actual agent session, remote deployment, third-party script execution, or full dependency compatibility matrix was tested by these jobs.

## 3. Decisions established

- Reuse Agent Skills specification, pinned skills-ref, Skills CLI, SkillsJars and Lychee; retain minimal repository-specific verification glue.
- Preserve existing three collections and stable skill names; no bulk expansion.
- Maintain checked-in generated catalog from source metadata and provenance mappings; CI detects staleness.
- Use a single root AGENTS.md and separate design, authoring and contribution documents.
- Treat Codex and Claude Code local installation layouts and local Maven archive/extraction as tested distribution scope; do not claim equivalent runtime or hosted support.
- Use offline Markdown file-link checks as a blocking baseline. External URL availability, anchors, non-link paths and Markdown style linting are outside this baseline. Markdownlint remains a candidate, not an installed gate; avoid broad style-only rewrites.
- Keep behavioral evaluation separate from deterministic validation. No requirement to build a custom evaluation runner in Phase 01.

## 4. Remaining work and ownership

| Finding / risk | Next action | Timing |
| --- | --- | --- |
| A04 provenance | Resolve missing exact notices and unmatched resource origins; retain evidence and applicable licenses | Before publication; parallel with Phase 02 design |
| A13 snapshot 401 | Maintainer checks namespace, token configuration and Maven server ID without sharing secret values | Before publication |
| A02/A03/A12 host behavior | Actual invocation, negative triggers, missing dependencies, strict hosted export if supported | Representative cases in Phase 02; systematic evaluation Phase 03 |
| A06 browser safety | Review scripts and authorization boundaries before running browser cases | Before enabling or endorsing browser execution |
| A07–A10 workflow contracts | Exercise the existing roles, approval gates, change control, generator selection and routing | Phase 02 |
| A01 version and template evidence | Review stale snapshot provenance, technical compatibility and templates | Targeted technical follow-up |
| A11 context efficiency | Compare measured behavior and context, not line count alone | Phase 03 |
| F10 reproducibility | Consider fully locked transitive dependencies and optional Markdown style checks based on need | Follow-up; not a claim of current coverage |

All original audit statuses remain authoritative in the remediation tracker. Only A05 is resolved on the review branch. Do not close other findings based on a green deterministic build.

## 5. Phase transition

The repository foundation is ready for Phase 02 planning and private/local or authorized review work within the documented scope. Proposed phase acceptance is conditional on maintainer review of the outstanding documents and acknowledgement of verification limits. No release approval is requested or inferred. Existing PR remains unmerged because pushes to main trigger snapshot publication and redistribution/authorization blockers remain.

Proceed with the [Phase 02 plan](../plans/phase-02-engineering-workflow.md) after maintainer review. Phase 02 focuses on demonstrated workflow gaps and representative behavior, not indiscriminate new skill creation. Systematic multi-model benchmarking remains Phase 03.
