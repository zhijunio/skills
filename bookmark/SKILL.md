---
name: bookmark
description: Saves links as bookmarks to local Markdown file. Use when user wants to save a URL for later reference, during research, or when collecting reference materials.
---

## Overview

This skill saves web links to a local Markdown bookmark file. The agent fetches page metadata (title, description) using available tools and generates 1-2 relevant tags if not provided by the user.

## Bookmark File Path

**Default:** `~/.claude/skills/bookmark/bookmarks.md`

**Customize:** User can specify a custom path:
- Set `BOOKMARK_FILE` environment variable
- Or specify in command: `/bookmark <url> file:/path/to/file.md`
- Or create `~/.bookmark-config.json`:
  ```json
  {
    "bookmark_file": "/path/to/your/bookmarks.md"
  }
  ```

## When to Use

- User provides a URL to save
- User says "bookmark this", "save link", "收藏这个" (save this link)
- User is doing research and collecting references
- User wants to organize links for later retrieval

## Process

### 1. Extract URL and Tags

Parse user input to extract:
- **URL**: The link to save (required)
- **Tags**: User-provided tags, format: `tag` or `#tag` (optional, # prefix is auto-added if missing)
- **File path**: Optional custom path (e.g., `file:/path/to/file.md`)

If URL is missing, ask for it.

**Tag normalization:**
- If user provides tags with `#`, keep them as-is
- If user provides tags without `#`, auto-add `#` prefix
- Convert to lowercase
- Max 2 tags

### 2. Auto-Generate Tags (If Not Provided)

If user did NOT provide tags:
- Use available tools to fetch page content
- Analyze title, description, and content to generate 1-2 tags
- Use lowercase English words only
- Prefer: technology names, topics, content types (e.g., `react`, `docs`, `tutorial`, `api`)
- Max 2 tags, keep them relevant and specific

If user PROVIDED tags, use those directly (skip auto-generation).

### 3. Validate and Deduplicate

- Check URL format (starts with `http://` or `https://`)
- Check if URL already exists in the bookmark file
- If duplicate, inform user and skip

### 4. Append to Bookmarks

Format:
```markdown
### YYYY-MM-DD

- [Page Title](https://example.com) #tag1 #tag2
  > Optional description
```

- Group by date (create new section if today's section doesn't exist)
- Use `###` for date headers (level 3 heading)
- Append to end of file
- One line per bookmark, description on next line with `>`

### 5. Confirm

Show the saved entry to user for confirmation.

## Common Rationalizations

| Excuse | Rebuttal |
|--------|----------|
| "I'll add the title later" | No - fetch metadata now or use URL as fallback |
| "Let me save multiple at once" | No - one at a time ensures each is validated |
| "I'll skip the date header" | No - date headers enable chronological browsing |
| "Tags can be added later" | No - capture context while fresh, max 2 keeps it simple |

## Red Flags

- Saving URLs that require login (authentication pages)
- Saving URLs with query parameters containing tokens/secrets
- Description contains sensitive information
- More than 2 tags requested (enforce simplicity)

## Verification

- [ ] URL is valid (starts with http:// or https://)
- [ ] Title is present (fetched or URL fallback)
- [ ] Date header exists for today (using `###` format)
- [ ] Entry appended correctly
- [ ] No duplicates in file
- [ ] Tags are lowercase, max 2 (or user-provided)

## Commands

### Save Bookmark

**Natural language:**
- "Save this link: https://..."
- "bookmark this: https://..."
- "Add to bookmarks https://..."
- "Save https://... #react #docs"
- "Save to ~/my-bookmarks.md https://..."

**Slash command:**
```
/bookmark <url> [#tag1] [#tag2] [file:/path/to/file.md]
```

### Query Bookmarks

**Slash command:**
```
/bookmark-query <range> [file:/path/to/file.md]
```

**Ranges:**
- `today` - Today's bookmarks (### YYYY-MM-DD where date equals today)
- `yesterday` - Yesterday's bookmarks
- `week` - This week's bookmarks (Monday to today)
- `lastweek` - Last week's bookmarks (previous Monday to Sunday)
- `month` - This month's bookmarks (### YYYY-MM where month equals current month)
- `all` - All bookmarks (return entire file content)
- Specific date: `2026-04-21` - Bookmarks from that specific date

**Process:**
1. Parse the range parameter and optional file path
2. Calculate target date(s) based on range
3. Read bookmark file and extract matching sections
4. Return matching entries in Markdown format

**Examples:**
- "Show today's bookmarks" → `/bookmark-query today`
- "Last week's bookmarks" → `/bookmark-query lastweek`
- "Bookmarks on 2026-04-20" → `/bookmark-query 2026-04-20`

## File Locations

- Skill: `~/.claude/skills/bookmark/SKILL.md`
- Bookmarks: `~/.claude/skills/bookmark/bookmarks.md` (default) or custom path
- Commands: `~/.claude/commands/bookmark.md`, `~/.claude/commands/bookmark-query.md`

## Available Tools for Metadata

Use any available tool to fetch page metadata:

- **[Defuddle](https://defuddle.md/)** — primary option when available: extracts clean, reader-style content from URLs (good for title, description, and tag generation from article pages)
- `xcrawl-scrape` — for scraping page content
- `WebFetch` — for fetching web content
- `curl` + parsing — as fallback
- Any other web fetching tools available in the environment

**Tool selection priority:**
1. Prefer [Defuddle](https://defuddle.md/) when the environment can call it (clean extraction improves metadata and tags)
2. Otherwise use the tool that is most reliable in your environment
3. Prefer tools that return clean text content
4. Have fallback options if primary tool fails
