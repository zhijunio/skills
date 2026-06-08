# Four-layer QA (de-AI and publish readiness)

Layered self-check (test-pyramid style): **L1 = hard bans**, **L2 = style**, **L3 = substance**, **L4 = human read**. Inspired by persona-writing skills that use **scan → fix → report**; **humanize** stays **audience-agnostic** — no single author persona, no punctuation bans that break technical Markdown.

**Do not import** from persona-only skills: blanket bans on `:` / `——` / `""`, «no headings», 4000-word chat cadence. Technical RFCs and explainers keep headings, tables, and colons when needed ([`handoff-humanize.md`](handoff-humanize.md)).

Run after in-place polish unless the user only wants a scan without edits.

## When to use

| User intent | Layers | Output |
|-------------|--------|--------|
| Default humanize / 润色 | L1 + L2 (fix in place) | **Revised body only** |
| 去 AI 味 + 质检 / QA report | L1–L4 | Body + report below, or report-only if asked |
| Technical long-form pre-publish | L1–L3 + `tech-mastery/SKILL.md` checklist | Body or review notes per user |

**Default:** apply L1–L2 silently; do **not** ship a report unless the user asks.

---

## L1 — Hard rules (must be zero hits after edit)

Full scan; any hit → rewrite before delivery.

### L1-1 Banned phrases (Chinese)

See [`write-zh.md`](write-zh.md) **High-frequency AI tells** and **Replacement table**.

**Scan workflow:** for each row in the replacement table, search the draft (mentally or with editor search). On hit → replace with **Prefer** column; if meaning shifts, shorten the sentence instead of swapping words only.

### L1-2 Mechanical tells

- Opener/closer templates: 综上所述、值得注意的是、不难发现、让我们来看看、总而言之
- Empty depth: 本质上、换句话说、不可否认、具有十分重要的意义
- Hollow triples: 不仅…而且…更… with no concrete fact in the third clause
- English drafts: [`write-en.md`](write-en.md) **Replacement table** (same L1 scan workflow)

### L1-3 Facts boundary (all modes)

| Do not | Do instead |
|--------|------------|
| Invent stats, quotes, 「比如有一次…」 scenes | Use only facts already in the draft or cited sources |
| Add runtime/API behavior not in draft/sources | Flag «needs source»; do not guess |
| Fabricate personal experience | Omit or keep author’s existing voice |

### L1-4 Technical long-form add-on (Chinese)

Also [`tech-mastery/SKILL.md`](../../tech-mastery/SKILL.md) completion checklist and [`article-structure.md`](../../tech-mastery/references/article-structure.md):

| Avoid | Prefer |
|-------|--------|
| 当前 master、main 分支 | **具体版本号**、对应 **tag** 的源码 |
| 最新代码一定… | **以你本机运行版本的源码为准** |
| 据说 / 一般来说（无来源） | 规范章节、release note、文件名 + 链接 |

- **Pattern:** classic / interview narrative → current LTS or production → how to read source for *your* version.
- No claim that **old release** defaults apply to **current LTS** without a version qualifier.
- Code: bounded loops or `// 仅示意，勿在生产抄`; flags note **version default** when behavior changed.

**Pass:** L1-1–L1-4 zero unresolved hits.

---

## L2 — Style consistency

Score each item **pass / fix**; fix in place when obvious.

### L2-1 Audience register

| Audience | Pass if |
|----------|---------|
| Tech blog | Reads like an engineer explaining to a peer, not a textbook or press release |
| RFC / design | Precise terms OK; still no empty openers |
| Personal blog | Colloquial OK; no 「笔者」「本文旨在」 unless author uses them |

### L2-2 Rhythm

- Paragraph openings vary (not every paragraph starts with 在…中 / 随着…)
- At least one **short punch sentence** in long explainers (optional for RFC) — not the only sentence in the §
- Lists and tables serve data, not replace prose for every argument

### L2-2b Paragraph density（去 AI 味时必查）

- **No outline-mode §:** after edits, mechanism sections still read as connected prose, not bullet notes.
- **Length sanity:** if word count dropped sharply with no user request, check whether explanation was wrongly deleted — restore from draft meaning.
- **Merge rule:** OK to merge two sentences that repeat the same fact; **not** OK to merge three causal steps into one telegraphic line.

### L2-3 Positive patterns (encourage, do not force)

- Lead with **what / why** before mechanism detail when the reader is new
- **Version row** before mixing **old interview lore** with **current production** defaults
- One **bridge sentence** after a tangent (扣回主线)
- **Concrete subject:** name API / struct / flag — not 「在该机制下」
- **Admit limits:** 「以对应 tag 源码为准」「并非所有场景都适用」

### L2-4 Technical long-form add-on

- Term first mention: 中文（English） or spec link; API / types / filenames in `monospace`
- No stray English glue in Chinese prose (~~alone~~ → 单靠、仅)
- Optional **TL;DR** (≈3 lines); must match body; **Further reading** = primary links only

**Pass:** L2-1 and L2-2 pass; L2-3/L2-4 best-effort.

---

## L3 — Content quality

### L3-1 Claim support

Each strong claim (performance, default flag, «always/never») has **source, version qualifier, or code/spec link** already in the draft.

### L3-2 Knowledge delivery

- No 「下面我们来介绍」「首先需要了解」 textbook blocks unless RFC-style
- Definitions appear when the term is first needed, not in a detached glossary dump

### L3-3 Coherence (light)

- Section openings tie to previous section’s conclusion
- No duplicate section numbers or TL;DR contradicting body

### L3-4 Technical publish gate

Run **`tech-mastery/SKILL.md`** completion checklist for technical deep-dives (structure, versions, code safety).

**Pass:** L3-1 and L3-2 pass; L3-4 required for technical long-form claiming «publishable».

---

## L4 — Human read (subjective)

Read once end-to-end. One core question:

**General:** «Would a knowledgeable peer write it this way, or does it still sound like generated filler between facts?»

**Technical blog:** «Would a senior engineer trust the version boundaries and stop mis-applying old-release lore to current LTS?»

### L4 dimensions (note only failures)

- **Temperature:** concrete verbs and scenarios vs abstract praise (显著提升、极具)
- **Stance:** collaborator, not lecturer or marketing
- **Flow:** any paragraph that forces re-read for logic gaps
- **Density:** any `###` that became a single vague sentence where the draft previously explained a mechanism

**Pass:** No paragraph feels «AI模板段»; no § wrongly compressed to outline stubs.

---

## Optional QA report format

Use when the user asked for 质检 / report / 检查清单:

```markdown
## Humanize 质检报告

**L1 硬性规则** ✅ / ❌
- 禁用套话：N 处（已改 / 待确认）
- 事实边界：✅ / ❌（说明）
- 技术项（若适用）：✅ / ❌

**L2 风格** ✅ / ❌
- 受众语气：✅ / ❌
- 节奏：✅ / ❌（…）
- 段落未过度压缩：✅ / ❌（…）
- 技术博客项（若适用）：✅ / ❌

**L3 内容** ✅ / ❌
- 论断有据：✅ / ❌
- 技术发布门（若适用）：✅ / ❌

**L4 活人感** ✅ / ❌
- 断点/模板段：（段落或 § 引用）

**修复优先级**（最多 3 条）：…
```

Do not paste long revised body inside the report unless the user asked for report-only.

---

## Layer map (quick)

```text
L1  search & replace bans + no invented facts
L2  audience, rhythm, encouraged tech patterns
L3  claims, coherence, tech-mastery checklist (tech)
L4  one full read — fix template paragraphs
```
