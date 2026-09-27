# audit-v18-full — MCP engine availability (probe phiên này, 2026-09-27)

Người dùng chốt (Q2): dùng **CẢ HAI engine cho MỌI luồng** ở Phase U.

## chrome-devtools MCP — DÙNG ĐƯỢC

Gọi thật trong phiên:
- `list_pages` → 1 page `about:blank`.
- `navigate_page { pageId:1, type:url, url:"http://localhost:5173/" }` → **Successfully navigated**;
  title `EngFlow - Nền tảng Tự học Tiếng Anh Miễn phí`.

Tools khả dụng: `new_page`, `navigate_page`, `take_snapshot`, `evaluate_script`, `list_console_messages`,
`list_network_requests`, `get_network_request`, `take_screenshot`, `emulate`, `resize_page`, `lighthouse_audit`,
`performance_start_trace`, `click`, `fill`, `fill_form`, `hover`, `press_key`, `wait_for`, `handle_dialog`,
`upload_file`, `take_heapsnapshot`.

## Playwright MCP — DÙNG ĐƯỢC (đã UNBLOCK từ v17 closing round)

Gọi thật trong phiên:
```
mcp__plugin_playwright_playwright__browser_navigate { url: "http://localhost:5173/" }
→ Page URL: http://localhost:5173/
  Page Title: EngFlow - Nền tảng Tự học Tiếng Anh Miễn phí
```
Cấu hình đã chứng minh (v17): env `PLAYWRIGHT_MCP_EXECUTABLE_PATH="C:/Program Files/BraveSoftware/Brave-Browser/Application/brave.exe"`
trong `~/.claude/settings.json` → `env`, cộng `--executable-path` trong plugin `.mcp.json`. **Cả hai engine chạy được
trong phiên này.**

## Engine thứ ba (cross-validation, không thay thế MCP)

Harness headless `playwright-core` + Chromium bundle (`ui-sweep.js`, `design-v2.js`, `cls-probe.js`) — dùng cho
sweep hàng loạt (Phase 3), KHÔNG thay thế tương tác thủ công MCP (Phase U).

## Kỷ luật

- **MỌI luồng Phase U chạy LẦN LƯỢT BẰNG CẢ 2 ENGINE**, đối chiếu kết quả 2 engine với nhau.
- Nếu engine nào fail giữa phiên → ghi **BLOCKED + lỗi thật**, KHÔNG thay thế im lặng.
