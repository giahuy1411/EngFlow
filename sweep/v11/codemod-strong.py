"""
audit-v11 F132 codemod 2 — white text on a vivid fill.

Only rewrites `bg-accent` -> `bg-accent-strong` (and `bg-secondary` -> `bg-secondary-strong`)
INSIDE a single class attribute that also contains `text-white`. Everything else keeps the
vivid hue: fills under dark ink (bg-tertiary/bg-quaternary with text-foreground = 8.76 / 7.61:1),
borders, decorative shapes, and /opacity tints are all correct as-is.

Scans class="..." and :class="..." attributes. Prefixed forms (hover:bg-accent) are handled by
the same token-boundary regex.
"""
import os, re

STRONG = {"accent": "accent-strong", "secondary": "secondary-strong"}

# match a class-ish attribute value: class="..."  or  :class="..."
attr_re = re.compile(r'((?::)?class)="([^"]*)"')
bg_re = re.compile(r"\bbg-(accent|secondary)\b(?!-)(/[0-9]+)?")


def fix_attr(m):
    attr, val = m.group(1), m.group(2)
    # only when the SAME element also has white text
    if "text-white" not in val:
        return m.group(0)
    new = bg_re.sub(lambda b: "bg-" + STRONG[b.group(1)] + (b.group(2) or ""), val)
    return attr + '="' + new + '"'


changed = []
for root, _, files in os.walk("."):
    for fn in files:
        if not fn.endswith(".vue"):
            continue
        p = os.path.join(root, fn)
        s = open(p, encoding="utf-8").read()
        o = s
        s = attr_re.sub(fix_attr, s)
        if s != o:
            open(p, "w", encoding="utf-8").write(s)
            changed.append(p.replace(os.sep, "/"))

print("files changed:", len(changed))
for c in sorted(changed):
    print("  " + c)
