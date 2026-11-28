import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("analyze_skill_references.py")
SPEC = importlib.util.spec_from_file_location("analyze_skill_references", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ReferenceGraphTests(unittest.TestCase):
    def test_transitive_cycle_and_supporting_reference(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for name in ("alpha", "beta", "gamma", "unused"):
                directory = root / "spring-boot-skills" / name
                directory.mkdir(parents=True)
                (directory / "SKILL.md").write_text("# Skill\n", encoding="utf-8")
            alpha = root / "spring-boot-skills/alpha"
            (alpha / "references").mkdir()
            (alpha / "references/guide.md").write_text(
                "[beta](../../beta/SKILL.md)\n[web](https://example.com)\n",
                encoding="utf-8",
            )
            (root / "spring-boot-skills/beta/SKILL.md").write_text(
                "[gamma](../gamma/SKILL.md)", encoding="utf-8"
            )
            (root / "spring-boot-skills/gamma/SKILL.md").write_text(
                "[alpha](../alpha/SKILL.md)", encoding="utf-8"
            )
            (root / "README.md").write_text("# Root", encoding="utf-8")
            (alpha / "SKILL.md").write_text(
                "[repo](../../README.md)", encoding="utf-8"
            )
            result = MODULE.report(root, ["alpha"])
            self.assertEqual(result["skill_count"], 4)
            self.assertEqual(result["reference_closure"], ["alpha", "beta", "gamma"])
            self.assertEqual(result["direct_references"]["alpha"], ["beta"])
            self.assertEqual(len(result["repository_resources_outside_skills"]), 1)
            self.assertEqual(len(result["reference_evidence"]), 3)

    def test_unknown_selection(self):
        with self.assertRaisesRegex(ValueError, "Unknown skill"):
            MODULE.closure({"alpha": set()}, ["missing"])

    def test_duplicate_names(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for collection in ("engineering", "spring-boot-skills"):
                directory = root / collection / "duplicate"
                directory.mkdir(parents=True)
                (directory / "SKILL.md").write_text("# Skill", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "Duplicate skill"):
                MODULE.analyze(root)


if __name__ == "__main__":
    unittest.main()
