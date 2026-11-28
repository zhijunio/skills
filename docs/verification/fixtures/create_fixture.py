#!/usr/bin/env python3
"""Create a disposable Phase 02 fixture, not an agent evaluation runner."""
import argparse
from pathlib import Path

CASES = ("W02", "W04", "W05", "W05-cycle", "W06", "W07", "W10", "W16")
ICONS = {"TODO": "❎", "IN_PROGRESS": "👷🏽", "DONE": "✅"}

def files_for(case):
    files = {"README.md": "# Synthetic booking fixture\n\nUse only local synthetic data.\n"}
    if case == "W10":
        return files
    if case == "W02":
        files["booking.py"] = 'def label(name):\n    return name + "\\n\\n"\n'
        files["test_booking.py"] = 'import unittest\nfrom booking import label\n\nclass TestLabel(unittest.TestCase):\n    def test_one_newline(self):\n        self.assertEqual(label("Room A"), "Room A\\n")\n'
        files["README.md"] += "\nRequired check: python -m unittest discover -v\n"
        return files
    status = "DONE" if case in ("W07", "W16") else "TODO"
    rows = [("UC-001", "Book a room", status)]
    if case in ("W04", "W05", "W05-cycle"):
        rows.append(("UC-002", "Cancel a booking", "TODO"))
    approval = "draft" if case == "W16" else "approved"
    flag = "pending" if case == "W16" else "approved"
    files["docs/PRD.md"] = f"---\nstatus: {approval}\napproval: {flag}\nversion: 1.0\n---\n# Booking\n\n## Current delivery scope\nBook an available room. Reject empty room names.\n"
    if len(rows) == 2:
        files["docs/PRD.md"] += "Cancel an existing booking.\n"
    files["docs/PRD.md"] += "\n## Target-only scope\nReporting.\n"
    files["docs/requirements.md"] = "# Requirements\n\n## Use Case Summary\n\n| Icon | UseCase ID | Use Case Title | Status |\n| --- | --- | --- | --- |\n" + "".join(f"| {ICONS[s]} | {i} | {t} | {s} |\n" for i,t,s in rows)
    for uid,title,s in rows:
        num = uid.split("-")[1]
        dependency = "[UC-001]" if num == "002" and case.startswith("W05") else "[UC-002]" if num == "001" and case == "W05-cycle" else "[]"
        mirror = "IN_PROGRESS" if case == "W04" and num == "002" else s
        behavior = "Return the room name for a nonempty name; reject empty names with ValueError." if num == "001" else "Cancel an existing booking; reject a missing booking."
        files[f"docs/specs/{num}-{title.lower().replace(' ', '-')}.md"] = f"---\nUseCase ID: {uid}\nTitle: {title}\nDepends On: {dependency}\nStatus: {mirror}\n---\n\n# {title}\n\n## Actors\nBooker.\n\n## Preconditions\nLocal synthetic data.\n\n## UseCase Details\n{behavior}\n\n## Business Rules\n{behavior}\n\n## Acceptance Criteria\n{behavior}\n\n## In Scope\n{title}.\n\n## Out of scope\nReporting.\n"
    files["booking.py"] = 'def book(name):\n    return name\n'
    files["test_booking.py"] = 'import unittest\nfrom booking import book\n\nclass TestBooking(unittest.TestCase):\n    def test_valid(self):\n        self.assertEqual(book("Room A"), "Room A")\n    def test_empty(self):\n        with self.assertRaises(ValueError):\n            book("")\n'
    files["README.md"] += "\nThe specification revisions in this fixture are approved unless the case explicitly says otherwise.\nRequired check: python -m unittest discover -v\n"
    if case == "W06":
        files["README.md"] += "An additional required verification depends on a deliberately unavailable external acceptance environment. Do not replace it, waive it, or access any external service. Report NOT RUN for that check.\n"
    if case == "W07":
        files["README.md"] += "Prior completion evidence is stale: the empty-name regression now fails.\n"
    return files

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("case", choices=CASES)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    dest = args.destination
    # Refuse all pre-existing destinations, including symlinks.
    dest.mkdir(parents=False, exist_ok=False)
    for name, content in files_for(args.case).items():
        path = dest / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
    print(dest.resolve())

if __name__ == "__main__":
    main()
