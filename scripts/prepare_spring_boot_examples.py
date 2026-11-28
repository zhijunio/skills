#!/usr/bin/env python3
"""Stage byte-identical, explicitly selected Java examples for the isolated Maven fixture."""
import hashlib
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
FIXTURE = ROOT / "verification/spring-boot-examples"


def main():
    manifest = json.loads((FIXTURE / "sources.json").read_text(encoding="utf-8"))
    if manifest["schema"] != 1:
        raise ValueError("Unsupported source manifest")
    output = FIXTURE / "target/skill-sources"
    if output.is_symlink() or not output.resolve().is_relative_to(FIXTURE.resolve()):
        raise ValueError("Unsafe staging directory")
    planned = []
    seen = set()
    for item in manifest["sources"]:
        src = (ROOT / item["source"]).resolve()
        relative = Path(item["destination"])
        if (not src.is_relative_to(ROOT / "spring-boot-skills")
                or not src.is_file() or src.suffix != ".java"
                or relative.is_absolute() or ".." in relative.parts
                or relative.suffix != ".java" or relative in seen):
            raise ValueError(f"Invalid source entry: {item}")
        seen.add(relative)
        planned.append((src, relative, item["source"]))
    if not planned:
        raise ValueError("Empty compile manifest")
    if output.exists():
        shutil.rmtree(output)
    evidence = []
    for src, relative, original in planned:
        dest = output / relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        data = src.read_bytes()
        dest.write_bytes(data)
        evidence.append({"source": original, "destination": relative.as_posix(),
                         "sha256": hashlib.sha256(data).hexdigest()})
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    main()
