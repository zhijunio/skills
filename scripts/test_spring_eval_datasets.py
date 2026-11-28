"""Validate Spring evaluation case metadata, not Java or agent behavior."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class SpringEvalDatasetTest(unittest.TestCase):
    def test_cases_and_references(self):
        rows = json.loads((ROOT / "evals/spring-boot/cases.json").read_text(encoding="utf-8"))
        self.assertEqual({r["id"] for r in rows}, {f"SB{i:02}" for i in range(1, 19)})
        self.assertEqual(len(rows), 18)
        for row in rows:
            with self.subTest(case=row["id"]):
                self.assertIn(row["priority"], {"P0", "P1"})
                self.assertTrue(row["fixture"].strip())
                self.assertTrue(row["prompt"].strip())
                self.assertTrue(row["expectations"])
                self.assertTrue(row["evidence"])
                self.assertTrue(row["skills"])
                self.assertEqual(len(set(row["skills"])), len(row["skills"]))
                for skill in row["skills"]:
                    self.assertTrue((ROOT / "spring-boot-skills" / skill / "SKILL.md").is_file(), skill)
                self.assertIn(row["fixture_status"], {"MANUAL_SETUP_REQUIRED", "VERIFIED"})
                self.assertEqual(row["behavioral_status"], "NOT RUN")


if __name__ == "__main__":
    unittest.main()
