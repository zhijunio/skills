#!/usr/bin/env python3
"""Compare isolated CLI installations to the source skill inventory."""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COLLECTIONS = ("engineering", "spring-boot-skills", "collected-skills")
AGENTS = {"codex": ".agents/skills", "claude-code": ".claude/skills"}


def verify(root: Path, project: Path) -> list[str]:
    expected = {}
    errors = []
    for collection in COLLECTIONS:
        for marker in sorted((root / collection).rglob("SKILL.md")):
            name = marker.parent.name
            if name in expected:
                errors.append(f"Duplicate source skill: {name}")
            expected[name] = marker.parent
    if not expected:
        return ["No source skills discovered"]
    for agent, relative in AGENTS.items():
        destination = project / relative
        actual = {
            marker.parent.name: marker.parent
            for marker in destination.glob("*/SKILL.md")
        }
        print(f"{agent}: expected {len(expected)}, discovered {len(actual)} installed skills")
        for name in sorted(expected.keys() - actual.keys()):
            errors.append(f"{agent}: missing skill {name}")
        for name in sorted(actual.keys() - expected.keys()):
            errors.append(f"{agent}: unexpected skill {name}")
        for name in sorted(expected.keys() & actual.keys()):
            source = expected[name]
            installed = actual[name]
            for local in sorted(source.rglob("*")):
                if not local.is_file():
                    continue
                path = local.relative_to(source)
                copied = installed / path
                if not copied.is_file():
                    errors.append(f"{agent}: missing resource {name}/{path}")
                elif copied.read_bytes() != local.read_bytes():
                    errors.append(f"{agent}: changed resource {name}/{path}")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("project", type=Path)
    args = parser.parse_args()
    errors = verify(ROOT, args.project)
    for error in errors:
        print(f"ERROR: {error}")
    if errors:
        raise SystemExit(1)
    print("Isolated installation inventory and source resources verified.")


if __name__ == "__main__":
    main()
