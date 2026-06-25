# Research-oriented fetch — sources and failures

Use in **topic Source** step with **article-read**. General fetch flow → [`router.md`](router.md).

## 1. Preferred primary sources (technical)

| Domain | Examples |
|--------|----------|
| Java / JVM | [openjdk.org/jeps](https://openjdk.org/jeps/), JLS/JVMS, `raw.githubusercontent.com/openjdk/jdk/**` |
| Specs | IETF RFC, W3C, vendor official docs |
| Code | GitHub **tag** matching release, not only default branch |
| Papers | PDF via `read.sh` or author-hosted |

**OpenJDK source URLs:** prefer paths under a **release tag** (`jdk-21+35`) when explaining production; use **main** only for “upcoming / in development” with that caveat.

## 2. GitHub raw paths

**OpenJDK source paths (JDK 9+ layout):** raw files live under  
`src/java.base/share/classes/…` (not legacy `src/java.base/java/…`).  
Example:

```bash
bash article-read/scripts/read.sh \
  "https://raw.githubusercontent.com/openjdk/jdk/master/src/java.base/share/classes/java/lang/ClassLoader.java"
```

Prefer a **release tag** matching the narrative `runtime` when explaining production; if only `master` works, note that in `sources/` frontmatter.

## 3. When fetch fails

| Symptom | Next step |
|---------|-----------|
| 404 on blog | Try alternate URL, archive, or official mirror |
| Empty body | Next hop in `cascade.md`; then `mcp-fallback.md` |
| Paywall | Do not save wall as content; ask user for paste or creds |
| Firecrawl unavailable | `read.sh` only; note method in topic notes |

**Never** fabricate article or source text.

## 4. Record for topic Source

Each fetch → file under `<topic-dir>/sources/` + line in `sources/INDEX.md` (not a separate plan file).

## 5. Anti-patterns

| Do not | Do instead |
|--------|------------|
| Cite SEO roundup as primary | JEP / spec / source file |
| Say “current master proves…” | JDK version + tagged path |
| Re-fetch same URL every session | Keep saved file in `sources/` |
