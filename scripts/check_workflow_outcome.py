#!/usr/bin/env python3
"""Check selected final-state assertions; never execute candidate code."""
import argparse
import json
import stat
from pathlib import Path

PROTECTED = ("booking.py", "test_booking.py", "docs/PRD.md", "docs/requirements.md")
SPEC = "docs/specs/001-book-a-room.md"
CASES = ("W04", "W05", "W05-cycle", "W06", "W07", "W16")


def read(root, name):
    path = root / name
    for part in [path, *path.parents]:
        if part == root:
            break
        if part.is_symlink():
            raise ValueError(f"Symlink not accepted: {name}")
    if not stat.S_ISREG(path.lstat().st_mode):
        raise ValueError(f"Not a regular file: {name}")
    return path.read_bytes()


def fixture_paths(root):
    """Inventory fixture files without descending into symlinks or host metadata."""
    ignored = {".git", ".agents", ".claude", "__pycache__"}
    paths = set()
    pending = [root]
    while pending:
        directory = pending.pop()
        for path in directory.iterdir():
            if path.name in ignored:
                continue
            mode = path.lstat().st_mode
            if stat.S_ISDIR(mode):
                pending.append(path)
            elif stat.S_ISREG(mode):
                paths.add(path.relative_to(root).as_posix())
            else:
                raise ValueError(f"Unexpected file type: {path.relative_to(root)}")
    return paths


def checks(case, before, after):
    if case not in CASES:
        raise ValueError("Unsupported case")
    if not before.is_dir() or not after.is_dir() or before.resolve() == after.resolve():
        raise ValueError("Two distinct fixture directories required")
    before, after = before.resolve(), after.resolve()
    results = []

    def assertion(name, condition):
        results.append({"assertion": name, "status": "PASS" if condition else "FAIL"})

    def unchanged(name):
        try:
            assertion(name + " unchanged", read(before, name) == read(after, name))
        except (OSError, ValueError):
            assertion(name + " unchanged", False)

    if case in ("W04", "W05", "W05-cycle", "W16"):
        try:
            original_paths = fixture_paths(before)
            candidate_paths = fixture_paths(after)
            assertion("fixture file inventory unchanged", original_paths == candidate_paths)
            for name in sorted(original_paths | set(PROTECTED)):
                unchanged(name)
        except (OSError, ValueError):
            assertion("fixture inventory readable and regular", False)
    else:
        unchanged("docs/PRD.md")
        unchanged("test_booking.py")
        status, icon = ("IN_PROGRESS", "👷🏽") if case == "W06" else ("DONE", "✅")
        try:
            index = read(after, "docs/requirements.md").decode("utf-8")
            spec = read(after, SPEC).decode("utf-8")
            expected = f"| {icon} | UC-001 | Book a room | {status} |"
            matches = [line for line in index.splitlines() if "| UC-001 |" in line]
            assertion("canonical index status and icon", matches == [expected])
            old_status, old_icon = ("TODO", "❎") if case == "W06" else ("DONE", "✅")
            old_row = f"| {old_icon} | UC-001 | Book a room | {old_status} |"
            original_index = read(before, "docs/requirements.md").decode("utf-8")
            assertion("index otherwise unchanged", original_index.count(old_row) == 1
                      and index == original_index.replace(old_row, expected, 1))
            frontmatter = spec.split("---", 2)
            metadata = frontmatter[1].splitlines() if len(frontmatter) == 3 and not frontmatter[0] else []
            assertion("specification mirror status", [line for line in metadata if line.startswith("Status:")] == [f"Status: {status}"])
            # The only authorized document change here is the status transition.
            original = read(before, SPEC).decode("utf-8")
            old = "TODO" if case == "W06" else "DONE"
            assertion("approved specification otherwise unchanged", spec == original.replace("Status: " + old, "Status: " + status, 1))
        except (OSError, UnicodeError, ValueError):
            assertion("required status evidence readable", False)
    return {"case": case, "assertions": results,
            "final_state_verdict": "FAIL" if any(r["status"] == "FAIL" for r in results) else "PASS",
            "behavioral_verdict": "NOT RUN",
            "limitations": "No agent trace, actual command evidence, transient writes or remote effects evaluated."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("case", choices=CASES)
    parser.add_argument("before", type=Path)
    parser.add_argument("after", type=Path)
    args = parser.parse_args()
    result = checks(args.case, args.before, args.after)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if result["final_state_verdict"] == "FAIL" else 0


if __name__ == "__main__":
    raise SystemExit(main())
