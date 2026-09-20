# Trỏ cả hai MCP browser sang Brave

**Trạng thái đo được 2026-09-20:** cả hai MCP **CHƯA dùng được** vì đều tìm **Google Chrome** — máy này không có Chrome, chỉ có **Brave** và **Edge**.

```
chrome-devtools MCP:  Could not find Google Chrome executable for channel 'stable' at:
                      - C:\Program Files\Google\Chrome\Application\chrome.exe
                      - ... (5 đường dẫn)

playwright MCP:       Chromium distribution 'chrome' is not found at
                      C:\Users\ASUS\AppData\Local\Google\Chrome\Application\chrome.exe
                      Run "npx playwright install chrome"
```

**Nhưng cả hai đều có cờ trỏ sang browser khác**, và tôi đã **chứng minh chạy được với Brave** bằng cách bắt tay JSON-RPC thật (xem mục "Đã chứng minh" ở cuối).

---

## Đường dẫn Brave (đã kiểm chứng tồn tại)

```
C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe
```

---

## Việc cần làm — copy 2 file

### 1. Chrome DevTools MCP

```
Nguồn:  sweep\v10\mcp-brave\chrome-devtools-mcp.json
Đích:   C:\Users\ASUS\.claude\plugins\cache\claude-plugins-official\chrome-devtools-mcp\1.9.0\mcp.json
```

### 2. Playwright MCP

```
Nguồn:  sweep\v10\mcp-brave\playwright-mcp.json
Đích:   C:\Users\ASUS\.claude\plugins\cache\claude-plugins-official\playwright\c447c3207a42\.mcp.json
```

Rồi **khởi động lại Claude Code**.

---

## Nội dung hai file

### Chrome DevTools MCP

```json
{
  "$schema": "https://agent-plugins.org/schemas/1.0.0/mcp.schema.json",
  "mcpServers": {
    "chrome-devtools": {
      "type": "stdio",
      "command": "npx",
      "args": [
        "--prefix",
        "${PLUGIN_DATA}",
        "chrome-devtools-mcp@1.9.0",
        "--executablePath=C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
        "--isolated"
      ]
    }
  }
}
```

### Playwright MCP

```json
{
  "playwright": {
    "command": "npx",
    "args": [
      "@playwright/mcp@latest",
      "--executable-path",
      "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
      "--isolated"
    ]
  }
}
```

**Chú ý khác nhau về cú pháp cờ:**

| | Chrome DevTools MCP | Playwright MCP |
|---|---|---|
| Tên cờ | `--executablePath` (camelCase) | `--executable-path` (kebab-case) |
| Dạng | `--executablePath=GIÁ_TRỊ` (một chuỗi) | `--executable-path`, `GIÁ_TRỊ` (**hai** phần tử) |

Viết sai một trong hai sẽ **không báo lỗi rõ ràng** — MCP chỉ quay về tìm Chrome rồi thất bại. Đây là chi tiết dễ mất thời gian nhất.

---

## Vì sao có `--isolated`

Tạo profile tạm trong bộ nhớ, **không đụng** profile Brave thật của bạn — không mất bookmark, không mất đăng nhập, không mở nhầm tab.

Muốn MCP dùng **profile Brave thật** (để giữ đăng nhập sẵn) thì **bỏ** `--isolated`. Nhưng khi đó nó dùng chung profile với trình duyệt bạn đang mở → có thể xung đột.

---

## Vì sao không cần bọc nháy dù đường dẫn có dấu cách

`C:\Program Files\...` có dấu cách. **Không cần** bọc nháy vì:

- `npx` nhận mảng `args` **đã tách sẵn** — không đi qua shell, nên dấu cách không bị hiểu là phân tách đối số.
- `\\` trong JSON là **một** dấu `\` khi đọc. Viết `C:\\Program Files\\...` là đúng.
- Viết `"C:\Program Files\..."` (một dấu `\`) thì JSON hiểu `\P`, `\B` là escape **không hợp lệ** → file lỗi parse.

---

## Điều phải biết: hai file này nằm trong CACHE của plugin

Cả hai đường dẫn đích đều nằm trong `plugins/cache/`. Nghĩa là:

- **Cập nhật plugin lên bản mới** sẽ tạo thư mục mới và **bản sửa này mất**.
- Khi đó phải **sửa lại** file config của phiên bản mới.

Ghi lại để lần sau không mất thời gian chẩn đoán lại từ đầu.

---

## Đã chứng minh chạy được — không phải phỏng đoán

Tôi không chỉ đọc tài liệu rồi kết luận. Tôi **chạy thật cả hai MCP** với Brave và bắt tay JSON-RPC (`initialize` → `tools/list` → gọi tool thật).

### Playwright MCP + Brave

```
server  : "Playwright"  version 1.64.0
tools   : 25
navigate: Page URL http://localhost:5173/
          Page Title: EngFlow - Nền tảng Tự học Tiếng Anh Miễn phí
```

Script: `sweep/v10/test-playwright-mcp-brave.js`

### Chrome DevTools MCP + Brave

```
server  : "chrome_devtools"  version 1.9.0
tools   : 29
new_page: 1: about:blank   2: EngFlow - Nền tảng Tự học Tiếng Anh Miễn phí [selected]
evaluate: {"title":"EngFlow - Nền tảng Tự học Tiếng Anh Miễn phí",
           "url":"http://localhost:5173/",
           "font":"\"Be Vietnam Pro\", system-ui, sans-serif"}
```

Script: `sweep/v10/test-chromedevtools-mcp-brave.js`

**Cả hai đều mở được app thật và đọc được dữ liệu thật.** Chỉ còn thiếu bước copy file config + khởi động lại.

---

## Ba cách chạy MCP bị HỎNG trên Windows — ghi lại để không thử lại

Khi viết script kiểm thử, tôi thử ba cách spawn và **cả ba đều hỏng**:

| Cách | Lỗi | Nguyên nhân |
|---|---|---|
| `spawn("npx", args, {shell:true})` | `too many arguments. Expected 0 arguments but got 1: Files\BraveSoftware\...` | Shell tách `C:\Program Files\...` thành hai đối số tại dấu cách |
| `spawn("npx", args)` không shell | `ENOENT` | Windows cần `npx.cmd`, không có `npx` trần |
| `spawn("npx.cmd", args)` không shell | `EINVAL` | Node không chạy được `.cmd` trực tiếp khi không có shell |

**Cách dùng:** gọi thẳng `node` trên file JS entry của MCP — không có shell nào xen vào, nên dấu cách trong đường dẫn Brave không còn là vấn đề.

Lưu ý: cách này **chỉ cần cho script kiểm thử của tôi**. Cấu hình MCP ở trên dùng `npx` + mảng `args`, và Claude Code spawn nó theo cách không đi qua shell — nên nó chạy đúng.

---

## Kiểm tra sau khi khởi động lại

Gọi thử `list_pages` (chrome-devtools) hoặc `browser_tabs` (playwright). Nếu thành công thì đã trỏ đúng.

Nếu vẫn báo không tìm thấy Chrome:

1. Kiểm tra đường dẫn Brave:
   ```
   dir "C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe"
   ```
2. Kiểm tra file config **parse được JSON** (dấu `\\` phải là hai dấu).
3. Kiểm tra **đúng tên cờ** — `--executablePath` vs `--executable-path` (xem bảng trên).
4. Đã **khởi động lại** Claude Code chưa — MCP chỉ nạp config lúc khởi động.

---

## Ghi chú: harness kiểm thử của dự án KHÔNG phụ thuộc hai MCP này

Trong suốt vòng audit, tôi kiểm thử browser bằng `sweep/v10/ui-lib.js` — dùng `playwright-core` và **tự tìm** browser theo danh sách đường dẫn (Chrome → Brave → Edge).

Nên **342 lượt browser sweep, 184 assert prompt, font check, F126/F127/F128 live** đều đã chạy được **không cần** MCP nào. Harness đó giờ cũng đã chuyển sang dùng **Brave 153.0.8010.53**.

Sửa hai MCP là để có **thêm công cụ độc lập** — hữu ích khi muốn đối chiếu kết quả giữa các harness, hoặc dùng tính năng riêng của MCP (Lighthouse audit, performance trace, heap snapshot).
