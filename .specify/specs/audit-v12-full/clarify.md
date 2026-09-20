# audit-v12-full — clarification log

**Method:** các điểm mơ hồ trong yêu cầu người dùng được chốt **trước** khi lập kế hoạch. Mỗi mục ghi câu hỏi,
câu trả lời, và hệ quả cho kế hoạch. Câu trả lời đến từ người dùng qua bảng hỏi có cấu trúc ở đầu phiên.

---

## C1 — Phạm vi: v12 thực sự làm gì?

**Mơ hồ.** Yêu cầu nói "quét toàn bộ codebase và CSDL", nhưng đã có audit v1→v11 và v11 (cùng ngày) báo xanh.
Quét lại theo nghĩa đen có nguy cơ **bịa ra phát hiện** cho việc đã xong.

**Lựa chọn đưa ra:**
- (a) bù khoảng trống v11 + vòng 2 rộng hơn;
- (b) **làm lại từ đầu, không giả định gì**;
- (c) chỉ verify design-system/font.

**Trả lời: (b) "làm lại từ đầu, toàn diện".**

**Hệ quả.** Báo cáo cũ hạ xuống thành **giả thuyết**. Mọi số trong báo cáo v12 do lệnh của phiên này sinh ra.
Trùng v11 = **xác nhận**, không phải nguồn; lệch = **phát hiện**. Spec nói thẳng font và design system **đã
triển khai** — audit mà "phát hiện" chúng là bịa.

## C2 — Khoảng trống coverage API (phát hiện của phiên này)

**Đo được.** Source: **131 endpoint / 26 controller**. `sweep/v11/api-sweep.js`: **~27 endpoint**. **12 controller
chưa từng được probe**: Flashcard, Game, Leaderboard, LessonSubmission, LessonSnapshot, Progress, SpeakingPrompt,
AdminExerciseSeed, LessonStructure, AdminAnswerBackfill, Dashboard, MediaProxy.

**Hệ quả.** v12 **phải** phủ hết, và inventory phải **sinh từ source** (không viết tay danh sách) để không tái
diễn khoảng trống. Đây là phần việc lõi mới của v12.

## C3 — Ghi DB: audit có được tạo/xoá dữ liệu?

**Trả lời: được, kèm backup, dọn trong cùng run, và re-assert parity.**

**Hệ quả.** Phase 2/4 được tạo row (payment order, submission, lesson/deck CRUD) — nhưng mọi run ghi phải
(a) dùng namespace audit, (b) dọn **trong cùng run**, (c) re-assert parity. Bước phá huỷ cần **ID liệt kê**, cấm
`LIKE` trần (từng xoá mất 4 user baseline).

## C4 — "Thay toàn bộ font thành Be Vietnam Pro" có phải việc thật?

**Giải quyết bằng đo, không giả định.** Việc **đã xong**: DOM live chỉ có **1** họ font `"Be Vietnam Pro"`;
`index.html` nạp duy nhất `Be+Vietnam+Pro:wght@400;500;600;700;800;900`; `tailwind.config.js` map sans/heading/mono
→ BVP; `design-system.css --geo-font` = BVP; **0** hit Outfit/Plus Jakarta trong `frontend/src`.

**Hệ quả.** v12 **không** thực hiện thay font. Nó **verify** và báo là đã đạt. Báo cáo thành việc mới = bịa.

## C5 — Dùng browser nào?

**Trả lời.** Cả hai MCP đã verify sống ở đầu phiên (chrome-devtools `list_pages` → page; Playwright `browser_tabs`
→ tab). v12 dùng **cả hai làm driver hạng nhất**, ghi rõ driver nào ra số nào, **không trộn** (engine khác nhau,
font rendering có thể khác).

## C6 — "Lỗ hổng của prompt" xử lý sao?

**Giải quyết theo tiền lệ v11.** Prompt sai về sản phẩm → sửa **prompt**. Chỗ có hậu quả a11y thực tế → **defect
trong code**, sửa kèm số đo. Ghi H1–H12 ở `spec.md`.

## C7 — Xử lý doc-drift?

**Trả lời: sửa tài liệu cho đúng thực tế (safe fix).**

**Hệ quả.** README/CLAUDE.md được sửa theo số đo thật, kèm bằng chứng. Đây là sửa tài liệu, không đổi hành vi
sản phẩm ⇒ không thể làm vỡ app.

## C8 — `POST /api/vocabulary` cho mọi user tạo từ GLOBAL?

**Trả lời: điều tra kỹ rồi mới quyết.**

**Hệ quả.** v12 probe 3 role live, đọc call-site (`VideoLesson.vue saveWordToDeck`), đo blast-radius (row có hiện
với mọi người qua `/search` không), rồi **nêu finding kèm số và đề xuất fix** — **không tự đổi authorization**
(đổi hợp đồng API đã ship là quyết định sản phẩm).

## C9 — Vòng 2 chạy tới đâu?

**Trả lời: chạy đến khi 2 vòng liên tiếp không tìm thêm lỗi mới (loop-until-dry).**

## C10 — taskstoissues / commit / AI vòng 2?

**Trả lời:** tạo **issue thật** trên GitHub (`giahuy1411/EngFlow`) cho finding chưa fix; **commit** fix + artifact
(Conventional Commits); vòng 2 **chạy AI pipeline đến xong**.

---

## Câu hỏi cố ý KHÔNG hỏi

- **Sửa lỗi nào** — không thể biết trước khi Phase 1–4 đo. Kế hoạch sửa cái đo được, báo cái còn lại.
- **Vòng 2 chạy bao lâu** — người dùng nói "không quan trọng thời gian", nên vòng 2 giới hạn bằng **độ phủ**, không
  bằng đồng hồ.
