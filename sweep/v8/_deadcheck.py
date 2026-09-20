import os, re
prefixes=["geo-btn","geo-input","geo-badge","geo-marquee","geo-confetti","geo-squiggle","geo-wiggle","geo-pop-in","geo-shadow","geo-heading","geo-label","geo-checkbox","geo-dot-grid","geo-blob-","geo-body"]
seen={p:set() for p in prefixes}
dyn=set()
for root,dirs,files in os.walk("frontend/src"):
    for f in files:
        if f.endswith((".vue",".js")):
            p=os.path.join(root,f); t=open(p,encoding="utf-8",errors="ignore").read()
            for pr in prefixes:
                if pr in t: seen[pr].add(os.path.relpath(p,"frontend/src"))
            for m in re.finditer(r"geo-[a-z-]+[-_]?\x24", t): dyn.add(m.group())
for pr in prefixes:
    if seen[pr]: print("USED-PREFIX",pr,"->",", ".join(sorted(seen[pr])))
print("DYNAMIC:", dyn if dyn else "NONE")
