"""Check static measurement semantics, not model token consumption."""
import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location("measure", Path(__file__).with_name("measure_skill_context.py"))
module = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(module)


class ContextMeasurementTest(unittest.TestCase):
    def test_utf8_and_resource_exclusion(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            skill = root / "engineering/example"
            skill.mkdir(parents=True)
            raw = "# 测试\nhello\n".encode("utf-8")
            (skill / "SKILL.md").write_bytes(raw)
            (skill / "reference.md").write_text("not counted", encoding="utf-8")
            rows = module.measure(root)
            self.assertEqual(len(rows), 1)
            row = rows[0]
            self.assertEqual(row["bytes"], len(raw))
            self.assertEqual(row["code_points"], len(raw.decode()))
            self.assertEqual(row["lines"], 2)
            self.assertEqual(row["blob_sha"], hashlib.sha1(f"blob {len(raw)}\0".encode() + raw).hexdigest())

    def test_stable_order(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in ("spring-boot-skills/z", "engineering/b", "engineering/a"):
                path = root / name / "SKILL.md"
                path.parent.mkdir(parents=True)
                path.write_text("# Skill\n", encoding="utf-8")
            rows = module.measure(root)
            self.assertEqual([r["path"] for r in rows], sorted(r["path"] for r in rows))


if __name__ == "__main__":
    unittest.main()
