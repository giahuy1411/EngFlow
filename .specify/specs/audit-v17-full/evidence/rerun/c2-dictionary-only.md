# audit-v17-full (closing round) — C2: tra từ = từ điển là nguồn DUY NHẤT

**Quyết định người dùng (chốt):** giữ API bên thứ 3 (`dictionaryapi.dev`) làm chính; **chỉ** gỡ local khỏi
**đường tra từ**; **giữ nguyên** bảng `vocabulary` + `deck_words` + `user_vocabulary_progress` (bảng là kho
**bộ từ** — 100/118 hàng nằm trong `deck_words`).

## Vì sao gỡ (đo được)

| Tầng | Nguồn | Vai trò |
|---|---|---|
| 1 | `dictionaryapi.dev` qua proxy `/api/vocabulary/dictionary/{word}` (cache Redis 1 h) | **CHÍNH** — giàu: `serendipity` → phonetic + audio + NOUN + 2 định nghĩa + syn/ant |
| 2 (đã gỡ) | Bảng `vocabulary` local (118 hàng) | **0/118 có audio**, 1 nghĩa → làm **suy giảm** câu trả lời |

## Thay đổi

- `frontend/src/services/vocabularyService.js`: bỏ `localExact()` **và** bước DB fallback
  (`/api/vocabulary/search`) → chỉ còn proxy từ điển → direct browser (retry 1). Bỏ helper `mapBackendRows`.
- `VocabularyController.search`: bỏ `@RequestParam exact` (luôn `findByWordContainingIgnoreCase`).
- `VocabularyRepository`: bỏ `findByWordIgnoreCase` (đo: **chỉ** fast path dùng).
- **Chặn trần chờ** `DICT_BUDGET_MS = 6000`: quá 6 s → TIMEOUT (UI: "Tra cứu quá lâu…") nhưng request **vẫn chạy**
  để warm cache. **Không** hạ read-timeout backend.

## Đo thật (trước/sau)

| Phép đo | Kết quả |
|---|---|
| proxy cold (`serendipity`) | **19.99 s** |
| proxy warm (cùng từ) | **0.032 s** |
| UI `/search` "serendipity" | entry giàu (phonetic + nút audio + NOUN + 2 định nghĩa + syn/ant) |
| network khi tra | **chỉ** `GET /api/vocabulary/dictionary/serendipity` — **KHÔNG** `/api/vocabulary/search` |
| UI tra từ không tồn tại (`zzzqqqnotaword`) | "Tra cứu quá lâu…" (bounded) — **không** "Không tìm thấy" sai |

Bằng chứng network (chrome-devtools MCP), sau 2 lần tra:
```
reqid=189 GET /api/vocabulary/dictionary/serendipity   [200]
reqid=191 GET /api/vocabulary/dictionary/zzzqqqnotaword [200]
reqid=193 GET /api/vocabulary/dictionary/zzzqqqnotaword [200]
```
Không có request nào tới `/api/vocabulary/search` ⇒ bảng local **không** còn trên đường tra.

## Không hồi quy — bảng `vocabulary` nguyên vẹn

- UI `/decks` → "10 bộ"; `/decks/10007` (AWL) → **10 từ đọc từ bảng** (ANALYZE, CONCEPT, EVIDENT, METHOD,
  CATEGORY, ECONOMY, IDENTIFY, OCCUR, PERIOD, STRUCTURE).
- `api-sweep` decks/srs/flashcard/game/ai = **145/0/0**.
- Test `vocabulary-search-order` (5) + `vocabulary-search-errors` (4) xanh; **mutation-test**: thêm lại fast path
  `exact=true` → ca "NEVER requests the local exact fast path" **FAIL** (đã khôi phục).

## Giới hạn nói thật

- Lần đầu một từ mới **không thể <1 s** nếu upstream chậm; trần chờ chỉ ngăn *treo*, không tạo kết quả từ hư không.
- Bảng `vocabulary` **vẫn còn** (cho Decks/SRS/game/flashcard/AI) — chỉ không còn là fallback tra từ.
- **Từ điển KHÔNG cache kết quả "không có từ"** (`DictionaryService` `@Cacheable(unless = "#result == '[]'")`):
  một từ 404 (hoặc timeout nội bộ backend 30 s) trả `"[]"` ⇒ **không** vào cache ⇒ lần tra lại cùng từ đó vẫn
  tốn thời gian. Đây là **hành vi có sẵn** (cố ý: để một từ được thêm sau vẫn tra được), **không** phải thứ vòng
  closing thay đổi; trần chờ 6 s của UI chỉ giới hạn thời gian *chờ*, không sửa được chi phí upstream.
