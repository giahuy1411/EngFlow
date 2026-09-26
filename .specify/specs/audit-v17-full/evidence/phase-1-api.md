# audit-v17-full — Phase 1: API sweep

**Chạy phiên này** (`--audit audit-v17-full`), đo từ run log.

## Kết quả

| Probe | Kết quả | Artifact |
|---|---|---|
| `sweep/v12/api-sweep.js` | **pass=143 fail=0 blocked=1 n_a=2** | `api-sweep.json`, `api-sweep.log` |
| `sweep/harness/deep-probe.js` | **pass=58 fail=0 blocked=0 n_a=0** | `deep-probe.json` |
| `sweep/harness/search-sort.js` | 0 finding mới | `search-sort.log` |

### api-sweep theo nhóm
auth 13 · lessons 18 · streak 5 · search 8 · crud 23 · game 13 · flashcard 7 · srs 6 · misc 6 ·
submission 6 · speaking 9 · video 7 · admin 10 · ai 5 · payment 7 (+1 blocked).

- **blocked=1:** webhook SePay chữ ký HMAC hợp lệ — biên real-money, không lặp (v11 đã chứng minh + dọn).
- **n_a=2:** upload cần media thật.
- **Cleanup:** `AUDIT_PAY=0 AUDIT_STUDY_DAYS=0 …` — 0 residue; `Msg` scan 0 lỗi.

### deep-probe theo nhóm
mc-guard 18 · sort 2 · contract 13 · roles 25. `DEEP_SD_REMAINING=0` (F-16-01 giữ nguyên fix).

## Quan sát mới (không phải finding)

- **`GET /api/vocabulary?sort=word` CÓ tôn trọng `sort`** (asc≠desc → `false`); trong khi lessons/decks/
  admin-exercises **bỏ qua** `sort` (asc==desc==no-sort → `true`). Đây là **xác nhận** F-13-11 (lessons/decks
  cố định thứ tự) + bổ sung: **vocab thì sort được**. Không phải lỗi.
- Case-insensitive ✅; `<2` ký tự → `[]` ✅; `sort=notacolumn` → **400** ✅.

## Parity sau Phase 1
`1470|43738|5|118|29|4|3|12|10` — **khớp**, không residue.
