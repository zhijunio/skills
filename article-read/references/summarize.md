# Summarize mode

User wants **摘要 / 总结** and provides a **URL** (or PDF path).

## Order

1. **Fetch** — `read.sh` (same as convert). Never summarize from memory or parametric knowledge alone.
2. **If fetch fails** — stop; `vet.md`; do not fabricate summary.
3. **Summarize** — faithful to fetched body only:
   - 3–7 bullets OR 1 short paragraph (user preference)
   - Preserve numbers, names, links from source
   - Mark uncertainty if body was truncated (`preview`)

## Boundaries

| Do | Don't |
|----|-------|
| Cite which URL was fetched | Add facts not in body |
| Offer full save after summary | Replace fetch with WebSearch snippets |
| Separate「正文」and「摘要」sections in reply | Merge into one wall of text without labels |

## With convert + summarize in one turn

Deliver:

```text
## 正文
{wrapper + content or truncated note}

## 摘要
{summary}
```
