import io, sys, json, glob
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
for p in sorted(glob.glob("sweep/v8/ui/routes-*.json")):
    rows = json.load(open(p, encoding="utf-8"))
    prob = [r for r in rows if r["nav"] != "ok" or r["errors"] or r["failed"] or not r["info"].get("hasApp")]
    over = [r["route"] for r in rows if r["info"].get("scrollW", 0) > r["info"].get("clientW", 10 ** 9) + 1]
    fonts = sorted({f for r in rows for f in (r["info"].get("fonts") or [])})
    thin = [(r["route"], r["info"].get("textLen")) for r in rows if (r["info"].get("textLen") or 0) < 300]
    print("==", p, "routes=" + str(len(rows)), "PROBLEM=" + str(len(prob)))
    for r in prob:
        print("   !", r["route"], r["label"], r["nav"], json.dumps(r["errors"][:2])[:160], json.dumps(r["failed"][:2])[:160])
    print("   fonts:", fonts)
    print("   overflow-list (scrollW>clientW):", len(over), over[:6])
    print("   sparse pages (<300 chars):", thin[:8])
