# audit-v19-full — MCP engine availability (probe phiên này)

Người dùng chốt: **CẢ HAI engine cho MỌI luồng** (gồm Premium).

## chrome-devtools MCP — DÙNG ĐƯỢC
`list_pages` → 1 page đang mở `http://localhost:5173/`. Tools: navigate/snapshot/click/fill/fill_form/
evaluate_script/list_console_messages/list_network_requests/get_network_request/take_screenshot/resize_page/
lighthouse_audit/performance_start_trace.

## Playwright MCP — DÙNG ĐƯỢC (Brave)
`browser_navigate { url: "http://localhost:5173/" }` → **Page URL: http://localhost:5173/**, title
`EngFlow - Nền tảng Tự học Tiếng Anh Miễn phí`.

## Quan sát: bounce `/` → `/login?redirect=/` khi token hết hạn (KHÔNG phải bug)
Lần navigate đầu, Playwright còn giữ `localStorage.token` từ phiên v18 (JWT TTL **15 phút** đã hết). `App.vue:52`
gọi `auth.fetchUser()` khi `isLoggedIn` → `/api/auth/me` trả **401** → interceptor `api.js` bounce về
`/login?redirect=/`. Sau khi `localStorage.clear()` + reload → `/` render đúng (h1 "Học Tiếng AnhVui Vẻ").
**Đây là hành vi ĐÚNG của "401 giữa phiên → logout"** (khớp `mcp-walkthrough` v17/v18), không phải regression.
→ Nhắc nhở: harness/Phase U phải dùng token MỚI (TTL 15'), không tái dùng token cũ.
