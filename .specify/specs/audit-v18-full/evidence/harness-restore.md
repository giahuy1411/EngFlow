# audit-v18-full — khôi phục / gia cố harness (nguồn + fix)

**Ngày:** 2026-09-27 (+07) · **Nguồn:** kế thừa `audit-v17-full/evidence/harness-restore.md` +
`audit-v17-full` (commit `c76cc1c`) · suite tracked tại **`sweep/harness/`**.

## Nguồn (source) của bản harness này

- Suite audit nằm ở **`sweep/harness/`** và **được track** (`.gitignore`: `sweep/*` + `!sweep/harness/`).
  Không tạo `sweep/v18/` gitignored rồi xoá — đúng cơ chế L2 mà v15 đã dựng.
- Namespace là **dữ liệu**, không phải code: `node sweep/harness/<probe>.js [--audit <name>] [--out <dir>]`
  qua `sweep/harness/_config.js`. Vòng này chạy với `--audit audit-v18-full` (default đã đổi ở 2 chỗ:
  `_config.js` + `sweep/v12/api-sweep.js`).
- Output runtime (`*.json`, `*.log`, `shots/`, `_*.sql`) vẫn **ignored** — không commit kết quả.

## Fix đã áp trong v18

| Nguồn | Fix | File |
|---|---|---|
| T0.4 | default audit round → `audit-v18-full` ở **cả hai** nơi khai báo | `sweep/harness/_config.js`, `sweep/v12/api-sweep.js` |
| (kế thừa v17) | `cleanupExerciseAttempts`, `isThirdPartyConsoleNoise`, `shotName()` từ `VER`, `EXERCISE_ATTEMPTS_BASELINE` scoped 2 tài khoản probe | `sweep/v8/ui/lib.js`, `p16-parity.sql`, `ui-sweep.js` |
| (kế thừa v17) | G6/G7/G8/L2 bọc `try/finally`, tự dọn `study_days`/MinIO/`is_premium` | `g6-*.py`, `g7-*.py`, `g8-*.py`, `l2-*.py` |

## Cách chạy

```bash
# mặc định = audit-v18-full (default đã cập nhật)
node sweep/harness/assert-harness.js
node sweep/harness/ui-sweep.js --audit audit-v18-full --out .specify/specs/audit-v18-full/evidence
node sweep/harness/deep-probe.js --audit audit-v18-full --out .specify/specs/audit-v18-full/evidence
node sweep/v12/api-sweep.js   --audit audit-v18-full --out .specify/specs/audit-v18-full/evidence
python sweep/harness/g6-sepay-signed.py
python sweep/harness/g8-speaking-human-audio.py
```

## Guard chống tái diễn

`assert-harness.js` giữ 7 check: route tồn tại · `require()` resolve · không baseline cứng · route
write-on-mount phải cleanup · **file này non-empty có mục Nguồn/Fix** · **default `_config.js` trỏ
vòng MỚI NHẤT có thật** · **không harness nào hardcode path audit** (đóng cả lớp F-17-13/16/17).
