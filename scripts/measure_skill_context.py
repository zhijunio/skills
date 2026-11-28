#!/usr/bin/env python3
"""Measure static skill entry-point size; not token usage."""
import argparse
import hashlib
import json
from pathlib import Path

COLLECTIONS = ("engineering", "spring-boot-skills", "collected-skills")


def measure(root):
    rows = []
    for collection in COLLECTIONS:
        for path in sorted((root / collection).glob("*/SKILL.md")):
            raw = path.read_bytes()
            content = raw.decode("utf-8")
            rows.append({
                "path": path.relative_to(root).as_posix(),
                "bytes": len(raw),
                "code_points": len(content),
                "lines": len(content.splitlines()),
                "blob_sha": hashlib.sha1(f"blob {len(raw)}\0".encode() + raw).hexdigest(),
            })
    return sorted(rows, key=lambda r: r["path"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    rows = measure(args.root)
    if not rows:
        parser.error("No skill entry points found")
    print(json.dumps({"measurement": "Static full SKILL.md size, not tokens", "skills": rows}, indent=2))


if __name__ == "__main__":
    main()
