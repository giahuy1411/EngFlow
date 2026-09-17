import io, sys, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p, pat = sys.argv[1], sys.argv[2]
L = open(p, encoding="utf-8", errors="replace").read().split(chr(10))
rx = re.compile(pat)
hits = [i for i, l in enumerate(L, 1) if rx.search(l)]
for i in hits:
    print("----", i)
    for k in range(max(1, i - 2), min(len(L), i + 14) + 1):
        print(k, L[k - 1])
