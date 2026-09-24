# audit-v14-full — Phase 4: E2E 3 tầng UI → API → DB (dữ liệu THẬT)

**Ngày:** 2026-09-25 (+07) · **Harness:** `sweep/v14/e2e-3tier.js` · **Data:** `e2e-flows.json`
**Kết quả:** **7/7 PASS** · **parity = `1470|43735|72|118|29|15|4|126|10|5`** (baseline) · study_days = 4

Mỗi flow: API thật (HTTP :8080) ↔ hàng DB thật (sqlcmd). Tầng UI do `ui-sweep.js` phủ (network capture).
v14 siết so v13: assert **đẳng thức** ở tầng DB, không chỉ "row đổi".

| Flow | Kỳ vọng | Kết quả |
|---|---|---|
| **T4.4 Register → login → `/me` → `users`** | 200/201 + hàng DB | ✅ `dbRow=1`, dọn `cleaned=0` |
| **T4.3 Streak** | `currentStreak` == số ngày liên tiếp | ✅ `currentStreak=0` == computed 0 (user nghỉ 3 ngày); 7 field đủ |
| **T4.5 Search** | id kết quả == id DB | ✅ `resultId=10051` (`habitat`) == DB `VOCAB=1` |
| **T4.6 CRUD** | DB row 1→1→0 | ✅ create 200 → update 200 → delete 204 → `afterDelete=0` |
| **T4.2 F-13-01 khép kín** | MC tạo → render options → grade 100% | ✅ `listHasExercise=true`, `grade=200`, `percentage=100` |
| **T4.7 AI validate** | chặn chữ cái, cho options thật | ✅ `q1Valid=false`, `q2Valid=true` |
| **T4.8 Payment** | create-order + sai chữ ký | ✅ orderCode `ENG…`, `webhookRejected=true` |

## Sửa 2 lỗi probe tự gây (ghi lại minh bạch)

| # | Lỗi probe | Đúng | Fix |
|---|---|---|---|
| E1 | `T4_streak` assert `currentStreak == tổng số hàng study_days` | `currentStreak` = số ngày **liên tiếp** kết thúc hôm nay (hoặc hôm qua), KHÔNG phải tổng | Tính lại kỳ vọng từ `studiedDays` trả về; assert đẳng thức với giá trị tự tính |
| E2 | Payment cleanup xoá **1 order_code** | `create-order` = 1 hàng, nhưng UI sweep để lại hàng cùng ngày → parity 126→130 | Xoá **cả cửa sổ ngày VN** (như `cleanupAuditPayments`) |

→ Sau fix: `parityAfter=1470|43735|72|118|29|15|4|126|10|5 studyDays=4 ok=true`, exit 0.

## Ghi chú phương pháp

- **Không mock**: HTTP thật :8080, hàng DB thật, token từ login tài khoản seed thật.
- Mọi write dọn **trong cùng run**; re-assert parity + `study_days` cuối script (bài học F-14-01).
- MC closure dùng lesson 445 (đã khôi phục sau sự cố F-14-01).
