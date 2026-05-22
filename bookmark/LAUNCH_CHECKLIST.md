# Bookmark skill — pre-launch checklist

## Code quality

- [x] All tests pass (unit + integration) — 25 tests
- [x] No debug `console.log`
- [x] No TODO comments
- [x] Code reviewed
- [x] README.md present

## Security

- [x] No hardcoded secrets
- [x] No sensitive env exposure
- [x] Touches bookmark file only, not arbitrary user data

## Performance

- [x] Tests < 200ms (~134ms)
- [x] No N+1 patterns
- [x] Sync file API OK for small data

## Accessibility

- [x] N/A (no UI)
- [x] Clear errors on test failure

## Infrastructure

- [x] Node.js >= 22 (v24.15.0)
- [x] vitest ^4.1.5
- [x] vitest config present

## Documentation

- [x] README.md
- [x] SKILL.md usage
- [x] Test comments in English
- [x] This checklist

## Monitoring

- [x] 25 tests cover core paths
- [x] Clear failure output

## Rollback

- [x] Simple markdown format — manual revert OK
- [x] Tests use isolated suffix — no prod pollution

## Post-launch verification

- [x] `/bookmark` tested (2 entries saved)
- [x] `/bookmark-query` tested
- [x] File format correct
- [ ] Auto-tag generation — continue validation

## File list

```
bookmark/
├── SKILL.md
├── README.md
├── LAUNCH_CHECKLIST.md
├── package.json
├── vitest.config.js
├── bookmarks.md
└── tests/
    ├── test-utils.js
    ├── bookmark.test.js
    └── bookmark.integration.test.js
```
