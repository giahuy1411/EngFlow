# audit-v14-full — T0.3/T0.4/T0.5: khôi phục + đổi nhãn harness

**Ngày:** 2026-09-25 (+07) · **Nguồn:** `a627569:sweep/v13/*` (pre-purge tree)

## Vì sao khôi phục từ git

Commit `2590caf` ("chore: purge committed junk, local artifacts, rewrite .gitignore") đã xoá
`sweep/v13/` khỏi working tree. Kiểm chứng: `git diff a627569 6b2836f -- sweep/v13/` = **rỗng** →
bản ở `a627569` byte-identical với bản trước khi purge. Vậy khôi phục từ git là an toàn và không mất gì.

## 16 file đã khôi phục → `sweep/v14/`

```
_cleanup.sql  _deep-cleanup.sql  api-inventory.js  api-sweep.js  cls-before.json  cls-probe.js
danger-tint.js  deep-probe.js  f1302-a1-a11y.js  f1302-a1-blocks-live.js  f1320-api-redirect-live.js
focused-probe.js  perf-before.json  perf-probe.js  search-sort.js  ui-sweep.js
```

## sha256 khớp (spot-check 4 file chính)

| file | kết quả |
|---|---|
| api-sweep.js | MATCH |
| deep-probe.js | MATCH |
| ui-sweep.js | MATCH |
| perf-probe.js | MATCH |

## T0.4 — Đổi namespace + tham số hoá output

- `AUDIT-V13` → `AUDIT-V14` (mọi file .js/.sql)
- `audit-v13-full` → `audit-v14-full` (đường dẫn output)
- `sweep/v13/` → `sweep/v14/` (mọi tham chiếu chạy + đường dẫn `_cleanup.sql`)
- Xoá `cls-before.json` + `perf-before.json` khôi phục (là output cũ của v13, sẽ được tạo lại)
- **Sửa lớp lỗi F-13-21**: `api-sweep.js:554` v13 gọi `sweep/v13/_cleanup.sql` trong khi file nó ghi là
  `_cleanup.sql` cạnh nó → nay trỏ `sweep/v14/_cleanup.sql`.

**Bằng chứng pass:** `node sweep/v14/assert-namespace.js` → `PASS: 0 foreign namespace refs; all 8 writers labelled 'audit-v14-full'`

## T0.5 — Tham số hoá ngày cleanup (D8)

v13 hardcode `CONVERT(date, '2026-09-21')` trong batch cleanup payment → chạy ngày khác sẽ **rò rỉ**
row. Nay `api-sweep.js` tính `VN_RUN_DATE` từ đồng hồ host theo giờ VN (+07), overridable bằng
`VN_RUN_DATE=YYYY-MM-DD`; `_cleanup.sql` template dùng ngày đó.

**Bằng chứng pass:** `grep -rn "2026-09-21" sweep/v14/*.js` = 0 (chỉ còn comment giải thích D8 và
template `_cleanup.sql` dùng ngày hiện tại 2026-09-25).

## Thêm mới

- `sweep/v14/assert-namespace.js` — guard D4, fail nếu bất kỳ file nào trỏ namespace audit khác
  hoặc writer không mang nhãn `audit-v14-full`.
