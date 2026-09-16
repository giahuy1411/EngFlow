#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Task 5 — Build authoritative backend endpoint inventory from SOURCE.

Reads every @*Mapping annotation in src/main/java/**/controller/**/*.java
(including BARE @GetMapping/@PostMapping that inherit the class-level prefix)
and emits evidence/endpoint-inventory.json.

No inference from UI names. No stale documentation.
"""
import json
import os
import re

ROOT = r"C:\Users\ASUS\Documents\LAPTRINH\engflow"
CTRL = os.path.join(ROOT, "src", "main", "java", "com", "datn", "engflow", "controller")
OUT = os.path.join(ROOT, ".specify", "specs", "audit-v8-full", "evidence", "endpoint-inventory.json")

# Matches @GetMapping / @GetMapping("...") / @PostMapping(value={...}, consumes="...")
ANN_RE = re.compile(r'@(Get|Post|Put|Delete|Patch|Request)Mapping\b', re.S)
STR_RE = re.compile(r'"([^"]*)"')


def read_balanced(src, start):
    """Given index just after the annotation name, return (argstr, end_index).
    argstr is '' when the annotation is bare."""
    i = start
    while i < len(src) and src[i] in " \t\r\n":
        i += 1
    if i >= len(src) or src[i] != '(':
        return "", i
    depth = 0
    j = i
    while j < len(src):
        if src[j] == '(':
            depth += 1
        elif src[j] == ')':
            depth -= 1
            if depth == 0:
                return src[i + 1:j], j + 1
        j += 1
    return src[i:], len(src)


def main():
    rows = []
    bare_count = 0
    for dirpath, _d, filenames in os.walk(CTRL):
        for fn in sorted(f for f in filenames if f.endswith(".java")):
            full = os.path.join(dirpath, fn)
            with open(full, encoding="utf-8") as fh:
                src = fh.read()
            cls = os.path.splitext(fn)[0]
            rel = os.path.relpath(full, ROOT).replace("\\", "/")

            class_prefix = ""
            class_seen = False
            for m in ANN_RE.finditer(src):
                kind = m.group(1)
                argstr, _end = read_balanced(src, m.end())
                if kind == 'Request':
                    lits = [p for p in STR_RE.findall(argstr) if p.startswith("/")]
                    if not class_seen and lits:
                        class_prefix = lits[0]
                        class_seen = True
                    continue

                lits = [p for p in STR_RE.findall(argstr) if p.startswith("/")]
                consumes = None
                mc = re.search(r'consumes\s*=\s*"([^"]+)"', argstr)
                if mc:
                    consumes = mc.group(1)
                params_cond = None
                mp = re.search(r'params\s*=\s*"([^"]+)"', argstr)
                if mp:
                    params_cond = mp.group(1)
                required = None
                if 'required' in argstr:
                    required = 'required' in argstr

                if not lits:
                    # bare mapping -> inherits class prefix (may be "")
                    bare_count += 1
                    paths = [class_prefix or "/"]
                else:
                    paths = [p if p.startswith("/api") or not class_prefix
                             else class_prefix + p for p in lits]

                for p in paths:
                    p = re.sub(r'/+', '/', p)
                    if not p.startswith("/"):
                        p = "/" + p
                    rows.append({
                        "controller": cls,
                        "source": rel,
                        "httpMethod": kind.upper(),
                        "path": p,
                        "classPrefix": class_prefix,
                        "consumes": consumes,
                        "params": params_cond,
                    })

    seen, uniq = set(), []
    for r in rows:
        key = (r["controller"], r["httpMethod"], r["path"])
        if key in seen:
            continue
        seen.add(key)
        uniq.append(r)
    uniq.sort(key=lambda r: (r["controller"], r["path"], r["httpMethod"]))

    by_ctrl = {}
    by_method = {}
    for r in uniq:
        by_ctrl[r["controller"]] = by_ctrl.get(r["controller"], 0) + 1
        by_method[r["httpMethod"]] = by_method.get(r["httpMethod"], 0) + 1

    payload = {
        "generatedFrom": "src/main/java/com/datn/engflow/controller/**/*.java",
        "generator": "sweep/v8/endpoint_inventory.py",
        "controllerCount": len(by_ctrl),
        "mappingCount": len(uniq),
        "bareMappingsInheritingClassPrefix": bare_count,
        "byHttpMethod": dict(sorted(by_method.items())),
        "perController": dict(sorted(by_ctrl.items())),
        "endpoints": uniq,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(payload, fh, indent=1, ensure_ascii=False)
    print("controllers=%d unique mappings=%d (bare=%d) -> %s"
          % (len(by_ctrl), len(uniq), bare_count, OUT))
    print("by method:", payload["byHttpMethod"])
    for c, n in sorted(by_ctrl.items()):
        print("  %-45s %d" % (c, n))


if __name__ == "__main__":
    main()
