# Tech-mastery formats

按需加载本文件；**Write** 另读 [`article-structure.md`](article-structure.md)，**Interviewize** 另读 [`interviewization.md`](interviewization.md)。

---

## Mission（mastery-map → ## Mission）

| Section | Rule |
|---------|------|
| **Why** | 1–3 句；掌握后的具体结果，非「理解 X」 |
| **Success looks like** | 读者可观察能力（能讲、能 demo、能答追问）；不写 humanize/文件清单 |
| **Out of scope** | 刻意不做的相邻主题 |
| **Constraints** | 可选：版本、runtime、语言、深度 |

- Success + Out of scope 不齐 → 不开 Source  
- Mission 超一屏 → 拆题  
- refresh 改 Mission 时 append learning record

---

## Sources & INDEX

| Layer | 示例 |
|-------|------|
| **L1** | RFC、JLS、OpenJDK tag、厂商规范 |
| **L2** | 框架/产品官方文档或源码 |
| **L3** | 维护者博客、设计说明 |
| **L4** | 二手资料 — 仅辅助，不能单独支撑核心主张 |

**INDEX.md**：每条 = 链接 + 一行（何时用 / 支撑主稿哪节）。维护 **Gaps**、**Open**。核心主张缺 L1/L2 → 不写或标 Open。

推荐 frontmatter：`url` · `layer` · `fetched_at` · `version_or_tag`

---

## Verification

| 字段 | 含义 |
|------|------|
| `TOPIC.yaml` → `runtime` | 叙述基线（规范/JDK 版本） |
| `verified_on` | demo **实际运行**环境 + 日期 |
| `code_repo` | 可选；源码在外部仓，输出仍粘贴到 vault `examples/README.md` |

**mastery-map 模板：**

```markdown
## Verification
- **Type:** demo | walkthrough | version-compare | repro
- **Command:** …
- **Code:** examples/ 或 code_repo
- **Verified on:** …
- **Narrative baseline:** …
- **Sample output:** → examples/README.md
```

`runtime` ≠ `verified_on` 时：practice / examples 必须双写并粘贴真实输出。  
有 demo 时 **Command 五处一致**：mastery-map · examples/README · topic README · practice · 主稿最小验证节。

| vault `examples/` | 外部 `code_repo` |
|-------------------|------------------|
| 单脚本、≤2 小文件 | Maven/Gradle、多文件、CI |

refresh 重跑 demo 后：更新 `verified_on` + examples 样例输出。

---

## Glossary

文件 `<slug>-glossary.md`；机制/规范类建议有。

```markdown
**Term**:
定义 1–2 句
_Avoid_: 易混别名
```

主稿、分稿、interview 术语与 glossary 一致。

---

## Learning record

路径 `learning-records/NNNN-slug.md`，递增编号。

**何时写：** 纠正非显然误解 · 记录用户已有深度 · Mission/边界变更  
**不写：**  mere 覆盖、重复 glossary、会话流水账

```markdown
# 短标题
1–3 句：纠正/新边界及对后续 run 的含义
```

---

## TOPIC.yaml（模板）

```yaml
slug: topic-slug
mode: new          # new | refresh
phase: shipped     # 或 in_progress
language: zh
runtime: "叙述基线，如 JDK 21"
verified_on: "OpenJDK 8 — mvn test 2026-06-05"
code_repo: ""      # 可选 URL
gaps: []
```
