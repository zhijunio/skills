# Keep — CLI and credentials

Load when running or debugging `scripts/fetch_keep_run.py`.

## Dependencies

- Python 3, `requests` — `pip install -r keep/scripts/requirements.txt`

## Credentials (env preferred)

| Variable | Meaning |
|----------|---------|
| `KEEP_MOBILE` | Login phone (`--mobile` overrides) |
| `KEEP_PASSWORD` | Password (`--password` overrides) |
| `MAX_HR` | Default `180` |
| `RESTING_HR` | Default `55` |
| `RUNNER_WEIGHT_KG` | Default `70` |

Never log passwords. Do not commit credentials.

## CLI

`--output` is **relative to the script directory**. In this skills repo set explicitly:

```bash
python keep/scripts/fetch_keep_run.py \
  --mobile "$KEEP_MOBILE" \
  --password "$KEEP_PASSWORD" \
  --output data/running.json
```

| Flag | Meaning |
|------|---------|
| `--output` | Output file (relative to script dir) |
| `--full` | Full fetch; ignore incremental stop |
| `--limit N` | Max records (debug) |
| `--debug` | Verbose logs |

## Output

- **`stats`**: totals, VDOT, training load, `period_stats`, Shanghai `statistics_time`
- **`runs`**: `startTime`, `distance`, `duration`, HR zones, segments — match upstream `running.json` consumers

Merge by `startTime`; `--full` replaces incremental logic per script comments.

## Debug

1. Confirm network to Keep API.
2. First field debug: `--limit 1 --debug`.
3. Do not rename `running.json` keys when sharing with Garmin export scripts.
