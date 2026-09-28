#!/usr/bin/env python3
"""comment_only.py — chứng minh một diff là "comment-only" (không đổi code).

Vì sao cần: audit-v20 F-20-06 — một đợt thêm Javadoc đã XOÁ mất dòng
`public class AdminService {`, làm `mvnw test` BUILD FAILURE. Công cụ stripper
ngây thơ (không hiểu Java text block ba dấu nháy) từng báo oan 2 file (F-20-08).

Cách làm: strip comment + string khỏi CẢ HAI phiên bản (HEAD và working tree),
chuẩn hoá whitespace, rồi so sánh. Nếu phần code còn lại giống hệt → comment-only.

Hỗ trợ: Java (line comment //, block /* */, text block ba nháy), JS/Vue (//, /* */,
template string backtick), CSS (/* */), HTML (<!-- -->).

Dùng:
  python sweep/harness/comment_only.py                 # so HEAD vs worktree
  python sweep/harness/comment_only.py --files a.java  # chỉ vài file
  python sweep/harness/comment_only.py --rev HEAD~1    # so với revision khác
"""
import argparse
import re
import subprocess
import sys


def strip_code(src: str, kind: str) -> str:
    """Trả về phần CODE của src (bỏ comment, giữ string ở dạng placeholder).

    LƯU Ý (audit-v21 F-21-03): với file `.vue`, comment trong `<template>` KHÔNG
    được bỏ qua như comment thường — một comment HTML ở CẤP GỐC của template là
    một ROOT NODE, biến component thành fragment (2 root) và làm
    `wrapper.attributes()` trả `undefined` → vỡ test. Vì vậy với `kind == "vue"`
    ta GIỮ NGUYÊN comment template (coi là code) để thay đổi đó bị bắt là code-changed.
    """
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ""
        # Java text block """ ... """
        if kind == "java" and src.startswith('"""', i):
            j = src.find('"""', i + 3)
            j = n if j < 0 else j + 3
            out.append('"TB"')  # placeholder — nội dung text block KHÔNG phải code
            i = j
            continue
        # line comment
        if c == "/" and nxt == "/":
            j = src.find("\n", i)
            i = n if j < 0 else j
            continue
        # block comment
        if c == "/" and nxt == "*":
            j = src.find("*/", i + 2)
            i = n if j < 0 else j + 2
            continue
        # HTML comment — CHỈ strip khi kind == "html".
        # Với "vue" thì GIỮ (xem docstring): comment template là structural.
        if kind == "html" and src.startswith("<!--", i):
            j = src.find("-->", i + 4)
            i = n if j < 0 else j + 3
            continue
        # string literal (giữ placeholder để không phá cấu trúc)
        if c == '"':
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == '"':
                    break
                j += 1
            out.append('"S"')
            i = min(j + 1, n)
            continue
        if c == "'":
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == "'":
                    break
                j += 1
            out.append("'C'")
            i = min(j + 1, n)
            continue
        if c == "`" and kind in ("js", "vue"):
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == "`":
                    break
                j += 1
            out.append("`T`")
            i = min(j + 1, n)
            continue
        out.append(c)
        i += 1
    # bỏ mọi whitespace để so cấu trúc
    return re.sub(r"\s+", "", "".join(out))


def kind_of(path: str) -> str:
    if path.endswith(".java"):
        return "java"
    if path.endswith(".vue"):
        return "vue"
    if path.endswith((".js", ".ts", ".mjs")):
        return "js"
    if path.endswith(".css"):
        return "css"
    if path.endswith((".html", ".htm")):
        return "html"
    return "js"


def git_show(rev: str, path: str) -> str:
    r = subprocess.run(["git", "show", f"{rev}:{path}"],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout if r.returncode == 0 else None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--rev", default="HEAD")
    ap.add_argument("--files", nargs="*")
    args = ap.parse_args()

    if args.files:
        files = args.files
    else:
        r = subprocess.run(["git", "diff", "--name-only", args.rev],
                           capture_output=True, text=True, encoding="utf-8")
        files = [f for f in r.stdout.splitlines() if f.strip()]

    changed, same, skipped = [], [], []
    for f in files:
        old = git_show(args.rev, f)
        try:
            with open(f, encoding="utf-8") as fh:
                new = fh.read()
        except OSError:
            skipped.append((f, "không đọc được worktree"))
            continue
        if old is None:
            skipped.append((f, "file mới (không có ở HEAD)"))
            continue
        k = kind_of(f)
        if strip_code(old, k) == strip_code(new, k):
            same.append(f)
        else:
            changed.append(f)

    print(f"comment-only: {len(same)}/{len(same) + len(changed)} file")
    if changed:
        print("\nCODE ĐỔI (KHÔNG comment-only):")
        for f in changed:
            print("  " + f)
    if skipped:
        print("\nBỏ qua:")
        for f, why in skipped:
            print(f"  {f}  ({why})")
    return 1 if changed else 0


if __name__ == "__main__":
    sys.exit(main())
