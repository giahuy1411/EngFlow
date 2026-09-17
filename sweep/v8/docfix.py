# -*- coding: utf-8 -*-
import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")

p = "AGENTS.md"
L = open(p, encoding="utf-8").read().split("\n")
new_bullet = ("- **Cloudinary keys THAT đã có trong `.env`** (`CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET`, "
  "verify lại 2026-09-15): `POST /api/auth/avatar/upload` và `POST /api/admin/audio-upload` trả **200 + URL "
  "Cloudinary thật** khi upload file media THẬT (PNG 74 B, WAV 563 KB RIFF/WAVE). Chỉ khi dùng bytes giả thì "
  "mới nhận 400 `Unsupported video format or file` (Cloudinary từ chối nội dung, không phải thiếu key). "
  "audit-v8 từng kết luận SAI `demo creds` chỉ vì fixture giả — đừng lặp lại: verify đường upload phải dùng file thật.")
hit = 0
for i, l in enumerate(L):
    if l.startswith("- Cloudinary") and "demo" in l:
        L[i] = new_bullet; hit += 1
assert hit == 1, hit
open(p, "w", encoding="utf-8", newline="").write("\n".join(L))
print("AGENTS.md bullet replaced")
