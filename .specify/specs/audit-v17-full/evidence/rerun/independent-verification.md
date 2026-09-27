# audit-v17-full (closing round) — kiểm chứng ĐỘC LẬP kết quả cuối

**Cách làm:** workflow `verify-closing-round` — **6 verifier độc lập** (mỗi mục C1–C6 một người, đọc file thật +
chạy lệnh thật, **không tin tài liệu**) + **1 completeness critic**. 7/7 agent xong, 0 lỗi.

## Verdict

| Mục | Verdict | Bằng chứng verifier tự đo |
|---|---|---|
| C1 Playwright MCP | **DONE** | `settings.json:10` có env; Brave tồn tại (4 468 816 B); plugin `.mcp.json` có `--executable-path`. Verifier **tự chạy lại** `browser_navigate → localhost:5173/login` **THÀNH CÔNG**, `browser_evaluate → {isBrave:true}` |
| C2 Tra từ = từ điển | **DONE** | `vocabularyService.js` chỉ gọi `/api/vocabulary/dictionary/` (dòng 98); `DICT_BUDGET_MS=6000` (142) + race (186) + `clearTimeout` trong finally (187-190); `VocabularyController` không còn `exact`; `findByWordIgnoreCase` đã gỡ; grep `/api/vocabulary/search` = 3 hit, **đều là comment/assert** |
| C3+C4 harness guard | **DONE** | `_config.js:38` default v17; `focused-probe.js:20` dùng `_config.OUT`; `assert-harness` chạy **ALL CLEAN exit 0**, check 6 so **vòng cao nhất** |
| C5 speaking người thật | **DONE** | Verifier **tự chạy lại probe**: `bytes=563524`, `status=COMPLETED`, `recall 0.97`, `G8 RESULT: PASS`, cleanup `SUB_ROWS=0`, gốc còn nguyên 563524 B; tự transcribe Whisper ra đúng referenceText |
| C6 SePay replay | **DONE** | `g6:133-141` ký stale HMAC, `:156` AND vào `ok`; `api-sweep.js:541` chỉ còn **comment**; `api-sweep.json` pass=145 fail=0 blocked=0 |
| **Suite numbers** | **BAN ĐẦU `done:false`** | Log "final" cũ **hơn** code C2 ⇒ số 520/192 chưa chứng nhận cây hiện tại; `9.39 s` **không log nào chứa** |

## Critic bắt 3 lỗi provenance THẬT (đã sửa)

1. **`9.39 s` không có artefact** (grep toàn repo = 0) → **gỡ**; build thật **5.81 s**.
2. **Log "final" cũ hơn code** (`backend-final.log` 23:25 vs Java 00:34; `frontend-final4.log` 23:47 vs service 01:04)
   → **chạy lại trên cây hiện tại**, lưu `backend-closing.log` / `frontend-closing.log` / `build-closing.log`.
3. **Số frontend** — verifier đoán "≥193"; đo lại thật: **192 pass + 1 skip = 193 total** ⇒ số "192" trong doc
   **đúng** (chỉ là nhãn "192/1 skip").

## Đo lại trên cây hiện tại (log lưu cạnh file này)

| Suite | Kết quả | Log (mtime 11:55 > mọi code edit ≤01:04) |
|---|---|---|
| Backend | **520 / 0 / 0 / 11** BUILD SUCCESS | `backend-closing.log` |
| Frontend | **192 pass / 1 skip** (32 file) | `frontend-closing.log` |
| Build | ✓ **5.81 s** | `build-closing.log` |
| parity | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` + `STUDY_DAYS=4 PENDING_PAYMENTS=0 EXERCISE_ATTEMPTS=33` | — |

## Còn lại (nói thật)

- **Chưa commit** — toàn bộ closing round nằm ở working tree (HEAD `e7a1182`).
- `api-sweep.json` chỉ lưu status (detail=null) ⇒ body phản hồi của C6 không persist trong artefact
  (nhưng đã in ra stdout lúc chạy: `"No pending order"` / `"Invalid signature"`).
- C5/C6 vẫn là **môi trường local** (clip người có sẵn; secret local) — đã disclose trong doc gốc.
