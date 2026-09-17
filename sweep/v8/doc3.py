import io, sys
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
old = "| 7 (1 fail = probe sai enum `B1`) \u2192 **8/8** |"
new = "| **16/16** (v\u00f2ng 3: harness s\u1eeda expectation \u2014 enum \u0111\u00fang l\u00e0 `PRE_INTERMEDIATE`, update c\u1ea7n \u2265 2 d\u00f2ng, `transcript` ph\u1ea3i c\u00f3 m\u1eb7t) ; th\u00eam 404-cho-nh\u00e1p (F89) |"
if old not in t:
    print("NOT FOUND"); sys.exit(1)
t = t.replace(old, new, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("ok")
