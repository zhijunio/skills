#!/usr/bin/env python3
"""Check repository metadata using the upstream Agent Skills reference validator.

Known Claude Code extensions are checked separately; this does not produce a
strictly portable export or change any source file.
"""
from pathlib import Path

from skills_ref.errors import ParseError
from skills_ref.parser import parse_frontmatter
from skills_ref.validator import validate_metadata
import strictyaml

ROOT = Path(__file__).resolve().parents[1]
COLLECTIONS = ("engineering", "spring-boot-skills", "collected-skills")
EXTENSIONS = {"disable-model-invocation", "argument-hint"}


def validate(root):
    errors, names, extended = [], set(), 0
    for collection in COLLECTIONS:
        for marker in sorted((root / collection).rglob("SKILL.md")):
            relative = marker.relative_to(root)
            try:
                meta, _ = parse_frontmatter(marker.read_text(encoding="utf-8"))
            except ParseError as exc:
                errors.append(f"{relative}: {exc}")
                continue
            extensions = {key: meta[key] for key in EXTENSIONS if key in meta}
            portable = {key: value for key, value in meta.items() if key not in EXTENSIONS}
            errors.extend(f"{relative}: {error}" for error in validate_metadata(portable, marker.parent))
            name = portable.get("name")
            if isinstance(name, str):
                if name in names:
                    errors.append(f"{relative}: duplicate skill name {name}")
                names.add(name)
            if extensions:
                extended += 1
                print(f"Host extensions: {relative}: {', '.join(sorted(extensions))}")
            manual = extensions.get("disable-model-invocation")
            if manual is not None and manual not in ("true", "false", True, False):
                errors.append(f"{relative}: invalid disable-model-invocation value")
            hint = extensions.get("argument-hint")
            if hint is not None and (not isinstance(hint, str) or not hint.strip()):
                errors.append(f"{relative}: argument-hint must be a nonempty string")
            agent = marker.parent / "agents/openai.yaml"
            if agent.exists():
                try:
                    config = strictyaml.load(agent.read_text(encoding="utf-8")).data
                    policy = config.get("policy", {})
                    implicit = policy.get("allow_implicit_invocation")
                except (strictyaml.YAMLError, AttributeError) as exc:
                    errors.append(f"{relative}: invalid Codex configuration: {exc}")
                    implicit = None
                if implicit is not None and implicit not in ("true", "false", True, False):
                    errors.append(f"{relative}: invalid Codex invocation policy")
            else:
                implicit = None
            if manual in ("true", True) and implicit not in ("false", False):
                errors.append(f"{relative}: manual-only skill lacks matching Codex policy")
            if implicit in ("false", False) and manual not in ("true", True):
                errors.append(f"{relative}: Codex manual-only policy lacks matching Claude Code declaration")
    print(f"Distinct names: {len(names)}; entries with known host extensions: {extended}")
    if not names:
        errors.append("No skills found")
    return errors


if __name__ == "__main__":
    failures = validate(ROOT)
    for failure in failures:
        print(f"ERROR: {failure}")
    if failures:
        raise SystemExit(1)
    print("Portable metadata projection and local extension policy verified.")
