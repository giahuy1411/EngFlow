import io, sys
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
old = "Json k\u1ebft qu\u1ea3: `sweep/v8/p1.json`, `p2.json`, `p3a.json`, `p3b.json`, `p4a\u2026p4d.json`, **`p5.json`**, `ui/routes-*.json`, **`ui/v3ui.json`**."
new = ("Json k\u1ebft qu\u1ea3: `sweep/v8/p1.json`, `p2.json`, `p3a.json`, `p3b.json`, `p4a\u2026p4d.json`, **`p5.json`**, "
       "`ui/routes-*.json`, **`ui/v3ui.json`**, **`ui/v3b.json`**, **`ui/v4edit.js` log** (`ui/routes-*.log`), `r-p4*.log`, `t-f88.log`.")
if old not in t:
    print("NOT FOUND"); sys.exit(1)
t = t.replace(old, new, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("ok")
