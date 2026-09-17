import os, re
D = 'src/main/java/com/datn/engflow/model/dto'
AT = chr(64)
out = []
for r, d, fs in os.walk(D):
    for f in sorted(fs):
        if not f.endswith('.java'):
            continue
        s = open(os.path.join(r, f), encoding='utf-8', errors='replace').read()
        flds = re.findall(r'^\s*private\s+([\w<>,\. \[\]]+?)\s+(\w+)\s*;', s, re.M)
        ann = re.findall(AT + r'(?:Not\w+|NotBlank|NotNull|Size|Min|Max|Email|Pattern)', s)
        out.append(f + ' :: ' + ', '.join(t.strip() + ' ' + n for t, n in flds) + ' | ann=' + ','.join(sorted(set(ann))))
open('sweep/v8/dtos.txt', 'w', encoding='utf-8').write(chr(10).join(out))
print('files', len(out))
