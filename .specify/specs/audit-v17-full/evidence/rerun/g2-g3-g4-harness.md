# audit-v17-full (rerun) — Phase 9': harness + MCP unblock (G2/G3/G4)

## G2 — F-17-06: kết luận v17 SAI, đã sửa + chống tái diễn

**Phát hiện:** v17 ghi `consoleErrors:1` ở `/videos/1` là **warn YouTube `postMessage`** → **SAI**. Đọc text thật
trong `ui-sweep.json`: `"Failed to load resource: net::ERR_UNSAFE_REDIRECT"`.

**Tái hiện 4 lần** (profile mới, seed premium, có tương tác player): **0 error** mỗi lần ⇒ **sự kiện thoáng qua**,
không phải defect app/harness. (Bài học: phải đọc **text** lỗi, không suy từ tên route.)

**Đã làm:**
- Thêm `isThirdPartyConsoleNoise(text)` + regex **dùng chung** trong `sweep/v8/ui/lib.js` (nâng từ bản sao inline
  ở `routes-all.js:122-133`); `ui-sweep.js` console handler + `lib.js:visit()` dùng chung ⇒ 1 nguồn, không lệch.
- **Không** whitelist mù: regex chỉ các origin/noise đã verify từng cái; **không** sửa app (app đúng).
- `ui-sweep` chạy lại: **`consoleErrors: 0`** (run 3, run 4).

## G3 — F-17-07: `exercise_attempts` vào cleanup + parity (đã CHỨNG MINH)

**Đã làm:**
- `cleanupExerciseAttempts(from)` trong `sweep/v8/ui/lib.js` — khuôn `cleanupAuditPayments` (count **sau** delete,
  parse `rk[1]` đúng, không copy 2 lỗi tiềm ẩn của `cleanupStudyDays`). Cửa sổ `completed_at` + **chỉ 2 tài khoản probe**.
- Marker `EXERCISE_ATTEMPTS=` trong `p16-parity.sql`; `assertClean` kiểm thêm `exerciseAttempts`;
  `EXERCISE_ATTEMPTS_BASELINE` (ban đầu 36 = **cả bảng**; vòng 2 đổi còn **33** = **chỉ 2 tài khoản probe**, xem F-17-21); `ui-sweep` gọi cleanup trong `finally` + đưa `ok` vào exit code.

**Bằng chứng guard TỰ BẮT residue thật:**
1. Lần chạy đầu với baseline 37: `cleanupExerciseAttempts: candidates=1 remaining=0 -> SELF-CLEAN OK` rồi
   `assertClean … exercise_attempts=36 -> DIRTY: 36 != expected 37` → **exit 1**. Row bị dọn là `attempt_id=70144`
   (user 2, lesson 447, 14:07 — residue **trước phiên**). ⇒ **Guard hoạt động đúng**; baseline sửa còn **36**.
2. **Test sống:** tự INSERT một attempt giả (user 2, hôm nay) → marker lên **37** → chạy `cleanupExerciseAttempts()`
   → `candidates=1 remaining=0 SELF-CLEAN OK` → marker về **36**.
3. **An toàn:** học viên THẬT `giahuy8906@gmail.com` **vẫn còn 3** attempt (không bị đụng).

## G4 — F-17-13: tên ảnh theo namespace

`ui-sweep.js` dùng `VER` từ `_config.js` → `shots/v17-home-1440.png`, `v17-lessons-360.png`,
`v17-admin-dashboard-1440.png`, `v17-profile-1280.png`, `v17-premium-768.png`, `v17-lessons-1920.png`
(6 ảnh cũ `v13-*` đã xoá). Chạy lại: **exit 0**, `assertClean … CLEAN`.

## Ghi nhận thêm (chưa sửa — không thuộc phạm vi fix lỗi app)

- **F-17-16:** `_config.js:34` mặc định còn `audit-v15-full` (mọi lệnh đều truyền `--audit` tường minh).
- **F-17-17:** `focused-probe.js:188` hardcode `audit-v15-full` (cùng lớp F-17-13).
