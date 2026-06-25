# Proxy cascade

Generic https (after platform branches in `read.sh`):

1. `defuddle.md/{url}`
2. `r.jina.ai/{url}`
3. direct `curl`

WeChat uses **jina → defuddle** — see `domain-cn.md`.

## Paywall heuristics (`nonempty_ok`)

Reject hop when first ~30 lines match (case-insensitive):

`subscribe`, `sign in`, `登录`, `继续阅读`, `付费内容`, `此内容已被发布者删除`

Minimum body: 60 chars (120 for WeChat length-only check).

## When all hops fail

Exit 1; stderr lists `[read] tier=proxy status=fail …`  
→ `vet.md` → `mcp-fallback.md`.

## Anti-patterns

| Don't | Do |
|-------|-----|
| Bare curl as first choice for topic sources | `read.sh` |
| Save login wall | Report paywall |
| Summarize without body | Fetch first |
