# audit-v20-full — khôi phục / gia cố harness (nguồn + fix)

**Ngày:** 2026-09-28 (+07) · **Nguồn (source):** kế thừa `audit-v19-full/evidence/harness-restore.md`
+ suite tracked tại **`sweep/harness/`** · commit gốc `d08dba5`.

## Nguồn (source) của bản harness này

- Suite audit nằm ở **`sweep/harness/`** và **được track** (`.gitignore`: `sweep/*` + `!sweep/harness/`).
  Không tạo `sweep/v20/` rồi xoá — đúng cơ chế L2 mà audit-v15 đã dựng.
- Namespace là **dữ liệu**, không phải code: `node sweep/harness/<probe>.js [--audit <name>] [--out <dir>]`
  qua `sweep/harness/_config.js`. Vòng này chạy với `--audit audit-v20-full`.
- Output runtime (`*.json`, `*.log`, `shots/`, `_*.sql`) vẫn **ignored** — không commit kết quả.

## Fix đã áp trong v20

| Mã | Fix | File | Bằng chứng |
|---|---|---|---|
| **F-20-01** | Baseline parity `payments 12 → 13`. Row thêm là **giao dịch SePay THẬT** (`ENG73E2D3AA2DF6`, giữ theo V9), KHÔNG phải rác — `PENDING_PAYMENTS=0`, `STUDY_DAYS=4` vẫn khớp. | `sweep/v8/ui/lib.js` (`PARITY_BASELINE`, `PAYMENTS_BASELINE`) | `evidence/baseline-runtime.md` (parity đo thật) |
| **F-20-02** | Default audit round `audit-v19-full → audit-v20-full` | `sweep/harness/_config.js` | `assert-harness.js` check 6: FAIL trước → PASS sau |
| **F-20-02b** | **Guard hole:** `sweep/v12/api-sweep.js:41` vẫn `arg("audit","audit-v19-full")` — check 7 chỉ soi literal `.specify/specs/audit-vN-full`, KHÔNG soi default arg → lọt. Đã sửa default **và** thêm **check 8** bắt đúng lớp này. | `sweep/v12/api-sweep.js`, `sweep/harness/assert-harness.js` | check 8 PASS |
| **F-20-03** | File này (harness-restore.md) — bắt buộc bởi `assert-harness.js` check 5 | `.specify/specs/audit-v20-full/evidence/harness-restore.md` | check 5 PASS |

## Cách chạy

```bash
# mặc định = audit-v20-full (default đã cập nhật ở vòng này)
node sweep/harness/assert-harness.js
node sweep/harness/ui-sweep.js    --audit audit-v20-full --out .specify/specs/audit-v20-full/evidence
node sweep/harness/deep-probe.js  --audit audit-v20-full --out .specify/specs/audit-v20-full/evidence
node sweep/v12/api-sweep.js       --audit audit-v20-full --out .specify/specs/audit-v20-full/evidence
python sweep/harness/g6-sepay-signed.py
python sweep/harness/g8-speaking-human-audio.py
python sweep/harness/l2-negative-cache-proof.py
```

## Guard chống tái diễn

`assert-harness.js` giữ **8 check**: route tồn tại · `require()` resolve · không baseline cứng · route
write-on-mount phải cleanup · **file này non-empty có mục Nguồn/Fix** · **default `_config.js` trỏ
vòng MỚI NHẤT có thật** · **không harness nào hardcode path audit** · **không harness nào default
`--audit` về vòng cũ** (mới ở v20).

> Ghi chú vòng này: `sweep/v12/api-sweep.js` khai báo default thứ hai. v19 từng sửa **cả hai** nơi
> (`_config.js` + file này) nhưng v20 ban đầu chỉ sửa `_config.js` → nơi kia lệch lại và **không**
> bị check 7 bắt (vì nó là tên vòng trần, không phải literal path). Đã sửa default + thêm check 8.
