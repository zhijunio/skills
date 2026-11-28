"""Regression tests for the generated catalog."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location("catalog", Path(__file__).with_name("generate-skill-catalog.py"))
catalog = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(catalog)


class CatalogTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        (self.root / "docs").mkdir()
        self.marker = self.root / "engineering/example/SKILL.md"
        self.marker.parent.mkdir(parents=True)
        self.marker.write_text("---\nname: example\ndescription: >\n  Review a change\n  with evidence.\n---\n# Example\n", encoding="utf-8")
        self.register = self.root / "docs/skill-provenance.md"
        self.register.write_text("## Source groups\n| O | Original | Evidence |\n## Per-skill register\n| [example](../engineering/example/SKILL.md) | O | Original |\n", encoding="utf-8")

    def test_render(self):
        result = catalog.render(self.root)
        self.assertIn("| Total | 1 |", result)
        self.assertIn("| [example](../engineering/example/SKILL.md) | Review a change with evidence. | O | Not restricted |", result)
        self.assertEqual(result, catalog.render(self.root))

    def test_missing_provenance(self):
        self.register.write_text("## Source groups\n| O | Original | Evidence |\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "Missing or mismatched provenance"):
            catalog.render(self.root)

    def test_manual_only(self):
        self.marker.write_text("---\nname: example\ndescription: Review a change.\ndisable-model-invocation: true\n---\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "Mismatched invocation"):
            catalog.render(self.root)
        config = self.marker.parent / "agents/openai.yaml"
        config.parent.mkdir()
        config.write_text("policy:\n  allow_implicit_invocation: false\n", encoding="utf-8")
        self.assertIn("| O | Explicit only |", catalog.render(self.root))

    def test_escape(self):
        self.assertEqual(catalog.cell("a | <b> [c]"), r"a \| &lt;b&gt; \[c\]")


if __name__ == "__main__":
    unittest.main()
