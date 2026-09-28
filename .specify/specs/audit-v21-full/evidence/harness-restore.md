# audit-v21-full — khôi phục / gia cố harness (nguồn + fix)

**Ngày:** 2026-09-28 (+07) · **Nguồn (source):** kế thừa `audit-v20-full/evidence/harness-restore.md`
+ suite tracked tại **`sweep/harness/`** · commit gốc `b539d11`.

## Nguồn (source) của bản harness này

- Suite audit nằm ở **`sweep/harness/`** và **được track** (`.gitignore`: `sweep/*` + `!sweep/harness/`).
  Không tạo `sweep/v21/` rồi xoá — đúng cơ chế L2 mà audit-v15 đã dựng.
- Namespace là **dữ liệu**, không phải code: `node sweep/harness/<probe>.js [--audit <name>] [--out <dir>]`
  qua `sweep/harness/_config.js`. Vòng này chạy với `--audit audit-v21-full`.
- Output runtime (`*.json`, `*.log`, `shots/`, `_*.sql`) vẫn **ignored** — không commit kết quả.

## Fix đã áp trong v21

| Mã | Fix | File | Bằng chứng |
|---|---|---|---|
| **F-21-01** | Default audit round `audit-v20-full → audit-v21-full` ở **CẢ HAI** nơi (bài học F-20-02b: `api-sweep.js` có default thứ hai dạng tên vòng trần, check 7 không bắt). | `sweep/harness/_config.js`, `sweep/v12/api-sweep.js` | `assert-harness.js` check 6 + 8: FAIL trước → PASS sau |
| **F-21-02** | File này (`harness-restore.md`) — bắt buộc bởi `assert-harness.js` check 5. | `.specify/specs/audit-v21-full/evidence/harness-restore.md` | check 5 PASS |

## Cách chạy

```bash
# mặc định = audit-v21-full (default đã cập nhật ở vòng này)
node sweep/harness/assert-harness.js
node sweep/harness/ui-sweep.js    --audit audit-v21-full --out .specify/specs/audit-v21-full/evidence
node sweep/harness/deep-probe.js  --audit audit-v21-full --out .specify/specs/audit-v21-full/evidence
node sweep/v12/api-sweep.js       --audit audit-v21-full --out .specify/specs/audit-v21-full/evidence
python sweep/harness/g6-sepay-signed.py
python sweep/harness/g8-speaking-human-audio.py
python sweep/harness/l2-negative-cache-proof.py
```

## Guard chống tái diễn

`assert-harness.js` giữ **8 check**: route tồn tại · `require()` resolve · không baseline cứng · route
write-on-mount phải cleanup · **file này non-empty có mục Nguồn/Fix** · **default `_config.js` trỏ
vòng MỚI NHẤT có thật** · **không harness nào hardcode path audit** · **không harness nào default
`--audit` về vòng cũ**.

> Nhắc lại bài học: sửa default vòng sau phải sửa **CẢ HAI** file (`_config.js` + `sweep/v12/api-sweep.js`).
> Check 8 (thêm ở v20) là lưới bắt lớp này; check 6 chỉ soi `_config.js`.
