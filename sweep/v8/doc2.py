import io, sys
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
old = "| P3a (m\u1edbi) | AI:"
new = "| P3a (m\u1edbi, v\u00f2ng 3 ch\u1ea1y l\u1ea1i **36/36**) | AI:"
if old not in t:
    print("A NOT FOUND"); sys.exit(1)
t = t.replace(old, new, 1)
old2 = "| 37 probes \u2192 **1 l\u1ed7i th\u1eadt** (F86)"
new2 = "| 36 probes \u2192 **1 l\u1ed7i th\u1eadt** (F86)"
if old2 in t:
    t = t.replace(old2, new2, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("ok")
