# Source ingest

Load during **Step 2: Source**. Formats: `formats.md` → Sources & INDEX.

## When

- Before citing a URL or PDF in mastery-map or main article
- When refreshing and adding a new L1/L2 anchor
- When a normative doc is paywalled → record in **Gaps**, do not fabricate

## Archive layout

```text
sources/
  INDEX.md
  l1-rfc9110-caching.md      # optional per-source note
  l2-spring-cache-docs.md
  raw/                       # optional fetched markdown blobs
```

One INDEX row per source: link + one line (when to use / which section it supports).

## Per-source note (optional)

```markdown
---
url: https://…
layer: L1
fetched_at: 2026-06-25
version_or_tag: RFC 9110
---

# Short title

Key excerpts or summary tied to mastery-map sections.
```

## Ingest steps

1. Fetch URL or PDF with **`article-read`** (`scripts/read.sh`); normative URLs → `article-read/references/topic-sources.md`.
2. Save under `sources/` or `sources/raw/`; never leave evidence only in chat.
3. Add INDEX entry under the correct **L1–L4** heading.
4. If the claim is core, ensure **≥1 L1 or L2** entry exists before Write.
5. Mark unknowns in **Gaps** or **Open** — do not promote parametric memory to verified prose.

```bash
READ_ROOT="<path-to>/article-read"
bash "${READ_ROOT}/scripts/read.sh" "https://…" > sources/raw/example.md
```

## Quality bar

| Good | Bad |
|------|-----|
| INDEX link + layer + fetch date | URL only in main article, not in INDEX |
| Version/tag on normative sources |「我记得 RFC 是这么写的」无锚点 |
| Gaps listed when L1 missing | Silent skip of verification because sources are thin |

## Refresh

Merge new sources into INDEX; do not duplicate rows. Re-fetch when `version_or_tag` drifts from `TOPIC.yaml` → `runtime`.
