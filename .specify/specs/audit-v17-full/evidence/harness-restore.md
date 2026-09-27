# audit-v17-full — khôi phục / gia cố harness (nguồn + fix)

**Ngày:** 2026-09-27 (+07) · **Nguồn:** kế thừa `audit-v15-full/evidence/harness-restore.md` +
`audit-v16-full` (commit `baaa61f`) · suite tracked tại **`sweep/harness/`**.

## Nguồn (source) của bản harness này

- Suite audit vẫn nằm ở **`sweep/harness/`** và **được track** (`.gitignore`: `sweep/*` +
  `!sweep/harness/`). Không tạo `sweep/v17/` gitignored rồi xoá — đúng cơ chế L2 mà v15 đã dựng.
- Namespace là **dữ liệu**, không phải code: `node sweep/harness/<probe>.js [--audit <name>] [--out <dir>]`
  qua `sweep/harness/_config.js`. Vòng này chạy với `--audit audit-v17-full`.
- Output runtime (`*.json`, `*.log`, `shots/`, `_*.sql`) vẫn **ignored** — không commit kết quả.

## Fix đã áp trong v17 (đã commit) + đóng nốt ở vòng closing

| Nguồn | Fix | File |
|---|---|---|
| F-17-06/18 | `isThirdPartyConsoleNoise()` dùng **chung** (nâng từ bản sao inline); regex chỉ origin/noise đã verify | `sweep/v8/ui/lib.js`, `ui-sweep.js` |
| F-17-07 | `cleanupExerciseAttempts(from)` + marker `EXERCISE_ATTEMPTS=` trong parity | `sweep/v8/ui/lib.js`, `p16-parity.sql` |
| F-17-13 | tên ảnh lấy từ `VER` (`shotName()`), không hardcode `v13-*` | `ui-sweep.js` |
| F-17-14 | probe nộp speaking phải dọn `study_days` (assess ghi 1 hàng) | `g7-speaking-real-audio.py` |
| F-17-15 | đọc `media_object_key` từ DB **trước khi** xoá row để xoá đúng object MinIO | `g7-speaking-real-audio.py` |
| F-17-21 | `EXERCISE_ATTEMPTS_BASELINE` scoped **2 tài khoản probe**, không cả bảng | `sweep/v8/ui/lib.js` |
| F-17-22 | G6 hoàn nguyên `is_premium`/`premium_expiry` trong `finally` | `g6-sepay-signed.py` |
| F-17-23 | G6 negative control **assert** (chữ ký sai bị từ chối), không chỉ in | `g6-sepay-signed.py` |
| F-17-24 | G7 dọn `study_days` theo **ngày VN** (`time.gmtime(now+7h)`), không `GETDATE()` UTC | `g7-speaking-real-audio.py` |
| F-17-25 | mọi probe bọc `try/finally`; verdict gồm cả kết quả cleanup | `g6-*.py`, `g7-*.py` |
| F-17-16 | `_config.js` default → `audit-v17-full`; `assert-harness` fail nếu default trỏ vòng không tồn tại | `_config.js`, `assert-harness.js` |
| F-17-17 | `focused-probe.js` dùng `_config.js.OUT` thay hardcode `audit-v15-full`; `assert-harness` cấm literal audit path | `focused-probe.js`, `assert-harness.js` |

## Cách chạy

```bash
# mặc định = audit-v17-full (default đã cập nhật)
node sweep/harness/assert-harness.js
node sweep/harness/ui-sweep.js --audit audit-v17-full --out .specify/specs/audit-v17-full/evidence/rerun
node sweep/harness/deep-probe.js --audit audit-v17-full --out …/rerun
node sweep/v12/api-sweep.js   --audit audit-v17-full --out …/rerun
python sweep/harness/g6-sepay-signed.py
python sweep/harness/g8-speaking-human-audio.py
```

## Guard chống tái diễn

`assert-harness.js` giữ 7 check: route tồn tại · `require()` resolve · không baseline cứng · route
write-on-mount phải cleanup · **file này non-empty có mục Nguồn/Fix** · **default `_config.js` trỏ
vòng có thật** · **không harness nào hardcode path audit** (đóng cả lớp F-17-13/16/17).
