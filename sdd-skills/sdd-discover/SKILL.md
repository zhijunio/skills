---
name: sdd-discover
description: Use before product scoping when the problem, target user, or supporting evidence is unclear, or when research and stakeholder inputs need synthesis. Frame the problem, evidence, assumptions, and key unknowns; return findings to sdd-prd. Optional, not a gate. Not for creating a PRD or feature specification.
---

# sdd-discover

## Role

Clarify whether a product problem is understood well enough to frame scope. Separate observed evidence from stakeholder interpretation and assumptions. Produce a concise exploration summary in chat by default; this is working input for [`sdd-prd`](../sdd-prd/SKILL.md), not a second requirements document or an approval artifact.

Use the user's language. Do not assume interviews have happened, invent research, or treat a stakeholder request as proof of user need.

## When to use

- The user has an early idea but the problem or target user is unclear
- Interviews, feedback, analytics, support cases, or stakeholder input need synthesis before scope is chosen
- The user asks whether an opportunity is understood or worth taking into PRD

## When not to use

- Product direction and problem are clear enough to draft product scope → [`sdd-prd`](../sdd-prd/SKILL.md)
- The user explicitly wants a PRD → use `sdd-prd` directly; incorporate focused clarification there
- A feature needs shared domain/API contracts, a detailed spec, planning, or implementation → use the corresponding downstream SDD skill

## Process

1. Read relevant repository guidance and existing product context. Do not treat planned capabilities as implemented or infer user need from code.
2. Identify the target user and context, the problem and its consequences, current alternatives/workarounds, and the desired outcome. Mark unknowns instead of filling gaps.
3. Classify inputs as direct evidence, stakeholder interpretation, repository fact, assumption, or hypothesis. Preserve source and date when available; note conflicts and evidence limits.
4. Assess whether competitor or market-alternative research could change the problem framing or product direction. When it could, research a bounded set of relevant products or substitute approaches and compare only decision-relevant dimensions against the target user's needs. Use current, attributable sources; prefer vendors' own documentation for claims about product capabilities and identify the source date. Separate verified facts from interpretation, record material gaps in public information, and do not treat a competitor's feature as evidence of user need. Keep this research proportional; omit it when it would not change a decision.
5. Identify only the assumptions and unanswered questions that could change the problem framing or product direction. Ask a small number of focused questions when answers are available now; otherwise propose how to validate them. Do not conduct or claim to conduct user interviews.
6. Summarize the problem statement, evidence, assumptions, alternatives, and key unknowns using [template.md](references/template.md). Include the competitive and alternative landscape only when the research in step 4 is decision-relevant. Omit sections that have no useful information. Keep the result concise and do not produce product requirements, release phases, solution design, acceptance criteria, or API/domain contracts.
7. Give a bounded recommendation: **Ready for PRD**, **Needs validation**, **Reframe**, or **Insufficient evidence**. Explain the reason and the next useful action. This recommendation is advisory; it does not approve product scope or block a user who chooses to proceed with an explicitly provisional PRD.
8. By default, present the summary in chat. Persist it only when requested or when repository convention requires it, following the shared [SDD artifact path policy](../references/output-paths.md). Do not create a discovery file merely as a prerequisite to PRD.

## Output

Return the concise exploration summary and recommendation. If the user wants to continue, pass the confirmed findings, assumptions, and unresolved questions directly into `sdd-prd`; do not require them to copy or approve an intermediate artifact.

For persisted summaries, use a date and concise change history only when repository conventions require versioned records. Discovery notes are provisional and do not receive semantic versions by default.

## Quality checklist

- [ ] The problem, user, context, consequence, and desired outcome are explicit or marked unknown
- [ ] Evidence is sourced and separated from interpretation, assumptions, and hypotheses
- [ ] Competitive research is included only when decision-relevant; product claims have attributable sources and dates, and competitor features are not treated as proof of user need
- [ ] Missing research is not implied to exist
- [ ] Questions and validation actions focus on uncertainties that could change the product direction
- [ ] The recommendation states what is known, what remains uncertain, and a practical next action
- [ ] No product requirements, scope approval, design/API contracts, or implementation decisions are invented
- [ ] Findings can feed directly into `sdd-prd` without a separate mandatory document step

## References

- [Exploration summary template](references/template.md)
- [Worked example](references/example.md)
