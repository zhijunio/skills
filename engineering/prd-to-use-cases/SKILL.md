---
name: prd-to-use-cases
description: Generate or regenerate a requirements.md use-case summary from a product requirements document (PRD.md). Use when translating a PRD into implementable use cases and statuses; do not use for implementation or implementation-status audits.
disable-model-invocation: true
---

# PRD to Use Cases

Create `requirements.md` from `prd.md`, treating the PRD as the source of truth and the repository's existing requirements document as the format reference.

## Inputs and output

- Honor paths supplied by the user. Otherwise, use the established PRD path relative to the target project root, preferring `docs/PRD.md` and falling back to a root-level `PRD.md` if it is the only existing PRD. If both exist and the authoritative file is unclear, ask the user. Default output is `docs/requirements.md`.
- Before writing, read the entire PRD and, when present, the entire existing `docs/requirements.md` to learn the repository's current format and conventions.
- Treat the existing requirements document as a structural example and the canonical identity/status register for existing use cases, not as authority for product scope. Inspect matching specifications when reconciling identities. Resolve content against the PRD.
- Write only the requested requirements file. Do not implement features or modify the PRD.
- Establish the authoritative product scope from the user's instructions and existing project records. For the default PRD metadata, `status: approved` and `approval: approved` identify an approved revision when consistent with the recorded authority; draft/pending or conflicting values require reconciliation. If the PRD is explicitly draft or pending, or authority is disputed, surface that fact and request the necessary scope decision before changing the authoritative index. An explicit approval of the exact revision may establish authority; ask the PRD owner to reconcile conflicting metadata rather than modifying the PRD here. A clearly labeled preview may be returned without changing files. Do not infer approval solely from a missing metadata field or force a new metadata schema on legacy projects.

## Derive the requirements

- Establish the approved current delivery scope separately from the long-term target product scope. If the requested decomposition scope is unclear, clarify before writing; target-only, proposed and deferred capabilities must not become current committed use cases. A full-product index is appropriate when explicitly approved as the decomposition scope.
- Identify every explicit, in-scope capability in the PRD and represent it as a summary table row with an ID, a concise verb-led title, and a status.
- Exclude items explicitly marked optional, future, later, or out of scope.
- Keep titles grounded in the PRD without inventing behavior to fill gaps.
- Use separate rows for independently implementable capabilities; combine duplicate statements that describe the same capability.
- Order foundational use cases before dependent ones. Preserve IDs and statuses for matching existing use cases; allocate new sequential IDs after the highest known current or retired ID. Never renumber existing use cases. If scope changes would remove or materially alter an existing use case, report the conflict and obtain approval before changing the index; do not silently delete existing entries or legacy details.
- Set each newly derived use case's status to `TODO` and its status icon to `❎`. Do not infer implementation status from the codebase unless the user explicitly asks for a status audit. The allowed status values are `TODO`, `IN_PROGRESS`, and `DONE`, with icons `❎`, `👷🏽`, and `✅` respectively.

## Required document shape

1. `# <Product Name> Requirements`
2. A short introduction linking to the actual PRD path, explaining scope exclusions and status meaning.
3. `## Use Case Summary` containing a Markdown table with `Icon`, `UseCase ID`, `Use Case Title`, and `Status` columns, in that order. Put the status icon in the first column and the status code alone in `Status`.

## Verify before finishing

Confirm that every in-scope PRD capability is represented in the summary, excluded scope was not promoted into requirements, IDs are unique, new IDs are sequential, and existing IDs and statuses are preserved. Check known retired IDs before allocating a new ID; do not reuse them. Keep the summary as an index; detailed behavior and acceptance criteria belong in the matching specification. If an existing specification conflicts with the proposed index, stop and request reconciliation rather than modifying it.
