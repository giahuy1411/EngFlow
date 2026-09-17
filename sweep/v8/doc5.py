import io, sys
p = ".specify/specs/audit-v8-full/REPORT.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
anchor = "\n---\n\n## 2."
extra = """
- **V\u00f2ng 3 l\u1ea7n 2 (2026-09-16, sau khi \u0111\u00e3 rebuild container)** \u2014 \u0111\u1ecdt n\u00e0y **c\u00f3 s\u1eeda `src/main` + `frontend/src`** (kh\u00e1c v\u00f2ng 3 l\u1ea7n 1):
  - ch\u1ea1y l\u1ea1i **to\u00e0n b\u1ed9** P1\u2013P5 tr\u00ean b\u1ea3n d\u1ef1ng cu\u1ed1i: P1 124/124, P2 61/61, P3a 36/36 (generate-all 86 s), P3b 19/19, P4a 18/18, P4b 16/16, P4c 10/10, P4d 6/6, P4e 6/6 (+1 case ri\u00eang), burst (\u0111o th\u00f4 `hist`), **0 fail**;
  - ph\u00e1t hi\u1ec7n + fix **F88** (admin kh\u00f4ng l\u01b0u \u0111\u01b0\u1ee3c b\u00e0i h\u1ecdc video \u2014 3 t\u1ea7ng) v\u00e0 **F89** (b\u00e0i NH\u00c1P `is_published=false` l\u1ed9 c\u00f4ng khai qua 4 endpoint detail/list con);
  - v\u00f2ng 3 l\u1ea7n 2 c\u0169ng x\u00e1c minh `openrouter.*` **kh\u00f4ng ph\u1ea3i cloud**: 1 call `ai-generate` sinh \u0111\u00fang **1 d\u00f2ng m\u1edbi** trong `%LOCALAPPDATA%\\Ollama\\server.log` (`POST /v1/chat/completions` 02:07:16, 5.24 s \u2248 latency 5.3 s c\u1ee7a API) \u2014 xem \u00a73.12;
  - d\u1ecdn d\u1eb9p xong: parity v\u1ec1 **\u0111\u00fang** baseline, `mc rm` 6 object MinIO m\u1ed3 c\u00f4i, xo\u00e1 25 row payment probe.
"""
if anchor not in t:
    print("ANCHOR NOT FOUND"); sys.exit(1)
t = t.replace(anchor, extra + anchor, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("ok")
