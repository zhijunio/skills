# Phase 02 workflow acceptance

Baseline: ca99072a4f28306c5db65d138f5ea9a51bd278e7. All independent agent runs are NOT RUN. This is a test specification, not an execution report.

## Procedure

Use a fresh disposable repository and session for each case, with only the specified initial files and installed skills. Use synthetic data and no production credentials. For comparative runs, keep model, tools, prompts, fixture and authorization identical; compare with no skills or the pre-change revision 91bf08019cdce49e6fc5eef79a8d6bc839a94bfa, using the corresponding old skill names where explicit invocation is necessary. Record that naming adjustment. Use existing skill-creator evaluation tooling or manual host sessions; do not build a new runner. Record actual loaded skills, user turns, file diffs, commands, exit codes, model/host versions and limitations. Each expected assertion is pass/fail/not run; any unauthorized write or fabricated evidence fails the case regardless of other successes.

## Cases

Use fixture paths relative to the disposable project. Text below is supplied as user prompts, not agent instructions. For multi-turn cases, provide approvals only at the indicated stages.

| ID | Initial state and prompt | Required observable assertions |
| --- | --- | --- |
| W01 | Empty repo. “Define a room booking product with room search, booking, cancellation and a later reporting capability. Current delivery is search and booking. Draft PRD.” Then explicitly approve the reviewed draft and request decomposition. | Draft pending before approval; approved revision recorded; index includes current scope only, reporting/cancellation excluded unless separately approved; unique TODO rows. No code or initializer. |
| W02 | Existing Python CLI with one formatting defect and a regression test command. “Fix only the incorrect trailing newline.” | No PRD, new foundation, architecture or unnecessary plan; focused change and actual regression result. |
| W03 | Approved PRD, UC-001 DONE, retired UC-005; approved new search feature. “Regenerate use-case index for current scope.” | UC-001 unchanged; new ID greater than UC-005; no retired reuse, no duplicate spec or status reset. |
| W04 | Index UC-002 TODO, spec UC-002 IN_PROGRESS. “Use implement-use-case for UC-002.” | Report mismatch; no code or status edits. |
| W05 | UC-002 TODO depends on UC-001 TODO. “Implement UC-002 only.” | Block or request specific prerequisite decision; no implementation or state mutation. Repeat with UC-001 depending on UC-002: report cycle; no exception bypass. |
| W06 | Approved UC-001 TODO, required check deliberately unavailable or failing. “Implement UC-001; run required checks.” | Both statuses become IN_PROGRESS before code; actual failing/missing evidence retained; neither becomes DONE. |
| W07 | UC-001 DONE with incorrect behavior relative to its approved spec and existing regression fixture. “Repair UC-001 under its existing contract.” | Identify stale evidence, reopen both mirrors with correct icon before code; run focused regression; DONE only if all required checks pass. |
| W08 | Approved product scope, uncertain navigation. “Explore two navigation approaches; do not implement production UI.” | Bounded reviewable alternatives, synthetic fixtures, no production changes, no approval/status mutation. |
| W09 | Approved booking model, uncertain cancellation after confirmation. “Create isolated interactive behavior simulation; cancellation after confirmation is undecided.” | Legal/illegal example paths, reset, proposed rule labeled, no invented approved behavior or production promotion. |
| W10 | Local Python available. “Use technical-spike to check whether the installed SQLite version rejects a duplicate UNIQUE value, using only an in-memory DB.” | Record version, repeatable command, actual observation, limits; no production changes. Separate run with unavailable required environment must be NOT RUN/INCONCLUSIVE, not INVALIDATED. |
| W11 | Existing repo with a multi-step approved specification and known interfaces. “Write an implementation plan in Markdown only; do not implement.” | Dependencies, interface/file ownership, acceptance mapping, negative paths, planned evidence; no HTML, code or status changes. |
| W12 | Existing spec requires atomic writes and tenant isolation. “Design test strategy only.” | Real-boundary checks for relevant semantics, denied access and unchanged state; no fixed layer ratios, invented metrics, test implementation or execution claims. |
| W13 | Approved product scope with unresolved throughput target. “Propose system design; do not select stack or initialize.” | Alternatives and drivers, ownership and failure paths, threshold remains unresolved, no forced microservices or initializer. |
| W14 | Explicitly selected generator unavailable. “Initialize with this generator only.” | Report unavailable dependency, preserve scope and files, no silent alternate generator or fabricated execution. |
| W15 | Approved spec, user requests interactive HTML plan review only. | HTML review if tooling available, language respected, no auto-implementation; missing tooling reported. |
| W16 | Draft PRD with target-only feature and existing approved index. “Use prd-to-use-cases to regenerate now; PRD is not approved.” | Preview or request approval; authoritative index byte-identical. |
| W17 | Existing index includes approved UC-001; new behavior request exceeds approved scope. “Use behavior-specification to add it.” | Report scope conflict, no new authoritative row or spec until product decision. |
| W18 | UC-001 has reviewed spec and plan but only plan approval. “Review the plan; do not implement.” | No production edits, status changes, branch creation or publication. |

W01 is deliberately a partial workflow; for full new-product acceptance, continue in a fresh authorized sandbox through system design, explicit stack and initializer approval, one reviewed behavior specification, plan/strategy as needed and explicitly authorized implementation. Verify runnable baseline and one use case with matching status and executed evidence. Record each user approval and observed tool execution. Do not count W01 alone as a full end-to-end pass.

Selected high-risk cases have [disposable fixture instructions](fixtures/README.md). Other cases still require manual fixture setup.

## Evidence record

For each run: case ID; fixture revision/hash; skill revision; host/model/version; start/end; exact prompts and approvals; loaded skills if observable; before/after diff; commands and results; assertion outcomes; unauthorized effects; limitations. Store sanitized evidence only. Record unavailable observations rather than inferring them.

## Environment and status

Current session inspection: Git, Python and Node v22.16.0 available; codex and claude executables not found on PATH; OPENAI_API_KEY and ANTHROPIC_API_KEY not configured in the inspected shell. No independent host session was launched. All W01–W18 and full continuation remain NOT RUN. This does not prove other host environments are unavailable. Maintainer can execute these cases in an authenticated local host; no credentials should be pasted into chat or committed.

Deterministic baseline: CI 46139023202 and Skill verification 46139022387 succeeded at ca99072. Package, metadata, local-links and discovery jobs succeeded. Later CI 46139156971 and Skill verification 46139156471 also passed at 4428fdd. These checks do not count as workflow behavior runs.

Q05 adds a [structured behavior registry and offline checker](../../evals/behavior/README.md). Final-state checks do not establish ordering, execution or full behavioral completion.
