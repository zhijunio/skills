"""Check compatibility case metadata, not native host behavior."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class HostCaseTest(unittest.TestCase):
    def test_host_cases(self):
        rows = json.loads((ROOT / "evals/compatibility/host-cases.json").read_text(encoding="utf-8"))
        self.assertEqual({r["id"] for r in rows}, {f"H{i:02}" for i in range(1, 11)})
        self.assertEqual(len(rows), 10)
        for row in rows:
            self.assertEqual(set(row["hosts"]), {"codex", "claude-code"})
            self.assertTrue(row["prompt"].strip())
            self.assertTrue(row["expectation"].strip())
            self.assertEqual(row["status"], "NOT RUN")
