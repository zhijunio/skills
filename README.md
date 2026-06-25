# zhijunio-skills

面向 **Cursor** 等 AI 编程助手的 Agent Skills 库。每个 skill 一个目录，入口为 `SKILL.md`（YAML `name` / `description` 供发现）；复杂流程放在 `references/`，可重复执行放在 `scripts/`。

## 读 / 写 / 学 / 审

四个核心 skill 覆盖内容工作与代码审查的主路径。**每次任务只显式触发一个**；skill 正文可**引用**其他 skill 说明边界与衔接，但不得替用户自动跑完整条外 skill 流程。

| 目标 | 目录 | 做什么 |
|------|------|--------|
| **读** | [`article-read`](article-read/SKILL.md) | URL / PDF → Markdown；抓取、存档、可选摘要 |
| **写** | [`article-write`](article-write/SKILL.md) | 选题 → 调研 → 成稿 → 定稿（文档 / 文章） |
| **学** | [`topic-learn`](topic-learn/SKILL.md) | 单主题溯源、验证、mastery 资产、访谈层 |
| **审** | [`codebase-audit`](codebase-audit/SKILL.md) | 只读代码库健康审查：架构、代码质量、安全、测试 / CI、依赖与文档 |

典型顺序（**每步须用户单独开口**；括号内为 skill 名）：

```text
读链接 (article-read) → 学透主题 (topic-learn) → 写成稿 (article-write) → 去 AI 味 (humanizer，可选)
审代码库 (codebase-audit) — 独立，针对目标仓库
长任务防漂移 (flowguard) — 可包裹任意多步实现
```

### 与相近能力的边界

| 本库 skill | 不是 |
|------------|------|
| `article-read` | 写稿、主题学习工作区、审代码 |
| `article-write` | 整页 URL 抓取、系统化主题学习、审代码 |
| `topic-learn` | 单链摘要、营销向空写、审代码 |
| `codebase-audit` | 写文章、主题课、抓链接 |

与 **[shadcn/improve](https://github.com/shadcn/improve)** 的区别：`codebase-audit` 做多柱只读审查与路线图；`improve` 产出可执行的 `plans/` 改进方案（本库未收录）。

## 全部 skill

### 核心四件套

| 目录 | 说明 |
|------|------|
| [`article-read`](article-read/SKILL.md) | `read.sh`；convert / summarize / save / preview |
| [`article-write`](article-write/SKILL.md) | 五阶段；`voice-default.md`；outline / draft / publish-ready |
| [`topic-learn`](topic-learn/SKILL.md) | `new` / `refresh`；`topic_scaffold.sh` + `topic_check.sh`；mastery-map、验证、访谈层 |
| [`codebase-audit`](codebase-audit/SKILL.md) | snapshot / standard / deep；P0 / P1 / P2 路线图 |

### 辅助

| 目录 | 说明 |
|------|------|
| [`humanizer`](humanizer/SKILL.md) | 已有文稿去 AI 味；touch / standard / deep |
| [`flowguard`](flowguard/SKILL.md) | 长任务防漂移：preflight、压缩、handoff |
| [`skill-audit`](skill-audit/SKILL.md) | 审查、分类、设计 skill 库与触发描述 |
| [`skill-creator`](skill-creator/SKILL.md) | 新建 skill、跑 eval、优化 description |
| [`keep`](keep/SKILL.md) | Keep 跑步记录 → Garmin 风格 `running.json` |
| [`jinrishici`](jinrishici/SKILL.md) | 今日诗词 API |

### 去 AI 味 vs 写作定稿

| 场景 | 用 |
|------|-----|
| 已有文稿、中性去 AI 味 | `humanizer` |
| 写作流程中的作者声线 + publish-ready | `article-write` refine 阶段 |

## 安装

使用 [skills CLI](https://github.com/vercel-labs/skills)（`npx skills`）。需 **Node.js 18+**。  
GitHub 仓库：[zhijunio/skills](https://github.com/zhijunio/skills)（旧名 `zhijunio-skills` 会重定向）。

### 推荐：`npx skills`（Cursor 全局）

```bash
# 查看本库可安装的 skill
npx skills add zhijunio/skills --list

# 安装全部 10 个 skill 到 Cursor（全局）
npx skills add zhijunio/skills -g -a cursor -y --all

# 只装四件套
npx skills add zhijunio/skills -g -a cursor -y \
  --skill article-read --skill article-write \
  --skill topic-learn --skill codebase-audit
```

安装后文件在 `~/.agents/skills/<name>/`（skills CLI 会挂到 Cursor）。验证：

```bash
npx skills ls -a cursor -g
```

**新开 Agent 对话** 刷新索引。

### 本地开发（改 skill 即时生效）

在克隆目录内，从本地路径安装（不拉 GitHub）：

```bash
cd ~/github/zhijunio-skills
npx skills add . -g -a cursor -y --all
```

### 更新 / 卸载

```bash
npx skills update -g -a cursor -y          # 更新已安装 skill
npx skills remove article-read -g -a cursor -y
```

### 手动 symlink（可选）

不用 `npx` 时，可链到 `~/.cursor/skills/`（开发中改文件即时生效）：

```bash
REPO=~/github/zhijunio-skills
mkdir -p ~/.cursor/skills
for skill in article-read article-write codebase-audit topic-learn \
  humanizer flowguard skill-audit skill-creator keep jinrishici; do
  ln -sfn "$REPO/$skill" ~/.cursor/skills/$skill
done
```

勿同时用 `npx` 全局安装 **和** 手动链同一 skill，以免重复加载。

### 清理旧 skill 名

```bash
npx skills remove architecture-audit content-craft tech-mastery \
  fetch-content mj-writer -g -a cursor -y 2>/dev/null || true
rm -f ~/.cursor/skills/{architecture-audit,content-craft,tech-mastery,fetch-content,mj-writer}
```

### 脚本依赖

| Skill | 依赖 |
|-------|------|
| `article-read` | `bash`, `python3`, `requests`（见 `article-read/scripts/requirements.txt`） |
| `keep` | `python3`, `requests` |
| `jinrishici` | `python3` |

## 外部推荐（不收录本库）

| 来源 | 用途 | 安装 |
|------|------|------|
| [shadcn/improve](https://github.com/shadcn/improve) | 通用代码库改进审计 → `plans/` | `npx skills add shadcn/improve` |
| [mattpocock/skills](https://github.com/mattpocock/skills) | grill-me、handoff、caveman、edit-article 等 | 克隆后 symlink 到 `~/.cursor/skills/` |

## 目录结构

```text
<skill>/
  SKILL.md        # 入口：name、description、路由
  references/     # 可选：长文档、检查表
  scripts/        # 可选：shell / Python
  evals/          # 可选：行为断言
```

维护本仓库时，Agent 路由见 [`AGENTS.md`](AGENTS.md)。

### 校验

```bash
bash scripts/validate-skills.sh
```

检查：frontmatter `name` 与目录一致、`description` 存在、eval JSON 合法、`scripts/*.sh` 的 `bash -n`、`scripts/*.py` 的 `py_compile`。无 eval 的 skill（除 `skill-creator`）会 WARN。`skill-creator` 允许 SKILL 超过 120 行。

## 致谢与许可

`article-read` 早期分层参考了 [Waza `read`](https://github.com/tw93/Waza) 与 [web-content-fetcher](https://github.com/shirenchuang/web-content-fetcher)。工作流思路与 [mattpocock/skills](https://github.com/mattpocock/skills)、[shadcn/improve](https://github.com/shadcn/improve) 有重叠 — 请直接安装上游。

[MIT License](LICENSE)
