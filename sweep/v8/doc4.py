import io, sys
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
old = "| 5 (415 do thi\u1ebfu `Content-Type` part meta) \u2192 **8** |"
new = "| **6/6** (v\u00f2ng 3) + 1 check `non-admin (user token)` = 7 case, 0 fail; harness d\u1ecdn s\u1ea1ch `leftover-ZZ=0` |"
if old not in t:
    print("NOT FOUND"); sys.exit(1)
t = t.replace(old, new, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("ok")
