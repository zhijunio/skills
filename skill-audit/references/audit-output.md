# Skill audit — output format and gotchas

## Advisory output

1. Decision: create, improve, split, merge, or do not create
2. Category: one primary taxonomy category
3. Skill brief: filled from `assets/skill-brief-template.md`
4. Implementation notes: files to create/edit and validation commands
5. Risks: overbreadth, obviousness, missing setup, missing verification, weak trigger

## Repository work

Create or update files, then:

```bash
bash scripts/validate-skills.sh
```

## Gotchas

- Do not make a knowledge dump — use decisions, checklists, templates, verification.
- Do not put all details in SKILL.md — long material belongs in support files.
- Description is a **trigger contract** — concrete user phrases, not marketing.
- Provide defaults, decision criteria, escape hatches — not brittle railroading.
- Ship with validation: commands, evals, example prompts, or usage metrics.
- Name direct actions and escalation boundaries — not vague "be proactive."
