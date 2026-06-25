# 小红书平台约束与产物目录

写小红书笔记时加载本文件。配图生成、发布 MCP、竞品库脚本等能力不在本仓库维护；此处只规定**平台硬约束**与**产物落盘规范**。

## 平台硬约束

| 约束 | 限制 |
|------|------|
| 标题 | ≤20 个中文字（英文单词按 1 字计，数字/标点按 1 字计） |
| 正文 | ≤1000 字 |
| 配图 | 1–18 张，推荐 3:4 竖版（1080×1440） |
| 标签 | 通过 tags 参数传入，不要写在正文里 |

## 文件组织

每篇笔记的所有产物（HTML、图片、文案）统一存放在独立目录，防止覆盖：

```text
<工作目录>/posts/
└── YYYYMMDD-<slug>/          # 如 20260206-opus46
    ├── cover.html            # HTML 源（保留，可微调重截）
    ├── cover.png
    ├── features.html
    ├── features.png
    ├── ...
    └── content.md            # 文案 + 标签 + 发布元数据
```

**目录命名：** `YYYYMMDD-<slug>`

- 日期：发布/创建日期
- slug：2–4 个词的英文标识（如 `opus46`、`cursor-tips`）

**content.md 格式：**

```markdown
---
title: 标题
date: 2026-02-06
status: published | draft
feed_id: （发布后回填）
---

## 正文

文案内容...

## 标签

tag1, tag2, tag3, ...
```

**工作流集成：**

- 生成配图时，HTML 与 PNG 存入该目录
- 写文案时，保存 `content.md` 到该目录
- 发布时从 `content.md` 读取；图片用该目录绝对路径
- 发布后回填 `feed_id` 到 frontmatter

## 网感与翻译腔

若输出出现翻译腔、英文思维链污染，加载同目录下的 `chinese-netfeel-guard.md` 与 `chinese-netfeel/` 参考语料。
