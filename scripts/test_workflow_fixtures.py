"""Verify disposable fixture inputs, not agent behavior."""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

GENERATOR = Path(__file__).resolve().parents[1] / "docs/verification/fixtures/create_fixture.py"
CASES = ("W02", "W04", "W05", "W05-cycle", "W06", "W07", "W10", "W16")


class WorkflowFixtureTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)

    def create(self, case, name=None):
        dest = self.root / (name or case)
        result = subprocess.run(
            [sys.executable, str(GENERATOR), case, str(dest)],
            capture_output=True, text=True, timeout=15,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        return dest

    def read(self, dest, path):
        return (dest / path).read_text(encoding="utf-8")

    def snapshot(self, dest):
        return {str(p.relative_to(dest)): p.read_bytes()
                for p in dest.rglob("*") if p.is_file()}

    def run_tests(self, dest):
        return subprocess.run(
            [sys.executable, "-m", "unittest", "discover", "-v"],
            cwd=dest, capture_output=True, text=True, timeout=15,
        )

    def test_all_cases_are_repeatable(self):
        for case in CASES:
            with self.subTest(case=case):
                first = self.create(case)
                second = self.create(case, case + "-repeat")
                self.assertEqual(self.snapshot(first), self.snapshot(second))

    def test_existing_destination_is_unchanged(self):
        dest = self.root / "existing"
        dest.mkdir()
        (dest / "sentinel").write_text("keep", encoding="utf-8")
        before = self.snapshot(dest)
        result = subprocess.run(
            [sys.executable, str(GENERATOR), "W07", str(dest)],
            capture_output=True, timeout=15,
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(before, self.snapshot(dest))

    def test_lightweight_baseline_and_repair(self):
        dest = self.create("W02")
        self.assertFalse((dest / "docs").exists())
        result = self.run_tests(dest)
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn("FAIL: test_one_newline", result.stderr)
        (dest / "booking.py").write_text('def label(name):\n    return name + "\\n"\n', encoding="utf-8")
        result = self.run_tests(dest)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Ran 1 test", result.stderr)

    def test_conflicting_status(self):
        dest = self.create("W04")
        self.assertIn("| ❎ | UC-002 | Cancel a booking | TODO |", self.read(dest, "docs/requirements.md"))
        self.assertIn("Status: IN_PROGRESS", self.read(dest, "docs/specs/002-cancel-a-booking.md"))

    def test_dependencies(self):
        for case in ("W05", "W05-cycle"):
            with self.subTest(case=case):
                dest = self.create(case)
                self.assertIn("| ❎ | UC-001 | Book a room | TODO |", self.read(dest, "docs/requirements.md"))
                self.assertIn("Depends On: [UC-001]", self.read(dest, "docs/specs/002-cancel-a-booking.md"))
                expected = "[UC-002]" if case == "W05-cycle" else "[]"
                self.assertIn("Depends On: " + expected, self.read(dest, "docs/specs/001-book-a-room.md"))

    def test_unavailable_required_check(self):
        dest = self.create("W06")
        self.assertIn("deliberately unavailable external acceptance environment", self.read(dest, "README.md"))
        self.assertIn("Status: TODO", self.read(dest, "docs/specs/001-book-a-room.md"))

    def test_done_repair_baseline_and_repair(self):
        dest = self.create("W07")
        self.assertIn("| ✅ | UC-001 | Book a room | DONE |", self.read(dest, "docs/requirements.md"))
        self.assertIn("Status: DONE", self.read(dest, "docs/specs/001-book-a-room.md"))
        result = self.run_tests(dest)
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn("FAIL: test_empty", result.stderr)
        (dest / "booking.py").write_text('def book(name):\n    if not name:\n        raise ValueError("empty")\n    return name\n', encoding="utf-8")
        result = self.run_tests(dest)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Ran 2 tests", result.stderr)

    def test_spike_has_no_precomputed_evidence(self):
        dest = self.create("W10")
        self.assertEqual(set(self.snapshot(dest)), {"README.md"})

    def test_unapproved_prd(self):
        dest = self.create("W16")
        prd = self.read(dest, "docs/PRD.md")
        self.assertIn("status: draft\napproval: pending", prd)
        self.assertIn("## Target-only scope\nReporting.", prd)
        self.assertIn("| ✅ | UC-001 | Book a room | DONE |", self.read(dest, "docs/requirements.md"))
        self.assertNotIn("Reporting", self.read(dest, "docs/requirements.md"))


if __name__ == "__main__":
    unittest.main()
