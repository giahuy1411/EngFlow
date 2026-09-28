# audit-v21-full — Sự cố git + khôi phục (2026-09-28)

**Mức:** HIGH (nguy cơ mất lịch sử) · **Trạng thái:** ĐÃ KHÔI PHỤC, 0 dữ liệu mất

## Triệu chứng

Sau khi commit `f9e5fcc` (xoá 2 method dead-code), phiên tiếp theo phát hiện:

```
$ git status --porcelain
A  .agents/AGENTS.md
A  .agents/skills/...
A  .specify/specs/audit-*/...
... (~1884 dòng, TOÀN BỘ repo hiện là file mới "A")

$ git rev-parse HEAD
fatal: ambiguous argument 'HEAD': unknown revision
$ git branch --show-current
fatal: failed to resolve HEAD as a valid ref
$ git log
fatal: your current branch appears to be broken
```

## Chẩn đoán (đo, không đoán)

| Kiểm tra | Kết quả |
|---|---|
| `.git/HEAD` | **Hợp lệ**: `ref: refs/heads/audit-v15-full` |
| `.git/refs/heads/audit-v15-full` | **HỎNG** — chứa toàn ký tự trắng, KHÔNG có SHA |
| `.git/refs/heads/main` | Bình thường (`6f046b6...`) |
| `.git/packed-refs` | Bình thường |
| `.git/index.stash.23172.lock`, `.git/index.stash.4796.lock` | **Còn sót** — dấu hiệu một thao tác git bị ngắt giữa chừng |
| `git cat-file -t f9e5fcc` | **`commit`** — object còn nguyên |
| `.git/logs/refs/heads/audit-v15-full` (reflog) | **Nguyên vẹn**, dòng cuối = `f9e5fcc` |

**Nguyên nhân:** một thao tác git (có dấu hiệu `stash`) bị ngắt giữa chừng → file ref của nhánh bị ghi đè bằng nội dung rỗng. **Không mất object nào** — chỉ mất con trỏ nhánh.

## Khôi phục

1. **Backup `.git` trước khi sửa**: `cp -r .git ~/engflow-git-backup-<ts>` (an toàn tuyệt đối).
2. Xoá lock files còn sót: `rm -f .git/index.stash.*.lock`.
3. Ghi lại ref đúng SHA lấy từ **reflog** (nguồn sự thật):
   `printf 'f9e5fccdf88245ecc8a58c97ec069c9885ff4503\n' > .git/refs/heads/audit-v15-full`

## Kiểm chứng sau khôi phục

| Kiểm tra | Kết quả |
|---|---|
| `git rev-parse HEAD` | `f9e5fcc...` ✓ |
| `git branch --show-current` | `audit-v15-full` ✓ |
| `git log --oneline -3` | 3 commit v21 hiện đúng ✓ |
| `git rev-list --count HEAD` | **221 commit** ✓ |
| `git status --porcelain` | **rỗng** (working tree khớp `f9e5fcc`) ✓ |
| `git fsck` | chỉ **dangling commit** (bình thường), **0 missing/corrupt** ✓ |
| `mvnw test` | **541 / 0 / 0 / 11 BUILD SUCCESS** ✓ |
| `vitest run` | **194 passed / 1 skipped (32 files)** ✓ |
| `assert-harness.js` | **ALL CLEAN** ✓ |
| 8 container + API | login 200, lessons 200, frontend 200 ✓ |

## Phòng ngừa cho vòng sau

- **Không dùng `git stash`** trong repo này khi chưa commit — nguyên nhân nghi vấn. Nếu buộc dùng, kiểm `git status` ngay sau.
- **Backup `.git` trước mọi thao tác git rủi ro** (đã làm ở đây).
- Reflog là nguồn khôi phục đáng tin: `git reflog` / `.git/logs/refs/heads/<branch>` giữ đủ SHA kể cả khi ref hỏng.
- Nếu gặp lại: `git fsck` để xác nhận object còn, rồi ghi lại ref từ reflog — KHÔNG `git init` lại (sẽ mất lịch sử).
