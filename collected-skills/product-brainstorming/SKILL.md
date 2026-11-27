---
name: product-brainstorming
description: Facilitate product brainstorming or MVP scoping, confirm the final summary, and draft a PRD.md from the approved decisions.
---

# Product Brainstorming

Turn a product idea into a short, decision-backed summary. Use the user's language and begin with the context and decisions they have already shared.

## Guide the discussion

1. **Ground the problem.** Identify the primary user and ask about a recent occasion when they tried to achieve the outcome. Capture the trigger, current workaround, and where it falls short. Treat an unobserved problem or assumed audience as a hypothesis.
2. **Explore options.** For a decision that materially changes the product, compare two or three plausible approaches using the user's priorities. Explain what each would make easier for the user and what it would cost in effort, adoption, or complexity. Invite the user to choose or revise the options.
3. **Set the MVP boundary.** Describe the smallest end-to-end journey that delivers the intended outcome. Agree on which capabilities it needs and which ideas to defer. Confirm proposals before recording them as commitments.
4. **Define a success signal.** Identify an observable user behavior or outcome that would suggest the MVP works. Record unresolved decisions, risks, and assumptions for the PRD.

Ask one or more related questions together when that makes the discussion easier. For each question, offer a short set of plausible answers when useful, mark one as recommended, and allow the user to provide another answer. Keep the questions focused on decisions that affect the product. Skip questions already answered. If the user asks to wrap up early, summarize what is known and carry the gaps forward as open questions.

## Produce the summary

Return a concise Markdown summary with these fields once the primary user, problem, goal, and MVP boundary are clear, or when the user asks to wrap up:

- What we are going to build
- Vision / goal
- Problem and current workaround
- Target users
- MVP scope and core user journey
- Out of scope for now
- Success signal
- Open questions, risks, and assumptions

Keep agreed decisions in the main summary and place material uncertainties in the final field. Keep the scope at the product level. Then ask the user to confirm the summary or request changes. If they request changes, revise the summary and ask for confirmation again. Treat an explicit confirmation with no requested changes as approval to draft the PRD.

After the user confirms without changes, generate a self-contained `PRD.md` from the approved summary, following the user's requested format when provided. Otherwise use concise, standard PRD sections for overview, problem, goals, target users, MVP features, out-of-scope items, success metrics, risks and assumptions, and open questions. Preserve unresolved decisions as open questions or assumptions. Write the file in the current project directory. If `PRD.md` already exists there, preserve it and ask the user where to save the new PRD before writing. Report the file path when complete.
