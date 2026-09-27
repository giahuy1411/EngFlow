# audit-v18-full — analyze (cuối kỳ)

## Chất lượng bằng chứng

**Mạnh:**
- **Phase U là trọng tâm đúng yêu cầu:** 13 luồng chức năng đi bằng **CẢ 2 MCP engine** (chrome-devtools + Playwright),
  đối chiếu 2 engine ↔ nhau; 24 ảnh lưu `shots/mcp/<engine>/`. Mọi luồng trọng yếu khớp số API↔UI.
- **Số liệu đo thật, không chép:** baseline v18 = **537 backend / 194 frontend** (khác v17 520/192 — ghi rõ "phát hiện");
  admin stats 5/1470/43738/3 khớp DB; dictionary cold 19.8s → warm 86ms.
- **Tự bác bỏ 2 giả thuyết của chính mình:** (1) tưởng `study_days` không tăng khi submit là bug → thực ra payload
  key sai (`answer` vs `userAnswer`); (2) tưởng demo doc file:line lệch → thực ra `grep` của tôi khớp overload khác.
  → Đúng kỷ luật V4 "kiểm chứng, không suy đoán".
- **Review chéo đối kháng** tìm thêm 3 probe còn hardcode chromium + comment/marker cũ; tôi **đối chiếu lại code**,
  xác nhận đúng, rồi **mở rộng fix** (đóng cả lớp F-18-01).
- **Prompt flaw audit đo lại** (không chép): 4 lỗ hổng y hệt v17 + ghi rõ "thay font = verify" + optional gap.

**Yếu / giới hạn (ghi rõ, không giấu):**
- **Premium flow (U19) chỉ 1 engine** — bù bằng deep-probe + g6; không tô vẽ thành "2 engine".
- **Webhook SePay chữ ký THẬT** vẫn BLOCKED (biên real-money) — g6 chỉ chứng minh **logic** HMAC/replay.
- **AI model nhỏ** trả gloss tiếng Trung (N3) — hạn chế model, không fix trong vòng này.
- **`smallTargets=115`** là false positive (đã triage kỹ: inline link + label checkbox), không phải vi phạm.
- **Perf "không win"** → không tối ưu (P5). Không tạo việc giả.

## Rủi ro còn lại

- Từ điển ngoài ~20s cold (F-17-05): demo `hello` lần đầu có thể chậm → warm cache trước demo.
- AI phụ thuộc GPU 4GB: swap model ~5.7s; model nhỏ đôi khi trả nội dung lệch ngôn ngữ.
- Harness revision Chromium: đã đóng lớp (resolveChromium), nhưng nếu `playwright-core` đổi API cần kiểm lại.

## Đối chiếu spec ↔ plan ↔ tasks ↔ evidence

Mọi O/R có artifact (`converge.md`). Coverage: 121 annotation / 137 expanded / 135 distinct — api-sweep 145 probe;
6 nhóm chức năng demo: 29 CONFIRMED + 1 REFUTED(doc, đã sửa) + 1 OBSERVATION.

## Điểm khác biệt của vòng này

| | v17 | v18 |
|---|---|---|
| Trọng tâm | Phase U 22 luồng (chủ yếu 1 engine) | **Phase U 13 luồng × CẢ 2 engine** |
| Baseline | 520/192 | **537/194** |
| Finding | 11 | **1 harness FIXED + 1 doc + 1 rút lại** |
| Review chéo | bắt 2 defect | bắt 3 probe + 2 comment → mở rộng fix |
| Tự bác bỏ giả thuyết | 1 | **2** |
| Prompt | kiểm + sửa | kiểm lại + ghi "font = verify" |

## Kết luận

Vòng này **hoàn thành** yêu cầu: quét codebase+DB, chạy toàn bộ API↔UI, **kiểm thử thủ công MỌI chức năng qua
CẢ 2 MCP engine**, đi sâu 6 nhóm chức năng, đo perf, verify design + prompt, 2 vòng, dọn rác, cập nhật tài liệu,
báo cáo. **0 regression.** Điểm mạnh nhất: **kỷ luật tự bác bỏ** (2 lần) + **review chéo mở rộng fix**.
