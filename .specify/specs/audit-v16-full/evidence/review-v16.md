# audit-v16-full — Phase 6: review chéo đối kháng (review-v16)

**Bắt buộc:** mọi code AI sinh trong phiên phải được review đối kháng trước khi commit (yêu cầu người dùng +
constitution P8). Dùng subagent `general-purpose` với chỉ dẫn **"cố gắng PHẢN BÁC, không xác nhận"**.

## Kết quả review (tóm tắt verdict)

| Hạng mục | Verdict của reviewer | Tôi đối chiếu | Hành động |
|---|---|---|---|
| **F-16-01** root cause ("login ghi study_days") | **CONFIRMED-DEFECT — SAI** | ✅ đúng: `grep recordStudy` → Exercise/Flashcard/Srs/Streak/Speaking; **login chỉ đọc** | **Sửa lại** attribution + code |
| F-16-01 helper dùng equality 1 ngày (false-pass nếu qua nửa đêm) | **CONFIRMED-DEFECT** | ✅ đúng (payments helper đã dùng half-open window) | **Sửa** → cửa sổ `[VN_RUN_DATE, today]` |
| F-16-01 `design-v2.js` ngoài `finally` | **CONFIRMED-DEFECT** | ✅ đúng | **Sửa** → bọc `try/finally` |
| F-16-01 `ui-sweep` bỏ `cleanSd.ok` khỏi exit code | **CONFIRMED-DEFECT** | ✅ đúng (lớp v15 L1-c) | **Sửa** |
| F-16-01 bỏ sót `deep-probe` (ghi qua `games/submit`) | **CONFIRMED-DEFECT** | ✅ đúng | **Sửa** → inline cleanup + assert |
| F-16-02 details[open] hard shadow | CLEAN | ✅ | giữ |
| F-16-03 skeleton shimmer | CLEAN (yếu hơn nhưng còn shimmer) | ✅ | giữ |
| **F-16-04** topbar full-opacity accent | **PLAUSIBLE — không nên đổi** | ✅ đúng (offset không blur, đổi gây viền đôi) | **Hoàn nguyên** |
| **F-16-05** nav `--geo-shadow-xs` | **CONFIRMED-DEFECT — tôi tự gây** | ✅ đúng: sidebar = `#1E293B` → shadow vô hình | **Hoàn nguyên** |
| F-16-06 Flashcard text-shadow | CLEAN (syntax + visibility OK) | ✅ (đã rebuild xác nhận) | giữ |
| F-16-07/08 doc counts | CLEAN (đếm lại = 25 / V1–V10) | ✅ | giữ |
| **Demo doc**: streak claim đảo ngược | **CONFIRMED-DEFECT** | ✅ đúng: `submitExercises` gọi `recordStudy` | **Sửa** |
| **Demo doc**: khoá ở lần thứ 6 | **CONFIRMED-DEFECT** | ✅ đúng: `fails >= MAX_LOGIN_FAILS(5)` → khoá ở lần **5** | **Sửa** |
| Demo doc: citation drift (Login.vue:67→71, …) | CONFIRMED (nhỏ) | ✅ | **Sửa** |

## Giá trị của review chéo (bằng chứng)

Review **bắt được 4 defect thật trong fix của chính tôi** (F-16-01 attribution sai + 2 đường false-pass/leak +
F-16-05 regression vô hình) và **2 lỗi nội dung trong tài liệu demo** — tất cả **trước khi commit**. Đây đúng
là lý do bước review chéo là bắt buộc: nếu commit thẳng, sản phẩm sẽ có (a) harness tưởng đã dọn mà thực ra
không, (b) một tài liệu demo nói SAI về streak ngay trước hội đồng.

## Ghi chú kỷ luật
- Reviewer **cũng có thể sai** → tôi **đối chiếu lại từng claim** bằng grep/đọc code trước khi sửa (đã làm; mọi
  claim ở trên đều verify đúng).
- Các "process defect" reviewer nêu (checklist R6 thiếu số đo before/after cho F-16-02..06; `design-conformance.md`
  nói "0 finding design" trong khi findings.md liệt kê 5) → **đã sửa**: đo before/after bằng grep; sửa câu trong
  `design-conformance.md` (5 micro-drift phát hiện ở tầng CSS, không phải ở tầng token/font mà design-v2 đo).
