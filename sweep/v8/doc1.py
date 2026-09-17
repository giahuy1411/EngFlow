import io, sys
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")

reps = [
("Baseline backend **332 \u2192 354 tests xanh**\n(+22 regression m\u1edbi), frontend **79 \u2192 82 tests (17 files)**, `vite build` OK (entry 176.68 kB \u2014 b\u1eb1ng baseline).",
 "Baseline backend **332 \u2192 369 tests xanh**\n(+37 regression m\u1edbi qua 3 v\u00f2ng), frontend **79 \u2192 85 tests (18 files)**, `vite build` OK (entry 176.68 kB \u2014 b\u1eb1ng baseline)."),
("Qu\u00e9t **139 mapping/97 handler**, **387 probe HTTP ch\u1ee7 \u0111\u1ed9ng** (P1 124 \u00b7 P2 61 \u00b7 P3a 37 \u00b7 P3b 19 \u00b7 P4a 18 \u00b7 P4b 7 \u00b7 P4c 8 \u00b7 P4d 5 \u00b7 P4e 8 \u00b7 **P5/v\u00f2ng 3: 95**)",
 "Qu\u00e9t **139 mapping/97 handler**, **447 probe HTTP ch\u1ee7 \u0111\u1ed9ng** \u2014 v\u00f2ng 3 \u0111o l\u1ea1i to\u00e0n b\u1ed9 tr\u00ean b\u1ea3n d\u1ef1ng cu\u1ed1i: P1 124 \u00b7 P2 61 \u00b7 P3a 36 \u00b7 P3b 19 \u00b7 P4a 18 \u00b7 P4b 16 \u00b7 P4c 10 \u00b7 P4d 6 \u00b7 P4e 6 \u00b7 **P5 95** \u00b7 **F88/F89 live 16** (b\u1ea3n tr\u01b0\u1edbc ghi P3a 37/P4b 7/P4c 8/P4d 5/P4e 8 l\u00e0 s\u1ed1 c\u1ee7a l\u1ea7n ch\u1ea1y \u0111\u1ea7u, tr\u01b0\u1edbc khi harness \u0111\u01b0\u1ee3c s\u1eeda expectation)"),
("**10 l\u1ed7i th\u1eadt t\u00ecm th\u1ea5y v\u00e0 fix t\u1eadn g\u1ed1c** (F81\u2013F87 + UI-1/UI-2/UI-3, trong \u0111\u00f3 1 chu\u1ed7i stored-XSS \u2192 \u0111\u00e1nh c\u1eafp JWT\n\u0111\u00e3 ch\u1ee9ng minh end-to-end tr\u01b0\u1edbc khi s\u1eeda). *(B\u1ea3n tr\u01b0\u1edbc ghi \"8 l\u1ed7i\" trong khi b\u1ea3ng \u00a72 \u0111\u00e3 li\u1ec7t k\u00ea 9 d\u00f2ng \u2014 \u0111\u1ebfm sai; nay \u0111\u1ebfm l\u1ea1i theo b\u1ea3ng: 10.)*",
 "**12 l\u1ed7i th\u1eadt t\u00ecm th\u1ea5y v\u00e0 fix t\u1eadn g\u1ed1c** (F81\u2013F89 + UI-1/UI-2/UI-3, trong \u0111\u00f3 1 chu\u1ed7i stored-XSS \u2192 \u0111\u00e1nh c\u1eafp JWT\n\u0111\u00e3 ch\u1ee9ng minh end-to-end tr\u01b0\u1edbc khi s\u1eeda; F88 = admin kh\u00f4ng l\u01b0u \u0111\u01b0\u1ee3c b\u00e0i h\u1ecdc video, F89 = b\u00e0i NH\u00c1P l\u1ed9 c\u00f4ng khai \u2014 c\u1ea3 hai t\u00ecm \u1edf v\u00f2ng 3). *(B\u1ea3n tr\u01b0\u1edbc ghi \"8 l\u1ed7i\" trong khi b\u1ea3ng \u00a72 \u0111\u00e3 li\u1ec7t k\u00ea 9 d\u00f2ng \u2014 \u0111\u1ebfm sai; v\u00f2ng 3 \u0111\u1ebfm l\u1ea1i: 10, sau F88/F89: 12.)*"),
]
for old, new in reps:
    if old not in t:
        print("NOT FOUND ::", old[:70].replace("\n", "\\n")); sys.exit(1)
    t = t.replace(old, new, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("header updated")
