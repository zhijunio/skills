---
name: keep
description: Log in via Keep Open API and fetch runs into Garmin-compatible running.json (VDOT, training load, period stats, optional per-km segments). Use for Keep run sync, fetch_keep_run, running.json, or exporting Keep running data.
---

# Keep run sync

Pull **Keep** runs into Garmin-style **`running.json`** via [`scripts/fetch_keep_run.py`](scripts/fetch_keep_run.py).

Aligned with upstream [fetch-keep-run.py](https://github.com/zhijunio/zhijunio.github.io/blob/main/scripts/fetch-keep-run.py) — sync from there when updating behavior.

## When to use

- User mentions **Keep API**, **`running.json`**, **VDOT**, **training load**, **`fetch_keep_run.py`**
- Incremental sync or **`--full`** export

## Hard rules

1. **Credentials** via env or CLI — never repeat passwords in chat or logs.
2. **Set `--output` explicitly** in this repo (default path targets Jekyll site layout).
3. **Preserve `running.json` key names** for downstream Garmin consumers.

## Workflow

1. Read `references/usage.md` for env, flags, output shape.
2. Run script; use `--limit 1 --debug` on first API/field issues.
3. Report merge result (new runs count, output path) — not raw credentials.

## Validation

```bash
python3 -m py_compile keep/scripts/fetch_keep_run.py
```

## References

- `references/usage.md` — CLI, credentials, output, debug
