# Article Structure

Use this as the default article skeleton.

## Recommended sections

1. What problem this topic solves
2. Core principle
3. Key flow or mechanism
4. Key API / abstraction / module / class
5. Source or implementation path
6. Version differences
7. Common mistakes
8. Engineering practice
9. Minimal verification — **same command** as mastery-map / `examples/README.md`; link sample output; if `code_repo` set, do not point at vault `examples/*.java`
10. Self-test
11. Sources

## Writing rules

- Prefer complete sentences.
- One main idea per paragraph.
- Split long paragraphs.
- Let the main article carry the main line, not every small teaching unit.
- Use tables only when comparison is genuinely clearer than prose.
- Use diagrams to reduce explanation cost, not as decoration.
- If a section can be understood faster from a diagram, default to drawing it.
- Prefer diagrams for:
  - object models
  - call chains
  - decision paths
  - version timelines
  - architecture evolution
  - file-role relationships in multi-file topics
- For mechanism-heavy topics, default to at least:
  - one topic-map / object-model diagram
  - one key flow / call chain diagram
  - one version-delta / architecture-evolution diagram when versions matter
- If a diagram would make the explanation meaningfully clearer, include it.

## Teaching carry-over

For dense topics, prefer this split:

- the main article explains the full model
- `reference/` pages compress key rules, timelines, and decision paths
- focused subtopic notes isolate one sharp edge at a time
- `learning-record.md` captures what this run actually clarified

## Depth rule

Every article should support two layers:

- **Fundamentals**: enough to explain how the topic works
- **Extended**: enough to discuss version changes, trade-offs, and practical boundaries

If the topic is too broad to support both layers well, split it into two articles.

## Multi-file organization

When a topic needs more than one file, group them in a dedicated topic directory (v2):

```text
<topic-dir>/
  TOPIC.yaml
  README.md
  <slug>-mastery-map.md
  <slug>.md
  <slug>-*.md              # 0–2 side articles (mechanism, practice, …)
  <slug>-interview.md
  <slug>-glossary.md       # mechanism / spec topics
  sources/INDEX.md
  reference/
  examples/                 # README.md: commands + pasted output; source here OR in code_repo
  learning-records/
```

`README.md` should explain file roles, reading order (**Recall** / **Explain**), **`examples/README.md`** when demos exist, and a diagram of how files relate.
