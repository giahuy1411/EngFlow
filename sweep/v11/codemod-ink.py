import os, re

INK = {
    "accent": "accent-ink",
    "secondary": "secondary-ink",
    "tertiary": "tertiary-ink",
    "quaternary": "quaternary-ink",
    "success": "success-ink",
    "warning": "warning-ink",
}

# bare: text-<vivid> NOT followed by '-<word>' (so text-accent-fg is untouched)
pat = re.compile(r"\btext-(accent|secondary|tertiary|quaternary|success|warning)\b(?!-)")
# prefixed: hover:text-<vivid>, group-hover:text-<vivid>, focus:, md:, etc.
pat_pref = re.compile(r"\b((?:[a-z-]+):)text-(accent|secondary|tertiary|quaternary|success|warning)\b(?!-)")

changed = []
for root, _, files in os.walk("."):
    for fn in files:
        if not fn.endswith(".vue"):
            continue
        p = os.path.join(root, fn)
        s = open(p, encoding="utf-8").read()
        o = s
        s = pat.sub(lambda m: "text-" + INK[m.group(1)], s)
        s = pat_pref.sub(lambda m: m.group(1) + "text-" + INK[m.group(2)], s)
        if s != o:
            open(p, "w", encoding="utf-8").write(s)
            changed.append(p.replace(os.sep, "/"))

print("files changed:", len(changed))
for c in sorted(changed):
    print("  " + c)
