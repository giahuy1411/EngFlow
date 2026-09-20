# audit-v12-full — converge (bù việc chưa xong)

**Ngày:** 2026-09-21 (+07) · Đối chiếu codebase + evidence với `spec.md` / `plan.md` / `tasks.md`, rồi **bổ sung
việc còn thiếu**.

---

## 1. Đối chiếu requirement → artifact

| # | Requirement (spec.md) | Artifact | Trạng thái |
|---|---|---|---|
| R1 | Mọi con số là đo của phiên này | `evidence/baseline.md`, mọi evidence | ✅ |
| R2 | API surface được **chạy** (131 endpoint), kiểm hợp đồng, role 2 chiều | `evidence/api-sweep.md`, `endpoint-inventory.json` | ✅ |
| R3 | UI **đối chiếu** với API, không test rời | `evidence/ui-sweep.md` §4 | ✅ |
| R4 | DB trong Docker audit như dữ liệu, constraint **có hiệu lực** | `evidence/db-audit.md` §1 | ✅ |
| R5 | Design system verify, tìm lỗ hổng của prompt | `evidence/ui-sweep.md` §1, `spec.md` H1–H12 | ✅ |
| R6 | Vòng 2 rộng hơn, lặp đến cạn lỗi | `evidence/second-pass.md` | ✅ |
| R7 | Kỷ luật bằng chứng, không cap im lặng | mọi evidence; `findings.md` §probe bugs | ✅ |

## 2. Việc trong `plan.md` đã làm / chưa làm

| Phase | Trạng thái | Ghi chú |
|---|---|---|
| 0 — freeze & baseline | ✅ | 7 task, artifact `baseline.md` |
| 1 — DB audit | ✅ | gồm T1.1 (hiệu lực constraint) mà v11 để OPEN |
| 2 — API sweep | ✅ | **131 endpoint phủ hết**, 12 controller v11 bỏ sót nay có probe |
| 3 — UI sweep 2 MCP | ✅ | chrome-devtools dùng thật (v11 chỉ `list_pages`) |
| 4 — CRUD qua UI admin | ✅ | lesson + deck, verify UI lẫn API, 0 rác |
| 5 — fix lỗi đo được | ✅ | F146, F149 fix; F147/F148/F150 ghi OPEN/owner |
| 6 — perf trước/sau | ✅ | 22 endpoint, từ chối tối ưu có số |
| 7 — vòng 2 | ✅ | 0 lỗi mới; 3 lỗi probe bị falsify |
| 8 — đóng | ✅ | analyze + converge + secret scan + issues + commit |

## 3. Việc CHƯA làm — ghi rõ, không giấu

| Hạng mục | Trạng thái | Lý do |
|---|---|---|
| **F147** `POST /api/vocabulary` mở cho mọi user ghi bảng global | **OPEN** | **Quyết định sản phẩm** — hai hướng fix ngược nhau; đã đo blast-radius và đề xuất cả hai, chờ owner |
| **F148** `/api/srs/*` 0 caller frontend | **OPEN** | Giữ / bổ sung UI / retire — quyết định owner |
| **F150** CLS 0.104 trên `/` | **OPEN** | Đã truy gốc tới 1 phần tử, đã đo 1 phương án fix (0.104 → 0.098, không đủ) và **revert**; fix thật cần đổi kiến trúc render |
| **C6** dependency Azure Speech SDK (~15MB, 0 import) | **OWNER DECISION** | Xoá cần rebuild + full test; không tự quyết |
| Giao dịch ngân hàng thật với SePay | **BLOCKED by design** | Là hành động thanh toán thật, không phải test. Đã kiểm phía nhận (chữ ký sai → reject) |
| Chấm điểm phát âm bằng giọng thật | **N/A** | Mic giả phát im lặng → Whisper trả text rỗng → `FAILED` **đúng thiết kế** |
| `POST /api/admin/lessons/{id}/snapshots` + `/restore` | **N/A** | Ghi snapshot / khôi phục nội dung thật — tránh mutate content |
| `POST /api/v1/speaking-submissions/upload` + `/assess` | **N/A** | Cần media MinIO thật; v11 đã có `speaking-record-test.js` 11/11 |
| `POST /api/auth/avatar/upload` | **N/A** | Cần media Cloudinary thật |

## 4. Việc bổ sung sau khi đối chiếu (converge)

Đối chiếu cho thấy **không requirement nào chưa có artifact**. Ba mục mới được bổ sung trong quá trình:

1. **`sweep/v12/streak-scenarios.js`** — bản v12 sửa probe drift KB4 (đọc cutover từ DB thay vì tính "hôm qua").
2. **`frontend/src/views/admin/AdminLessons.placeholder-contrast.test.js`** — regression test cho F149.
3. **`sweep/v12/perf-probe.js`** — mở rộng 9 endpoint mới (leaderboard/progress/game/SRS/flashcard/prompts/video/payment).

## 5. Không có "cap im lặng"

Mọi sweep có giới hạn đều **ghi rõ đã bỏ gì**:
- API sweep: 3 endpoint **N/A** + 1 **BLOCKED** với lý do (mục §3 trên).
- Perf: đo 22/131 endpoint (hot read path + path mới) — **không** đo toàn bộ 131 (vô nghĩa cho mục đích perf).
- UI: 20 route × 3 role (không phải cả 39 route) + responsive 3 route × 5 width — đủ phủ mọi loại guard và layout.

**Verdict: CONVERGED.** Mọi requirement có artifact; hạng mục chưa đóng đều được ghi rõ với lý do và phân loại
(owner-decision / blocked-by-design / N-A).
