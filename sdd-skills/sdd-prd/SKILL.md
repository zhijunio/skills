---
name: sdd-prd
description: Clarify product direction or revise scope. Maintain the master PRD and obtain explicit approval for each revision.
---

# Maintain the PRD

1. Start with known decisions and relevant evidence. Clarify the users, problem, goal, current scope, exclusions, and success measures. Ask only questions that can change a decision. Offer concise choices and a recommendation where useful. Research alternatives or sketch a low-fidelity flow if a product scope decision depends on it. Carry confirmed decisions into the PRD and distinguish evidence from assumptions. Use `TBD` for unknown targets.
2. Present a concise scope summary and get the user's confirmation. Then save the PRD as `Draft (approval pending)` using the fixed [template](references/template.md). Use the existing canonical path or default to `docs/PRD.md`. Revise the same PRD across iterations. A separate PRD is for a separately managed product.
3. Keep the PRD to one or two pages. Write product goals and capabilities in plain language. Put unconfirmed decisions in Open Questions. Leave technical design, detailed use cases, and acceptance criteria to downstream artifacts.
4. Present the saved draft for explicit approval of that exact revision. Keep every edit Draft until approved, including wording changes. Record the approver and date when marking it `Approved`. The earlier scope confirmation authorizes drafting.
5. Start at `0.1`. Increment the minor version for substantive scope changes. Update the date and Change History while retaining earlier approvals. While the canonical PRD is Draft, pause downstream work that requires current approval. Once approved, route affected requirements and specs for reconciliation.

Report the path and current approval status.
