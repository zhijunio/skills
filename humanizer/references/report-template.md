# QA Report Template (deep mode)

Report in **Chinese** unless draft is English-only. Paths and quotes stay literal.

```markdown
# 去 AI 味质检 — {title or filename}

> 日期: YYYY-MM-DD
> 语言: zh | en | bilingual
> 档位: deep

## 结论

{一句：通过 / 需返工；主要残留问题}

## L1 内容完整性

{pass | fail + 说明}

## L2 Tell 清除

| 模式类 | 命中数（修前→修后） | 备注 |
|--------|---------------------|------|

## L3 人声

{pass | fail + 段落引用}

## L4 朗读

{pass | fail + 具体句}

## 已排除（vet）

| 原文片段 | 未改原因 |

## 未处理

{用户 forbidden 项、刻意保留的正式语体等}
```

Deliver **revised text first**, report second — unless user asked report only.
