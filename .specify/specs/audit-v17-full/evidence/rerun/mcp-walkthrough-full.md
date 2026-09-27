# audit-v17-full (rerun) — Phase U': đi sâu logic MỌI chức năng qua UI/UX (MCP)

> **⚠️ RIÊNG dòng U'-6 (tra từ F-17-05) ĐÃ THAY THẾ (closing round 2026-09-27):** đường tra từ nay **chỉ** dùng
> từ điển (đã gỡ `search?exact=true`). Xem `c2-dictionary-only.md`. Các dòng khác vẫn đúng.

chrome-devtools MCP (engine 1) + probe `playwright-core` (engine 2). Mỗi luồng: bấm thật → console → network →
đối chiếu API↔UI. **Dọn residue trong cùng run.**

## Đã kiểm trong phiên này (ngoài 22 luồng của v17)

| # | Chức năng | Thao tác thật | Kết quả |
|---|---|---|---|
| U'-1 | Admin CRUD bài học | tạo draft `AUDIT-V17-UI-CRUD` → tìm trong list → xoá | **create 200 (id=124436) · found=1 · delete 204**; lessons về **1470** |
| U'-2 | Deck CRUD | tạo `AUDIT-V17-UI-DECK` → đọc → xoá | **200 / 200 / 200**; decks về baseline, 0 residue |
| U'-3 | SRS | `/api/srs/due/10006` → review `quality=4` | **due=10, review 200** "Review recorded successfully"; **ghi `study_days`** → đã dọn |
| U'-4 | Game | tạo phiên quiz | session OK |
| U'-5 | AI vocab (Ollama) | `topic=weather, count=3` | **200 trong 6.2 s, đúng 3 từ** (`wind`,`cloud`) — trần `count` (F84) giữ |
| U'-6 | Tra từ (F-17-05) | gõ `ambitious` → Tra từ | **1 request** `search?exact=true` 200, render tức thì (không gọi từ điển) |
| U'-7 | Speaking list + record | `/speaking` (7 đề) → `/speaking/50007/record` | render passage + `enable-microphone-button` |
| U'-8 | Mid-session 401 | JWT hết hạn → điều hướng | **`/login?redirect=/`** (đúng contract) |

## Giới hạn (ghi thật)

- **Mic trong browser tự động không xin được quyền** (cần `--use-fake-ui-for-media-stream`, AGENTS.md) ⇒
  không bấm được chuỗi record→stop→submit **trong UI**. **Bù lại:** toàn bộ pipeline đã được chứng minh thật bằng
  **G7** (TTS WAV thật → upload → assess → `COMPLETED`, transcript 181 ký tự, score 9.7).
- Engine 2 (Playwright MCP) đã **unblock và chứng minh chạy** (G1) nhưng **tool trong session này** cần restart để
  nạp cấu hình mới; probe ngoài session đã xác nhận `browser_navigate` → `/lessons` OK.

## Residue đã dọn trong run này

`study_days` 40091 (G7 assess), 40094 (SRS); `exercise_attempts` (harness tự dọn); payment row (G6 tự dọn);
speaking row + MinIO object (G7 tự dọn); lesson 124436 + deck 70211 (U'-1/U'-2). Parity cuối không đổi.
