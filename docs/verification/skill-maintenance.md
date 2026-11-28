# Skill maintenance and regression

Status: Q10 maintenance protocol and deterministic checker hardening; independent agent regressions NOT RUN.

## Evidence-driven change

Classify a finding as activation, instruction/authorization, technical correctness, composition, compatibility, distribution, provenance, stale reference, efficiency or evaluator defect. Record the exact revision, affected skill, reproduction and evidence. Separate a proposed risk from observed failure. Prioritize unauthorized effects, incorrect behavior and invalid evidence over style or size.

Reproduce on a frozen baseline, add a stable regression case with required assertions, make the smallest justified correction, and rerun the failing case and relevant neighbors. Use existing [quality gates](quality-gates.md) and [evaluation standard](../skill-evaluation-standard.md). Do not modify expected results to hide failures. If a case was wrong, document why and retain its history. No independent agent result can be inferred from a synthetic checker test.

## Upstream and technical drift

For imported skills, consult [provenance](../skill-provenance.md); compare pinned and proposed upstream revisions, entry points, resources, license and dependencies. Separate upstream changes from local edits; retain intentional local boundaries. Check official version-specific documentation and actual resolved dependencies for technical changes. Missing upstream or external link access is a blocker to verification, not proof of deletion or permission. No unattended upstream imports. Review source and rights before copying.

Review broken references, host upgrades, framework changes and reproducible defects when relevant changes arise. The offline link check does not verify external freshness. Keep historical baseline snapshots unchanged and record new evidence separately.

## Optimization and migration

Compare actual matched outcomes before shortening instructions or changing descriptions. Keep holdout prompts out of tuning. Preserve correctness, approval gates and relevant safety rules. Do not optimize a skill based only on byte size. For renames, merges, replacements and removals, identify callers, dependency paths, invocation policies, distribution and dataset impact; record migration mapping and obtain review for material changes. Regenerate catalog and update provenance, README and affected contracts in the same change. Do not create aliases by default.

## Regression record

Record finding ID, source revision, reproduction, expected and observed behavior, fix revision, exact checks, PASS/FAIL/NOT RUN and remaining risks. Keep test inputs outside packaged skills and sanitized results outside production projects. Review applicable release blockers separately.

## Q10 evaluator correction

At 35b80f6 the selected final-state checker compared only existing protected files for blocked cases and only the target index row for completion cases. This could miss new fixture files or unrelated index edits. Q10 adds strict fixture inventory and byte comparisons for W04/W05/W05-cycle/W16, whole-index comparison allowing only the intended status transition for W06/W07, and rejection of symlinks and nonregular protected files. Host metadata (.git, .agents, .claude, __pycache__) is excluded from strict inventory; keep evaluation outputs outside the candidate. The checker is not a sandbox and does not establish transient writes, remote effects or actual test execution. Added regression tests for new files, unrelated index rows, nonregular files and symlinks; CI pending.

No skill instruction changes or independent model regression results are claimed.
