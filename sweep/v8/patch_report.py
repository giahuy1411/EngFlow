import io
import sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")

P = r"C:\Users\ASUS\Documents\LAPTRINH\engflow"
rep = P + r"\.specify\specs\audit-v8-full\REPORT.md"
tsk = P + r"\.specify\specs\audit-v8-full\tasks.md"

rows = open(P + r"\sweep\v8\p4rows.txt", encoding="utf-8-sig").read().rstrip().split(chr(10))


def edit(path, pairs):
    L = open(path, encoding="utf-8").read().split(chr(10))
    for needle, action, newlines in pairs:
        hit = [k for k, l in enumerate(L) if needle in l]
        print(path.split(chr(92))[-1], repr(needle[:28]).encode("ascii", "replace").decode(), "hits", len(hit))
        assert len(hit) == 1, needle
        k = hit[0]
        if action == "replace":
            L[k:k + 1] = newlines
        else:
            L[k + 1:k + 1] = newlines
    open(path, "w", encoding="utf-8", newline="").write(chr(10).join(L))


# 1. header probe count
L = open(rep, encoding="utf-8").read().split(chr(10))
k = [i for i, l in enumerate(L) if "241 probe" in l]
print("header hits", len(k))
if k:
    i = k[0]
    L[i] = L[i].replace(
        "**241 probe HTTP ch\u1ee7 \u0111\u1ed9ng** (P1 124, P2 61, P3a 37, P3b 19)",
        "**287 probe HTTP ch\u1ee7 \u0111\u1ed9ng** (P1 124 \u00b7 P2 61 \u00b7 P3a 37 \u00b7 P3b 19 \u00b7 "
        "P4a 18 \u00b7 P4b 7 \u00b7 P4c 8 \u00b7 P4d 5 \u00b7 P4e 8)")
    open(rep, "w", encoding="utf-8", newline="").write(chr(10).join(L))
    print("header updated:", "287 probe" in open(rep, encoding="utf-8").read())

# 2. insert P4 rows after the P3b table row
edit(rep, [("| P3b (m\u1edbi)", "after", rows)])

# 3. tasks.md: append T27
edit(tsk, [("- [x] T26", "after", [
    "- [x] T27 \u0110\u00f3ng gap coverage c\u00f2n s\u1ed1t (authz\u2192write, CRUD video, multipart, manual grade): p4a\u2013p4e, "
    "287 probe, DB v\u1ec1 baseline",
    "- [x] T28 \u0110\u00ednh ch\u00ednh b\u00e1o c\u00e1o: s\u1ed1 probe 241\u2192287, g\u1ee1 claim \"demo creds\", nh\u1eadn 2 l\u1ea7n "
    "b\u00e1o s\u1ed1i khi ch\u01b0a c\u00f3 file b\u1eb1ng ch\u1ee9ng (\"21/21\" p4c, \"80 tests\" frontend)"])])
