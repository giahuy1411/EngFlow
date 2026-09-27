# audit-v19-full — W1: AI gloss tiếng Trung (fix tận gốc)

**Ngày:** 2026-09-27 · **Files:** `src/main/java/com/datn/engflow/service/AiVocabService.java`,
`src/test/java/com/datn/engflow/service/AiVocabServiceLanguageGuardTest.java`

## Đính chính v18

v18 báo *"AI model nhỏ trả gloss tiếng Trung — hạn chế model, không phải bug code"* → **SAI một phần**:
khảo sát chỉ ra prompt **không nêu ngôn ngữ** cho field nào (chỉ có *tên key* `definitionVi`), và **không có
guard CJK nào** trong toàn bộ `src/main/java`. → **Là bug code có thể sửa.**

## Root cause

`AiVocabService.generateVocabByTopic` (`:76-82`) + `enrichWord` (`:141-147`): prompt tiếng Anh, không nói
`definitionVi` phải là **tiếng Việt**; `definitionEn`/`definitionVi` chỉ được phân biệt bằng **tên key**.
Model `qwen2.5:1.5b` (zh-centric) tự do điền slot đó bằng tiếng Trung. **Không** validator nào chặn.
Gloss chảy thẳng: parse → `Vocabulary.meaning` (`:101`/`:164`) → JSON → DB → game/flashcard/SRS.

## Bằng chứng BEFORE (tái hiện thật)

- HTTP (batch đầu): `ocean life`→"鱼", `weather`→"雨水", `music`→"一种起源…", `health`→"健康".
- **Playwright MCP** (`/ai-vocab-generator`, chủ đề "weather"): UI hiện **天气 / 气候** — ảnh
  `shots/mcp/playwright/U15-ai-cjk-before.png`.
- Ngắt quãng: 4/10 lần đầu, 0/25 lần sau → **không thể chỉ dựa prompt**.

## Fix 2 lớp (tận gốc)

1. **Prompt** (cả 2 method): ghi rõ từng field — `definitionEn` **in ENGLISH**, `definitionVi`
   **in VIETNAMESE / tiếng Việt** — cộng dòng **"STRICT LANGUAGE RULES: … NO Chinese characters (汉字)
   anywhere. … never Chinese."** (theo pattern đã có ở `AiPromptService.java:67`).
2. **Guard tất định** `containsCjk(String)`: dải `U+4E00–U+9FFF` + `U+3400–U+4DBF`. Nếu `definitionVi` hoặc
   `definitionEn` chứa CJK → **loại item** (generate) / **từ chối** (enrich). Vì model 1.5b **không đủ tin cậy**
   để prompt một mình là đủ.

## Bằng chứng AFTER (đo thật, live)

Rebuild container → gọi `POST /api/ai/generate-vocab` **×15** chủ đề:
```
call1..15: items=3 (hoặc 0 khi guard loại hết) cjk_items=0  []
```
→ **0 CJK** trên mọi call. Các call `items=0` = guard đã **loại** item CJK (đúng thiết kế), không phải lỗi.

## Test + mutation-test

- `AiVocabServiceLanguageGuardTest` **4/4 PASS** (detect CJK; KHÔNG false-positive tiếng Việt/Anh; dấu tiếng
  Việt là Latin Extended Additional không bị nhầm; lenient parse vẫn chạy).
- **Mutation-test:** sửa `containsCjk` → luôn `false` → test **FAIL 1** (detectCjk) → test thật sự bảo vệ;
  khôi phục → 4/4 PASS.

## Kết luận

**FIXED.** Bug code thật (không phải "hạn chế model không sửa được"). Prompt + guard tất định; 0 CJK live ×15;
test mutation-tested. `enrichWord` cũng được sửa cùng.
