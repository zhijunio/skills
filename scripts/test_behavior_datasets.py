"""Test behavior case data and offline checker against synthetic states."""
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("outcome", ROOT / "scripts/check_workflow_outcome.py")
outcome = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(outcome)


class BehaviorDatasetTest(unittest.TestCase):
    def test_registry_coverage(self):
        rows = json.loads((ROOT / "evals/behavior/workflow-cases.json").read_text(encoding="utf-8"))
        expected = {f"W{i:02}" for i in range(1, 19)} | {"W05-cycle", "W10-unavailable"}
        self.assertEqual({r["id"] for r in rows}, expected)
        self.assertEqual(len(rows), len(expected))
        for row in rows:
            self.assertTrue(row["prompt"].strip())
            self.assertTrue(row["expectations"])
            self.assertTrue(any((ROOT / c / row["skill"] / "SKILL.md").is_file()
                                for c in ("engineering", "spring-boot-skills", "collected-skills")))

    def pair(self, case):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        before = Path(tmp.name) / "before"
        after = Path(tmp.name) / "after"
        subprocess.run([sys.executable, str(ROOT / "docs/verification/fixtures/create_fixture.py"),
                        case, str(before)], check=True, capture_output=True, timeout=15)
        shutil.copytree(before, after)
        return before, after

    def test_protected_changes_detected(self):
        for case in ("W04", "W05", "W05-cycle", "W16"):
            with self.subTest(case=case):
                before, after = self.pair(case)
                result = outcome.checks(case, before, after)
                self.assertEqual(result["final_state_verdict"], "PASS")
                self.assertEqual(result["behavioral_verdict"], "NOT RUN")
                (after / "docs/requirements.md").write_text("changed", encoding="utf-8")
                self.assertEqual(outcome.checks(case, before, after)["final_state_verdict"], "FAIL")

    def test_missing_file_fails(self):
        before, after = self.pair("W04")
        (after / "booking.py").unlink()
        self.assertEqual(outcome.checks("W04", before, after)["final_state_verdict"], "FAIL")

    def test_blocked_completion(self):
        before, after = self.pair("W06")
        self.assertEqual(outcome.checks("W06", before, after)["final_state_verdict"], "FAIL")
        index = after / "docs/requirements.md"
        spec = after / outcome.SPEC
        index.write_text(index.read_text(encoding="utf-8").replace("❎", "👷🏽").replace("TODO", "IN_PROGRESS"), encoding="utf-8")
        spec.write_text(spec.read_text(encoding="utf-8").replace("Status: TODO", "Status: IN_PROGRESS"), encoding="utf-8")
        self.assertEqual(outcome.checks("W06", before, after)["final_state_verdict"], "PASS")
        spec.write_text(spec.read_text(encoding="utf-8").replace("IN_PROGRESS", "DONE"), encoding="utf-8")
        self.assertEqual(outcome.checks("W06", before, after)["final_state_verdict"], "FAIL")

    def test_done_is_not_proof_of_repair(self):
        before, after = self.pair("W07")
        result = outcome.checks("W07", before, after)
        self.assertEqual(result["final_state_verdict"], "PASS")
        self.assertEqual(result["behavioral_verdict"], "NOT RUN")
        spec = after / outcome.SPEC
        spec.write_text(spec.read_text(encoding="utf-8") + "\nChanged rule\n", encoding="utf-8")
        self.assertEqual(outcome.checks("W07", before, after)["final_state_verdict"], "FAIL")


if __name__ == "__main__":
    unittest.main()
