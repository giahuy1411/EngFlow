# audit-v12-full — requirements quality checklist

Checklist là "unit test cho yêu cầu". Mỗi mục hỏi **spec có tốt không**, không hỏi code có đúng không. Câu trả lời
là của phiên này, kèm artifact đỡ.

---

## A. Đo lường được

- [x] **A1.** Mọi tiêu chí thành công đo được bằng lệnh, không bằng cảm nhận?
      — Có. Mỗi dòng trong bảng success criteria của `spec.md` nêu probe và artifact. Không dòng nào ghi "nhìn ổn".
- [x] **A2.** Mỗi phép đo nêu phương pháp (median N, driver nào, log nào)?
      — Có: T6.1 chốt "median ≥5"; Phase 3 ghi rõ driver nào ra số nào; Phase 0 chốt "đếm từ run log".
- [x] **A3.** Số đếm lấy từ nguồn không thể nói dối?
      — Có, kèm bẫy đã ghi: `target/surefire-reports` XML từng làm aggregate ảo +8 (XML cũ của class đã xoá),
      nên số đếm lấy từ **run log**.

## B. Trung thực

- [x] **B1.** Spec có cấm nhận việc đã xong là việc mới?
      — Có. `spec.md` "Why this audit exists" nêu ba điều **đã xong** (font, design system, audit cũ) và nói v12
      không được giả vờ phát hiện. Đây là mục quan trọng nhất.
- [x] **B2.** Có luật cấm sao chép số cũ vào kết luận?
      — Có: R1.
- [x] **B3.** Lỗi của probe có bị coi nghiêm trọng như lỗi bỏ sót?
      — Có: R7 + risk register. Lý do: probe báo lỗi app không có sẽ dẫn tới "sửa" code đang đúng. v11 gặp đúng
      điều này 10 lần.
- [x] **B4.** Mục chưa verify phải báo BLOCKED/PARTIAL, không được bỏ qua?
      — Có: R7 + Boundaries.
- [x] **B5.** Kế hoạch có nêu điều sẽ KHÔNG làm, và vì sao?
      — Có: T2.10 nêu biên giới tiền thật/phá huỷ; Phase 6 ghi lý do **từ chối** tối ưu; Phase 5 chỉ sửa lỗi đo được.

## C. Đủ so với yêu cầu người dùng

- [x] **C1.** "Quét toàn bộ codebase" — có đáp ứng?
      — Có, qua inventory endpoint tái dựng từ source (T0.7) + tập file design-system. Ý **định** của yêu cầu là
      độ phủ; đi từng file trong 337 file sinh ra khối lượng, không sinh bằng chứng.
- [x] **C2.** "Kiểm DB trong Docker" — có?
      — Có: Phase 1, gồm **hiệu lực** constraint (insert vi phạm có bị chặn thật).
- [x] **C3.** "Chạy toàn bộ API" — có?
      — Có: Phase 2, **phủ hết 131 endpoint** (v11 chỉ ~27), role đúng 2 chiều, kiểm hợp đồng; biên giới tiền
      thật/phá huỷ ghi rõ ở T2.10.
- [x] **C4.** "Tương tác toàn bộ UI tương ứng từng API" — có?
      — Có: Phase 3 T3.8 bắt network call từng flow UI và so với hợp đồng API, thay vì test UI và API rời nhau.
- [x] **C5.** "6 chức năng chính" — map đủ?
      — Có: mỗi chức năng có dòng riêng ở Phase 2 (API) và Phase 3 (UI↔API), cộng Phase 4 cho CRUD qua UI admin.
- [x] **C6.** "Tối ưu hiệu năng" — có?
      — Có: Phase 6, trước **và** sau, theo P5.
- [x] **C7.** "Test UI/UX bằng chrome-devtools-mcp và playwright-mcp" — có?
      — Có: Phase 3 dùng **cả hai làm driver hạng nhất**; cả hai đã verify sống ở đầu phiên. Đây là chỗ v12 sửa
      thiếu sót của v11 (chrome-devtools gần như chỉ `list_pages`).
- [x] **C8.** "Verify UI khớp prompt" — có?
      — Có: Phase 3 T3.10, và lỗ hổng của prompt phân tích ở `spec.md` H1–H12.
- [x] **C9.** "Thay toàn bộ font thành Be Vietnam Pro" — có?
      — Có, và giải quyết là **đã xong** (C4 trong `clarify.md`), verify chứ không làm lại.
- [x] **C10.** "Dùng skill/plugin từ `C:\Users\ASUS\.claude`" — có?
      — Có: bảng skill theo phase ở cuối `tasks.md` và bảng trong report. Skill **thực sự nạp** được liệt kê,
      không chỉ liệt kê cái có sẵn.
- [x] **C11.** "Kiểm prompt có lỗ hổng, sửa, triển khai" — có?
      — Có: H1–H12 kèm bằng chứng, cộng C6 trong `clarify.md` (sửa **prompt** khi prompt sai, không bẻ code).
- [x] **C12.** "Theo workflow constitution → specify → clarify → checklist → plan → tasks → implement → converge →
      analyze → taskstoissues" — có?
      — Có: artifact tồn tại đúng thứ tự trong thư mục này.
- [x] **C13.** "Chạy vòng 2 toàn diện hơn, đến khi cạn lỗi" — có?
      — Có: Phase 7, loop-until-dry (2 vòng liên tiếp không lỗi mới).
- [x] **C14.** "Báo cáo đã làm/chưa làm/đã fix & cách fix/skill đã nạp" — có?
      — Có: T7.5, cấu trúc mục cố định.

## D. Nhất quán

- [x] **D1.** Spec, plan, tasks khớp tên phase và số thứ tự?
      — Có. Phase 0–8 ở cả ba, cùng tên.
- [x] **D2.** Có yêu cầu nào mâu thuẫn nhau?
      — Đã kiểm. Một căng thẳng bề mặt được giải thích rõ: C3 cho phép **ghi** DB trong khi Phase 1 **read-only**
      — khác phase, và mọi ghi đều theo sau bằng assert parity. Không mâu thuẫn.
- [x] **D3.** Kế hoạch tôn trọng ràng buộc hiến pháp?
      — Có, kiểm từng mục ở `constitution.md`. P3 (không Flyway), P5 (đo trước), P6 (một font), P7 (a11y),
      P8 (bằng chứng runtime) đều được giữ.
- [x] **D4.** Thứ tự cột của parity line có cố định để không trôi?
      — Có: `sweep/v8/p16-parity.sql` cố định thứ tự; v12 dùng đúng query đó, không đổi nhãn.

## E. Rủi ro

- [x] **E1.** Rác DB do sweep ghi có được kiểm soát?
      — Có: namespace audit, dọn trong cùng run, re-assert parity, quét `Msg \d+`. Rò đã biết
      (`PremiumCheckout.vue` tạo row lúc mount) được nêu trong risk register.
- [x] **E2.** Rủi ro "không có điểm rollback" đã đóng?
      — Có: checkpoint `30bc9b6`, tree sạch.
- [x] **E3.** Hành vi gây nhầm của tool đã ghi lại để không học lại?
      — Có: `sqlcmd` exit-0-khi-batch-lỗi, `MSYS_NO_PATHCONV` cho đường dẫn `/opt/...` trong Git-Bash, lệch
      scrollbar ~15px là false positive, `alt=""` là hợp lệ.
- [x] **E4.** Bucket rate-limit có thể làm harness tự chặn chính nó?
      — Biết và xử lý: flush `rate_limit:*` trước mỗi batch (bài học F109).
- [x] **E5.** `cmd /c "mvnw.cmd test"` từ Git-Bash có chạy được?
      — **KHÔNG** (đã đo). Dùng `mvn -o test` (Maven 3.9.16 / JDK 25) — cùng hiệu lực. Ghi ở `baseline.md`.

## Verdict

**Checklist: PASS.** Không mục nào bỏ trống. Hai mục dễ bị bỏ nhất ở kỳ audit tiếp theo — **B1** (đừng nhận việc
đã xong) và **C4** (đối chiếu UI với API thay vì test rời) — đều là yêu cầu tường minh có task tên riêng.
