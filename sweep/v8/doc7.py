import io, sys
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
old = "**C\u1ed9ng**: 22 test backend m\u1edbi (332\u2192354), 3 test frontend m\u1edbi (79\u219282), 0 test c\u0169 ph\u1ea3i b\u1ecf;"
new = ("**C\u1ed9ng**: 37 test backend m\u1edbi (332\u2192369: 22 \u1edf v\u00f2ng 1\u20132 + **15 c\u1ee7a F88/F89**), "
       "6 test frontend m\u1edbi (79\u219285: 3 c\u1ee7a UI-2/sanitize-a11y + **3 c\u1ee7a F88**), 0 test c\u0169 ph\u1ea3i b\u1ecf;")
if old not in t:
    print("NOT FOUND"); sys.exit(1)
t = t.replace(old, new, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("ok")
