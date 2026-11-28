#!/usr/bin/env python3
"""Inventory explicit local Markdown references between skills.

Edges describe resource links, not mandatory activation or runtime dependencies.
The graph is intentionally conservative: inline relative links only.
"""
import argparse
import json
from pathlib import Path
import re
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
COLLECTIONS = ("engineering", "spring-boot-skills", "collected-skills")
LINK = re.compile(r"\]\(([^)\s]+)\)")


def analyze(root):
    root = root.resolve()
    skills = {}
    for collection in COLLECTIONS:
        for marker in sorted((root / collection).rglob("SKILL.md")):
            name = marker.parent.name
            if name in skills:
                raise ValueError(f"Duplicate skill: {name}")
            skills[name] = marker.parent.resolve()
    if not skills:
        raise ValueError("No skills found")
    owners = {path: name for name, path in skills.items()}
    edges = {name: set() for name in skills}
    evidence = []
    external_local = []
    for name, directory in sorted(skills.items()):
        for doc in sorted(directory.rglob("*.md")):
            for raw in LINK.findall(doc.read_text(encoding="utf-8")):
                try:
                    url = urlsplit(raw)
                except ValueError:
                    continue
                if url.scheme or url.netloc or not url.path or url.path.startswith("/"):
                    continue
                target = (doc.parent / unquote(url.path)).resolve()
                if not target.is_relative_to(root) or not target.exists():
                    continue  # Source link lint owns unresolved links.
                owner = next((owners[p] for p in (target, *target.parents) if p in owners), None)
                record = {"source": doc.relative_to(root).as_posix(),
                          "target": target.relative_to(root).as_posix()}
                if owner is not None and owner != name:
                    edges[name].add(owner)
                    evidence.append({"from": name, "to": owner, **record})
                elif owner is None:
                    external_local.append({"skill": name, **record})
    return edges, evidence, external_local


def closure(edges, selected):
    pending = list(selected)
    seen = set()
    while pending:
        name = pending.pop()
        if name not in edges:
            raise ValueError(f"Unknown skill: {name}")
        if name not in seen:
            seen.add(name)
            pending.extend(edges[name] - seen)
    return sorted(seen)


def report(root, selected):
    edges, evidence, external_local = analyze(root)
    return {
        "scope": "Explicit inline Markdown resource references, not runtime dependencies",
        "skill_count": len(edges),
        "direct_references": {name: sorted(targets) for name, targets in sorted(edges.items())},
        "reference_evidence": evidence,
        "repository_resources_outside_skills": external_local,
        "selected": sorted(set(selected)),
        "reference_closure": closure(edges, selected),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--skill", action="append", default=[])
    args = parser.parse_args()
    try:
        print(json.dumps(report(ROOT, args.skill), indent=2, ensure_ascii=False))
    except ValueError as exc:
        parser.exit(1, f"ERROR: {exc}\n")


if __name__ == "__main__":
    main()
