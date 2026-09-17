import io
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
a = "| **UI-3** |"
i = t.index(a)
j = t.index("\n", t.index("nh\u01b0ng `82/82` v\u1eabn xanh", i))
seg = t[i:j]
t2 = t.replace(seg, seg.replace("`82/82` v\u1eabn xanh", "`85/85` v\u1eabn xanh (sau F88)"), 1)
old = "- **V\u00f2ng 3 (2026-09-16) ch\u1ea1y l\u1ea1i tr\u00ean b\u1ea3n d\u1ef1ng cu\u1ed1i, kh\u00f4ng s\u1eeda `src/`**: `354/354` backend (fresh run), `82/82` frontend (17 files),"
new = ("- **V\u00f2ng 3 l\u1ea7n 1 (2026-09-16)**: `354/354` backend (fresh run), `82/82` frontend (17 files), "
       "(kh\u00f4ng s\u1eeda `src/`; v\u00f2ng 3 **l\u1ea7n 2** c\u00f3 s\u1eeda \u2192 `369/369` + `85/85`, xem \u00a71.7). Ghi ch\u00fa c\u0169:")
if old not in t2:
    print("NOT FOUND B"); raise SystemExit(1)
t2 = t2.replace(old, new, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t2.replace("\n", "\r\n") if crlf else t2)
print("ok")
