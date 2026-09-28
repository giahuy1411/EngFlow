#!/usr/bin/env python3
"""comment_only.py — chứng minh một diff là "comment-only" (không đổi code).

Vì sao cần: audit-v20 F-20-06 — một đợt thêm Javadoc đã XOÁ mất dòng
`public class AdminService {`, làm `mvnw test` BUILD FAILURE. Công cụ stripper
ngây thơ (không hiểu Java text block ba dấu nháy) từng báo oan 2 file (F-20-08).

Cách làm: bỏ comment khỏi CẢ HAI phiên bản (HEAD và working tree) nhưng GIỮ NGUYÊN
nội dung string (audit-v21 F-21-04: mask string từng làm thay đổi chỉ-ở-string —
đổi URL/tên role/regex — lọt lưới), chuẩn hoá whitespace, rồi so sánh. Nếu phần code
còn lại giống hệt → comment-only.

Hỗ trợ: Java (// , /* */ , text block ba nháy), JS/Vue (// , /* */ , backtick), CSS (/* */),
HTML (<!-- -->). File `.vue` xử lý đặc biệt: comment trong `<template>` được GIỮ NGUYÊN
vì ở cấp gốc nó là một root node (xem strip_code).

Dùng:
  python sweep/harness/comment_only.py                 # so HEAD vs worktree
  python sweep/harness/comment_only.py --files a.java  # chỉ vài file
  python sweep/harness/comment_only.py --rev HEAD~1    # so với revision khác
"""
import argparse
import re
import subprocess
import sys

# Windows console mặc định cp1252 không in được tiếng Việt → ép UTF-8 để không crash
# khi in báo cáo (audit-v21: gặp UnicodeEncodeError thật khi liệt kê file code-changed).
try:
    sys.stdout.reconfigure(encoding="utf-8")
except (AttributeError, ValueError):
    pass

# Bảng extension → ngôn ngữ, dùng cho CẢ việc lọc file lẫn chọn cách strip (audit-v21:
# trước đây `kind_of` và `TEXT_EXT` là hai danh sách song song, dễ lệch nhau).
EXT_KIND = {
    ".java": "java",
    ".vue": "vue",
    ".js": "js", ".ts": "js", ".mjs": "js",
    ".css": "css",
    ".html": "html", ".htm": "html",
    ".sql": "sql", ".py": "py",
}


def strip_code(src: str, kind: str) -> str:
    """Trả về phần CODE của src: bỏ comment, GIỮ NGUYÊN string.

    LƯU Ý (audit-v21 F-21-03): với file `.vue`, comment trong `<template>` KHÔNG
    được bỏ qua như comment thường — một comment HTML ở CẤP GỐC của template là
    một ROOT NODE, biến component thành fragment (2 root) và làm
    `wrapper.attributes()` trả `undefined` → vỡ test. Vì vậy với `kind == "vue"`
    ta GIỮ NGUYÊN comment template (coi là code) để thay đổi đó bị bắt là code-changed.
    (Đây là quy tắc bảo thủ: comment template lồng bên trong cũng bị coi là code —
    chấp nhận để không bỏ sót ca fragment, ca nguy hiểm thật.)
    """
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ""
        # Java text block """ ... """ — GIỮ NGUYÊN nội dung (nhất quán với luật giữ string).
        if kind == "java" and src.startswith('"""', i):
            j = src.find('"""', i + 3)
            j = n if j < 0 else j + 3
            out.append(src[i:j])
            i = j
            continue
        # line comment
        if c == "/" and nxt == "/":
            j = src.find("\n", i)
            i = n if j < 0 else j
            continue
        # Python line comment (#) — chỉ khi kind == "py"
        if kind == "py" and c == "#":
            j = src.find("\n", i)
            i = n if j < 0 else j
            continue
        # SQL line comment (--) — chỉ khi kind == "sql"
        if kind == "sql" and c == "-" and nxt == "-":
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
        # string literal — GIỮ NGUYÊN nội dung (không mask).
        # audit-v21 F-21-04 (security review): masking "…" bằng placeholder làm công cụ
        # KHÔNG bắt được thay đổi chỉ ở string (đổi URL, tên role, regex…). Nay giữ
        # nội dung để thay đổi string thật sự bị coi là code-changed.
        # Ba loại dấu nháy (" ' `) dùng chung một vòng quét; backtick chỉ áp dụng cho js/vue.
        if c in ('"', "'") or (c == "`" and kind in ("js", "vue")):
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == c:
                    break
                j += 1
            end = min(j + 1, n)
            out.append(src[i:end])
            i = end
            continue
        out.append(c)
        i += 1
    # bỏ mọi whitespace để so cấu trúc
    return re.sub(r"\s+", "", "".join(out))


def kind_of(path: str) -> str:
    """Ngôn ngữ của file theo bảng EXT_KIND (mặc định 'js' cho extension lạ)."""
    for ext, kind in EXT_KIND.items():
        if path.endswith(ext):
            return kind
    return "js"


def git_show(rev: str, path: str) -> str | None:
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
    # Chỉ so file source văn bản (theo EXT_KIND) — bỏ binary (PNG/JSON/log…)
    # để không crash UnicodeDecodeError.
    for f in files:
        if not any(f.endswith(ext) for ext in EXT_KIND):
            skipped.append((f, "không phải file source văn bản"))
            continue
        old = git_show(args.rev, f)
        try:
            with open(f, encoding="utf-8") as fh:
                new = fh.read()
        except (OSError, UnicodeDecodeError):
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
