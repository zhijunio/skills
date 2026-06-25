#!/usr/bin/env python3
"""Convert github.com/.../blob/<ref>/<path> to raw.githubusercontent.com URL. Print nothing if not a blob URL."""
import re
import sys
import urllib.parse


def main() -> None:
    if len(sys.argv) < 2:
        return
    u = sys.argv[1].strip()
    m = re.match(
        r"^https://github\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.+)$",
        u,
    )
    if not m:
        return
    owner, repo, ref, path = m.groups()
    path = urllib.parse.unquote(path)
    # Strip query/fragment from path segment if user pasted ?plain=1 etc.
    path = path.split("?")[0].split("#")[0]
    print(
        f"https://raw.githubusercontent.com/{owner}/{repo}/{ref}/{path}",
        end="",
    )


if __name__ == "__main__":
    main()
