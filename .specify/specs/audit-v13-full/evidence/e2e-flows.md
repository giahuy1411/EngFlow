# audit-v13 — Phase 4 E2E: UI → API → DB (dữ liệu THẬT, không mock)

Mỗi flow: thao tác UI thật (Playwright MCP) → bắt network call → đối chiếu API → **đọc hàng DB đổi**.

---

## T4.2b — E2E khép kín của bug người dùng báo (F-13-01) — **PASS**

Đây là phép đo "trước/sau" của F-13-01, chạy trên **dữ liệu thật**, không mock.

### Bước 1 — Admin tạo bài Trắc nghiệm (API thật, cùng payload UI gửi)
```
POST /api/admin/exercises
{"lessonId":91920,"question":"E2E Which form is correct? She ___ to school every day.",
 "options":"[\"go\",\"goes\",\"going\",\"went\"]","correctAnswer":"goes",
 "exerciseType":"MULTIPLE_CHOICE","difficulty":"EASY"}
-> HTTP 200, id=777464
```

### Bước 2 — Học viên đọc lại qua API thật
```
GET /api/lessons/91920/exercises (Bearer student)
-> {exerciseType: "MULTIPLE_CHOICE", options: "[\"go\",\"goes\",\"going\",\"went\"]"}
```

### Bước 3 — UI live (Playwright MCP) — **đây là chỗ bug cũ lộ ra**
```
url: /lessons/91920 (tab Bài tập)
totalOptionButtons: 12
textInputs: 0
e2eOptionsPresent: ["go","goes","going","went"]   <- render thành NÚT BẤM
```
Network bắt được: `GET /api/lessons/91920/exercises => 200`.

**Trước fix (cùng bài, cùng chỗ):** `textInputs: 2`, `option buttons: 0` → học viên thấy ô điền từ.
**Sau fix:** `textInputs: 0`, options render thành nút bấm. → **type không còn bị đổi âm thầm.**

### Bước 4 — Chấm điểm server-side (API thật)
```
POST /api/lessons/91920/exercises/grade  {"answers":[{"exerciseId":777464,"userAnswer":"goes"}]}
-> 200 {"percentage":100.0,"results":[{"correct":true,"correctAnswer":"goes",...}],"score":1,"total":1}

POST /api/lessons/91920/exercises/submit  (cùng body)
-> 200 {"percentage":100.0,"score":1,"total":1}
```

### Bước 5 — Hàng DB thật đổi
```
exercise_attempts: attempt_id=40123, lesson_id=91920, user_id=2, score=1, total=1, percentage=100.00,
                   details=[{"exerciseId":777464,"question":"E2E Which form is correct?...","userAnswer":"goes","correctAnswer":"goes","isCorrect":true}],
                   completed_at=2026-09-22 03:07:21
study_days: STUDY_DAYS_TODAY=1   <- submit ghi nhận ngày học (tích hợp streak)
```
→ **3 tầng khớp nhau**: UI (nút bấm) ↔ API (grade 100%) ↔ DB (`exercise_attempts` + `study_days`).

### Dọn dẹp
`DELETE exercises WHERE exercise_id=777464` (và attempts cùng lesson nếu cần) — parity quay lại đúng baseline.

---

## Ghi chú phương pháp

- **Không mock**: mọi bước dùng HTTP thật trên `:8080`, UI thật qua Playwright MCP, và SELECT thật trên container SQL Server.
- Token lấy từ `POST /api/auth/login` với tài khoản seed thật (`user@gmail.com`, `admin@gmail.com`).
- Đây là flow "UI → API → DB" mà audit v12 chưa làm ở mức 3 tầng.

---

## T4.1 — Đăng ký → Đăng nhập → `/me` → hàng `users` — **PASS**

```
POST /api/auth/register {username, email, password, fullName}
-> HTTP 201  {"id":201653, "email":"auditv13.<ts>@example.com", "isAdmin":false, ...}
DB: SELECT users WHERE email='...' -> 1 row, user_id=201653, is_admin=0, created_at=2026-09-22 03:22:14

POST /api/auth/login {email, password}
-> HTTP 200, token len=251

GET /api/auth/me (Bearer)
-> HTTP 200  {"email":"auditv13.<ts>@example.com","isAdmin":false,"currentLevel":"ELEMENTARY"}
```
**Dọn:** xoá user theo email liệt kê (`REG_LEFT=0`); parity quay lại baseline.
→ 3 tầng khớp: UI/API (201→200→200) ↔ DB (row thật).

## T4.4 — SRS review → `user_vocabulary_progress` — **PASS**

```
BEFORE: uvp id=30003 vocab 10018 | ease 2.06 | rep 0 | interval 1 | review_count 5
POST /api/srs/review {"vocabId":10018,"quality":5}
-> HTTP 200 {"message":"Review recorded successfully"}
AFTER:  uvp id=30003 vocab 10018 | ease 2.16 | rep 1 | interval 1 | review_count 6
```
→ SM-2 thật sự cập nhật (`ease +0.10`, `repetitions 0→1`, `review_count +1`).
**Cap F106 xác nhận:** `MAX(srs_interval) user 2 = 365` (không vượt cap).
**Dọn:** khôi phục hàng về giá trị trước đo.

## T4.5 — Tìm kiếm → tham số thật → kết quả khớp DB — **PASS**

```
GET /api/vocabulary/search?q=habitat  -> HTTP 200, 1 kết quả
   API: vocabId=10051, word='habitat'
   DB : vocab_id=10051, word='habitat', meaning='môi trường sống'
```
→ Kết quả API khớp **đúng** hàng DB (không thừa, không thiếu).

## T4.2b — E2E khép kín F-13-01 — **PASS** (xem phần trên)

## T4.2c — Chống regression khối lượng lớn — **PASS**

Lesson 567 có **12 hàng `MULTIPLE_CHOICE` với `options IS NULL`** (đúng hình dạng của 32 814 hàng):
- UI: **39 ô nhập text** (12 MC-NULL + 27 FILL_BLANK), **0** nút lựa chọn → mọi thẻ đều **trả lời được**.
- Đây là phép đo chứng minh bản fix đầu (xoá ô text) đã bị đảo ngược đúng.

## T4.6 — CRUD admin (lesson) qua API → DB — **PASS**

```
CREATE  POST /api/admin/lessons {"title":"AUDIT-V13 CRUD lesson",...}
        -> HTTP 200, lesson_id=103625
        DB: SELECT lessons WHERE lesson_id=103625 AND title='AUDIT-V13 CRUD lesson' -> CRUD_ROW=1
UPDATE  PUT  /api/admin/lessons/103625 {"title":"AUDIT-V13 CRUD lesson UPDATED",...}
        -> HTTP 200
        DB: SELECT ... title LIKE '%UPDATED%' -> UPDATED=1
DELETE  DELETE /api/admin/lessons/103625
        -> HTTP 204
        DB: CRUD_ROW=0
```
→ Cả 3 tầng (API status ↔ DB row) khớp ở mọi bước. Dọn sạch: `AUDIT13_LESSONS=0`, parity về baseline.

## T4.3 — Streak: hoạt động → snapshot → `study_days` — **PASS**

```
GET /api/streak/snapshot (Bearer student)
-> HTTP 200, đủ 7 field: currentStreak, effectiveFrom, legacyAccessDays,
   legacyHistoryAvailable, studiedDays, studiedToday, today
   currentStreak=2, studiedToday=true, today=2026-09-22

DB: SELECT study_days WHERE user_id=2 -> 2026-09-22, 2026-09-21  (STUDY_DAYS_U2=2)
```
→ `currentStreak=2` khớp **chính xác** 2 hàng `study_days` liên tiếp. Hợp đồng 7 field đủ và đúng kiểu.

## T4.7 — AI: validate guard cho lựa chọn chữ cái (đóng lỗ bypass) — **PASS**

```
POST /api/admin/exercises/ai/validate
 {"exercises":[
   {"question":"q1","options":"[\"a\",\"b\",\"c\",\"d\"]","correctAnswer":"a","exerciseType":"MULTIPLE_CHOICE"},
   {"question":"q2","options":"[\"rise\",\"rises\",\"rose\"]","correctAnswer":"rises","exerciseType":"MULTIPLE_CHOICE"}]}
-> HTTP 200
   q1: valid=false  "MULTIPLE_CHOICE options need real answer text, not only letters"
   q2: valid=true
```
→ Đường AI (nơi model local có thể sinh `["a","b","c","d"]` — distinct và size 4, lọt qua mọi check cũ) **nay bị chặn**. Đây là lỗ mà adversarial review phát hiện (HIGH) và đã đóng.
**Ghi chú:** endpoint `validate` không gọi Ollama nên chạy nhanh; đường `generate-async` cần GPU/Ollama nên ghi **BLOCKED-time** nếu chạy (xem giới hạn).
