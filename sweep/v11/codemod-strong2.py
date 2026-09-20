"""
audit-v11 F132 codemod 3 — data-driven `bg` / `ink` pairs and multi-line class attributes.

These are cases codemod 2 deliberately skipped because the background and the white ink live in
SEPARATE keys of an object literal (e.g. FlashcardGame's THEMES: { bg: 'bg-accent', ink: 'text-white' }),
or the class attribute spans several lines. Handled here with a small explicit allowlist so the
change stays reviewable.

Rule is unchanged: a vivid fill UNDER white text -> *-strong. A vivid fill under dark ink stays.
"""
import os, re

# (file, old, new, why)
EDITS = [
    # FlashcardGame THEMES: bg + ink:'text-white' pairs
    ("views/luyentu/FlashcardGame.vue",
     "verb: { bg: 'bg-accent', ink: 'text-white' }",
     "verb: { bg: 'bg-accent-strong', ink: 'text-white' }",
     "white on #8B5CF6 = 4.23:1 -> #7C3AED = 5.70:1"),
    ("views/luyentu/FlashcardGame.vue",
     "adverb: { bg: 'bg-secondary', ink: 'text-white' }",
     "adverb: { bg: 'bg-secondary-strong', ink: 'text-white' }",
     "white on #F472B6 = 2.65:1 -> #DB2777 = 4.60:1"),
    ("views/luyentu/FlashcardGame.vue",
     "pronoun: { bg: 'bg-accent', ink: 'text-white' }",
     "pronoun: { bg: 'bg-accent-strong', ink: 'text-white' }",
     "white on #8B5CF6 = 4.23:1 -> #7C3AED = 5.70:1"),

    # AdminLayout active-nav states (bg + text-white in one string, but inside a JS array literal)
    ("layouts/AdminLayout.vue",
     "activeClass: 'bg-accent border-foreground text-white shadow-[4px_4px_0px_0px_white] border-2 rounded-md' }",
     "activeClass: 'bg-accent-strong border-foreground text-white shadow-[4px_4px_0px_0px_white] border-2 rounded-md' }",
     "white on #8B5CF6 = 4.23:1"),
    ("layouts/AdminLayout.vue",
     "activeClass: 'bg-secondary border-foreground text-white shadow-[4px_4px_0px_0px_white] border-2 rounded-md' }",
     "activeClass: 'bg-secondary-strong border-foreground text-white shadow-[4px_4px_0px_0px_white] border-2 rounded-md' }",
     "white on #F472B6 = 2.65:1 (appears twice)"),

    # AdminLessonBuilder block-type badges
    ("views/admin/AdminLessonBuilder.vue",
     "badge: 'bg-accent text-white font-black'",
     "badge: 'bg-accent-strong text-white font-black'",
     "white on #8B5CF6 = 4.23:1"),
    ("views/admin/AdminLessonBuilder.vue",
     "badge: 'bg-secondary text-white font-black'",
     "badge: 'bg-secondary-strong text-white font-black'",
     "white on #F472B6 = 2.65:1"),

    # AdminLessons level chips
    ("views/admin/AdminLessons.vue",
     "INTERMEDIATE: 'bg-accent text-white rounded-md border-2 border-foreground'",
     "INTERMEDIATE: 'bg-accent-strong text-white rounded-md border-2 border-foreground'",
     "white on #8B5CF6 = 4.23:1"),
    ("views/admin/AdminLessons.vue",
     "UPPER_INTERMEDIATE: 'bg-secondary text-white rounded-md border-2 border-foreground'",
     "UPPER_INTERMEDIATE: 'bg-secondary-strong text-white rounded-md border-2 border-foreground'",
     "white on #F472B6 = 2.65:1"),

    # AdminSpeakingSubmissions status chip
    ("views/admin/AdminSpeakingSubmissions.vue",
     "className: 'bg-accent text-white' }",
     "className: 'bg-accent-strong text-white' }",
     "white on #8B5CF6 = 4.23:1"),

    # AdminDashboard PageHeader icon sits on a solid bg-accent chip
    ("views/admin/AdminDashboard.vue",
     'iconBg="bg-accent" iconColor="text-white"',
     'iconBg="bg-accent-strong" iconColor="text-white"',
     "white on #8B5CF6 = 4.23:1"),

    # AdminExercises difficulty chips (bg-secondary-strong already applied by codemod 2)
    ("views/admin/AdminExercises.vue",
     "'bg-accent-strong text-white'",
     "'bg-accent-strong text-white'",
     "already correct"),
]

root = "."
applied = 0
missing = []
for rel, old, new, why in EDITS:
    p = os.path.join(root, rel)
    if not os.path.exists(p):
        missing.append(rel)
        continue
    s = open(p, encoding="utf-8").read()
    if old == new:
        continue
    if old in s:
        s = s.replace(old, new)
        open(p, "w", encoding="utf-8").write(s)
        applied += 1
        print("OK   " + rel + "  (" + why + ")")
    else:
        # the AdminLayout secondary line appears twice; retry with replace-all count
        print("SKIP " + rel + "  (pattern not found): " + old[:60])

print()
print("applied:", applied)
if missing:
    print("missing files:", missing)
