---
name: sdd-yolo
description: Orchestrate the SDD workflow from product framing through implementation when the user explicitly requests an end-to-end run.
---

# sdd-yolo

Run the existing SDD skills in sequence for one user-approved product scope. This skill coordinates the workflow; it does not replace the rules in its stages or create a separate orchestration document.

## Workflow

1. Read repository guidance and inspect the current PRD, design/API contracts, specs, plans, and their approval/readiness status. Check whether any downstream artifact is stale against an approved PRD revision and resume from the earliest incomplete or stale stage. Follow existing project paths and the shared [SDD artifact path policy](../references/output-paths.md) only when the repository has no convention. Reuse and revise canonical artifacts instead of creating duplicates.
2. If the problem or evidence is unclear, use [`sdd-discover`](../sdd-discover/SKILL.md) as optional framing. Otherwise go directly to [`sdd-prd`](../sdd-prd/SKILL.md). Create or revise the master PRD only when the requested scope is new or changed, or the current PRD is not Approved. Present changed or new scope for approval, stop until approved, and persist it. When the approved scope already covers the request, resume without asking for duplicate approval.
3. For the approved delivery batch or selected approved requirement IDs, run [`sdd-design`](../sdd-design/SKILL.md). Update domain/API contracts only for real gaps. If contract-owner acceptance is needed, stop for it. For a routine no-change result, carry the Ready result and requirement coverage into the resulting spec; create a separate assessment only when the assessment is substantial, spans multiple specs, the user requests it, or repository convention requires it.
4. Use [`sdd-spec`](../sdd-spec/SKILL.md) to define independently verifiable feature slices. Skip the breakdown map for a single slice; use it for multiple slices. Ordinarily present each spec for approval. For an explicitly requested small-feature package, do not ask for separate spec approval; hand the complete Draft spec to [`sdd-plan`](../sdd-plan/SKILL.md) for joint preparation and presentation in the next step.
5. Create one plan per spec with [`sdd-plan`](../sdd-plan/SKILL.md); ordinarily, the spec must already be approved. Map every AC to a verifiable slice and keep design/API contracts as linked sources of truth. For the joint small-feature flow, reuse the plan prepared in step 4 and present the spec and plan together for one approval; persist both after approval. Otherwise present the plan for approval, then persist it. A thin one-slice plan is valid.
6. Run [`sdd-build`](../sdd-build/SKILL.md) only for an approved spec-plan pair. Implement one spec at a time, follow its verification steps, and record evidence in the plan. If implementation requires changing product behavior, shared contracts, ACs, or slice boundaries, stop and route back to the owning stage.
7. Stop after the requested scope is implemented and evidence is recorded. Do not automatically run [`sdd-review`](../sdd-review/SKILL.md); use it when requested. [`sdd-improve`](../sdd-improve/SKILL.md) is a separate broad-scope scan, not part of this pipeline.

## Approval and scope

- Read and follow the linked stage skill before performing that stage. Its detailed instructions are authoritative for artifact content, quality, and routing.
- Never approve product scope, contract changes, a spec, or a plan on the user's behalf. File presence is not approval. Stop at each required approval point and resume only after explicit user approval.
- An end-to-end request authorizes orchestration, not unresolved product decisions or approval of artifacts the user has not reviewed.
- Work only on the user's requested delivery batch. Do not interpret a whole-product request as approval to implement every roadmap item. For multiple features, complete and verify one approved spec-plan pair at a time.
- Keep discovery summaries and routine no-change design assessments in chat or in the spec as defined by the stage skills. Do not create empty contracts, duplicate PRDs, or a separate yolo report.
- At each stop, report the completed stages, artifact paths, pending decision or approval, and the precise next action. On resume, re-read artifact status instead of inferring approval from the previous run.

## Completion

The run is complete when the requested approved scope has been implemented, the plan records verification evidence and material deviations, and all unresolved checks are visible. Present the outcome and stop.
