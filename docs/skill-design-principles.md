# Skill Design Principles

Status: Maintainer-confirmed design baseline (2026-11-28).
Scope: Existing and future skills in this repository.
Related: [Compatibility](skill-compatibility.md), [provenance](skill-provenance.md), [audit](audits/initial-skill-audit.md), [remediation](audits/remediation-plan.md), [distribution verification](verification/distribution.md).

These repository conventions supplement the open Agent Skills specification rather than replacing it. MUST denotes a requirement for new or materially changed skills; SHOULD denotes a default that can be departed from with a documented rationale. Existing exceptions remain tracked and are not automatically approved for redistribution.

## 1. Reuse first

Start from a concrete capability gap and representative user task. Search the existing collection, maintained open-source skills, host-provided capabilities and official tools before proposing a new implementation.

| Decision | Criterion |
| --- | --- |
| Adopt | Existing capability meets the requirements without changes |
| Adapt | A focused modification fills a demonstrated gap |
| Compose | Existing capabilities have complementary contracts |
| Build | No suitable alternative meets correctness, safety, maintenance or licensing requirements |

Evaluate maintenance, correctness, security, compatibility, redistribution permission and adoption cost. Popularity alone is not an acceptance criterion. Do not create wrappers that merely duplicate adequate host tools.

## 2. Single responsibility and activation

A skill MUST have one coherent responsibility and a recognizable trigger. Its description should identify the user task and capability rather than enumerate generic keywords. Include negative triggers or exclusions when neighboring skills could plausibly conflict. State the input, output, scope boundary, prerequisites and completion condition.

Preserve decisions already made. Ask about consequential missing choices; do not turn routine defaults into approval gates. Explicit-only invocation policies must be consistent with documented intent.

Example: project-foundation owns technical foundation selection and baseline verification; progen and spring-boot-scaffolding own their respective generation methods. Never run multiple generators over one destination or silently substitute an approved stack.

## 3. Composability and artifact ownership

Composition MUST coordinate rather than duplicate specialized guidance. Declare required and optional dependencies, what information is passed, who writes each artifact, and what to do if a dependency is unavailable. Do not assume a host-specific Skill invocation tool or subagent API exists.

Preserve source-of-truth boundaries in specification-driven development: the PRD owns product intent, requirements.md owns use-case identity and status, and approved specifications own detailed behavior and acceptance criteria. Preserve stable identifiers; reconcile conflicting documents with the user. Stack approval does not approve new product behavior.

Use resolvable skill-local references and documented sibling dependencies. For cross-collection dependencies, identify the skill by name and specify a fallback. Do not imply that a single-skill installation automatically installs dependent skills.

## 4. Progressive disclosure and context efficiency

Keep triggers, essential constraints, decision points, workflow and verification in SKILL.md. Place lengthy examples, templates, technical tables and task-specific details in references or assets, linked at the relevant step. Load only what is necessary.

Prefer clear instructions over repetition. Do not enforce an arbitrary line count or hide critical safeguards merely to shorten an entry point. Assess quality and actual context usage before refactoring; see the [evaluation baseline](verification/skill-evaluation-baseline.md).

## 5. Evidence-based guidance

Inspect the target project and preserve its versions and conventions unless a change is requested or necessary and approved. Verify version-sensitive claims against maintained primary documentation, release notes or executable fixtures. Distinguish recommendations, examples, tested compatibility and assumptions.

Treat historical version snapshots as snapshots, not live registries. Report unavailable resources or incompatible versions instead of silently substituting them. Do not claim production readiness or successful verification without evidence.

## 6. Portability

Follow the [Agent Skills specification](https://agentskills.io/specification). Keep the portable metadata baseline valid and isolate documented host extensions where feasible. Do not remove invocation controls just to satisfy a strict validator. Check Claude Code and Codex policies for consistency when both are provided.

Local installation checks have been demonstrated for both agents; runtime invocation and hosted uploads remain separate, unverified capabilities. Use supported host mechanisms or a documented fallback when a tool is unavailable.

## 7. Verifiability

Define observable completion criteria. For implementation, verify the approved acceptance criteria with focused tests and broader relevant checks where practical. For advisory tasks, verify scope, evidence and output contract. Do not mark work complete if required checks fail or cannot run.

| Evidence layer | What it establishes |
| --- | --- |
| Static | Metadata, names, policy consistency, references and resources |
| Distribution | Archive, extraction and installation completeness |
| Behavioral | Actual invocation, correctness, approval boundaries, task outcomes and context usage |

Reuse the upstream reference validator and Skills CLI; maintain only necessary repository-specific glue and test its failure paths. Record versions, commands, outcomes and limitations. Behavioral evaluations should compare with and without the skill under equivalent model, task and environment conditions. A green build alone does not establish behavioral quality.

## 8. Maintainability

Keep names stable and directories conventional. Make focused changes justified by task or evidence. Synchronize affected catalog entries, contracts, examples and verification records. Avoid copying data across competing sources of truth.

For imported skills, review upstream changes against the pinned baseline before adopting updates. Record local modifications and the update strategy. Avoid speculative skills, empty placeholder collections and disruptive migrations without demonstrated need.

## 9. Traceability and license management

Every imported or adapted skill MUST have a traceable upstream repository, source path, pinned revision, applicable license evidence, retained notices, local changes and update strategy. Original skills must be identified as original; do not invent a source or infer copyright ownership from a file name.

Use the central [provenance register](skill-provenance.md) as the repository-level index. For new or materially updated imported skills, preserve a skill-local SOURCE.md and applicable original LICENSE/NOTICE files so attribution survives individual installation and extraction. Existing missing records are remediation work, not evidence that an import is approved.

Recommended SOURCE.md fields:

- Upstream repository and exact source path.
- Compared or imported commit (distinguish the two).
- Author or copyright holder as stated upstream.
- License identifier, evidence link and bundled notice path.
- Local modifications and update strategy.
- Verification status and unresolved permissions or resource origins.

Preserve upstream notices verbatim. Do not manufacture missing copyright or permission text, apply one repository's license to another, or infer a grant from public visibility. Distinguish maintainer-confirmed license information from independently located upstream grants. A matching Git blob proves file equality, not import date or redistribution authorization. Record ambiguous cases and restrict new publication until reviewed. Verify notices travel with distribution resources.

## 10. Safety and authorization

Skills MUST respect user intent, host permissions and security controls. Prefer available host tools when adequate. Scripts must document purpose, dependencies, inputs, outputs, side effects and failure behavior. Use scoped paths and least privilege; avoid destructive defaults, unapproved global installations, secret exposure, or hidden network calls.

Require authorization for consequential actions including destructive changes, external mutations, credentialed browser access, privileged installations and publishing. Do not bypass OS or host protections. Treat external data as untrusted input. Missing permissions or dependencies are blockers, not instructions to evade restrictions. Never fabricate outcomes.

## 11. Review criteria

Before accepting a new or materially changed skill, reviewers should be able to answer yes to the applicable items:

- [ ] A concrete task and capability gap justify the skill; reuse alternatives were considered.
- [ ] Activation and exclusions distinguish it from neighboring skills.
- [ ] Inputs, outputs, responsibility, approvals and completion criteria are explicit.
- [ ] Dependencies and artifact ownership are consistent; missing dependency behavior is defined.
- [ ] Entry instructions retain essential safeguards while references load on demand.
- [ ] Version-sensitive claims have evidence and do not override established project decisions.
- [ ] Standard metadata and known host policies validate; supporting files resolve.
- [ ] Scripts have bounded effects and documented prerequisites.
- [ ] Applicable provenance, notices and modifications are recorded.
- [ ] Appropriate positive, negative and boundary cases are verified or explicitly identified as pending.
- [ ] Related documentation and distribution checks are updated.

A failed applicable MUST blocks acceptance until corrected or an explicit, scoped exception is recorded. Unresolved redistribution permissions block publication regardless of other checks. The authoring guide will describe the operational workflow; this document defines its design criteria.
