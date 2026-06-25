# MCP fallback

Scripts (`read.sh`) are the **reproducible default**. Use MCP when signals below or after script failure.

## When

| Signal | Examples |
|--------|----------|
| Heavy client-side JS | SPA, empty curl with large HTML |
| Anti-bot | Repeated `[read] tier=proxy status=fail` |
| CN stealth table | `domain-stealth.md` |
| User requests Firecrawl / browser | Follow user |

## Firecrawl MCP

1. Scrape per host docs.
2. Normalize to Markdown-like text.
3. Same wrapper as `output.md`.

Do not skip MCP when configured and scripts already failed for same URL.

## Order

```text
1. read.sh (ordinary URLs)
2. MCP — on failure or stealth signals
3. pdf-extract / user paste
```

## Merge rules

Prefer longer structured body; never merge invented paragraphs. Document method (script vs MCP) in topic notes.
