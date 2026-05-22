---
name: jinrishici
description: Fetch a random classical Chinese poem line from the Jinrishici HTTP API and format with standard Chinese attribution punctuation (author, title). Use for daily poem, briefing garnish, 今日诗词.
---

# Jinrishici (今日诗词)

## API

- **URL**: `https://v1.jinrishici.com/all.json`
- **Method**: `GET`
- **Tips**: Set a reasonable **User-Agent**; rate-limit politely. If the platform requires a token in production, follow [Jinrishici Open Platform](https://www.jinrishici.com/) current docs.

## Response fields

Common JSON root fields (actual response may vary):

| Field | Meaning |
|-------|---------|
| `content` | Poem line |
| `author` | Author |
| `origin` | Work title |

Omit missing parts per display rules below.

## Display format

When `content` is non-empty:

1. Both `author` and `origin`: `{content}—— {author}《{origin}》`
2. `author` only: `{content}—— {author}`
3. `origin` only: `{content}《{origin}》`
4. Neither: `content` only

Do not wrap the line in extra quotes; preserve API punctuation. Output is **Chinese** by design.

## Minimal request

```bash
curl -fsS -H "User-Agent: YourApp/1.0" "https://v1.jinrishici.com/all.json"
```

## Local script

```bash
# Default: one formatted line
python jinrishici/scripts/fetch_poem.py

# Raw JSON
python jinrishici/scripts/fetch_poem.py --json
```

## Agent behavior

1. **Live line**: GET the URL, parse JSON, format one line (or body-only if asked).
2. **API failure**: Brief error; do not invent author or title; suggest retry or network check.
3. **Privacy**: Do not log full tokens; third-party content — no extra copyright claims beyond attribution.
