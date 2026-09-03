# REPORT: audit-v5-full — Vòng 3 toàn diện, sửa tận gốc

**Ngày:** 2026-09-03 · **Commits:** `02aa8c1` (fix+design+guards), `83a8ad1` (perf), `7a2c131` (server-side grading)
**Kết luận:** 15 finding gốc-rễ đã sửa; 67/67 API · 221/221 backend tests · 73/73 frontend tests · build sạch.

---

## 1. ĐÃ LÀM

### 1.1 Quét codebase + DB + docker
- Đọc toàn bộ controller/service Vue, đối chiếu endpoint ↔ UI; profile `exercises` theo `exercise_type` (43.727 dòng): MATCHING 481 (333 hợp lệ / 68 rỗng / 80 malformed), FILL_BLANK 9.119 (97 có options, 18 placeholder A-D, 3 degenerate), LISTENING 440 (81 thiếu audio), 5 dòng options `'null'`.
- `sp_updatestats` chạy lại; SQL Server cap 2048MB qua `sp_configure` (init-db.sql + live); `MSSQL_PID=Developer`.

### 1.2 Test backend bằng API + UI thật (playwright + chrome-devtools)
- Sweep 67 endpoint (auth/lessons/exercises/decks/dictionary/premium/webhook/admin/AI) — **67/67 PASS** trên container rebuilt.
- E2E qua UI: login/logout, register (verify DB row), forgot-password (OTP Redis + log), sai mật khẩu (alert), 6 deck games (quiz/typing/memory/listening/mixed/flashcard), lesson exercises 5 type, admin CRUD 7 trang (create→edit→delete lesson qua modal), AI generate-async (2/2 exercise), dictionary search, leaderboard, profile, speaking (mic→FAILED đúng hành vi silent-mic), video shadowing (attempt 7 → admin grade 8.5), premium checkout qua Tailscale funnel (order `ENG2B698A6BC850`, idempotent replay OK).
- **Thanh toán thật:** user xác nhận đã chuyển khoản cho `ENGF8AB9431CE85`; DB không có giao dịch thật khớp → reset premium, chạy lại E2E webhook sạch. Kết luận ghi ở mục 4.

### 1.3 Design system "Playful Geometric" + Be Vietnam Pro 100%
- Font: `index.html` bỏ Google Fonts Jakarta Sans; `design-system.css` bỏ JetBrains Mono; `tailwind.config.js` mono→BVP. Computed-style mọi trang public = **chỉ "Be Vietnam Pro"**.
- Token: `--geo-muted`/`--geo-border`/`--geo-shadow-xl` về đúng prompt; thêm `danger/warning/success` (55 class chết trước đó); `bg-pink-500`→`bg-accent`; FlashcardGame bỏ 9 gradient off-palette → flat token + ink tương phản (verify `rgb(52,211,153)`).
- A11y: xóa skip-link duplicate, toast `aria-label`, border token.

### 1.4 Tối ưu hiệu năng (P5: before→after)
| Metric | Before | After |
|---|---|---|
| GET /api/lessons (warm) | 39ms | **24ms** |
| Log backend /10 requests | ~7.5k dòng/giờ | **0** |
| Dictionary | 835ms cold | **11–14ms** warm (Redis) |
| SQL Server RAM | không cap (7.7GB host) | **2048MB** |
| LessonSnapshotService | N+1 blocks/section | **1 query/lesson** |
| LCP home (dev) | 513ms | **366–427ms** |
| CLS home (dev) | 0.17 | 0.18 (footer đã reserve 96px; phần còn lại = font-swap FOUT của dev-server — prod build khác) |

### 1.5 Vá lỗ hổng của chính prompt yêu cầu
Prompt gốc không nói rõ: (a) kiểm tra `.env` từng biến → đã bổ sung (bắt được F1/F4); (b) `@PreAuthorize` có enforce không → F2; (c) data seed có khớp contract UI không → F8/F9; (d) **frontend chấm điểm bằng gì khi server strip đáp án** → F15. Tất cả ghi vào spec §2/§2b.

### 1.6 Workflow SpecKit đầy đủ
`constitution (v1.0.1) → specify → clarify → checklist → plan → tasks → analyze → implement → converge` — artifacts tại `.specify/specs/audit-v5-full/` (6 file).

---

## 2. ĐÃ FIX + CÁCH FIX (15 finding)

| # | Lỗi | Cách sửa tận gốc |
|---|-----|------------------|
| F1 | SePay 401, polling chết | `SEPAY_API_TOKEN` trong `.env` bị nối `DB_PASSWORD` (91≠64 ký tự) → sửa token |
| F2 | `@PreAuthorize` trơ hết | Thêm `@EnableMethodSecurity` (SecurityConfig) |
| F3 | Free user upload → 500 | Handler `ResponseStatusException` trong GlobalExceptionHandler → 403 đúng |
| F4 | Rubric AI sai model/URL | `.env`: `qwen2.5:3b` + `host.docker.internal:11434` |
| F5 | 55 class màu chết | tailwind thêm danger/warning/success |
| F6 | Font phụ sót | Xóa Jakarta Sans + JetBrains Mono mọi nơi |
| F7 | Token drift | `--geo-muted/#F1F5F9`, `--geo-border/#E2E8F0` |
| F8 | MATCHING 2 cột trống | `MatchingExercise`: fallback text input khi không parse được `left\|right` |
| F9 | Nút A/B/C/D vô nghĩa | `parsedOptions` lọc placeholder + `'null'`; options thật → nút chọn |
| F10 | Gradient off-palette | FlashcardGame flat token + ink |
| F11 | show-sql spam | `${SPRING_JPA_SHOW_SQL:false}` |
| F12 | N+1 snapshot | `findBySectionIds` batch |
| F13 | SQL Server ăn RAM host | `sp_configure max server memory=2048` |
| F14 | `bg-pink-500` | `bg-accent` |
| **F15** | **Mọi câu đúng hiện "❌ SAI"** | Frontend tự chấm trên `ex.correctAnswer` đã bị server strip (includeAnswers=403) → **chuyển sang `POST /grade` server-side**; badge hiện đáp án server trả; row không key → "Không chấm được". Verified: 5/5 ĐÚNG, nộp bài → lịch sử 100%. |

Ngoài ra: xóa 13 module frontend chết (~800 dòng), `premium.js` catch, footer min-height.

---

## 3. SỐ LIỆU KIỂM CHỨNG CUỐI (loop 2, sau mọi thay đổi)
- Backend: `mvnw test` **221/221** · API sweep **67/67** · log volume 0
- Frontend: `vitest` **73/73 (14 files)** · `vite build` sạch
- E2E walkthrough lesson 41881 (user thường): 5/5 câu ĐÚNG khi trả lời đúng, nộp bài lưu lịch sử 100%
- Mobile 375px: 6 trang không overflow (shots `audit-v5-shots/mobile-*.png`)

## 4. CHƯA LÀM / CÒN TỒN TẠI (rõ ràng, không giấu)
1. **Data seed hỏng — ĐÃ SỬA TẬN GỐC phần lớn (vòng continue)**: 76/148 dòng MATCHING hỏng được phân loại + sửa: 75 dòng "MATCHING" thật ra là MULTIPLE_CHOICE dán nhãn sai (đáp án nằm trong options JSON) → UPDATE exercise_type; 1 dòng legacy `:::` viết lại options sạch. Còn lại 68 dòng options rỗng (55 có đáp án text tự do — UI fallback text-input xử lý tốt) + 4 dòng degenerate thật (đáp án không có trong options — UI fallback). Backup: bảng `exercises_bak_v5` (481 dòng). LISTENING thiếu audio (81) giữ fallback TTS trình duyệt. **Verified**: lesson 41881 Q3 render 3 nút is/am/are, chấm ĐÚNG; sweep 67/67.
2. **Giao dịch thật ENGF8AB9431CE85**: DB không có transaction khớp → hoặc SePay webhook chưa về lúc chuyển, hoặc nội dung CK sai cú pháp. Cần đối soát thủ công với sao kê; premium hiện active do E2E webhook test (expiry 2026-10-03).
3. **CLS dev-server 0.18**: footer đã reserve; phần còn lại là FOUT font-swap khi Vite dev serve unminified — cần đo lại bằng `vite preview`/prod build khi deploy.
4. ~~`/ai-vocab-generator`~~ **ĐÃ XONG (converge round)**: walkthrough đầy đủ — form Travel/B1/5 → Ollama sinh 5 từ (travel/destination/accommodation/transport/budget) → "Lưu tất cả" → toast "ĐÃ LƯU 5 TỪ VÀO DB!" → verify `vocabulary` rows 50170–50174 `source=AI_GENERATED` → dọn DB sạch.
5. ~~Memory game~~ **ĐÃ XONG (converge round)**: "0 CẶP/14 LẦN THỬ" trước đó là artifact của script lật-lại-cặp; chơi đúng bằng cách đọc `pairId` từ state → **8/8 CẶP, 8 LẦN THỬ, "Đã lưu: 8/8 đúng · +8 điểm"**. Lưu ý: deck 10016/50038/50039 ("Test Deck") có 0 từ → game hiện "Không tải được bộ từ" (đúng hành fail-soft, deck rỗng là dữ liệu test cũ).
6. **Hikari tuning / PagedModel serialization warning**: ghi nhận, chưa đụng (không thuộc DoD).

## 4b. Converge round (sau REPORT gốc)
- **Phát hiện + sửa F16**: `SpeakingDetail.vue` còn 3 class off-token/chết (`text-amber-700`, `text-primary`, `text-destructive` — 2 cái cuối không tồn tại trong tailwind config) → map `warning/accent/danger` (commit `6a520ab`). Scan off-token toàn `frontend/src` giờ = **0 hit**. vitest 73/73, build sạch.
- Skill nạp thêm: `speckit-converge` (đánh giá codebase ↔ spec/plan/tasks, append task).

## 4c. Vòng continue — data repair tận gốc (F17)
- **F17**: 75 dòng `exercise_type='MATCHING'` nhưng options là JSON-array MC và `correct_answer` nằm trong options → dán nhãn sai từ seed. UPDATE → `MULTIPLE_CHOICE` (giữ nguyên options/đáp án, không mất dữ liệu). 1 dòng legacy `:::` (745606) viết lại options sạch + đổi type.
- **F17b**: 71 dòng LISTENING có options thật (không phải placeholder A-D) với đáp án nằm trong options → cùng pattern dán nhãn sai, convert → `MULTIPLE_CHOICE`. 1 dòng (745798) đáp án lệch 1 ký tự so với options → chuẩn hóa đáp án về đúng option rồi convert. LISTENING còn lại: 368 (320 không options → text input + TTS; 38 có audio + options → giữ LISTENING đúng nghĩa; 8 dòng options "A. ..." với đáp án "A" — MC hợp lệ, giữ nguyên vì chọn letter vẫn chấm đúng).
- **F17c**: 137 dòng FILL_BLANK/TRANSLATION cùng pattern (options JSON thật + đáp án nằm trong options) → convert `MULTIPLE_CHOICE`. Phân bố cuối: MC 33540, FILL_BLANK 9045, MATCHING 405, LISTENING 368, TRANSLATION 377. Tổng không đổi 43727.
- Trước khi sửa: backup `exercises_bak_v5` (481 MATCHING) + `exercises_bak_v5b` (322 L/F/T có options). Rollback: UPDATE type từ bảng backup.
- Sau sửa: MATCHING = 333 valid-pipe + 68 empty (UI fallback) + 4 degenerate (UI fallback); MULTIPLE_CHOICE 33331→33403.
- Verified E2E: lesson 41881 Q3 (exercise 755962) giờ là MC với options `["is","am","are"]` → render 3 nút, chọn "is" → ✅ ĐÚNG. Sweep 67/67 pass.

## 4d. Vòng continue 2 — Match-the-user's-fix round (F18/F19)
- **F18 (68 MATCHING options rỗng)**: 55 dòng có đáp án scalar/text → convert `FILL_BLANK` (UI text-input + server exact-match grading); 13 dòng lesson 91900 không có đáp án ở bất kỳ đâu (câu đứt, 0 attempt tham chiếu) → cũng convert `FILL_BLANK` → grade trả `ungradeable` fail-soft, nhất quán với 28 dòng FILL_BLANK anh em cùng lesson. Kết quả: **MATCHING = 332 dòng all-valid** (options `l|r` + answer `l=r`), 0 rỗng, 0 degenerate.
- **F19 (letter-answer grading bug — always-wrong)**: 48 dòng có options thật nhưng `correct_answer` là "A"/"B"/"C"/"D" trong khi UI submit full option text → so exact-match luôn SAI (false-negative toàn bộ). Fix: resolve letter→option text (39 prefix-match `A. / A: / A, / A) / A]`, 3 positional-map theo sanity-check câu hỏi, 1 phonetics 745773 đáp án mojibake → `ˈsɛnsɪz`, 661177 tách `<br>`). 4 dòng degenerate MATCHING (745626/745641/745736/745801/745685/745661) thiếu option-đúng → append `correct_answer` vào options + convert MC.
- Verified E2E grade API: 651442 (trước: mọi câu trả lời SAI) → gửi "B. news" → `correct=true`. Sweep 67/67 pass.
- Backups: `exercises_bak_v5c` (55), `exercises_bak_v5d` (39) + id-list JSON. Phân bố cuối: MC 33547, FILL_BLANK 9112, MATCHING 332, LISTENING 367, TRANSLATION 377 (= 43727, không mất dòng).

## 4e. CLS prod build (T8.4)
- Đo prod build (`vite preview` :4173, cold): **CLS 0.176 → 0.108** sau 2 fix: (1) `html { scrollbar-gutter: stable }` diệt navbar shift 7px; (2) Header/Footer shell luôn mount (`v-if` chuyển vào trong component) + `min-height` reserve.
- **Không phải font FOUT** (giả định cũ sai): prod build CLS ≈ dev → loại font-swap.
- Shift còn lại 0.108 = footer `prev[0,791,1425,96]→[0,0,0,0]` ở t≈200-400ms: Vue mount route async làm main phình → footer bị đẩy ra viewport. Đã thử 4 cách (min-height wrapper, absolute slot, static placeholder, main min-height hard-code) — chỉ hard-code hết shift nhưng tạo khoảng trống thừa trên trang ngắn → **bác bỏ** (đánh đổi UX không chấp nhận được). **Warm cache CLS = 0**; production có HTTP cache/CDN → hành vi warm. Kết luận: 0.108 cold-load là SPA-mount artifact, < 0.25 (Not "Poor"), chấp nhận; nếu muốn <0.1 cần SSR/prerender (ngoài phạm vi).
- vitest 73/73 + build sạch sau khi revert về trạng thái tối ưu.

## 4f. TTS Supertonic local cho bài tập LISTENING do AI sinh (T8.1–T8.3)
- **Kiến trúc**: `supertonic_server.py` — FastAPI adapter đặt trên server chính chủ của supertonic (`create_app`), expose đúng contract Java `SupertonicProxyTtsService` chờ sẵn: `POST /synthesize {text,voice,lang} → WAV`, `GET /health → 200`. Adapter gọi thẳng `_do_synthesize`/`_audio_response` (giữ synth_lock + voice resolution của lib). `supertonic.Dockerfile` (python:3.12-slim, `supertonic[serve]==1.3.1`) + compose service `supertonic` (CPU-only, named volume `supertonic_models`) + env `AI_EXERCISE_TTS_SUPERTONIC_URL=http://supertonic:8001`. Provider flip: `${AI_EXERCISE_TTS_PROVIDER:supertonic}` — rollback = set env về `Z.ai`.
- **Root cause 422 (sửa tận gốc)**: Java `HttpClient` mặc định **HTTP/2** gửi h2c Upgrade; uvicorn chỉ nói HTTP/1.1, quá trình hạ cấp **làm mất POST body** → FastAPI đọc `body=b''` → 422 "Field required". Mọi test từ host (wget/PowerShell/.NET, kể cả chunked) đều 200 vì dùng HTTP/1.1. Fix: `.version(HttpClient.Version.HTTP_1_1)` trong `SupertonicProxyTtsService` (1 dòng, rebuild jar).
- **Đo hiệu năng (P5)**: synth 1 câu ~44 ký tự = **1.1–1.3s** CPU; RAM sidecar **~546–718MB**/3.7GB; model load + health trong 10s (volume cache).
- **E2E đã verify**: admin AI-generate LISTENING (lesson tạm 91916) → log `Supertonic TTS synthesized 264236 bytes` → `TTS audio generated: https://res.cloudinary.com/.../ai-listen-91916-*.wav` → `audio_url` có trong DB (2/2 bài). Dọn lesson sau test. Unit: 4 Mockito tests (unavailable/bytes/throws/null). Sweep 67/67.
- **Fail-soft**: sidecar chết → `isAvailable()` false → bài vẫn lưu không audio → browser TTS fallback (`speech.js`) hoạt động như cũ.
- Commit **b9bbbd3**.

## 4g. Full UI functional gate (T8.5) — 0 lỗi console
- Phạm vi qua UI thật (dev :5173, user@gmail.com + admin@gmail.com): Home, Lessons list/detail (3 tab + grade + submit + history), Decks (6 mode: Flashcard/Quiz/Nghe/Gõ từ/Ghép cặp/Tổng hợp), Videos (player + Shadowing + Quiz), Speaking (list/detail/record), Search (tra từ + kết quả), Profile, Leaderboard, Premium, Admin (dashboard + 6 trang + CRUD tạo lesson).
- **Kết quả: 0 console error/warning trên mọi trang.**
- **2 lỗi data tìm thấy & sửa**: lesson 41881 có 2 câu MC `correct_answer` không nằm trong options (755960: "doesn't like" thay vì "don't"; 755963: "doesn't" thay vì "don't" — sai cả 2 chiều) → UPDATE DB, verify UI trả "✅ Đúng".
- Ghi chú môi trường (không phải bug): speaking mic cần user-gesture thật (headless auto-deny → fail-soft message tiếng Việt lịch sự); :4173 preview bị CORS vì `cors.allowed-origins` dev-only (prod deploy same-origin → không áp dụng).
- Regression cuối: backend **225/225** (baseline 221 + 4 TTS tests mới), frontend **73/73**, API sweep **67/67**, prod build sạch.

## 5. SKILL ĐÃ NẠP
- `speckit-workflow` (pipeline constitution→…→converge) — dùng xuyên suốt, artifacts §1.6.
- (Các skill khác trong catalog như `accessibility`, `performance-optimization` đã có sẵn hướng dẫn tương đương trong constitution + checklist; không cần nạp thêm vì công việc đã đi theo đúng gate của chúng.)

## 6. KHUYẾN NGHỊ BƯỚC TIẾP
1. Đối soát giao dịch SePay thật với sao kê → xác nhận hoặc hoàn tất `ENGF8AB9431CE85`.
2. Chạy data-fix cho các dòng exercise hỏng (tắt guard sau khi sạch).
3. Bật `SPRING_JPA_SHOW_SQL=true` tạm khi debug — giờ chỉ cần env, không sửa code.
4. Đo Core Web Vitals bằng prod build (`vite preview`) để chốt CLS.
