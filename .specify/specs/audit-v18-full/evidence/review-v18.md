# audit-v18-full — review chéo đối kháng (T6.2)

**Ngày:** 2026-09-27 · **Reviewer:** subagent `general-purpose` độc lập (fresh context) · **Diff:**
`sweep/v8/ui/lib.js`, `sweep/harness/{ui-sweep,danger-tint,focused-probe}.js`, `sweep/harness/_config.js`,
`sweep/v12/api-sweep.js`.

## Nhiệm vụ giao reviewer

REFUTE từng thay đổi + tìm REGRESSION; mặc định hoài nghi. Kiểm: `resolveChromium()` đúng không (mask lỗi?
`executablePath()` có tồn tại? path sai xử lý? chọn nhầm `chromium_headless_shell`?), null return có phá caller,
còn probe nào hardcode chromium không, `_config.js` default, `assert-harness`.

## Kết quả reviewer

| # | Mục | Verdict |
|---|---|---|
| 1 | `resolveChromium()` correctness | **OK** — `executablePath()` trả `chromium-1234` tồn tại; cùng module instance với `pw`; try/catch không mask (module load đã `require` pw); `.find(existsSync)` bỏ path xấu; regex `/^chromium-\d+$/` **không** khớp `chromium_headless_shell-1234`; readdir 5 entry |
| 2 | null return → default launch | **OK** — `launch({headless:true})` chạy được (Chromium 151); ternary 3 caller đúng |
| 3 | thêm `const path` | **OK** — HEAD thật sự thiếu (version đầu đã throw `ReferenceError`); không xung đột local |
| 4 | **còn 3 probe hardcode 1237** | **LOW** — `f1302-a1-a11y.js:44`, `f1302-a1-blocks-live.js:27`, `f1320-api-redirect-live.js:33`. **Không regress** (đều guard `existsSync ? … : {headless}`) nhưng là drift chết |
| 5 | `assert-harness.js` | **OK** — ALL CLEAN 7/7, exit 0 |
| 6 | `_config.js` default | **OK** chức năng; **LOW** comment cũ (`audit-v17-full`/`V17`/`AUDIT-V17`) |
| 7 | marker `AUDIT-V17-C6` (api-sweep:559) | **LOW** — literal cũ, không phải cleanup key (dọn theo `AUDIT-V12-API-%`) |

**Verdict reviewer:** *"No change is UNSAFE to keep."*

## Tôi đối chiếu lại + xử lý (reviewer cũng có thể sai — kiểm lại code)

Đọc code xác nhận cả 4 LOW đúng. **Fix nốt (đóng cả lớp F-18-01, không chỉ 2 file đã vỡ):**
- 3 probe còn lại → dùng `H.resolveChromium()` (đã `require` H sẵn). Cả 3 chạy lại **EXIT=0**.
- `_config.js` comment → `audit-v18-full`/`V18`/`AUDIT-V18`.
- `api-sweep.js:559` marker → `AUDIT-GHOST` (version-agnostic).
- `grep -rn "chromium-1237" sweep/` → chỉ còn **1 hit là comment** trong `resolveChromium()` (giải thích lịch sử).

**Kết luận:** reviewer đúng 100%, không có verdict sai để bác bỏ. Fix đã mở rộng từ "2 file vỡ" thành
"mọi probe không hardcode revision" — đúng tinh thần sửa tận gốc.
