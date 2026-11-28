#!/usr/bin/env python3
"""Compare isolated installations or SkillsJars extraction to source skill resources."""
import argparse
from pathlib import Path
import re
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
COLLECTIONS = ("engineering", "spring-boot-skills", "collected-skills")
AGENTS = {"codex": ".agents/skills", "claude-code": ".claude/skills"}
LINK = re.compile(r"\]\(([^)\s]+)\)")


def inventory(root: Path):
    expected, errors = {}, []
    for collection in COLLECTIONS:
        for marker in sorted((root / collection).rglob("SKILL.md")):
            name = marker.parent.name
            if name in expected:
                errors.append(f"Duplicate source skill: {name}")
            expected[name] = marker.parent
    if not expected:
        errors.append("No source skills discovered")
    return expected, errors


def verify_destination(root: Path, destination: Path, label: str, expected: dict) -> list[str]:
    errors = []
    actual = {
        marker.parent.name: marker.parent
        for marker in destination.glob("*/SKILL.md")
    }
    print(f"{label}: expected {len(expected)}, discovered {len(actual)} installed skills")
    for name in sorted(expected.keys() - actual.keys()):
        errors.append(f"{label}: missing skill {name}")
    for name in sorted(actual.keys() - expected.keys()):
        errors.append(f"{label}: unexpected skill {name}")
    for name in sorted(expected.keys() & actual.keys()):
        source, installed = expected[name], actual[name]
        for local in sorted(source.rglob("*")):
            if not local.is_file():
                continue
            path = local.relative_to(source)
            copied = installed / path
            if not copied.is_file():
                errors.append(f"{label}: missing resource {name}/{path}")
            elif copied.read_bytes() != local.read_bytes():
                errors.append(f"{label}: changed resource {name}/{path}")
        # Only simple inline Markdown paths in the entry point that already resolve
        # in the source tree. Full Markdown/reference validation is a separate check.
        for link in LINK.findall((source / "SKILL.md").read_text(encoding="utf-8")):
            try:
                url = urlsplit(link)
            except ValueError:
                continue
            if url.scheme or url.netloc or not url.path or url.path.startswith("/"):
                continue
            relative = unquote(url.path)
            original = (source / relative).resolve()
            if original.is_relative_to(root.resolve()) and original.exists():
                if not (installed / relative).exists():
                    errors.append(f"{label}: installed link does not resolve: {name}/{relative}")
    return errors


def verify(root: Path, project: Path, extracted: bool = False) -> list[str]:
    expected, errors = inventory(root)
    if extracted:
        errors.extend(verify_destination(root, project, "SkillsJars extraction", expected))
    else:
        for agent, relative in AGENTS.items():
            errors.extend(verify_destination(root, project / relative, agent, expected))
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("project", type=Path)
    parser.add_argument("--extracted", action="store_true",
                        help="Check a flat SkillsJars extraction directory")
    args = parser.parse_args()
    errors = verify(ROOT, args.project, args.extracted)
    for error in errors:
        print(f"ERROR: {error}")
    if errors:
        raise SystemExit(1)
    print("Installation or extraction inventory, resources and entry-point links verified.")


if __name__ == "__main__":
    main()
