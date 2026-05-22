---
name: diary
description: Diary capture rules (paths, format, timestamps, images, git). Use when the user shares today's events, feelings, plans, or evening diary Q&A.
---

## When to trigger

| Scenario | Example | Action |
|----------|---------|--------|
| User shares what happened today | "Had a meeting this morning" | Record |
| Feelings / thoughts | "Feeling low today" | Record |
| Plans | "Hospital visit tomorrow" | Record |
| Evening 22:00 cron Q&A | Auto | Record answers |

**Do not trigger:**
- Obvious small talk: "hi", "you there?", "thanks"
- If unsure, ask: "Should I add this to your diary?"

## Storage

- **Vault root**: env **`DIARY_VAULT_ROOT`**; default **`~/github/notes`** (do not hardcode other machines' paths).
- **Diary dir**: `{VAULT}/journal/`
- **Daily file**: `{VAULT}/journal/YYYY-MM-DD.md`
- **Images**: `{VAULT}/journal/assets/YYYY-MM-DD/`

## Format

- **Filename**: `YYYY-MM-DD.md` (date in name)
- **Body**: do not repeat the calendar date in prose
- **Timestamp**: bold `**HH:mm:ss**` before each entry
- **Order**: ascending time (morning first)
- **Lists**: no list markup (keeps entries airy)
- **Tag**: `#diary`

## Recording rules

- **Direct capture**: record without requiring "save this"
- **Wording**: fix typos and grammar only; keep tone and meaning
- **Confirm** when a fix might change meaning
- **No split/merge** of user entries
- **Timestamp**: actual time (`date +%H:%M:%S`) or user-specified
- **One follow-up** only when image/context is missing: e.g. "What should this photo note say?"
- **No invented content**
- **Images**: save under `journal/assets/YYYY-MM-DD/`; link in diary with timestamp
- **Intent**: diary-like → record; small talk → skip; unsure → ask
- **Transitions**: avoid filler phrases; stay terse

## Git workflow

1. Before record: `git pull`
2. After save: `git add` → `git commit` → `git push`

Skip git only when the user explicitly says not to commit.

## Evening questions (22:00)

At 22:00, check whether today has diary entries; remind if empty.

After the user replies, append to today's file without extra confirmation.

## Template

```markdown
**09:00:00** Morning example entry.

**16:56:00** Afternoon example entry.

#diary
```

## Pitfalls

1. Do not require "remember this" for obvious diary content
2. Use real or user-given timestamps; never fabricate
3. No prefixes like "Evening Q&A answer:"
4. Do not split one message into multiple timestamped lines
5. No list formatting
6. **Easy miss**: `git pull` before write; `add/commit/push` after (unless user opts out)

## Verification

- [ ] Matches user intent
- [ ] Timestamps `**HH:mm:ss**`
- [ ] No extra content
- [ ] Git clean if expected: `git status`
- [ ] Images under correct path (if any)

```bash
V="${DIARY_VAULT_ROOT:-$HOME/github/notes}"
ls -la "$V/journal/$(date +%Y-%m-%d).md"
git -C "$V" status --short journal/
git -C "$V" log -1 --oneline
```
