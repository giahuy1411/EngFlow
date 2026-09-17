import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")
D = "C:/Users/ASUS/Documents/LAPTRINH/engflow/frontend/src/"
Q = chr(34)
A = chr(39)
NL = chr(10)

# 1) main.js: drop the eager hook import
p = D + "main.js"
s = open(p, encoding="utf-8").read()
needle = "import " + A + "./utils/sanitize-a11y" + A + NL
print("main had import:", needle in s)
s = s.replace(needle, "")
open(p, "w", encoding="utf-8", newline="").write(s)
print("main clean:", "sanitize-a11y" not in open(p, encoding="utf-8").read())

# 2) utils/markdown.js: register the hook here (already imports DOMPurify)
p = D + "utils/markdown.js"
s = open(p, encoding="utf-8").read()
if "sanitize-a11y" not in s:
    old = "import DOMPurify from " + A + "dompurify" + A
    s = s.replace(old, old + NL + "import " + A + "./sanitize-a11y" + A, 1)
    open(p, "w", encoding="utf-8", newline="").write(s)
print("markdown hook:", "sanitize-a11y" in open(p, encoding="utf-8").read())

# 3) views/lessons/LessonContent.vue: same, it sanitizes on its own
p = D + "views/lessons/LessonContent.vue"
s = open(p, encoding="utf-8").read()
if "sanitize-a11y" not in s:
    old = "import DOMPurify from " + A + "dompurify" + A
    assert old in s, "LessonContent import anchor missing"
    s = s.replace(old, old + NL + "import " + A + "../../utils/sanitize-a11y" + A, 1)
    open(p, "w", encoding="utf-8", newline="").write(s)
print("LessonContent hook:", "sanitize-a11y" in open(p, encoding="utf-8").read())
