---
name: implement-use-case
description: Implement and verify one approved use case from its specification file, using docs/requirements.md as the use-case index and status register. Use when the user asks to implement a specific UC-###; do not draft specifications or implement an unspecified group of use cases.
disable-model-invocation: true
---

# Implement a Use Case

Implement exactly one use case, following its specification and the repository's conventions. Keep `docs/requirements.md` as the concise use-case index; keep behavior details and acceptance criteria in the corresponding specification under `docs/specs/`.

## Identify the use case and its contract

1. Use the single `UC-###` identified by the user. If the immediate conversation clearly identifies one specification and no other use case, continue with that ID; otherwise ask which ID to implement. Never infer a collection of use cases.
2. Read the matching row in `docs/requirements.md` (or the user-supplied requirements path) and find exactly one specification whose numeric filename prefix matches the ID, such as `UC-001` → `docs/specs/001-*.md`.
3. Read the full specification, including its `UseCase Details`, `Business Rules`, `Acceptance Criteria`, `In Scope`, `Out of scope`, metadata, and dependencies. Read the PRD and other referenced product or technical documentation only as needed for context.
4. Treat the specification as the source of truth for behavior and acceptance. Treat the requirements summary as the canonical use-case identity and status index. Do not expect detailed use-case sections or acceptance criteria in `docs/requirements.md`.
5. If the index row or matching specification is missing, multiple specifications match, the ID/title/status differs between the index and specification (or dependency IDs disagree where the index records them), or the specification is incomplete, stop and ask for the documents to be reconciled. Do not edit files yet.

## Resolve blockers before implementation

Inspect the implementation and tests to find the smallest complete change. Read any reviewed implementation plan and reconcile it with the authoritative specification and applicable design. For material multi-step work without a plan, use implementation-planning guidance within the authorized scope before status or code edits; a bounded change may use a concise outline. Planning must not silently change scope or authorize additional use cases. Check the specification for missing behavior, contradictory rules, acceptance criteria that cannot be observed or implemented, significant technical decisions that the repository does not settle, and unmet dependencies.

- Ask concise, decision-oriented questions for unresolved product or technical decisions. Do not choose business behavior or a consequential stack/platform on the user's behalf.
- Required dependencies must be `DONE` with evidence applicable to the current contract. If a dependency is incomplete or its evidence is stale, normally stop or arrange separately authorized prerequisite work. An exception requires explicit authorization for the specific dependency, a recorded rationale, risk, boundaries and remaining verification obligations; do not mark the dependency DONE or bypass missing/cyclic identity checks. An exception cannot waive a necessary acceptance check or justify claiming behavior verified without evidence. Do not expand this use case to include the dependency.
- If the use case is `DONE`, report that and stop unless the user explicitly requests repair, reimplementation or an approved behavior change. Identify the affected prior completion evidence and downstream use cases; reconcile any changed behavior with the authoritative specification before reopening. Defect repair restores the existing contract, not an invented replacement. A defect report alone does not authorize reopening.
- Resolve dependency IDs against the canonical index and inspect their specifications as needed to detect missing IDs, self-dependencies and dependency cycles. Report the exact conflicting IDs rather than treating an unresolved dependency as complete.
- Check the summary icon against the canonical status before any status edit. Keep statuses unchanged while resolving blockers.

## Start implementation

Once the contract and prerequisites are clear, make the status transition the first repository change:

1. Change this use case from `TODO` to `IN_PROGRESS`, or from `DONE` to `IN_PROGRESS` for explicitly authorized repair or change after the preceding checks, in both the requirements summary row and specification frontmatter. In the summary, update the first-column icon from `❎` (TODO) or `✅` (DONE) to `👷🏽` as well.
2. Read both files back and confirm the canonical ID, title, and status agree before changing implementation files. Confirm the summary icon matches its status value.
3. If both already say `IN_PROGRESS` and the summary icon is `👷🏽`, resume only when the user's intent to continue is clear; otherwise ask. If the status values disagree, stop and ask which state is correct.

If updating either status document fails or read-back reveals a mismatch, stop before further implementation edits. Report which write succeeded and which did not. Restore prior consistent values only if it is safe to do so without overwriting concurrent changes; otherwise request reconciliation. Apply the same safeguard when completing a use case.

Do not change another use case's status. Do not copy detailed behavior or acceptance criteria into `docs/requirements.md`.

## Implement and verify

- Follow repository instructions, existing architecture, naming, and testing conventions.
- Implement every in-scope acceptance criterion, including validation, failures, authorization, privacy, and state transitions where specified. Do not add behavior from `Out of scope` or invent requirements.
- Read any applicable reviewed test strategy and map its required obligations to acceptance checks. Use test-strategy for material unresolved verification choices and stack-specific guidance for test implementation. Add or update focused automated tests for observable acceptance behavior. Run focused tests first, then broader relevant checks when practical.
- For a large use case, split work into independently verifiable tasks only when their file ownership and contracts can be defined without overlapping edits; integrate the results and run the relevant checks before completion.
- If implementation reveals a material ambiguity, pause and ask. Leave both status entries `IN_PROGRESS` and the summary icon `👷🏽` while unresolved.
- If tests fail, required checks cannot run, or work remains, do not mark the use case complete. Distinguish PASS, FAIL and NOT RUN in the completion report; include the actual commands or evidence, remaining work and reason for unavailable checks. Do not substitute an unexecuted plan or a prototype verdict for verification.
- Review the final diff for scope, accidental changes, and matching status values. For reopened work, refresh affected acceptance and regression evidence and report downstream verification needs without changing unrelated use-case statuses.

## Complete the use case

Only after all acceptance criteria are implemented and relevant verification passes:

1. Change the status to `DONE` in both the requirements summary row and specification frontmatter. Set the summary icon to `✅`.
2. Read both files back and confirm their statuses match, the summary icon is correct, and no other use-case status changed.
3. Report implemented behavior, tests and checks run with their results, and the final status.

`DONE` means the specified behavior is implemented and verified, not merely that code was written. In the summary, display its icon (`✅`) in the first column.
