"""Negative and positive fixtures for the repository distribution verification glue."""
import importlib.util
import shutil
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

HERE = Path(__file__).resolve().parent


def load_module(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


archive_verifier = load_module("archive_verifier", "verify-skill-jar.py")
installation_verifier = load_module("installation_verifier", "verify-skill-installation.py")


class DistributionVerifierTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "source"
        self.project = Path(self.temp.name) / "project"
        self.archive = Path(self.temp.name) / "skills.jar"
        self.alpha = self.root / "engineering/alpha"
        self.beta = self.root / "spring-boot-skills/beta"
        for folder in (self.alpha / "references", self.beta):
            folder.mkdir(parents=True)
        (self.alpha / "SKILL.md").write_text(
            "# Alpha\n[reference](references/example.md)\n", encoding="utf-8"
        )
        (self.alpha / "references/example.md").write_text("expected", encoding="utf-8")
        (self.beta / "SKILL.md").write_text("# Beta\n", encoding="utf-8")
        (self.root / "LICENSE").write_text("repository notice", encoding="utf-8")
        (self.root / "spring-boot-skills/LICENSE").write_text(
            "upstream notice", encoding="utf-8"
        )

    def build_jar(self, omit=None, altered=None):
        with ZipFile(self.archive, "w") as jar:
            for skill in (self.alpha, self.beta):
                for path in skill.rglob("*"):
                    if not path.is_file():
                        continue
                    relative = path.relative_to(skill).as_posix()
                    name = f"{skill.name}/{relative}"
                    if name == omit:
                        continue
                    data = b"changed" if name == altered else path.read_bytes()
                    jar.writestr(f"META-INF/skills/test/repo/{name}", data)
            jar.write(self.root / "LICENSE", "META-INF/skill-licenses/LICENSE")
            jar.write(self.root / "spring-boot-skills/LICENSE",
                      "META-INF/skill-licenses/spring-boot-skills/LICENSE")

    def install(self):
        for destination in (".agents/skills", ".claude/skills"):
            for skill in (self.alpha, self.beta):
                shutil.copytree(skill, self.project / destination / skill.name)

    def test_complete_jar(self):
        self.build_jar()
        self.assertEqual([], archive_verifier.verify(self.root, self.archive))

    def test_missing_skill_fails(self):
        self.build_jar(omit="beta/SKILL.md")
        self.assertIn("Missing skill: beta", archive_verifier.verify(self.root, self.archive))

    def test_altered_resource_fails(self):
        self.build_jar(altered="alpha/references/example.md")
        self.assertTrue(any("Resource differs:" in e for e in
                            archive_verifier.verify(self.root, self.archive)))

    def test_complete_installation(self):
        self.install()
        self.assertEqual([], installation_verifier.verify(self.root, self.project))

    def test_missing_resource_fails(self):
        self.install()
        (self.project / ".agents/skills/alpha/references/example.md").unlink()
        errors = installation_verifier.verify(self.root, self.project)
        self.assertTrue(any("missing resource" in e for e in errors))
        self.assertTrue(any("installed link does not resolve" in e for e in errors))

    def test_extraction_preserves_sibling_links(self):
        source_beta = self.root / "engineering/beta"
        self.beta.rename(source_beta)
        self.beta = source_beta
        (self.alpha / "SKILL.md").write_text(
            "# Alpha\n[Beta](../beta/SKILL.md)\n", encoding="utf-8"
        )
        extraction = self.project / "extracted"
        for skill in (self.alpha, self.beta):
            shutil.copytree(skill, extraction / skill.name)
        self.assertEqual([], installation_verifier.verify(self.root, extraction, True))
        shutil.rmtree(extraction / "beta")
        errors = installation_verifier.verify(self.root, extraction, True)
        self.assertTrue(any("missing skill beta" in e for e in errors))
        self.assertTrue(any("installed link does not resolve" in e for e in errors))


if __name__ == "__main__":
    unittest.main()
