# Workflow

## Intake

Capture before editing:

```text
input:          file path or paste
language_lane:  zh | en | bilingual
tone:           technical | casual | formal | (user-specified)
effort:         touch | standard | deep
preserve:       facts, headings, links, code blocks, tables
forbidden:      (user list, e.g. "keep 您", "don't shorten")
```

If language is ambiguous, infer from **draft body**, not chat language.

## Edit strategy

1. **Pass A — tells** — Remove pattern hits (patterns.md + lane file). Prefer sentence-level edits.
2. **Pass B — rhythm** — Mix short/long sentences; break rule-of-three; drop filler openers.
3. **Pass C — soul** — One opinion or concrete detail per major section *only if already implied by draft*; never fabricate.
4. **Pass D — vet** — Re-read for over-scrubbing (sterile Wikipedia voice) and missed tells.

## Delivery modes

| User wants | Deliver |
|------------|---------|
| Default | Revised full text only — minimal meta |
| "show changes" / diff | Short bullet list of pattern classes fixed |
| deep / 质检 | Revised text + report per `report-template.md` |

## File edits

When input is a path: edit in place unless user asked for a new file. Do not reformat unrelated files.
