# Router

`READ_ROOT` = directory containing `SKILL.md` and `scripts/`.

## Decision tree

```
Input?
├─ Path to repo text file (not .pdf fetch) → Read tool — STOP
├─ mode=summarize + URL → summarize.md
├─ feishu.cn / larksuite.com → domain-cn.md → read.sh
├─ mp.weixin.qq.com → domain-cn.md → read.sh
├─ zhuanlan.zhihu.com / juejin.cn / x.com → domain-stealth.md → read.sh → MCP if thin
├─ github.com / raw.githubusercontent.com → domain-github-pdf.md → read.sh
├─ .pdf URL or local .pdf → domain-github-pdf.md → read.sh
├─ read.sh fails or thin body → mcp-fallback.md
└─ generic https → cascade.md via read.sh
```

## Load map

| After classification | File |
|----------------------|------|
| Proxy / paywall rules | `cascade.md` |
| 微信 / 飞书 | `domain-cn.md` |
| SPA / stealth | `domain-stealth.md` |
| GitHub / PDF | `domain-github-pdf.md` |
| MCP | `mcp-fallback.md` |
| Delivery | `output.md` |
| Topic research sources | `topic-sources.md` |
| Failure report | `vet.md` |
