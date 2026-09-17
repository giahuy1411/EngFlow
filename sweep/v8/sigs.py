import os, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
D = 'src/main/java/com/datn/engflow/controller'
pat = re.compile(r'@(Get|Post|Put|Patch|Delete)Mapping(.{0,200}?)\r?\n\s*public\s+[\w<>?,. \[\]]+\s+(\w+)\s*\(([^{]*?)\)', re.S)
total = 0
for f in sorted(os.listdir(D)):
    if not f.endswith('.java'): continue
    s = open(os.path.join(D, f), encoding='utf-8', errors='replace').read()
    print('====', f)
    for m in pat.finditer(s):
        verb = m.group(1).upper()
        path = ' '.join(m.group(2).split())
        name = m.group(3)
        params = ' '.join(m.group(4).split())
        total += 1
        print('  ', verb, path, '|', name, '(', params, ')')
print('TOTAL', total)
