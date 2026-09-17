import io
import sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")

P = r"C:\Users\ASUS\Documents\LAPTRINH\engflow"


def read(p):
    return open(p, encoding="utf-8").read()


def write(p, s):
    open(p, "w", encoding="utf-8", newline="").write(s)


def replace_line(path, needle, newtext):
    L = read(path).split(chr(10))
    idx = [k for k, l in enumerate(L) if needle in l]
    print(path, "match", len(idx))
    assert len(idx) == 1, "needle " + needle
    L[idx[0]] = newtext
    write(path, chr(10).join(L))
    return idx[0]


rep = P + r"\.specify\specs\audit-v8-full\REPORT.md"
tasks = P + r"\.specify\specs\audit-v8-full\tasks.md"
ag = P + r"\AGENTS.md"

cloud = read(P + r"\sweep\v8\cloud_text.txt").rstrip().split(chr(10))
block = cloud[0] + chr(10) + cloud[1]

# REPORT item 1 in section 3 (starts with "1. **Cloudinary")
L = read(rep).split(chr(10))
start = [k for k, l in enumerate(L) if l.startswith("1. **Cloudinary")][0]
end = [k for k, l in enumerate(L) if l.startswith("2. **Kh")][0]
L[start:end] = [block]
write(rep, chr(10).join(L))
print("REPORT sec3 item1 replaced", start, end)

# REPORT P3b row: fix the wrong claim text
replace_line(rep, "| P3b (m",
    "| P3b (m\u1edbi) | multipart uploads (speaking/video/lesson/admin/audio-upload), **Whisper + 3b assess 14.6 s**, shadowing **ai-grade 6.8 s**, manual grade, SePay **create-order \u2192 QR**, avatar | **19/19** trong contract; 2 ph\u00e1t hi\u1ec7n (F83 bucket ch\u1ebft; l\u1ec7ch s\u1eeda k\u1ebft lu\u1eadn Cloudinary \u2014 xem \u00a73.1) |")

# tasks.md T26 no longer needed
replace_line(tasks, "- [ ] T26",
    "- [x] T26 X\u00f3a nghi v\u1ea5n Cloudinary: key **c\u00f3 th\u1eadt** trong `.env`, verify b\u1eb1ng file media th\u1eadt \u2192 avatar + audio-upload **200 + URL Cloudinary** (2026-09-15)")

# AGENTS.md stray ASCII
replace_line(ag, "Cloudinary keys THAT",
    "- **Cloudinary \u0111ang d\u00f9ng th\u1eadt, kh\u00f4ng ph\u1ea3i demo**: `.env` c\u00f3 `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET` (verify 2026-09-15). Upload b\u1eb1ng file media TH\u1eacT \u2192 `POST /api/auth/avatar/upload` v\u00e0 `POST /api/admin/audio-upload` tr\u1ea3 **200 + URL Cloudinary** (\u0111\u00e2y l\u00e0 \u0111\u01b0\u1eddng sinh listening audio c\u1ee7a MCP). Ch\u1ec9 khi d\u00f9ng **bytes gi\u1ea3** m\u1edbi b\u1ecb t\u1eeb ch\u1ed1i: audio-upload \u2192 400 `Unsupported video format or file`, avatar \u2192 500 (`AuthController` n\u00e9m `Exception` chung) \u2014 l\u1ec7ch th\u00f4ng \u0111i\u1ec7p, kh\u00f4ng m\u1ea5t \u0111\u0103ng d\u00f9ng. audit-v8 t\u1eebng k\u1ebft lu\u1eadn SAI `demo creds` v\u00ec fixture gi\u1ea3: \u0111\u1ec3 verify \u0111\u01b0\u1eddng upload, b\u1eafc bu\u1ed9c d\u00f9ng file th\u1eadt.")
