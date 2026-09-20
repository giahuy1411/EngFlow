#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""audit-v9 dead-CSS gate: every `.geo-*` CLASS selector in design-system.css
must have at least one literal usage in frontend/src (or index.html).
Comments and `--geo-*` CSS variables are stripped first so they cannot
count as a selector (the v9 draft checker reported a phantom `.geo-shadow-`
that was only a comment mentioning `.geo-shadow-*`)."""
import os, re

CSS = "frontend/src/assets/design-system.css"
ROOTS = ["frontend/src", "frontend/index.html"]

css = open(CSS, encoding="utf-8").read()
css = re.sub(r"/\*.*?\*/", "", css, flags=re.S)          # comments out
selectors = sorted(set(re.findall(r"\.(geo-[A-Za-z0-9_-]+)", css)))

texts = []
for root in ROOTS:
    if os.path.isfile(root):
        texts.append((root, open(root, encoding="utf-8", errors="ignore").read()))
        continue
    for dirpath, _d, files in os.walk(root):
        if "node_modules" in dirpath:
            continue
        for fn in files:
            if fn.endswith((".vue", ".js", ".ts", ".html")):
                p = os.path.join(dirpath, fn)
                texts.append((p, open(p, encoding="utf-8", errors="ignore").read()))

used, unused = {}, []
for sel in selectors:
    hits = [p for p, t in texts if sel in t]
    (used.setdefault(sel, hits) if hits else unused.append(sel))

print("selector | usages | first file")
for sel in selectors:
    if sel in used:
        print("USED   %-24s %d  %s" % (sel, len(used[sel]), used[sel][0]))
print("SUMMARY selectors=%d used=%d UNUSED=%d" % (len(selectors), len(used), len(unused)))
for s in unused:
    print("  UNUSED " + s)