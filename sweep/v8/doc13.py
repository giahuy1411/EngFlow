import io
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
a = "- **V\u00f2ng 3 (2026-09-16) ch\u1ea1y l\u1ea1i tr\u00ean b\u1ea3n d\u1ef1ng cu\u1ed1i, kh\u00f4ng s\u1eeda `src/`**: `354/354` backend (fresh run), `82/82` frontend (17 files),"
b = "- **V\u00f2ng 3 l\u1ea7n 1 (2026-09-16), kh\u00f4ng s\u1eeda `src/`**: `354/354` backend (fresh run), `82/82` frontend (17 files),"
if a not in t:
    print("A NOT FOUND"); raise SystemExit(1)
t = t.replace(a, b, 1)
c = "ch\u1ec9 1 test\n(`ExerciseServiceAdminPaginationTest`) c\u1eadp nh\u1eadt sang contract m\u1edbi"
d = "ch\u1ec9 1 test\n(`ExerciseServiceAdminPaginationTest`) c\u1eadp nh\u1eadt sang contract m\u1edbi"
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("ok")
