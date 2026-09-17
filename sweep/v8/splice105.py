import io
import sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")
P = "C:/Users/ASUS/Documents/LAPTRINH/engflow"
rep = P + "/.specify/specs/audit-v8-full/REPORT.md"
new = open(P + "/sweep/v8/cloud_vi.txt", encoding="utf-8-sig").read().rstrip().split(chr(10))
print("new lines", len(new))
L = open(rep, encoding="utf-8-sig").read().split(chr(10))
start = [k for k, l in enumerate(L) if l.startswith("1. **Cloudinary")][0]
end = [k for k, l in enumerate(L) if l.startswith("2. **")][0]
print("span", start, end)
L[start:end] = new
open(rep, "w", encoding="utf-8", newline="").write(chr(10).join(L))
out = open(rep, encoding="utf-8-sig").read().split(chr(10))
for k in range(start, start + len(new) + 1):
    print(k + 1, out[k][:110].encode("ascii", "backslashreplace").decode())
