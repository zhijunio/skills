---
name: product-requirements
description: Facilitate product brainstorming or MVP scoping, draft a PRD.md from confirmed decisions, and review, approve or revise product requirements when requested.
---

# Product Requirements

Turn a product idea into a short, decision-backed summary. Use the user's language and begin with the context and decisions they have already shared.

## Responsibility and handoff

Own product intent, users, MVP scope and the PRD. Use grilling for a requested stress-test of a specific decision, domain-modeling for canonical terminology and domain decisions, and behavior-specification for detailed behavior of an individual use case. Do not start those workflows automatically or decide technical architecture here. Hand the PRD to prd-to-use-cases when requested and its product scope authority is established. Distinguish confirmation of the discussion summary from review of the actual drafted PRD; do not represent a newly drafted document as formally approved.

## Guide the discussion

1. **Ground the problem.** Identify the primary user and ask about a recent occasion when they tried to achieve the outcome. Capture the trigger, current workaround, and where it falls short. Treat an unobserved problem or assumed audience as a hypothesis.
2. **Research the landscape.** Search the web for direct and adjacent competitors, and search GitHub for existing open-source projects that solve the same or a neighboring problem. Prefer product owners' documentation and repository pages. For each relevant result, capture its name, intended users, core workflow, differentiator, and any decision-relevant limits; for GitHub projects also check license, recent activity, release history, and implementation fit. Link sources and distinguish verified facts from inference. Keep the shortlist to the few results that could change the product decision. If the idea is clearly internal or the user asks to skip research, say why the landscape check is not useful and record it as an assumption.
3. **Compare build, reuse, and differentiation.** Use the research to identify what users can already do with competitors or existing projects. Compare two or three plausible approaches, including adopting or extending an open-source option when viable. Explain the user benefit and the costs in effort, adoption, maintenance, and complexity. Invite the user to choose or revise the options.
4. **Set the MVP boundary.** Describe the smallest end-to-end journey that delivers the intended outcome. Agree on which capabilities it needs, how it differs from available alternatives, and which ideas to defer. Confirm proposals before recording them as commitments.
5. **Define a success signal.** Identify an observable user behavior or outcome that would suggest the MVP works. Record unresolved decisions, risks, and assumptions for the PRD.

Ask one or more related questions together when that makes the discussion easier. For each question, offer a short set of plausible answers when useful, mark one as recommended, and allow the user to provide another answer. Keep the questions focused on decisions that affect the product. Skip questions already answered. If the user asks to wrap up early, summarize what is known and carry the gaps forward as open questions.

## Produce the summary

Return a concise Markdown summary with these fields once the primary user, problem, goal, and MVP boundary are clear, or when the user asks to wrap up:

- What we are going to build
- Vision / goal
- Problem and current workaround
- Target users
- Competitors and open-source alternatives
- MVP scope and core user journey
- Out of scope for now
- Success signal
- Open questions, risks, and assumptions

Keep agreed decisions in the main summary and place material uncertainties in the final field. Keep the scope at the product level. Then ask the user to confirm the summary or request changes. If they request changes, revise the summary and ask for confirmation again. Treat an explicit confirmation with no requested changes as approval to draft the PRD.

After the user confirms without changes, generate a self-contained `PRD.md` from the approved summary, following the user's requested format when provided. Otherwise start the file with YAML frontmatter containing `status: draft`, `approval: pending`, `version: 0.1`, and `date: YYYY-MM-DD` (use the date the PRD is generated), then use concise, standard sections for overview, problem, goals, target users, competitors and open-source alternatives, MVP features, out-of-scope items, success metrics, risks and assumptions, and open questions. Keep document lifecycle details in the frontmatter rather than repeating them as a title block. Preserve source links, unresolved decisions, and assumptions. Honor an explicit user path or the project's established PRD location; otherwise write `docs/PRD.md` relative to the target project root. If the root-level `PRD.md` or `docs/PRD.md` already exists, inspect it and ask before creating a duplicate or overwriting it. Report the actual file path, draft status, material unresolved decisions and the next review decision needed for the requirements handoff. Do not automatically change approval fields or begin implementation.

## Review, approval and change

When the user asks to review, approve or revise an existing PRD, read the authoritative file first. Do not restart discovery or recreate the PRD unnecessarily. Identify the exact revision and scope under review, including material unresolved decisions and proposed exclusions. Ask only about unresolved decisions that affect approval. An explicit instruction approving that exact revision is sufficient; confirmation of the earlier discussion summary is not.

For documents using the default metadata convention, record `status: approved` and `approval: approved` only after the product owner explicitly approves the actual document. Preserve established alternative lifecycle conventions and legacy approved documents; do not add metadata merely to migrate them. A material scope revision becomes `status: draft`, `approval: pending` until explicitly approved again. Preserve or identify the prior approved revision and its scope in existing version history or an agreed decision record. Do not silently overwrite a previously approved baseline or downstream index; obtain the requested revision scope and record affected use cases, specifications, plans and evidence. Nonmaterial corrections do not automatically invalidate approval. Never invent unresolved product decisions or treat approval as implementation or publication authorization.

Report the actual approval evidence, document path and applicable revision; hand off to requirements decomposition only when authoritative scope is clear.
