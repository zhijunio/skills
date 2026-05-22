# Bookmark Skill

Bookmark management for Claude Code: save and query web links.

## Features

- **Save** — `/bookmark <url> [#tag1] [#tag2]`
- **Query** — `/bookmark-query <range>`

## Install

Dependencies live under `~/.claude/skills/bookmark/` when installed there.

## Usage

### Save

```bash
/bookmark https://example.com
/bookmark https://react.dev #react #docs
/bookmark https://example.com file:~/my-bookmarks.md
```

### Query

```bash
/bookmark-query today
/bookmark-query yesterday
/bookmark-query week
/bookmark-query month
/bookmark-query all
/bookmark-query 2026-04-21
```

## Development

```bash
cd ~/.claude/skills/bookmark
npx vitest run
```

### Layout

```
bookmark/
├── SKILL.md
├── package.json
├── vitest.config.js
├── bookmarks.md
├── README.md
└── tests/
    ├── test-utils.js
    ├── bookmark.test.js
    └── bookmark.integration.test.js
```

## Bookmark format

```markdown
### 2026-04-21

- [ZhiJun Blog](https://blog.zhijun.io/) #blog #java
  > Blog on Java, Spring, microservices, architecture, Kubernetes, DevOps, AI tooling, and weekly notes
```

## License

MIT
