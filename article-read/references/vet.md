# Vet — failures and false success

## True failure

- `read.sh` exit ≠ 0
- Body &lt; threshold after all tiers
- Paywall hints in first 30 lines (`cascade.md`)

**Report:**

```text
## 抓取失败
- URL: …
- Tiers tried: (from [read] stderr lines)
- 建议: MCP / 浏览器粘贴 / 检查飞书凭证 / gh auth
```

Never output fabricated article text.

## False success

| Signal | Action |
|--------|--------|
| Login page HTML | Reject; do not deliver as article |
| 「订阅」「继续阅读」only | Reject |
| JSON error wrapper | Parse error field; fail |
| Lorem / placeholder | Fail |

## After failure

1. `mcp-fallback.md` if not tried
2. Ask user to paste or export PDF
3. Stop — do not continue to summarize mode
