# audit-v16-full — Phase 4: Core feature E2E (API ↔ UI)

Mỗi nhóm: kịch bản · kết quả API (thật) · kết quả UI (thật, chrome-devtools MCP) · bằng chứng.

## 1. Đăng nhập / Đăng ký

| Bước | Kết quả | Bằng chứng |
|---|---|---|
| Login `user@gmail.com`/`123456` trên UI | → chuyển `/lessons`, banner streak hiện | MCP snapshot (`shots/mcp-lessons-search.png`) |
| Guard: user vào `/admin/*` | → đá về `/` | ui-sweep: `student /admin/users -> /` |
| Login sai mật khẩu | 401 | api-sweep C1 |
| Đăng ký trùng email | 400/409 | api-sweep C1 |
| Forgot/reset/change-password | 200/400 đúng | api-sweep C1 |
| 0 console error trên `/login` | ✅ | ui-sweep |

**API↔UI:** nút "Đăng nhập" → `auth.login()` → `POST /api/auth/login` → lưu `token`+`user` → router guard 4 loại.

## 2. Bài học / Bài tập (verify sâu qua MCP)

| Bước | Kết quả (đo thật) |
|---|---|
| `/lessons` render | 1465 bài, phân trang `1 - 12 / 1465`, filter trình độ |
| Gõ tìm kiếm "present simple" | **29 / 1465**, mọi tiêu đề khớp |
| `/lessons/447` tab **Nội dung** | chỉ nội dung scrape (h1 + h3…), **không** còn "Nội dung biên soạn" |
| Tab **Bài tập** | **8 câu** (TRẮC NGHIỆM/ĐIỀN TỪ/NỐI TỪ/DỊCH/NGHE + audio thật 0:04) |
| Chọn "goes" → Kiểm tra | **✅ ĐÚNG · Đáp án: goes** (chấm server-side) |
| Bấm **Nộp bài (8 câu)** | **✅ ĐÃ LƯU KẾT QUẢ! XEM TAB LỊCH SỬ.** |
| Tab **Lịch sử** | **"LỊCH SỬ LÀM BÀI · 13% · CHƯA ĐẠT · 1/8 câu đúng · 0 phút trước"** |
| Anon không lộ đáp án | api-sweep: `includeAnswers=true` anon không có `correctAnswer` |

**Bằng chứng:** `shots/mcp-lessons-search.png`, `shots/mcp-lesson-history.png`; api-sweep C2 (18 pass).

## 3. Streak

| Bước | Kết quả |
|---|---|
| `GET /api/streak/snapshot` (auth) | 200, đủ 7 field (api-sweep C3) |
| `GET /api/streak/current` / `/history?days=30` | 200 |
| anon | 401 |
| UI `/profile` | ô Streak + lịch 30 ngày render (ui-sweep `student /profile`) |

**API↔UI:** `Profile.vue` → `streakService.getSnapshot()` → `GET /api/streak/snapshot`; số header + lịch cùng nguồn.

## 4. Tìm kiếm / Sắp xếp

| Bước | Kết quả |
|---|---|
| `/api/vocabulary/search?keyword=ab` (public) | 200 |
| `keyword=a` (1 ký tự) | 200 + `[]` (guard < 2 ký tự — đúng thiết kế) |
| `/api/vocabulary` anon | 401 (cần login) |
| `/api/admin/exercises?q=the` (admin) | 200 |
| `?sort=title,asc` vs `desc` | đo lại (search-sort probe) |
| UI `/lessons` search | 1465 → 29 (đo MCP) |

## 5. CRUD

| Bước | Kết quả |
|---|---|
| Deck C→R→U→D (student, tự dọn) | 200/201, round-trip, private deck ẩn khỏi anon, xoá → 404 |
| Lesson C→R→U→D (admin) | 200/201, xoá → 404 |
| Vocabulary (student cần deckId) | đúng; IDOR chặn 400 |
| F147/F152 guards | pass (api-sweep C5: 23 pass) |

## 6. AI

| Bước | Kết quả |
|---|---|
| `POST /api/ai/generate-vocab` (auth) | 200/429 (đo latency, Ollama local) |
| `POST /api/ai/enrich-word` | 200/429 |
| `POST /api/ai/save-vocab` | 200, header `X-AI-Linked-To-Deck` hiện (F145) |
| `GET /api/admin/exercises/ai/status` | 200 |
| anon | 401/403 |

## 7. Game / Flashcard / SRS

| Bước | Kết quả |
|---|---|
| 5 game mode (quiz/memory/typing/listening/mixed) | 200, trả `sessionId` |
| `POST /api/games/submit` session thật | 200 |
| Flashcard review `quality=4` | 200; `quality=9` → 400; body cũ `isKnown` → 400 |
| SRS due/stats/review | 200; thiếu field → 400 |
| UI 6 game route (`/decks/:id/play/*`) | render, 0 console error |

## 8. Payment

| Bước | Kết quả |
|---|---|
| `GET /api/v1/payment/status` | 200 |
| `POST /api/v1/payment/create-order` | 200, orderCode `ENG…` |
| Webhook chữ ký sai | 200 body `success:false` (đúng hợp đồng SePay) |
| Webhook chữ ký THẬT | **BLOCKED** (biên real-money — như v11) |
| Dọn PENDING cùng run | `remaining=0 after=12 baseline=12` |

## 9. Speaking / Video

| Bước | Kết quả |
|---|---|
| `GET /api/v1/speaking-prompts` (public) | 200; `?q=` 200 |
| `GET /api/v1/admin/speaking-prompts` (admin) | 200; student 403; anon 401 |
| `GET /api/v1/speaking-submissions` (auth) | 200 |
| `GET /api/v1/video-lessons` / `/{id}` | 200 |
| `GET /api/v1/video-attempts` / admin | 200; role đúng |
| Upload+assess (mic giả → FAILED) | đúng thiết kế (như v11) |

## Kết luận Phase 4

**6 nhóm chức năng chính verify API↔UI đều có bằng chứng.** Không phát hiện lỗi chức năng mới ở vòng 1.
Bài học/Bài tập verify **sâu nhất** (qua MCP thật: search → detail → tab → grade → submit → history).
