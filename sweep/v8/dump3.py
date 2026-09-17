import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")
R = r"C:\Users\ASUS\Documents\LAPTRINH\engflow"

def show(path, needle, ctx=0):
    L = open(R + path, encoding="utf-8").read().split("\n")
    for i, l in enumerate(L, 1):
        if needle in l:
            for k in range(max(1, i - ctx), min(len(L), i + ctx) + 1):
                print("%s:%d| %s" % (path, k, L[k - 1]))
            print("   ---")

show("/AGENTS.md", "Cloudinary keys")
show("/.specify/specs/audit-v8-full/REPORT.md", "Cloudinary")
show("/.specify/specs/audit-v8-full/tasks.md", "T26")
