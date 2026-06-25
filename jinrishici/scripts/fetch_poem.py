#!/usr/bin/env python3
"""Fetch one line from Jinrishici v1 API."""

import argparse
import json
import sys
from typing import Any, Dict, Optional
from urllib.request import Request, urlopen

SENTENCE_API = "https://v1.jinrishici.com/all.json"
USER_AGENT = "jinrishici-fetch-poem/1.0"


def _fetch_json(url: str, timeout: float = 10.0) -> Optional[Dict[str, Any]]:
    req = Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8", errors="replace")
        return json.loads(body)
    except Exception:
        return None


def format_poem_line(data: Dict[str, Any]) -> str:
    content = (data.get("content") or "").strip()
    origin = (data.get("origin") or "").strip()
    author = (data.get("author") or "").strip()
    if not content:
        return ""
    if author and origin:
        return "{}—— {}《{}》".format(content, author, origin)
    if author:
        return "{}—— {}".format(content, author)
    if origin:
        return "{}《{}》".format(content, origin)
    return content


def main() -> int:
    p = argparse.ArgumentParser(description="Fetch and format one line from jinrishici.com")
    p.add_argument(
        "--json",
        action="store_true",
        help="Print full JSON (pretty); skip formatted line",
    )
    p.add_argument(
        "--url",
        default=SENTENCE_API,
        help="API URL (default official v1 all.json)",
    )
    p.add_argument(
        "--timeout",
        type=float,
        default=10.0,
        metavar="SEC",
        help="Request timeout in seconds",
    )
    args = p.parse_args()

    raw = _fetch_json(args.url, timeout=args.timeout)
    if not raw:
        print("Request failed or response could not be parsed.", file=sys.stderr)
        return 1

    if args.json:
        print(json.dumps(raw, ensure_ascii=False, indent=2))
        return 0

    line = format_poem_line(raw)
    if not line:
        print("No valid poem line in response (empty content).", file=sys.stderr)
        return 1
    print(line)
    return 0


if __name__ == "__main__":
    sys.exit(main())
