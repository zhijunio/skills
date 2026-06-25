---
name: article-write
description: Article and document writing lifecycle (写) — ideate, gather, transcribe, compose, refine with author voice. Long-form, tutorials, scripts, talks, Xiaohongshu. Default voice in voice-default.md. Use for 写文档, 写文章, 选题, 审校定稿, publish-ready; not neutral-only de-AI on finished text, URL ingestion, topic learning, or codebase audit.
---

# Article Write

**Role: 写** — End-to-end **article/document production** in five stages. Pick **one stage** per run; load only that stage's references plus shared core.

**Default author voice:** `references/voice-default.md`. User style notes or samples override when provided.

**Does not:** full-page URL/PDF ingestion; systematic topic learning workspace; codebase audit; neutral-only de-AI without author persona (use session de-AI polish for that); multi-platform publish layout export.

## Hard rules

1. **No fabricated first-hand stories** — mark `[待作者补充]` instead of inventing「我实测」.
2. **Human owns angle; AI expands** — see `references/collaboration.md`.
3. **One stage, few files** — never bulk-read all of `references/`.
4. **Stay in scope** — gather does not auto-compose; refine does not reorder chapters unless asked.
5. **Voice L1** — drafts using default voice run `qa-layers.md` L1 before delivery.

## Stage router

| Stage | User signals | Read |
|-------|--------------|------|
| **ideate** | 选题、方向、标题、大纲对比 | `references/ideate.md` |
| **gather** | 写作调研、查资料、核实来源 | `references/gather.md` |
| **transcribe** | 语音/口述/会议记录 → 文章 | `references/transcribe.md` |
| **compose** | 从素材写成稿 | `references/compose.md` + form below |
| **refine** | 审校、定稿、作者声线、publish-ready | `references/refine.md` → `voice-default.md`, `qa-layers.md` |

**Not this skill:** 部署、排错、UI、与写作无关的搜索。

## Form (compose / refine)

| Form | When | Read |
|------|------|------|
| `long` (default) | 公众号长文、观点、故事 | `references/long-form.md` |
| `tutorial` | 配置、部署、手把手步骤 | `references/tutorial-form.md` |
| `script` | 视频脚本口语化 | `references/script-form.md` |
| `speech` | 分享、培训、B 站讲稿 | `references/speech-form.md` |
| `xiaohongshu` | 小红书笔记 | `references/xiaohongshu-form.md`; netfeel → `chinese-netfeel-guard.md` |

## Depth

| Level | Output |
|-------|--------|
| `outline` | 标题 + 分段大纲 + 要点 |
| `draft` (default) | 全文 |
| `publish-ready` | 全文 + L1–L4 摘要（`qa-layers.md` 报告格式） |

## Shared core

- `references/README.md` — stage/form index (read instead of listing all files)
- `references/intake.md` — triggers, HKR, missing-info questions
- `references/collaboration.md` — human / AI split
- `references/methodology.md` — 选题来源、爆款案例
- `references/style-examples.md` — 改稿对照

## Related skills

| Skill | When |
|-------|------|
| `article-read` | gather 前单链抓取；用户单独触发 |
| `topic-learn` | 系统化主题资产；非营销向空写 |
| `humanizer` | 中性去 AI，无作者声线 |
| `flowguard` | 长稿多阶段防漂移 |

## Compose flow (summary)

1. Intake + HKR (`intake.md`)
2. Pick form (`long-form.md` / `tutorial-form.md` / …)
3. Draft with `voice-default.md` (or user voice)
4. `draft` → L1 spot-check; `publish-ready` → full `qa-layers.md`
