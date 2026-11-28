"""Validate authored evaluation datasets, not model behavior."""
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
EVALS = ROOT / "evals"


class EvalDatasetTest(unittest.TestCase):
    def test_trigger_sets(self):
        folders = sorted((EVALS / "triggers").iterdir())
        self.assertGreaterEqual(len(folders), 7)
        for folder in folders:
            with self.subTest(skill=folder.name):
                matches = [p for collection in ("engineering", "spring-boot-skills", "collected-skills")
                           if (p := ROOT / collection / folder.name / "SKILL.md").is_file()]
                self.assertEqual(len(matches), 1)
                self.assertNotRegex(matches[0].read_text(encoding="utf-8"),
                                    r"(?m)^disable-model-invocation:\s*true\s*$")
                queries = set()
                for split, expected in (("development", 8), ("holdout", 4)):
                    rows = json.loads((folder / (split + ".json")).read_text(encoding="utf-8"))
                    self.assertEqual(len(rows), expected)
                    labels = []
                    for row in rows:
                        self.assertEqual(set(row), {"query", "should_trigger"})
                        self.assertIsInstance(row["query"], str)
                        query = " ".join(row["query"].split()).casefold()
                        self.assertTrue(query)
                        self.assertNotIn(query, queries)
                        queries.add(query)
                        self.assertIs(type(row["should_trigger"]), bool)
                        labels.append(row["should_trigger"])
                    self.assertEqual(sum(labels), expected // 2)

    def test_explicit_only_policy_coverage(self):
        rows = json.loads((EVALS / "invocation/explicit-only.json").read_text(encoding="utf-8"))
        catalog = (ROOT / "docs/skill-catalog.md").read_text(encoding="utf-8")
        expected = set(re.findall(r"(?m)^\| \[([^\]]+)\]\([^\n]+\| Explicit only \|$", catalog))
        actual = {row["skill"] for row in rows}
        self.assertEqual(len(actual), len(rows))
        self.assertEqual(expected, actual)
        for row in rows:
            self.assertTrue(row["implicit_prompt"].strip())
            self.assertTrue(row["explicit_prompt"].strip())
            self.assertNotEqual(row["implicit_prompt"], row["explicit_prompt"])
            self.assertEqual(row["expected_implicit"], "must_not_automatically_invoke")
            self.assertEqual(row["expected_explicit"], "eligible_for_invocation_subject_to_prerequisites")


if __name__ == "__main__":
    unittest.main()
