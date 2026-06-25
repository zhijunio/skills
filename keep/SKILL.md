---
name: keep-running-json
description: Log in via Keep Open API and fetch runs into Garmin-compatible running.json (VDOT, training load, period stats, optional per-km segments). Use for Keep run sync, fetch_keep_run, running.json, or exporting Keep running data.
---

# Keep run sync

Run **[`scripts/fetch_keep_run.py`](scripts/fetch_keep_run.py)** in this repo to pull Keep runs and write **`running.json`**.

## When to use

- Export **Keep** runs aligned with existing **Garmin-style** `running.json`.
- User mentions **`fetch_keep_run.py`**, **`running.json`**, **Keep API**, **VDOT**, **training load**.
- Explain deps, env vars, output path, or debugging.

Script lives at **`keep/scripts/fetch_keep_run.py`**, aligned with upstream **[zhijunio.github.io/scripts/fetch-keep-run.py](https://github.com/zhijunio/zhijunio.github.io/blob/main/scripts/fetch-keep-run.py)**; sync from that file going forward.

## Script behavior (summary)

1. Login with mobile + password; obtain session.
2. Paginate runs; **incremental** stop on existing `startTime`, or **`--full`**.
3. Merge with existing `runs` in output (dedupe by `startTime`, newer wins); recompute `stats`.
4. Display times and period stats use **Asia/Shanghai** (`UTC+8`).

## Dependencies

- **Python 3**
- **`requests`**

```bash
pip install requests
# or: pip install -r keep/scripts/requirements.txt
```

## Credentials

| Variable | Meaning |
|----------|---------|
| `KEEP_MOBILE` | Keep login phone (`--mobile` overrides) |
| `KEEP_PASSWORD` | Password (`--password` overrides) |
| `MAX_HR` | Max HR, default `180` (VDOT / zones) |
| `RESTING_HR` | Resting HR, default `55` |
| `RUNNER_WEIGHT_KG` | Weight kg for power estimate, default `70` |

Never repeat passwords in chat or logs; pass via env or local CLI only.

## CLI

`--output` is **relative to the script directory**. Upstream default **`../public/data/running.json`** (Jekyll site); in this skills repo **set output explicitly**, e.g.:

```bash
python keep/scripts/fetch_keep_run.py \
  --mobile "$KEEP_MOBILE" \
  --password "$KEEP_PASSWORD" \
  --output data/running.json
```

From `keep/scripts/`: `python fetch_keep_run.py ... --output data/running.json`.

| Flag | Meaning |
|------|---------|
| `--output` | Output file (relative to script dir); default `../public/data/running.json` |
| `--full` | Full fetch; ignore incremental early stop |
| `--limit N` | Max N records (debug safety cap) |
| `--debug` | Verbose request/conversion logs |

## Output shape

- **`stats`**: totals, VDOT, training load, `period_stats` (yesterday/week/month/year/all), `statistics_time` (Shanghai), per `_calculate_stats` in script.
- **`runs`**: `startTime` / `endTime` (Shanghai local strings), `distance` (km), `duration`, `averagePace`, `averageHeartRate`, `vDOT`, `trainingLoadScore`, `heartRateZone`, `segments`, etc. — keys match upstream consumers.

If `running.json` exists: merge `runs` by `startTime`, rewrite `stats`; `--full` uses full-fetch logic (see script comments).

## Agent tips

1. Confirm deps and network to Keep API.
2. Prefer env vars for credentials; never commit passwords.
3. First API/field debug: `--limit 1 --debug`.
4. When sharing `running.json` with **Garmin export scripts**, keep field names from `format_running_data`; do not rename keys casually.
