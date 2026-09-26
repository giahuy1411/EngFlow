# audit-v17-full (rerun) — G1: Playwright MCP unblock

## Vấn đề (đo lại)

`mcp__plugin_playwright_playwright__browser_navigate` →
```
Error: async initializeServer: Chromium distribution 'chrome' is not found at
C:\Users\ASUS\AppData\Local\Google\Chrome\Application\chrome.exe
```

## Root cause (đọc cấu hình thật)

Plugin `playwright@claude-plugins-official` khai server trong
`C:\Users\ASUS\.claude\plugins\cache\claude-plugins-official\playwright\fa59bc903774\.mcp.json`:
```json
{ "playwright": { "command": "npx", "args": ["@playwright/mcp@latest"] } }
```
Không có cờ chọn browser ⇒ mặc định = **chrome channel** ⇒ tìm `…\Google\Chrome\Application\chrome.exe` (máy không có).

## Cách sửa (đã áp, đã chứng minh)

`@playwright/mcp@0.0.82` hỗ trợ `--executable-path <path>` (env `PLAYWRIGHT_MCP_EXECUTABLE_PATH`). Thêm override
`mcpServers.playwright` vào **project scope** của `C:\Users\ASUS\.claude.json` (3 biến thể hoa/thường của đường dẫn
repo engflow).

**Browser chọn cuối cùng = Brave** (`C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe`):
```json
"playwright": { "command": "npx",
  "args": ["@playwright/mcp@latest", "--executable-path",
           "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe"] }
```

**Vì sao Brave (không phải Chromium bundle):** `@playwright/mcp@latest` = 0.0.82 → `playwright-core@1.64.0-alpha`,
pin chromium revision **1246**; bundle trên máy chỉ có **1234** (của playwright-core 1.62.1) ⇒ trỏ vào bundle là
**lệch version** (mỗi lần `@latest` bump lại hỏng). Brave là browser mà **chrome-devtools MCP (sibling, đang chạy
được)** đã cấu hình sẵn:
`…\chrome-devtools-mcp\1.9.0\mcp.json` → `--executablePath=…\Brave-Browser\Application\brave.exe`. Dùng cùng
browser ⇒ cùng môi trường đã được chứng minh trên máy này.
(Backup: `C:\Users\ASUS\.claude.json.bak-audit-v17`.)

## Bằng chứng (probe độc lập, ngoài session)

Spawn trực tiếp MCP server với `--executable-path` (Brave) rồi gọi `browser_navigate`:
```
navigate: {"result":{"content":[{"type":"text","text":"### Ran Playwright code
await page.goto('http://localhost:5173/lessons');
### Page
- Page URL: http://localhost:5173/lessons
- Page Title: EngFlow - Nền tảng Tự học Tiếng Anh Miễn phí
### Snapshot …
chrome-not-found? false
PASS: true
```
→ Server khởi động OK và **điều hướng thật tới `/lessons`**. **Fix đúng.**
(Đã thử cả `chromium-1234` và Brave: cả hai đều chạy; chọn Brave vì bền version.)

## Ghi chú quan trọng

- Bundle trên máy: **`chromium-1234`** (playwright-core 1.62.1) — bản `chromium-1237` từng có đã bị xoá trong phiên.
  Nhưng `@playwright/mcp@latest` cần revision **1246** ⇒ dùng **Brave** cho bền (xem trên).
- `npx playwright install chrome` **thất bại**: `Failed to install Google Chrome … insufficient privileges` (cần
  Administrator) → dùng `--executable-path` là đường đúng.
- **Tool trong session hiện tại vẫn báo lỗi cũ** vì MCP server được spawn **lúc bắt đầu session** và đang giữ args cũ.
  Cấu hình đã đúng cho **lần khởi động session kế tiếp**; đã chứng minh bằng probe ngoài session.
- Đây là **cấu hình harness của Claude Code**, KHÔNG nằm trong repo ⇒ không commit.
