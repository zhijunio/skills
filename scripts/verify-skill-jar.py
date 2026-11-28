#!/usr/bin/env python3
"""Verify that a locally built SkillsJars archive contains the complete source inventory."""
import argparse
from collections import Counter
from pathlib import Path, PurePosixPath
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
COLLECTIONS = ("engineering", "spring-boot-skills", "collected-skills")
PREFIX = "META-INF/skills/"


def verify(root: Path, archive: Path) -> list[str]:
    errors = []
    sources = {}
    for collection in COLLECTIONS:
        for marker in sorted((root / collection).rglob("SKILL.md")):
            name = marker.parent.name
            if name in sources:
                errors.append(f"Duplicate source skill: {name}")
            sources[name] = marker.parent
    if not sources:
        return ["No source skills discovered"]
    with ZipFile(archive) as jar:
        files = [entry.filename for entry in jar.infolist() if not entry.is_dir()]
        counts = Counter(files)
        for path, count in counts.items():
            if count > 1 and path.startswith(PREFIX):
                errors.append(f"Duplicate archive entry: {path}")
        actual = {}
        for path in files:
            if path.startswith(PREFIX) and path.endswith("/SKILL.md"):
                parent = PurePosixPath(path).parent
                actual.setdefault(parent.name, set()).add(str(parent))
        for name in sorted(sources.keys() - actual.keys()):
            errors.append(f"Missing skill: {name}")
        for name in sorted(actual.keys() - sources.keys()):
            errors.append(f"Unexpected skill: {name}")
        for name in sorted(sources.keys() & actual.keys()):
            if len(actual[name]) != 1:
                errors.append(f"Ambiguous archive locations for {name}: {actual[name]}")
                continue
            base = next(iter(actual[name]))
            source = sources[name]
            expected = {
                f"{base}/{p.relative_to(source).as_posix()}": p
                for p in source.rglob("*") if p.is_file()
            }
            for path, local in expected.items():
                if path not in counts:
                    errors.append(f"Missing resource: {path}")
                elif jar.read(path) != local.read_bytes():
                    errors.append(f"Resource differs: {path}")
            for path in sorted(set(files) - expected.keys()):
                if path.startswith(base + "/"):
                    errors.append(f"Unexpected resource: {path}")
        for local, path in [
            (root / "LICENSE", "META-INF/skill-licenses/LICENSE"),
            (root / "spring-boot-skills/LICENSE",
             "META-INF/skill-licenses/spring-boot-skills/LICENSE"),
        ]:
            if not local.is_file():
                errors.append(f"Missing source notice: {local}")
            elif path not in counts or jar.read(path) != local.read_bytes():
                errors.append(f"Missing or different archived notice: {path}")
        print(f"Expected {len(sources)} skills; found {len(actual)} distinct archive skill names.")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    args = parser.parse_args()
    errors = verify(ROOT, args.archive)
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        raise SystemExit(1)
    print("Skill archive inventory and resources verified.")


if __name__ == "__main__":
    main()
