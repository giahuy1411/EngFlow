#!/usr/bin/env python3
"""
doc_citation_remap.py — map lại trích dẫn dòng trong docs/demo-engflow-4-chuc-nang.md

VÌ SAO CẦN (audit-v20 F-20-12)
------------------------------
`docs/demo-engflow-4-chuc-nang.md` trích code bằng `File.java:start-end` ở 167 chỗ để
hội đồng đối chiếu khi demo. Mỗi lần thêm/bớt dòng trong source — ví dụ đợt bổ sung
Javadoc tiếng Việt ở audit-v20 — các số dòng đó LỆCH và tài liệu chỉ sai chỗ, nhưng vẫn
"trông đúng" nên rất dễ bỏ qua cho tới lúc demo.

Đo được ở audit-v20: thêm comment làm **121/167** trích dẫn trỏ sai.

CÁCH HOẠT ĐỘNG (neo theo NỘI DUNG, không theo số dòng)
-----------------------------------------------------
Mỗi trích dẫn thường có một khối code in ngay sau trong tài liệu. Lấy dòng đầu và dòng
cuối (đã chuẩn hoá khoảng trắng) của khối đó làm MỎ NEO, tìm vị trí DUY NHẤT của chúng
trong file nguồn hiện tại, rồi suy ra khoảng dòng mới.

An toàn:
  * Mỏ neo không duy nhất  -> GIỮ NGUYÊN và báo (không ghi số đoán mò).
  * Khoảng dòng hiện tại ĐÃ đúng -> giữ nguyên (chốt idempotency; chạy lại không hỏng).
  * Trích dẫn văn xuôi (không kèm khối code) -> fallback map dòng HEAD->worktree.

DÙNG:
  python sweep/harness/doc_citation_remap.py            # dry-run, in báo cáo
  python sweep/harness/doc_citation_remap.py --apply    # ghi vào file

LƯU Ý: chạy SAU khi source đã chốt. Sau khi apply, HAND-VERIFY vài trích dẫn trước khi demo.

CHỐNG CHẠY LẶP (idempotency)
----------------------------
Bản thân một bộ map dòng không thể vừa đúng trên tài liệu cũ vừa tự biết "đã map rồi".
Nên tool ghi một DẤU vào tài liệu sau mỗi lần apply; chạy lần 2 sẽ TỪ CHỐI trừ khi
truyền thêm `--force`. Điều này chặn đúng lỗi nguy hiểm nhất: dịch thêm lần nữa và làm
hỏng tài liệu vốn đã đúng.
"""
import difflib
import os
import re
import subprocess
import sys

# Windows console mặc định cp1252 -> in tiếng Việt sẽ crash. Ép UTF-8.
try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

DOC = 'docs/demo-engflow-4-chuc-nang.md'
ROOTS = ['src/main/java', 'src/main/resources', 'src/test/java', 'frontend/src']
APPLY = '--apply' in sys.argv
FORCE = '--force' in sys.argv
MARKER = '<!-- doc-citation-remap:'
MARKER_FULL = '<!-- doc-citation-remap: đã map -->'

# Chống chạy lặp: nếu tài liệu đã mang dấu và không --force thì dừng.
if os.path.exists(DOC) and MARKER in open(DOC, encoding='utf-8').read() and not FORCE:
    print('Tài liệu đã được map (thấy dấu). Không làm gì.')
    print('Nếu source vừa đổi và bạn CHẮC muốn map lại: thêm --force')
    sys.exit(0)

# ── chỉ mục file nguồn: theo basename, path đầy đủ, và hậu tố ──
index = {}
for root in ROOTS:
    for dp, dns, fns in os.walk(root):
        dns[:] = [d for d in dns if d not in ('.git', 'node_modules', 'target', 'dist')]
        for fn in fns:
            full = os.path.join(dp, fn).replace(os.sep, '/')
            index.setdefault(fn, full)
            index[full] = full
            index[full.split('/', 1)[1] if '/' in full else full] = full

_cache = {}


def get(path):
    if path not in _cache:
        head = subprocess.run(['git', 'show', f'HEAD:{path}'], capture_output=True, text=True,
                              encoding='utf-8', errors='replace').stdout.splitlines()
        work = open(path, encoding='utf-8', errors='replace').read().splitlines()
        _cache[path] = (head, work)
    return _cache[path]


def norm(s):
    return re.sub(r'\s+', ' ', s).strip()


def find_unique(lines, text):
    t = norm(text)
    pos = [i + 1 for i, l in enumerate(lines) if norm(l) == t]
    return pos[0] if len(pos) == 1 else None


def line_map(head, work):
    """Map chỉ số dòng HEAD (0-based) -> worktree, qua các khối 'equal' của difflib."""
    sm = difflib.SequenceMatcher(None, head, work, autojunk=False)
    ops = sm.get_opcodes()
    m = {}
    for tag, i1, i2, j1, j2 in ops:
        if tag == 'equal':
            for k in range(i2 - i1):
                m[i1 + k] = j1 + k
    for i in range(len(head)):
        if i in m:
            continue
        for tag, i1, i2, j1, j2 in ops:
            if tag == 'equal' and i1 > i:
                m[i] = j1
                break
        else:
            for tag, i1, i2, j1, j2 in reversed(ops):
                if tag == 'equal' and i2 <= i:
                    m[i] = max(0, j2 - 1)
                    break
            else:
                m[i] = 0
    return m


lines = open(DOC, encoding='utf-8').read().splitlines()
cit_re = re.compile(r'`([A-Za-z0-9_./-]+\.(?:java|js|vue)):(\d+)(?:-(\d+))?`')

report = {'A': [], 'B': [], 'unchanged': [], 'skipped': []}
edits = []

for li, line in enumerate(lines):
    for m in cit_re.finditer(line):
        raw, a = m.group(1), int(m.group(2))
        b = int(m.group(3)) if m.group(3) else None
        rel = index.get(raw) or index.get(os.path.basename(raw))
        if not rel or not os.path.exists(rel):
            report['skipped'].append(f'{raw}:{a} (không tìm thấy file)')
            continue
        head, work = get(rel)
        na = nb = None
        how = 'A'

        # ── case A: có khối code ngay sau ──
        j = li + 1
        while j < len(lines) and not lines[j].lstrip().startswith('```') and j - li < 40:
            j += 1
        block = None
        if j < len(lines) and lines[j].lstrip().startswith('```'):
            k = j + 1
            block = []
            while k < len(lines) and not lines[k].lstrip().startswith('```'):
                block.append(lines[k]); k += 1
        if block:
            # Một số trích dẫn đứng trước khối MERMAID (`sequenceDiagram`, `graph TD`...),
            # không phải code nguồn. Neo vào đó sẽ trượt -> phải loại để rơi xuống case B.
            is_mermaid = bool(block) and re.match(
                r'^\s*(sequenceDiagram|graph|flowchart|classDiagram|stateDiagram|erDiagram|journey|gantt|pie|mindmap|timeline)',
                block[0])
            sig = [] if is_mermaid else [
                x for x in block if norm(x) and not norm(x).startswith(('//', '*', '/*', '#'))]
            if sig:
                f = find_unique(work, sig[0])
                l = find_unique(work, sig[-1])
                if f and l and f <= l:
                    na, nb = f, (l if b else None)

        # ── case B: văn xuôi -> map dòng ──
        if na is None and a <= len(head):
            how = 'B'
            lm = line_map(head, work)
            hi = (b or a) - 1
            if (a - 1) in lm and hi in lm and lm[hi] >= lm[a - 1]:
                na = lm[a - 1] + 1
                nb = (lm[hi] + 1) if b else None

        if na is None:
            report['skipped'].append(f'{raw}:{a}{"-" + str(b) if b else ""}')
            continue
        if na == a and (nb or a) == (b or a):
            report['unchanged'].append(raw)
            continue
        txt = '`' + raw + ':' + str(na) + (('-' + str(nb)) if nb else '') + '`'
        edits.append((li, m.start(), m.end(), txt))
        report[how].append(f'{raw}:{a}{"-" + str(b) if b else ""} -> {na}{"-" + str(nb) if nb else ""}')

out = lines[:]
for li, s, e, txt in sorted(edits, key=lambda x: (x[0], -x[1])):
    out[li] = out[li][:s] + txt + out[li][e:]

print(f"{'APPLY' if APPLY else 'DRY RUN'} — {DOC}")
print(f"  A (neo code block) : {len(report['A'])}")
print(f"  B (map dòng)       : {len(report['B'])}")
print(f"  giữ nguyên         : {len(report['unchanged'])}")
print(f"  bỏ qua             : {len(report['skipped'])}")
for x in report['skipped'][:20]:
    print('   [skip] ' + x)

# ── FAIL LOUDLY on citation forms this tool cannot rewrite ────────────────────
# audit-v20: a comma-list citation `File.java:179-183, 193-197, 213-217` slipped
# through silently because cit_re needs a closing backtick after ONE range. The
# doc then kept stale numbers and nobody noticed until a verifier read the file.
# Enumerate every `File.ext:` citation and check the tool actually consumed it.
all_cits = re.findall(r'`([A-Za-z0-9_./-]+\.(?:java|js|vue)):([^`]*)`', open(DOC, encoding='utf-8').read())
handled = set()
for li, line in enumerate(lines):
    for m in cit_re.finditer(line):
        handled.add((m.group(1), m.group(0)[1:-1].split(':', 1)[1]))
unhandled = []
for raw, spec in all_cits:
    # a comma-list (or any spec with extra ranges) is NOT handled by cit_re
    if ',' in spec or spec.count('-') > 1 or not re.fullmatch(r'\d+(?:-\d+)?', spec):
        unhandled.append(f'{raw}:{spec}')
if unhandled:
    print('\n*** UNHANDLED CITATION FORMS — this tool did NOT rewrite these: ***')
    for u in unhandled:
        print('   ! ' + u)
    print('   Fix these by hand, or extend cit_re, before trusting the remap.')
else:
    print('\n  all citation forms are handled by this tool')

if APPLY:
    body = '\n'.join(out) + '\n'
    if MARKER not in body:
        # chèn dấu ngay dưới dòng tiêu đề H1
        b = body.split('\n')
        b.insert(1, '\n' + MARKER_FULL)
        body = '\n'.join(b)
    open(DOC, 'w', encoding='utf-8').write(body)
    print('  -> đã ghi + đóng dấu. Hãy hand-verify vài trích dẫn trước khi demo.')
