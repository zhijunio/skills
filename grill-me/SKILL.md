---
name: grill-me
description: Interrogate an existing plan or design one question at a time (with a recommended answer each) until decision-tree branches align; explore the codebase first when possible. Use for grill me, stress-test plan, challenge design. Writes no files; does not replace sdd-plan or tech-mastery full outputs.
argument-hint: "Topic to grill or path to a doc (optional)"
---

# Grill me — plan interrogation

Inspired by [mattpocock/skills `grill-me`](https://github.com/mattpocock/skills/tree/main/skills/productivity/grill-me) (MIT).

## Purpose

Interrogate the user’s **plan, design, or architecture idea** until both sides agree on each branch of the **decision tree**.

## Hard rules

- **One question at a time**; wait for an answer before the next.
- **Each question includes your recommended answer** (and a short rationale) so the user can decide quickly.
- **Explore the codebase/docs first** when that answers the question; do not ask the user for known facts.
- **No file writes in this skill** — no `docs/sdd/plans/`, no brainstorm design docs, no commits.
- After grilling, if the user wants artifacts on disk: suggest **`tech-mastery`** (long-form), **`sdd-brainstorm`** / **`sdd-plan`** (in the app repo SDD flow); user must switch explicitly.

## Flow

1. Confirm the subject (paste, path, or summary from the current thread).
2. List major decision branches (mental or brief list; no long essay).
3. Walk branches in dependency order: prerequisites → downstream questions.
4. Stop when open items are zero or clearly marked **deferred by user**.

## If argument-hint is provided

Treat it as scope (e.g. “database migration only”); do not expand into unrelated subsystems.
