# Analyze: audit-v5-full — vòng continue 2 (2026-09-03)

**Phạm vi:** spec.md + plan.md + tasks.md + REPORT.md (sau F17/F17b/F17c/F18/F19) + code thật.

## Bảng phát hiện

| ID | Category | Severity | Location | Summary | Recommendation |
|----|----------|----------|----------|---------|----------------|
| A1 | Coverage gap | HIGH | spec.md §3 "Out of scope: viết lại data seed" vs tasks.md (F17–F19 đã sửa data) | Spec nói "chỉ guard UI + ghi nhận" nhưng vòng continue đã sửa tận gốc 480+ dòng data. Phạm vi thực tế đã mở rộng hơn spec. | Cập nhật spec §3: data repair là in-scope cho các pattern có thể sửa máy được (label/type/answer-key), giữ out-of-scope phần nội dung học thuật. |
| A2 | Underspecification | HIGH | spec.md §4 DoD "[ ] Backend mvnw test xanh" | DoD chưa cập nhật sau khi thêm F17–F19: chưa có mục "0 dòng letter-answer bug", "MATCHING 100% valid-pipe". | Bổ sung 3 mục DoD đo được cho data quality. |
| A3 | Inconsistency | MEDIUM | tasks.md T4.1 "parsedOptions lọc placeholder" vs REPORT §4c "FILL_BLANK 9045→9112" | Số FILL_BLANK tăng do convert từ MATCHING — nhưng guard T4.1 vẫn lọc placeholder → dòng mới có options thật nên render nút bấm, nhất quán. Không phải bug, chỉ là drift số liệu giữa tasks và REPORT. | Ghi chú drift ở REPORT (đã có §4d); không sửa code. |
| A4 | Ambiguity | MEDIUM | REPORT §4c "8 dòng options A. ... đáp án A — MC hợp lệ, giữ nguyên vì chọn letter vẫn chấm đúng" | Claim này SAI: UI submit full option text ("A. news") không phải letter → đây chính là F19. | REPORT §4c đã được §4d sửa lại; giữ cả hai để audit trail. |
| A5 | Coverage gap | MEDIUM | User yêu cầu mới: TTS supertonic → Java | Chưa có spec/tasks cho tích hợp TTS Java (subagent đang khảo sát). | Append T8.x vào tasks.md sau khi có report feasibility. |
| A6 | Coverage gap | MEDIUM | User yêu cầu: CLS prod build | DoD "CLS đo được" mới đo dev-server. Chưa đo prod `vite preview`. | Task T8.4. |
| A7 | Coverage gap | HIGH | User yêu cầu: UI test toàn bộ chức năng 0 lỗi trước deploy | Vòng loop-test 1/2 đã quét; chưa có vòng cuối "zero-error gate" sau F17–F19 + trước deploy. | Task T8.5 (full UI test loop). |
| A8 | Duplication | LOW | exercises_bak_v5/c/d nhiều bảng backup | 4 bảng backup chiếm chỗ DB nhưng là audit trail có chủ đích. | Giữ đến sau deploy; dọn dẹp sau. |

## Coverage Summary (yêu cầu người dùng vòng này)

| Requirement | Has Task? | Task IDs | Notes |
|-------------|-----------|----------|-------|
| Fix 68 MATCHING rỗng | ✅ | (đã làm — F18) | Commit 4ab6589 |
| 81 LISTENING thiếu audio → TTS Java | ⏳ | T8.1–T8.3 (pending) | Chờ feasibility report |
| CLS prod build | ⏳ | T8.4 (pending) | vite preview |
| UI test toàn chức năng 0 lỗi → deploy gate | ⏳ | T8.5 (pending) | Loop cuối |
| SePay reconcile ENGF8AB9431CE85 | ❌ | — | Cần sao kê người dùng (không tự động được) |

## Constitution Alignment

- **P1 (baseline xanh):** backend 221/221 (không đổi code Java), frontend vitest + sweep 67/67 — PASS.
- **P3 (schema):** mọi data fix đều SQL trực tiếp + backup tables + verify container — PASS.
- **P4 (AI local):** TTS supertonic là local (không cloud key) — đúng nguyên tắc; tích hợp phải theo P4.
- **P5 (perf đo được):** CLS sẽ đo prod (T8.4) — hợp quy.
- **P8 (bằng chứng runtime):** F17–F19 verify qua grade API thật + sweep — PASS.

## Metrics

- Total requirements (vòng này): 5 · Covered: 3 · Pending: 2 (TTS, CLS/UI-gate đang chạy)
- Ambiguity: 1 (A4) · Duplication: 1 (A8) · Critical: 0

## Next Actions

1. Chờ TTS feasibility report → chốt option → doubt-driven review trước khi code (T8.1–T8.3).
2. Chạy T8.4 (CLS prod) song song — không phụ thuộc TTS.
3. Sau TTS + CLS: T8.5 full UI gate → báo deploy-ready.
4. A1/A2: cập nhật spec.md (DoD + scope) — sẽ làm cùng commit T8 cuối.
