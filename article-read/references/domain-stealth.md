# Stealth / JS-heavy domains

When `read.sh` body is thin/empty or host matches below → `mcp-fallback.md`.

## Prefer MCP after script failure

| Domain | Notes |
|--------|-------|
| `mp.weixin.qq.com` | read.sh first; MCP if still thin |
| `zhuanlan.zhihu.com` | Anti-bot + JS |
| `juejin.cn` | SPA |
| `x.com`, `twitter.com` | Dynamic content |
| Unknown SPA | Empty HTML shell from curl |

## Usually script-only

`sspai.com`, `blog.csdn.net`, `ruanyifeng.com`, many static blogs.

## Agent flow

1. `read.sh` once.
2. Thin body + table domain → MCP.
3. User said `stealth` → one script try, then MCP for table hosts.

Stealth tier in this repo = **MCP/browser**, not vendored Scrapling.
