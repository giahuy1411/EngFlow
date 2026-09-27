# audit-v17-full — CLOSING ROUND: C1–C6 (đóng nốt 5 mục "Còn lại")

**Ngày:** 2026-09-27 (+07) · **Nhánh:** `audit-v15-full` · **Tiền đề:** `e7a1182` (v17 hội tụ)
**Yêu cầu:** đóng nốt danh sách "Còn lại"; chốt C2 sau khi kiểm tra nguồn dữ liệu tra từ.

---

## C1 — Playwright MCP chạy được (env route)

**Vấn đề gốc:** tool trong session lỗi `Chromium distribution 'chrome' is not found` vì plugin
`playwright@claude-plugins-official` spawn `npx @playwright/mcp@latest` **không kèm** đường dẫn browser.
Override `mcpServers.playwright` ở project scope **không** thay server của plugin (tool vẫn tên
`mcp__plugin_playwright_playwright__*`).

**Đã làm (cấu hình harness, KHÔNG vào repo):**
1. `C:\Users\ASUS\.claude\settings.json` → `env.PLAYWRIGHT_MCP_EXECUTABLE_PATH =
   C:/Program Files/BraveSoftware/Brave-Browser/Application/brave.exe` (env toàn cục — plugin kế thừa).
2. `…/plugins/cache/claude-plugins-official/playwright/fa59bc903774/.mcp.json` → thêm
   `--executable-path <brave>` (belt-and-braces).

**Bằng chứng (đo thật):** chạy đúng lệnh plugin (`npx @playwright/mcp@latest`, **không** args) với env kế thừa:
```
navigate result: {"result":{"content":[{"text":"…await page.goto('http://localhost:5173/login');\n
  ### Open tabs\n- 0: (current) [EngFlow …](http://localhost:5173/login)"}]}}
chrome-not-found? false
C1 ENV ROUTE PASS: true
```
Trước fix, cùng lệnh cho: `Error: Chromium distribution 'chrome' is not found …`. Sau fix: `false` + navigate OK.
*(Tool MCP trong session này vẫn cần restart session để nạp lại — MCP spawn lúc bắt đầu session.)*

---

## C2 — Tra từ: từ điển là nguồn DUY NHẤT (chốt: gỡ local khỏi đường tra, giữ bảng)

**Quyết định người dùng:** giữ API bên thứ 3 làm chính; **chỉ** gỡ local khỏi **đường tra từ**;
**giữ nguyên** bảng `vocabulary` + `deck_words` + `user_vocabulary_progress` (bảng là kho **bộ từ**:
100/118 hàng nằm trong `deck_words`).

**Đã gỡ khỏi đường tra:**
- `frontend/src/services/vocabularyService.js`: bỏ `localExact()` **và** bước DB fallback
  (`/api/vocabulary/search`) → chỉ còn proxy `/api/vocabulary/dictionary/{word}` → direct browser.
  Bỏ luôn helper `mapBackendRows` (không còn ai dùng).
- `VocabularyController.search`: bỏ `@RequestParam exact`; `VocabularyRepository`: bỏ `findByWordIgnoreCase`
  (đo được: **chỉ** fast path dùng).

**Chặn trần chờ:** `DICT_BUDGET_MS = 6000` — quá 6 s trả TIMEOUT (UI hiện "Tra cứu quá lâu…"), nhưng request
**vẫn chạy tiếp** để warm Redis cache. **Không** hạ read-timeout backend (bài học F-17-05).

**Đo thật:**
| Phép đo | Kết quả |
|---|---|
| proxy cold (`serendipity`) | **19.99 s** |
| proxy warm (cùng từ) | **0.032 s** |
| UI `/search` "serendipity" | trả entry giàu: phonetic, nút audio, NOUN, 2 định nghĩa, syn/ant |
| network khi tra | **chỉ** `GET /api/vocabulary/dictionary/serendipity` — **KHÔNG** có `/api/vocabulary/search` |
| UI tra từ không tồn tại | hiện "Tra cứu quá lâu…" (bounded) — **không** báo "Không tìm thấy" sai |

**Không hồi quy (chứng minh bảng `vocabulary` nguyên vẹn):**
- UI `/decks` → "10 bộ"; `/decks/10007` (AWL) → 10 từ đọc **từ bảng** (ANALYZE, CONCEPT, EVIDENT…).
- `api-sweep`: decks/srs/flashcard/game/ai **145/0/0**.
- Test: `vocabulary-search-order` (5) + `vocabulary-search-errors` (4) xanh; **mutation-test** chứng minh
  (thêm lại fast path → ca "không gọi exact=true" FAIL).

**Giới hạn nói thật:** lần đầu một từ mới **không thể <1 s** nếu upstream chậm; trần chờ chỉ ngăn *treo*,
không tạo kết quả. Bảng `vocabulary` **vẫn còn** — chỉ không còn là fallback tra từ.

---

## C3 — F-17-16: `_config.js` default

`arg("audit", "audit-v15-full")` → **`audit-v17-full`**; cập nhật comment. Thêm **check 6** vào
`assert-harness.js`: FAIL nếu default không trỏ một thư mục có thật dưới `.specify/specs/`.
Chạy: `PASS  _config.js default audit round exists  -- default=audit-v17-full`.

## C4 — F-17-17: `focused-probe.js`

Hardcode `.specify/specs/audit-v15-full/evidence/` → `require("./_config.js").OUT`. Thêm **check 7**:
cấm literal `.specify/specs/audit-vN-full` ngoài `_config.js`. Chạy:
`PASS  no harness hardcodes an audit round path outside _config.js`.

---

## C5 — Speaking bằng GIỌNG NGƯỜI THẬT

**Phát hiện:** object MinIO `speaking-uploads/video-attempts/lesson-1/line-0/64357411-…` (**563 524 B**,
RIFF/WAVE) — **giọng người thật**. Verify trước khi tin: Whisper :9002 đọc ra **đúng** `referenceText` của
prompt 50007.

**Probe `sweep/harness/g8-speaking-human-audio.py` (tự dọn):**
```
human audio: bytes=563524
upload: 200 id=40053
assess: 200 status=COMPLETED transcript_len=177 scoreTotal=9.7
cleanup: row deleted=1 ; minio: Removed `…_audit-v17-human.wav`
cleanup: study_days rows removed=1
minio copy gone: True | source human object intact: True (bytes 563524 -> 563524)
reference/transcript word-set recall: 0.97
G8 RESULT: PASS (HUMAN recording -> real transcript matching the reference; cleanup verified)
```
→ Chấm **giọng người thật** qua cả chuỗi MinIO → Whisper → alignment → Ollama; recall **0.97**; dọn đủ
(row + object + study_days), object gốc **nguyên vẹn**.

**Giới hạn nói thật:** file người thật **có sẵn local**, **không phải** phiên production.

---

## C6 — SePay: replay window + gỡ `blocked` lỗi thời

- `g6-sepay-signed.py`: thêm ca **chữ ký HỢP LỆ nhưng timestamp cũ >5 phút** → phải bị từ chối; đưa vào `ok`.
  Kết quả: `webhook(stale-but-valid sig): 200 {"error":"Invalid signature","success":false} -> rejected: True`,
  `G6 RESULT: PASS` (kèm: valid sig → SUCCESS, bad sig → rejected, premium restored).
- `sweep/v12/api-sweep.js`: bỏ dòng `blocked("webhook with VALID HMAC signature", …)` **lỗi thời**; thay bằng
  **assert thật** (không đụng tiền thật): ký HỢP LỆ một payload có orderCode **không** tồn tại → server qua
  `isSignatureValid` rồi trả `"No pending order"` (**không** mutate hàng nào); + replay cũ → `Invalid signature`.
  `api-sweep`: **145 pass / 0 fail / 0 blocked** (trước: 143/0/1 blocked).

**Giới hạn nói thật:** vẫn **secret local**, **không phải** tiền thật / production.

---

## Kết quả tổng

> **Tất cả số dưới đây chạy lại trên CÂY LÀM VIỆC HIỆN TẠI (sau mọi fix C1–C6)**, log thật đã lưu cạnh file này.

| Suite | Kết quả | Log |
|---|---|---|
| Backend (JUnit) | **520 / 0 / 0 / 11** — BUILD SUCCESS | `backend-closing.log` |
| Frontend (Vitest) | **192 pass / 1 skip** (32 file) | `frontend-closing.log` |
| Frontend build | ✓ built in **5.81 s** | `build-closing.log` |
| API sweep | **145 / 0 / 0** (2 N/A) | `api-sweep.json` |
| deep-probe | **58 / 0 / 0** | `deep-probe.json` |
| ui-sweep | exit 0 — `contrastFails:0, consoleErrors:0, apiErrors:0, guardFails:0, overflowRoutes:0` | `ui-sweep.json` |
| parity | `1470\|43738\|5\|118\|29\|4\|3\|12\|10` + `STUDY_DAYS=4 PENDING_PAYMENTS=0 EXERCISE_ATTEMPTS=33` → **CLEAN** | `p16-parity.sql` |
| G6 (SePay) | **PASS** (replay window asserted) | `g6-sepay.md` |
| G8 (speaking human) | **PASS** (recall 0.97, tự dọn) | `g8-speaking-human.md` |

**0 regression.** Bảng `vocabulary` nguyên vẹn (decks/SRS/game/flashcard/AI xanh).

### Ghi chú provenance (tự review chéo bắt — đã sửa)

Vòng kiểm chứng độc lập (workflow 6 verifier + 1 critic) phát hiện các **claim provenance yếu** trong bản nháp
đầu của file này; đã **sửa để mọi số có log thật trên cây hiện tại**:

- **`9.39 s` không có log nào chứa** (grep toàn repo = 0 hit) → **gỡ**; thay bằng build thật **5.81 s** (`build-closing.log`).
- Số "final" cũ (`backend-final.log` 23:25, `frontend-final4.log` 23:47) **cũ hơn code C2** (00:34–01:04) →
  **chạy lại** backend + frontend + build trên cây hiện tại, lưu `*-closing.log`.
- `g8-speaking-human.md` ghi WAV "44.1 kHz" — header thật là **22 050 Hz** (`0x5622`, mono 16-bit) → **đã sửa**.
