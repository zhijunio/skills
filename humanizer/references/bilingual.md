# Bilingual Drafts

When the draft mixes Chinese and English (technical posts, product docs):

## Lane rules

1. **Polish each language with its lane file** — EN tells in EN spans; ZH tells in ZH spans.
2. **Do not translate** unless user asked — keep original language of each sentence.
3. **Identifiers** — API names, CLI flags, error codes stay English; surrounding grammar stays Chinese if draft does.

## Spacing

- CJK + Latin: one space between unless draft already consistent without (match file convention).
- Parenthetical EN in ZH: `配置（config）` OK; avoid `配置(config)` if rest of file uses full-width punctuation.

## Common bilingual tells

- ZH sentence ending with EN slogan line → integrate or drop slogan
- Repeated back-to-back definitions: `用户 User` every mention → define once
- Chinglish connectors: "我们需要 leverage 这个能力" → 用这个能力 / use this capability (pick draft register)

## Code and links

Never humanize inside fenced code blocks or inline `` ` `` unless user explicitly includes them in scope.
