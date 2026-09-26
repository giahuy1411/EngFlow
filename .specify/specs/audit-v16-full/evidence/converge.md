# audit-v16-full — converge (spec ↔ implementation ↔ evidence)

| Requirement (spec) | Thực thi | Bằng chứng | Trạng thái |
|---|---|---|---|
| O1 mọi endpoint có probe | `api-sweep` C1–C15 + `deep-probe` | `api-sweep.json` 143/0; `deep-probe.json` 58/0; `endpoint-inventory.json` 121/137/135 | ✅ |
| O2 mọi route × role render đúng | `ui-sweep` | `ui-sweep.json` 0 guard/console/api/page/contrast/overflow | ✅ |
| O3 DB toàn vẹn | `_db-audit.sql` + parity | `db-integrity.md` 22 FK/0 orphan; parity khớp | ✅ |
| O4 design khớp prompt | `design-v2` ×2 | `design-conformance.md` 0/0/0/0 | ✅ |
| O5 perf before/after | `perf-probe` ×2 | `perf-before.json`, `perf-conclusion.md` | ✅ |
| O6 6 nhóm E2E | MCP + api-sweep | `e2e-core-features.md` | ✅ |
| O7 2 vòng | loop-until-dry | `round-2.md` | ✅ |
| O8 docs + demo doc | README/CLAUDE/AGENTS + demo | `docs-drift.md`; demo doc (code đã verify) | ✅ |
| O9 dọn rác | cleanup | `cleanup-manifest.md` | ✅ |
| R1 số đo phiên này | mọi artifact có log | — | ✅ |
| R2 probe thứ 2 mỗi finding | findings.md | — | ✅ |
| R3 bằng chứng runtime | API + MCP thật | — | ✅ |
| R4 DML an toàn | xoá theo ID + verify | — | ✅ |
| R5 probe tự dọn | assertClean | parity khớp, STUDY_DAYS=4 | ✅ |
| R6 design chỉ fix lỗi thật | 5 drift đo được | findings.md | ✅ |
| R7 perf chỉ fix win | 0 win → không fix | perf-conclusion.md | ✅ |
| R8 dọn rác + liệt kê | cleanup-manifest.md | — | ✅ |
| R9 docs khớp + demo kèm code | verified | — | ✅ |

**Kết luận:** mọi requirement có artifact tương ứng. Không có requirement nào thiếu bằng chứng.

## Ngoài phạm vi (giữ nguyên, có lý do)
- Webhook SePay chữ ký thật (real-money boundary).
- Migrate timezone UTC.
- GitHub issues (V3).
- Tối ưu perf (không có win — P5).
