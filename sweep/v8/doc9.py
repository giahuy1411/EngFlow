import io, sys
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
old = """cmd /c "mvnw.cmd -o test"                                   # 354/354 BUILD SUCCESS
Set-Location frontend; cmd /c "npx vitest run"             # 82/82 (17 files)"""
new = """cmd /c "mvnw.cmd test"                                     # 369/369 BUILD SUCCESS (354 + 15 c\u1ee7a F88/F89)
Set-Location frontend; cmd /c "npx vitest run"             # 85/85 (18 files)"""
if old not in t:
    print("NOT FOUND A"); sys.exit(1)
t = t.replace(old, new, 1)

old2 = """# v\u00f2ng 3 (2026-09-16)
Set-Location sweep\\v8; node p5.js"""
new2 = """# v\u00f2ng 3 (2026-09-16) \u2014 CH\u1ea0Y T\u1eea TRONG sweep\\v8 (harness ghi JSON theo CWD)
Set-Location sweep\\v8; node p5.js"""
if old2 not in t:
    print("NOT FOUND B"); sys.exit(1)
t = t.replace(old2, new2, 1)

old3 = """cmd /c "set NODE_PATH=%APPDATA%\\npm\\node_modules&& node xss_poc.js\""""
new3 = old3 + """

# v\u00f2ng 3 l\u1ea7n 2 \u2014 F88 (admin s\u1eeda b\u00e0i h\u1ecdc video) + F89 (nh\u00e1p kh\u00f4ng l\u1ed9)
Set-Location sweep\\v8; node p88live.js                                   # 16/16 tr\u00ean container \u0111\u00e3 rebuild
cmd /c "set NODE_PATH=%APPDATA%\\npm\\node_modules&& node ui\\v4edit.js"  # 13/13: b\u1ea5m S\u1eeda -> PUT 200 -> toast -> ph\u1ee5 \u0111\u1ec1 gi\u1eef nguy\u00ean
python sweep\\v8\\sqlrun.py sweep\\v8\\clean_f88.sql                        # d\u1ecdn row probe (users/vocab/submissions/attempts/payments)
python sweep\\v8\\sqlrun.py sweep\\v8\\parity.sql                           # ph\u1ea3i v\u1ec1 \u0111\u00fang baseline"""
if old3 not in t:
    print("NOT FOUND C"); sys.exit(1)
t = t.replace(old3, new3, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("ok")
