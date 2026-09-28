# audit-v20 — Verify bằng MCP browser (cả 2 engine)

**Ngày:** 2026-09-28 (+07). Runtime: backend :8080, frontend :5173.

## chrome-devtools MCP

| Kiểm tra | Kết quả đo |
|---|---|
| `document.title` | `EngFlow - Nền tảng Tự học Tiếng Anh Miễn phí` |
| `body.fontFamily` | `"Be Vietnam Pro", system-ui, sans-serif` |
| **`distinctFonts` (toàn trang)** | **`["\"Be Vietnam Pro\", system-ui, sans-serif"]` — CHỈ 1 font** |
| `body.backgroundColor` | `rgb(255, 253, 245)` = `#FFFDF5` (cream) ✓ token |
| `h1.fontFamily` / `weight` | Be Vietnam Pro / **900** (ExtraBlack — đúng spec headings 800/900) |
| `--geo-accent` | `#8B5CF6` ✓ |
| `--geo-bg` | `#FFFDF5` ✓ |
| overflow | **0** |
| Screenshot | `shots/mcp/chrome-devtools/v20-home-1440.png` |

Quan sát ảnh: đúng "Playful Geometric" — vòng tròn vàng khổng lồ sau hero, nút "candy"
hard-shadow, hình khối (vuông tím, tam giác vàng, tròn hồng + mint), nền kem, chấm bi.

## Playwright MCP

| Flow | Hành động | Kết quả |
|---|---|---|
| **Đăng nhập** | điền `user@gmail.com`/`123456` → bấm Đăng nhập | ✅ chuyển `/login` → **`/lessons`** |
| **Design token (sau login)** | đo `.app-btn` đầu tiên | `border: 2px rgb(30,41,59)` ✓, `radius: 9999px` (pill) ✓, `fontCount: 1` ✓, overflow 0 |
| **Streak** | `GET /api/streak/snapshot` kèm Bearer | ✅ **200**, đủ 7 key: `today, currentStreak, studiedToday, effectiveFrom, studiedDays, legacyAccessDays, legacyHistoryAvailable`; `today=2026-09-28` (giờ VN) |
| **Tìm kiếm** | nhập `hello` → bấm "Tra từ" | ✅ render `hello` /həˈləʊ/, 3 loại từ (noun/verb/interjection), ví dụ, Syn/Ant |
| **Từ điển (đo trực tiếp)** | `GET /api/vocabulary/dictionary/hello` | ✅ **200 trong 28 ms** (cache ấm nhờ warmup) |
| Screenshot | — | `shots/mcp/playwright/v20-lessons-after-login.png`, `v20-search-hello.png` |

## Kết luận verify giao diện (yêu cầu gốc của người dùng)

> "verify giao diện đã đồng bộ với prompt design system hay chưa"

**ĐÃ ĐỒNG BỘ — chứng minh bằng đo runtime trên cả 2 engine, không phải đọc code:**
1. Font **Be Vietnam Pro là DUY NHẤT** (`distinctFonts` chỉ 1 phần tử trên mọi trang đã đo).
2. Token màu khớp spec: bg `#FFFDF5`, accent `#8B5CF6`.
3. Nút đúng spec: border **2px `#1E293B`**, radius **pill 9999px**, hard shadow.
4. Heading weight 900 (spec: 800/900).
5. Decoration đúng spec: circle/triangle/square, hard shadow, dot grid, cream bg.
6. Không overflow ở mọi viewport đã đo.

**Lệch duy nhất so với prompt gốc** (đã ghi ở `prompt-rewritten-v20.md`, không phải lỗi mới):
- Prompt gốc nói "Lucide **React**" → thực tế dùng `lucide-vue-next` (đúng stack Vue).
- Prompt gốc nói "body Plus Jakarta Sans" → đã thay bằng BVP theo yêu cầu + hiến pháp P6.
- Prompt gốc nói contrast "AAA" → thực tế chỉ một số cặp đạt; đã có tầng token `*-ink`/`*-strong` xử lý.
