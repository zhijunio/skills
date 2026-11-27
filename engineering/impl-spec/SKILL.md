---
name: impl-spec
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
5. If the index row or matching specification is missing, multiple specifications match, the ID/title/status differs between the index and specification, or the specification is incomplete, stop and ask for the documents to be reconciled. Do not edit files yet.

## Resolve blockers before implementation

Inspect the implementation and tests to find the smallest complete change. Check the specification for missing behavior, contradictory rules, acceptance criteria that cannot be observed or implemented, significant technical decisions that the repository does not settle, and unmet dependencies.

- Ask concise, decision-oriented questions for unresolved product or technical decisions. Do not choose business behavior or a consequential stack/platform on the user's behalf.
- If a dependency is not `DONE`, ask whether to implement that dependency first, proceed after it is completed, or stop. Do not expand this use case to include the dependency.
- If the use case is `DONE`, report that and stop unless the user explicitly requests repair or reimplementation.
- Keep statuses unchanged while resolving blockers.

## Start implementation

Once the contract and prerequisites are clear, make the status transition the first repository change:

1. Change this use case from `TODO` to `IN_PROGRESS` in both the requirements summary row and specification frontmatter. In the summary, update the first-column icon from `❎` to `👷🏽` as well.
2. Read both files back and confirm the canonical ID, title, and status agree before changing implementation files. Confirm the summary icon matches its status value.
3. If both already say `IN_PROGRESS` and the summary icon is `👷🏽`, resume only when the user's intent to continue is clear; otherwise ask. If the status values disagree, stop and ask which state is correct.

Do not change another use case's status. Do not copy detailed behavior or acceptance criteria into `docs/requirements.md`.

## Implement and verify

- Follow repository instructions, existing architecture, naming, and testing conventions.
- Implement every in-scope acceptance criterion, including validation, failures, authorization, privacy, and state transitions where specified. Do not add behavior from `Out of scope` or invent requirements.
- Add or update focused automated tests for observable acceptance behavior. Run focused tests first, then broader relevant checks when practical.
- For a large use case, split work into independently verifiable tasks only when their file ownership and contracts can be defined without overlapping edits; integrate the results and run the relevant checks before completion.
- If implementation reveals a material ambiguity, pause and ask. Leave both status entries `IN_PROGRESS` and the summary icon `👷🏽` while unresolved.
- If tests fail or work remains, do not mark the use case complete. Report the failure or remaining work and the verification evidence.
- Review the final diff for scope, accidental changes, and matching status values.

## Complete the use case

Only after all acceptance criteria are implemented and relevant verification passes:

1. Change the status to `DONE` in both the requirements summary row and specification frontmatter. Set the summary icon to `✅`.
2. Read both files back and confirm their statuses match, the summary icon is correct, and no other use-case status changed.
3. Report implemented behavior, tests and checks run with their results, and the final status.

`DONE` means the specified behavior is implemented and verified, not merely that code was written. In the summary, display its icon (`✅`) in the first column.
