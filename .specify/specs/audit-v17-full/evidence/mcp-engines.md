# audit-v17-full — MCP engine availability (probe phiên này)

## chrome-devtools MCP — DÙNG ĐƯỢC

Tools khả dụng và đã gọi thật trong phiên: `new_page`, `navigate_page`, `take_snapshot`, `evaluate_script`,
`list_console_messages`, `list_network_requests`, `take_screenshot`, `emulate`, `resize_page`, `lighthouse_audit`,
`performance_start_trace`. → **Engine chính** cho Phase U.

## Playwright MCP — BLOCKED (giới hạn môi trường, KHÔNG thay thế im lặng)

**Gọi thật trong phiên:**
```
mcp__plugin_playwright_playwright__browser_navigate { url: "http://localhost:5173/login" }
→ Error: async initializeServer: Chromium distribution 'chrome' is not found at
   C:\Users\ASUS\AppData\Local\Google\Chrome\Application\chrome.exe
   Run "npx playwright install chrome"
```

→ **Giống hệt v16** (`analyze.md`: "MCP playwright không dùng được — thiếu Chrome channel"). Đây là **giới hạn
môi trường**, không phải lỗi app. Ghi **BLOCKED**, không bịa kết quả.

**Engine thứ hai THAY THẾ (hợp lệ):** harness headless dùng `playwright-core` + Chromium bundle
(`C:\Users\ASUS\AppData\Local\ms-playwright\chromium-1237\chrome-win64\chrome.exe`) — **chạy được thật**
(`ui-sweep.js`, `design-v2.js`, `cls-probe.js` đều chạy trong phiên này). Vì là cùng engine Playwright nhưng
khác driver, nó đóng vai **cross-validation độc lập** cho chrome-devtools MCP ở các luồng trọng yếu.

> **Kỷ luật R11:** mọi luồng trọng yếu chạy (a) chrome-devtools MCP tương tác thật, và (b) harness headless
> playwright-core — hai đường độc lập. Playwright MCP ghi BLOCKED kèm lỗi thật ở trên.
