import io
p = "sweep/v8/p4e.js"
t = io.open(p, encoding="utf-8", newline="").read()
old = "    const ok = expect.indexOf(r.status) >= 0;"
new = ("    const ok = expect.indexOf(r.status) >= 0;\n"
       "    tally.total++; if (ok) { tally.pass++ } else { tally.fail++; tally.failed.push(label) }")
assert old in t
t = t.replace(old, new, 1)
old2 = 'console.log("  cleanup: created=" + ids.length + " leftover-ZZ=" + zz.length);'
new2 = (old2 + "\n"
        '  console.log("=== PHASE4E-UPLOAD total=" + tally.total + " FAIL=" + tally.fail'
        ' + (tally.fail ? " :: " + tally.failed.join(", ") : ""));')
assert old2 in t
t = t.replace(old2, new2, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t)
print("ok")
