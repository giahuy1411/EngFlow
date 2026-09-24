# audit-v14-full — specification

**Authored:** 2026-09-25 (+07) · **Branch:** `audit-v14-full` · **Checkpoint:** `e729b2c`
**Predecessor:** `audit-v13-full` · **Artifact home:** `.specify/specs/audit-v14-full/`
**Remote:** `github.com/giahuy1411/EngFlow` (KHÔNG tạo issue)

## Why this audit exists

Người dùng yêu cầu một bản **hoàn toàn mới, gắt gao và tỉ mỉ hơn mọi bản trước — more comprehensive,
deeper, end-to-end**: chạy toàn bộ API, drive toàn bộ UI tương ứng từng API, soi 6 chức năng chính
(Bài học/Bài tập, Streak, Đăng nhập/Đăng ký, Tìm kiếm/Sắp xếp, CRUD, AI), audit DB trong Docker, tối
ưu hiệu năng, test UI/UX bằng **cả hai** chrome-devtools-mcp **và** playwright-mcp, verify UI khớp
prompt Playful Geometric với **Be Vietnam Pro**, dùng skill/plugin trong `C:\Users\ASUS\.claude`, tìm
và sửa lỗ hổng của chính prompt, theo pipeline
`constitution → specify → clarify → checklist → plan → tasks → implement → converge → analyze`, chạy
**hai vòng** fix tận gốc, báo cáo trung thực.

### Ba sự thật v14 KHÔNG được "phát hiện lại" (đã đúng, đo trong phiên khảo sát)

| Sự thật | Bằng chứng |
|---|---|
| Be Vietnam Pro **đã là font duy nhất** | `frontend/index.html:54`; `tailwind.config.js:80-85`; `design-system.css --geo-font`; grep Outfit/Plus Jakarta = **0** |
| Playful Geometric **đã triển khai đủ** | `--geo-*` tokens, `shadow-pop-*`, border 2px, hero sun/blob, dashed connector, pricing scale(1.1)+badge, marquee, reduced-motion |
| Lỗ hổng contrast của prompt **đã vá ở v11/v12** | tầng `--geo-*-ink`/`--geo-*-strong` với tỷ lệ đo được ghi trong CSS |

v14 đo lại bằng probe của chính nó; khớp = **xác nhận**, lệch = **finding**.

### Ba khoảng trống THẬT của v13 mà v14 phải đóng

1. **Inventory không reconcile**: `endpoint-inventory.json.expandedRows=148` nhưng `api-sweep.json`
   = 143 pass + 1 blocked + 3 N/A = **147** → 1 hàng không status; R2 của v13 không chứng minh được.
2. **118 vi phạm tap-target AA đo rồi bỏ** khỏi `REPORT.md` §1.
3. **Số perf của v13 đo khi stack động** (backend rebuild+restart 2 lần trong cửa sổ perf; cùng
   endpoint 47.1 ms vs 14.4 ms).

## Objective

1. Tái lập baseline bằng đo của chính phiên này.
2. Sweep **toàn bộ** API (132 annotation → 148 row / 26 controller), đúng role, real body, kiểm
   **hợp đồng response**, authorization **hai chiều**, và **reconcile 148/148 có status**.
3. **E2E 3 tầng** cho 6 chức năng chính: UI thật → network call → API contract → **hàng DB đổi**.
4. Audit DB trong Docker như **dữ liệu**, gồm **hiệu lực constraint** (INSERT vi phạm phải bị chặn,
   trong scratch DB).
5. Hiệu năng **trước/sau**, chỉ tối ưu khi số biện minh (P5); số phải đo trên **stack tĩnh**.
6. Verify design system + **tìm lỗ hổng của chính prompt**.
7. **Fix mọi lỗi đo được tận gốc, kèm regression test.**
8. **Vòng 2, loop-until-dry**: dừng khi 2 vòng liên tiếp không thêm lỗi mới.
9. **Triage toàn bộ tap-target** (đóng khoảng trống v13).
10. **Dọn rác + liệt kê file đã xoá** (ràng buộc người dùng).
11. Báo cáo trung thực: đã làm / chưa làm / fix & cách fix / skill dùng / file đã xoá / giới hạn.

## Requirements

- **R1 — mọi con số là đo của phiên này.** Kết luận cũ là giả thuyết để falsify, không phải nguồn.
- **R2 — API surface được *chạy*, và **reconcile 148/148**.** Mỗi entry có status; 2xx không đủ —
  field name + type được kiểm; role violation test **hai chiều**.
- **R3 — UI được *đối chiếu* với API.** Mỗi chức năng: drive UI thật, bắt network call, so hợp đồng
  API, xác nhận hàng DB đổi (assert **đẳng thức**).
- **R4 — DB audit như dữ liệu.** Parity đo lại; constraint kiểm **hiệu lực**; orphan scan tách NULL FK.
- **R5 — design system verify + tìm lỗ hổng prompt.** Claim nào vi phạm a11y là defect kèm tỷ lệ đo.
- **R6 — vòng 2 rộng hơn vòng 1, lặp đến khi cạn** (2 vòng liên tiếp 0 lỗi mới).
- **R7 — kỷ luật bằng chứng, không cap im lặng.** Mọi claim trỏ artifact; chưa verify = BLOCKED/PARTIAL
  kèm lý do; lỗi của probe cũng là defect để ghi.
- **R8 — chỉ dữ liệu THẬT.** Không mock để chứng minh kết quả. Mock chỉ trong unit test và phải ghi rõ.
- **R9 — mọi fix AI sinh + mọi liên kết UI↔API↔DB mới đều review chéo** (adversarial) trước khi đóng.
- **R10 — mỗi finding có probe thứ 2 độc lập** (bác/xác nhận) trước khi đặt status cuối.
- **R11 — dọn rác + liệt kê.** Mọi file/folder rác sinh trong phiên phải xoá; REPORT liệt kê verbatim.
- **R12 — đo hiệu năng trên stack tĩnh.** Assert container `StartedAt` không đổi suốt cửa sổ perf.

## Out of scope (nêu để không bị bỏ im)

- Di chuyển tiền thật qua SePay (chỉ create-order + webhook sai chữ ký; biên giới ghi lại).
- Triển khai A2/A3 của Lesson Builder (F-13-02) — cần quyết định chủ sản phẩm (V2).
- Migrate timezone DB (F-13-09) — không có consumer thứ hai (V2).
- Tạo GitHub issue (V3).
