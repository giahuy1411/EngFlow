import io, sys, glob
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p = sys.argv[1]
a, b = int(sys.argv[2]), int(sys.argv[3])
L = open(p, encoding="utf-8", errors="replace").read().split(chr(10))
for i in range(a, b + 1):
    if 1 <= i <= len(L):
        print(i, L[i - 1])
