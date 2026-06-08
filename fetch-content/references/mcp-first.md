# MCP first — when scripts are fallback

Scripts (`fetch-url.sh`) are the **reproducible default**. Some pages need **MCP or browser** first.

## 1. Use MCP before scripts when

| Signal | Examples |
|--------|----------|
| Heavy client-side rendering | SPAs, empty curl body with large HTML shell |
| Known anti-bot | Repeated script failure on same domain |
| WeChat / paywalled after all hops | User still needs content |
| User says use Firecrawl / scrape / browser | Follow user + host Firecrawl skill |

## 2. Firecrawl skill

If the workspace has the **Firecrawl** skill (`firecrawl/SKILL.md`):

1. Read and follow that skill for scrape/search/crawl.
2. Normalize MCP output to Markdown-like text.
3. Apply the same **output wrapper** as scripts — see [`output-save-handoff.md`](output-save-handoff.md).

Do **not** skip Firecrawl when it is configured and scripts already failed once for the same URL.

## 3. Order of attempts (recommended)

```
1. MCP (Firecrawl or host fetch MCP) — if signals above OR scripts failed
2. fetch-url.sh — default for normal URLs
3. pdf-extract / user paste — last resort
```

For **ordinary blogs/docs** with no JS issues: **scripts first** keeps logs reproducible; MCP on failure is fine.

## 4. Merge MCP + script results

| Case | Rule |
|------|------|
| MCP succeeds, scripts also ran | Prefer **longer, structured** body; prefer MCP if script hit paywall |
| Both partial | Do not merge invented paragraphs; pick one source or ask user |
| MCP JSON | Extract markdown/content field before display |

## 5. Pairing note

`tech-mastery` Source: document which method fetched each source (script vs MCP) in `plan.md` or topic notes — helps re-fetch later.
