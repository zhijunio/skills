#!/usr/bin/env python3
"""Fetch Feishu / Lark docx (or wiki→docx) as Markdown via Open API.

Requires: pip install requests
Env: FEISHU_APP_ID, FEISHU_APP_SECRET

App scopes: docx:document:readonly, wiki:wiki:readonly

Usage:
  python3 fetch_feishu.py "https://xxx.feishu.cn/docx/TOKEN"
  python3 fetch_feishu.py "https://xxx.feishu.cn/wiki/WIKI_TOKEN"

Implements Feishu Open API docx block traversal for Markdown export.
"""
from __future__ import annotations

import json
import os
import re
import sys
from typing import Any, Dict, List, Optional, Tuple

try:
    import requests
except ImportError:
    print("Error: pip install requests", file=sys.stderr)
    sys.exit(1)

API = os.environ.get("FEISHU_OPEN_API_BASE", "https://open.feishu.cn/open-apis")


def yaml_escape(s: str) -> str:
    return json.dumps(s or "", ensure_ascii=False)


def get_token() -> Tuple[Optional[str], Optional[str]]:
    app_id = os.environ.get("FEISHU_APP_ID")
    app_secret = os.environ.get("FEISHU_APP_SECRET")
    if not app_id or not app_secret:
        return None, "FEISHU_APP_ID or FEISHU_APP_SECRET not set"
    resp = requests.post(
        f"{API}/auth/v3/tenant_access_token/internal",
        json={"app_id": app_id, "app_secret": app_secret},
        timeout=30,
    )
    d = resp.json()
    if d.get("code") != 0:
        return None, f"Auth failed: {d.get('msg', resp.text)}"
    return d["tenant_access_token"], None


def parse_url(url: str) -> Tuple[str, str]:
    patterns = [
        (r"feishu\.cn/docx/([A-Za-z0-9]+)", "docx"),
        (r"feishu\.cn/docs/([A-Za-z0-9]+)", "legacy_doc"),
        (r"feishu\.cn/wiki/([A-Za-z0-9]+)", "wiki"),
        (r"larksuite\.com/docx/([A-Za-z0-9]+)", "docx"),
        (r"larksuite\.com/docs/([A-Za-z0-9]+)", "legacy_doc"),
        (r"larksuite\.com/wiki/([A-Za-z0-9]+)", "wiki"),
    ]
    for pattern, doc_type in patterns:
        m = re.search(pattern, url)
        if m:
            return m.group(1), doc_type
    return "", "unknown"


def resolve_wiki(token: str, wiki_token: str) -> Tuple[Optional[str], Optional[str], Optional[str]]:
    resp = requests.get(
        f"{API}/wiki/v2/spaces/get_node",
        headers={"Authorization": f"Bearer {token}"},
        params={"token": wiki_token},
        timeout=30,
    )
    d = resp.json()
    if d.get("code") != 0:
        return None, None, d.get("msg", resp.text)
    node = d["data"]["node"]
    return node.get("obj_token"), node.get("obj_type"), None


def doc_title(token: str, document_id: str) -> str:
    resp = requests.get(
        f"{API}/docx/v1/documents/{document_id}",
        headers={"Authorization": f"Bearer {token}"},
        timeout=30,
    )
    d = resp.json()
    if d.get("code") != 0:
        return ""
    doc = d.get("data", {}).get("document", {})
    return (doc.get("title") or "").strip()


def fetch_all_blocks(
    token: str, document_id: str,
) -> Tuple[Dict[str, Dict[str, Any]], List[Dict[str, Any]]]:
    items: Dict[str, Dict[str, Any]] = {}
    order: List[Dict[str, Any]] = []
    page_token: Optional[str] = None
    while True:
        params: Dict[str, Any] = {"page_size": 500}
        if page_token:
            params["page_token"] = page_token
        resp = requests.get(
            f"{API}/docx/v1/documents/{document_id}/blocks",
            headers={"Authorization": f"Bearer {token}"},
            params=params,
            timeout=60,
        )
        d = resp.json()
        if d.get("code") != 0:
            raise RuntimeError(f"list blocks: {d.get('msg', resp.text)}")
        data = d.get("data") or {}
        for it in data.get("items") or []:
            bid = it.get("block_id")
            if bid:
                items[str(bid)] = it
                order.append(it)
        page_token = data.get("page_token") or None
        if not page_token:
            break
    return items, order


def elements_to_text(elements: Any) -> str:
    if not elements:
        return ""
    out: List[str] = []
    for el in elements:
        if not isinstance(el, dict):
            continue
        tr = el.get("text_run")
        if isinstance(tr, dict) and tr.get("content"):
            out.append(str(tr["content"]))
    return "".join(out)


def block_plain_text(block: Dict[str, Any]) -> str:
    for _k, val in block.items():
        if isinstance(val, dict) and "elements" in val:
            t = elements_to_text(val.get("elements"))
            if t.strip():
                return t.strip()
    return ""


def block_to_md(block: Dict[str, Any]) -> Optional[str]:
    bt = block.get("block_type")
    text = block_plain_text(block)
    if not text:
        return None
    # Common Feishu docx block types (see open.feishu.cn doc)
    if bt == 2:
        return text + "\n\n"
    if bt == 3:
        return "# " + text + "\n\n"
    if bt == 4:
        return "## " + text + "\n\n"
    if bt == 5:
        return "### " + text + "\n\n"
    if bt == 6:
        return "#### " + text + "\n\n"
    if bt in (12, 17):  # bullet / list-ish
        return "- " + text + "\n"
    if bt == 13:
        return "1. " + text + "\n"
    if bt == 14:  # code (often has style)
        return "```\n" + text + "\n```\n\n"
    return text + "\n\n"


def dfs_blocks(block_id: str, items: Dict[str, Dict[str, Any]], lines: List[str]) -> None:
    b = items.get(block_id)
    if not b:
        return
    md = block_to_md(b)
    if md:
        lines.append(md)
    for cid in b.get("children") or []:
        dfs_blocks(str(cid), items, lines)


def document_to_markdown(token: str, document_id: str) -> str:
    items, order = fetch_all_blocks(token, document_id)
    lines: List[str] = []
    page = next((b for b in items.values() if b.get("block_type") == 1), None)
    if page:
        for cid in page.get("children") or []:
            dfs_blocks(str(cid), items, lines)
    if not "".join(lines).strip():
        for b in order:
            if b.get("block_type") == 1:
                continue
            md = block_to_md(b)
            if md:
                lines.append(md)
    return "".join(lines).strip() + "\n"


def main() -> None:
    if len(sys.argv) < 2:
        print("usage: fetch_feishu.py <feishu_or_lark_url>", file=sys.stderr)
        sys.exit(2)
    url = sys.argv[1].strip()
    token, err = get_token()
    if not token:
        print(err or "auth error", file=sys.stderr)
        sys.exit(1)

    token_id, doc_type = parse_url(url)
    if not token_id:
        print("Could not parse Feishu/Lark URL", file=sys.stderr)
        sys.exit(1)

    if doc_type == "legacy_doc":
        print(
            "Legacy /docs/ is not supported. Convert to docx in Feishu or use a docx link.",
            file=sys.stderr,
        )
        sys.exit(1)

    document_id = token_id
    if doc_type == "wiki":
        obj_token, obj_type, werr = resolve_wiki(token, token_id)
        if werr or not obj_token:
            print(werr or "wiki resolve failed", file=sys.stderr)
            sys.exit(1)
        if obj_type != "docx":
            print(f"Wiki node is not docx (got {obj_type}). Export or open docx.", file=sys.stderr)
            sys.exit(1)
        document_id = obj_token

    title = doc_title(token, document_id)
    try:
        body = document_to_markdown(token, document_id)
    except Exception as e:
        print(str(e), file=sys.stderr)
        sys.exit(1)

    print(f"---\ntitle: {yaml_escape(title)}\nsource: feishu\ndocument_id: {yaml_escape(document_id)}\nurl: {yaml_escape(url)}\n---\n")
    print(body)


if __name__ == "__main__":
    main()
