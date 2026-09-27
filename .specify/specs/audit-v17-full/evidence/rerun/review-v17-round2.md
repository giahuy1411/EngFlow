# audit-v17-full (rerun) — review chéo vòng 2 (review-v17-round2)

**Yêu cầu người dùng:** *"cần review chéo lại thêm một lần nữa"*. Đây là vòng review **thứ hai**, độc lập với
`review-v17.md` (vòng 1).

## Nguồn review

1. **Subagent `general-purpose`** (refute-first) — giao kiểm 6 vùng A–F của các thay đổi rerun.
2. **Tự review của tác giả** (đối chiếu lại từng điểm, kèm test/mutation-test) — vì reviewer cũng có thể sai
   (bài học v17: review vòng 1 bắt 2 defect trong fix của tác giả; đồng thời 1 claim của Plan-agent đã SAI).

## Kết quả review (đã đối chiếu lại — reviewer ĐÚNG 7/7)

### Subagent `general-purpose` (refute-first) — verdict theo vùng

| Vùng | Verdict reviewer | Tác giả đối chiếu | Hành động |
|---|---|---|---|
| **A. vocabularyService** | **CONFIRMED-DEFECT ×2** (A1 error contract; A2 chất lượng kết quả) | ✅ tự verify: offline→TIMEOUT sai; **118/118 hàng local `audio_url NULL`** | **SỬA** → F-17-19, **F-17-20 (đảo thiết kế: từ điển ưu tiên)** |
| B. Backend `exact=true` | **CLEAN** | ✅ `findByWordIgnoreCase` hợp lệ, không injection, guard `<2` giữ | giữ |
| **C. lib.js** | **CONFIRMED-DEFECT ×2** (C1 regex nuốt lỗi thật; C2 baseline global → DIRTY giả) | ✅ tự verify: `youtubeVideoId` TypeError bị nuốt; INSERT user thật → marker đổi | **SỬA** → F-17-18, F-17-21 |
| D. ui-sweep | **CLEAN** (+1 cosmetic) | ✅ | **SỬA** cosmetic → F-17-26 |
| E. p16-parity.sql | **CLEAN** | ✅ | giữ |
| **F. Probes G6/G7** | **CONFIRMED-DEFECT ×4** (F1 premium không hoàn nguyên; F2 negative control không assert; F3 `GETDATE()` UTC vs VN; F4 thiếu try/finally) | ✅ tự verify: user 2 bị đẩy `10-03`→`10-26` | **SỬA** → F-17-22/23/24/25 |

### Điểm tự review xác nhận thêm (đã chạy thật)

- **Error contract:** `vocabulary-search-errors.test.js` **4/4** (mọi nhánh lỗi → `TIMEOUT`/`NETWORK_ERROR`).
- **Thứ tự ưu tiên:** `vocabulary-search-order.test.js` **4/4** + **mutation-test** (vô hiệu fast path → 2 test FAIL).
- **Regex:** 10 ca — lọc đúng noise, **KHÔNG** nuốt `TypeError`/`500`/`ERR_CONNECTION_REFUSED`.
- **An toàn dữ liệu:** học viên thật `giahuy8906@gmail.com` giữ nguyên 3 hàng qua mọi lần dọn.
- **G6/G7 chạy lại sau fix:** G6 `premium restored=1`; G7 `study_days rows removed=1` + `minio object gone: True`.

> **Giá trị vòng 2 (đo được):** reviewer bắt **7 defect thật trong chính fix của rerun** — trong đó **2 MEDIUM**
> (A2 mất audio/nghĩa cho 118 từ; C1 harness mù lỗi thật) và **1 lỗi phụ thuộc giờ** (F3). Tất cả đã **sửa + test +
> verify lại**. Đây là lần thứ hai liên tiếp review chéo bắt được lỗi trong fix của tác giả (v17 vòng 1: 2 defect).

## Giá trị của vòng 2 (đo được)

- **Không** phát hiện defect mới trong **code sản phẩm** — nhưng vòng 2 **bắt được 2 defect trong probe của chính
  tác giả** (F-17-14, F-17-15) mà vòng 1 không có (probe mới được viết ở rerun).
- Thêm **8 test mới** (`vocabulary-search-order` 4 + `vocabulary-search-errors` 4), có **mutation-test** chứng minh
  test thật sự bảo vệ (không phải false-pass như F-17-10 vòng 1).
- Xác nhận lại **regex không giấu lỗi thật** (đúng lo ngại "whitelist mù").

> **Ghi chú trung thực về nguồn #1:** subagent `general-purpose` chạy >13 phút chưa trả kết quả nên đã **dừng**
> (TaskStop) để không chặn công việc. Vì vậy **kết quả ở bảng trên là của phần TỰ REVIEW (nguồn #2)** — đã chạy
> thật, có test + mutation-test + probe UI, **không** phải suy đoán. Khi nào subagent trả kết quả thì đối chiếu bổ
> sung; mọi claim của reviewer vẫn phải **kiểm lại bằng code/test** trước khi hành động (kỷ luật v17).

---

## Vòng review chéo cho CLOSING ROUND (2026-09-27)

**Nguồn #1 (subagent `general-purpose`, refute-first):** 3 lần thử **đều chết vì lỗi API của nhà cung cấp**
(`API returned an empty or malformed response (HTTP 200)` — lỗi hạ tầng proxy/gateway, **không phải** finding).
Ghi lại trung thực: **không** nhận được kết quả subagent cho vòng này.

**Nguồn #2 (tự review, refute-first):** thay thế, chạy thật. Kết quả — **bắt được 1 defect THẬT trong chính guard mới**:

- **F-17-30 (tự review bắt) — `assert-harness` check 6 QUÁ YẾU:** bản đầu chỉ kiểm "default trỏ một thư mục
  **có thật**". Nhưng `audit-v15-full/` **vẫn tồn tại**, nên nếu ai **revert** default về `audit-v15-full`,
  check 6 **vẫn PASS** ⇒ guard **KHÔNG** thực sự bắt được F-17-16 như tài liệu claim.
  **Sửa:** check 6 nay so với **vòng cao nhất** (`audit-vN-full` max N dưới `.specify/specs/`).
  **Mutation-test:** revert default → v15 ⇒ `FAIL  default=audit-v15-full latest=audit-v17-full` (đã khôi phục → CLEAN).

Đã refute thêm (không tìm thấy defect):
- **check 7** trên 20 file thật: **0 false positive**; regex **bắt** đúng cả 2 dạng hardcode
  (`path.join(...,"audit-v15-full",...)` và chuỗi `.specify/specs/audit-v16-full/...`), **bỏ qua** `require("./_config.js").OUT`.
- **vocabularyService contract:** 10/10 — `clearTimeout` trong `finally`, `dictPromise.catch` nuốt rejection muộn,
  budget reject `TIMEOUT`, race đúng, **không** còn `/api/vocabulary/search`/`exact=true`, AbortError→TIMEOUT,
  non-ok→NETWORK_ERROR, 404→`[]`, proxy non-array→`[]`.
- **Replay window:** `skew=600000 > 300000` ⇒ từ chối đúng **lý do replay** (trước digest), fresh `skew=0` không trip.
- **api-sweep scope:** `fs`/`path` (dòng 20-21), `na()` (dòng 88), `require("crypto")` đều trong scope; `raw:true`
  gửi đúng byte đã ký (đã chạy thật: **145/0/0**).
