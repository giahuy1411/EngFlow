#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Coverage check: which inventory endpoints were ACTUALLY probed by p1..p6 jsons?

Matches segment-wise: an inventory template segment ({id}, {filename:.+}, {*})
matches ANY single concrete segment from a probe, and `**` matches the rest.
"""
import json
import os
import re
import glob

ROOT = r"C:\Users\ASUS\Documents\LAPTRINH\engflow"
INV = os.path.join(ROOT, ".specify", "specs", "audit-v8-full", "evidence", "endpoint-inventory.json")
SWEEP = os.path.join(ROOT, "sweep", "v8")
OUT = os.path.join(ROOT, ".specify", "specs", "audit-v8-full", "evidence", "coverage-round1.json")


def segs(p):
    p = p.split("?")[0]
    p = re.sub(r'/+', '/', p).rstrip('/')
    return [s for s in p.split('/') if s != '']


def seg_matches(tmpl, concrete):
    """Does one template segment match one concrete segment?"""
    if tmpl.startswith('{') and tmpl.endswith('}'):
        inner = tmpl[1:-1]
        if inner == '*':
            return True
        if ':' in inner:
            # Spring regex template {filename:.+} -> treat as "any non-empty"
            return concrete != ''
        return concrete != ''
    return tmpl == concrete


def path_matches(tmpl_segs, concrete_segs):
    """Match with ** support in template."""
    if '**' in tmpl_segs:
        i = tmpl_segs.index('**')
        head, tail = tmpl_segs[:i], tmpl_segs[i + 1:]
        if len(concrete_segs) < len(head) + len(tail):
            return False
        for a, b in zip(head, concrete_segs[:len(head)]):
            if not seg_matches(a, b):
                return False
        for a, b in zip(reversed(tail), reversed(concrete_segs)):
            if not seg_matches(a, b):
                return False
        return True
    if len(tmpl_segs) != len(concrete_segs):
        return False
    return all(seg_matches(a, b) for a, b in zip(tmpl_segs, concrete_segs))


def main():
    with open(INV, encoding='utf-8') as fh:
        inv = json.load(fh)

    probes = []   # list of (method, segs, code, exact_path)
    files = sorted(glob.glob(os.path.join(SWEEP, "p*.json")))
    files = [f for f in files if os.path.basename(f) != "p1-root.json"]
    for f in files:
        try:
            with open(f, encoding='utf-8') as fh:
                rows = json.load(fh)
        except Exception as e:
            print("skip %s: %s" % (f, e))
            continue
        if not isinstance(rows, list):
            continue
        for r in rows:
            if not isinstance(r, dict) or 'path' not in r:
                continue
            probes.append((r.get('method'), segs(r['path']), r.get('code'), r['path']))

    covered, uncovered = [], []
    for e in inv['endpoints']:
        t = segs(e['path'])
        hits = [p for p in probes
                if (p[0] == e['httpMethod'] or e['httpMethod'] == 'ANY')
                and path_matches(t, p[1])]
        if hits:
            covered.append(e)
        else:
            uncovered.append(e)

    print("inventory=%d  probe-requests=%d" % (len(inv['endpoints']), len(probes)))
    print("covered=%d  uncovered=%d" % (len(covered), len(uncovered)))
    if uncovered:
        print("\n--- UNCOVERED ---")
        for e in uncovered:
            print("  %-6s %-62s  %s" % (e['httpMethod'], e['path'], e['controller']))

    payload = {
        "probeFiles": [os.path.basename(f) for f in files],
        "probeRequestCount": len(probes),
        "inventoryCount": len(inv['endpoints']),
        "coveredCount": len(covered),
        "uncoveredCount": len(uncovered),
        "uncovered": [{"method": e['httpMethod'], "path": e['path'],
                       "controller": e['controller'], "source": e['source']}
                      for e in uncovered],
    }
    with open(OUT, 'w', encoding='utf-8') as fh:
        json.dump(payload, fh, indent=1, ensure_ascii=False)
    print("\nwrote " + OUT)


if __name__ == '__main__':
    main()
