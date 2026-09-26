# audit-v17-full — Phase 6.2: review chéo đối kháng (review-v17)

**Bắt buộc:** mọi code AI sinh phải được review đối kháng trước khi commit (yêu cầu người dùng + Hiến pháp P8).
Subagent `general-purpose`, chỉ dẫn **"cố gắng PHẢN BÁC, không xác nhận"**, phạm vi: 3 fix + 1 file test.

## Reviewer đã làm gì (không chỉ đọc)

- **Tự tính lại** công thức WCAG: white/#8B5CF6 = 4.234, white/#7C3AED = 5.699 — khớp số tôi báo.
- **Build Tailwind thật** để kiểm `bg-accent-strong` có resolve không (đề phòng typo → nền trong suốt, tệ hơn bản cũ):
  compile ra `.bg-accent-strong { background-color: rgb(124 58 237 / …) }` → **resolve thật**.
- **Mutation-test** chính file test của tôi (chuỗi nguồn giả) để thử false-pass.

## Verdict

| Vùng | Verdict | Tôi đối chiếu lại | Hành động |
|---|---|---|---|
| 1. Toast contrast + utility tồn tại | **CLEAN** | ✅ xác nhận bằng browser: `bg-accent-strong` → `rgb(124,58,237)` | giữ |
| 2. Fix `#E2E8F0` + fallback còn sót | **CONFIRMED-DEFECT** | ✅ tự grep: `lessonLevels.js:20,30` còn `#64748B` | **SỬA** → `#556070` (F-17-09) |
| 3. Guard `< 2` | **CLEAN** | ✅ `search()` là entry DUY NHẤT (`@click`+`@keyup.enter`); biên khớp doc+backend | giữ |
| 4. Độ mạnh của test | **CONFIRMED-DEFECT** | ✅ tự chạy regex: khớp cả guard bị comment-out → **false-pass thật** | **VIẾT LẠI** thành behavioral + mutation-test (F-17-10) |
| 5. Hiệu ứng bậc hai | **CLEAN** | ✅ suite 181/1 xanh; không test nào assert class cũ | giữ |

## Giá trị đo được của review (đây là lý do bước này bắt buộc)

Review bắt **2 defect thật trong chính fix của tôi**:

1. **Fix chưa trọn (F-17-09):** tôi sửa `Lessons.vue` nhưng bỏ sót **cùng lớp lỗi** ở `lessonLevels.js:20,30`
   (fallback `#64748B` — giá trị audit-v11 F138 đã thay bằng `#556070` vì 4.34:1 < 4.5:1, và nó là **màu chữ**
   ở `Lessons.vue:106`). → đã sửa cả 2 chỗ.
2. **Test vô dụng (F-17-10):** assertion `expect(src).toMatch(/term\.length\s*<\s*2/)` **khớp cả khi guard bị
   comment-out** → pass dù fix bị revert. → viết lại thành **behavioral** (mount component, click thật, assert
   service không được gọi với 1 ký tự / được gọi với 2 ký tự) + **mutation-test chứng minh** nó fail khi guard tắt.

## Kỷ luật: reviewer CŨNG có thể sai → tôi đối chiếu lại từng claim

- Claim #2: tự `grep -n "64748B" lessonLevels.js` → **đúng**, có 2 dòng.
- Claim #4: tự chạy `node -e` với regex trên chuỗi comment-out → **đúng**, trả `true` (false-pass thật).
- Claim #1: tự verify bằng chrome-devtools (`getComputedStyle`) → **đúng**, resolve `rgb(124,58,237)`.
- Claim #5: đọc `api.test.js`/`useToast` call sites → **đúng**, không nơi nào assert class cũ.

## Mutation-test (bằng chứng test mới thực sự bảo vệ)

```
# Tắt guard: 'if (term.length < 2) {' → 'if (false) {'
FAIL  audit-v17 F-17-02 > 1 character -> service is NOT called
Test Files  1 failed (1)   Tests  1 failed | 4 passed (5)
# Khôi phục:
Test Files  1 passed (1)   Tests  5 passed (5)
```
→ Test **fail khi fix bị revert** = có giá trị hồi quy thật.
