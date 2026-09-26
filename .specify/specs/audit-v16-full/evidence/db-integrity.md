# audit-v16-full — Phase 2: DB integrity

**Nguồn:** `.specify/specs/audit-v16-full/evidence/_db-audit.sql` → `db-audit-raw.txt` (đo 2026-09-26, DB `english_learning`).

## T2.2 — Orphan check (mọi FK)

Script tự sinh một truy vấn `NOT EXISTS` cho **mọi** FK đơn-cột trong `sys.foreign_keys`.

| Kết quả | Giá trị |
|---|---|
| Số FK kiểm tra | **22** |
| Tổng orphan | **0** |

→ **0 orphan** trên toàn bộ 22 FK. Khớp kết luận v15 (đã tái kiểm chứng, không chép).

## T2.3 — FK index coverage

Truy vấn: mọi FK column **không** có index với `key_ordinal = 1` (leading column).

| Kết quả | Giá trị |
|---|---|
| FK column thiếu leading index | **0** |

→ Mọi FK column đều được index dẫn đầu. Không có "win" tạo index ở đây.

## T2.3b — Đếm

| Chỉ số | Giá trị |
|---|---|
| `sys.foreign_keys` | **22** |
| `sys.tables` | **19** (18 bảng thật + `sysdiagrams`) |

## T2.1 — Parity

`1470|43738|5|118|29|4|3|12|10` — khớp baseline; `STUDY_DAYS=4`, `PENDING_PAYMENTS=0`.

## Kết luận

DB toàn vẹn: **0 orphan, 22 FK đều có index, parity khớp**. Không phát hiện vấn đề mới ở tầng toàn vẹn.
