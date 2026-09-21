# audit-v12-full — analyze (đối chiếu chéo artifact, read-only)

**Ngày:** 2026-09-21 (+07) · Phương pháp: đối chiếu `spec ↔ plan ↔ tasks ↔ findings ↔ evidence ↔ code`.
Không sửa gì trong bước này.

---

## 1. Tính nhất quán giữa các artifact

| Cặp đối chiếu | Kết quả |
|---|---|
| `spec.md` R1–R7 ↔ `evidence/*` | ✅ mỗi requirement trỏ tới ≥1 artifact |
| `plan.md` Phase 0–8 ↔ `tasks.md` | ✅ cùng số phase, cùng tên |
| `tasks.md` `[x]` ↔ artifact tồn tại | ✅ 53 mục `[x]`; mỗi mục nêu artifact hoặc số đo |
| `findings.md` ↔ `evidence/` | ✅ mỗi finding nêu probe sinh ra nó |
| `findings.md` F146 ↔ README/CLAUDE.md đã sửa | ✅ grep xác nhận 0 drift còn lại |
| `findings.md` F149 ↔ regression test | ✅ test tồn tại + pass |
| `constitution.md` P1 ↔ baseline đo | ✅ P1 nêu 485/119·23, khớp `.p0-v12-*.log` |
| `clarify.md` C1–C10 ↔ quyết định đã thực hiện | ✅ mỗi quyết định có hành động tương ứng |
| `checklist.md` A–E ↔ spec | ✅ không mục nào bỏ trống |

## 2. Số liệu có mâu thuẫn không?

| Số | Nguồn 1 | Nguồn 2 | Khớp? |
|---|---|---|---|
| Endpoint | inventory script: 131 annotation / 147 row | `grep -rhoE "@(Get\|Post…)"` = 131 | ✅ |
| Backend test | `.p0-v12-backend-test.log` = 485 | `.p5-v12-backend-test.log` = 485 | ✅ |
| Frontend test | `.p0` = 119/23 file | `.p5` = 119/24 file (+1 test mới) | ✅ (giải thích được) |
| Bundle | 177.44 kB | 177.44 kB sau fix | ✅ |
| Parity | baseline | sau mọi run | ✅ khớp `1471\|…\|5` |
| Contrast `/admin/lessons` | vòng 1: 2 | vòng 2: 0 | ✅ (F149 fix) |
| FK count | v12 đo: 25 | v11 ghi: 26 | ⚠️ **lệch — đã ghi là PHÁT HIỆN** ở `db-audit.md` §6 |

**Chỉ một lệch**, đã ghi rõ và phân loại (v11 đếm dư 1; đếm tay cũng ra 25; không có DDL giữa hai phiên). Không ảnh
hưởng kết luận.

## 3. Requirement coverage — có lỗ hổng không?

| Requirement | Bằng chứng | Đủ chưa? |
|---|---|---|
| "quét toàn bộ codebase" | inventory 26 controller + design-system file set | ✅ (ý định là độ phủ, không phải walk 337 file) |
| "chạy toàn bộ API" | 131/131 có status (PASS/N-A/BLOCKED) | ✅ |
| "tương tác toàn bộ UI tương ứng API" | UI↔API cross-check + route×role + CRUD UI | ✅ |
| "6 chức năng chính" | mỗi cái có dòng API + UI | ✅ |
| "kiểm DB trong Docker" | Phase 1 + hiệu lực constraint | ✅ |
| "tối ưu hiệu năng" | Phase 6 before/after, từ chối có số | ✅ |
| "chrome-devtools-mcp + playwright-mcp" | cả hai dùng thật | ✅ |
| "verify UI khớp design system + font BVP" | DOM đo: 1 font, token khớp, decor hiện diện | ✅ |
| "dùng skill/plugin" | bảng skill ở report + tasks | ✅ |
| "kiểm prompt có lỗ hổng, sửa" | H1–H12 | ✅ |
| "workflow constitution→…→taskstoissues" | artifact đủ thứ tự | ✅ |
| "chạy 2 vòng, sửa tận gốc" | Phase 7 loop-until-dry | ✅ |
| "báo cáo chi tiết" | `REPORT.md` | ✅ |

## 4. Phát hiện qua phân tích (không phải lỗi sản phẩm)

1. **11 ứng viên lỗi bị falsify** (V1–V11) — tỉ lệ cao hơn v11 (10), phản ánh việc v12 mở rộng sang 12 controller
   và đo các trạng thái mới (tap-target ngoại lệ, CLS, cutover theo lịch). Mỗi cái đều được ghi để không ai "sửa"
   code đang đúng.
2. **`?sort=` không tác dụng** — xác nhận lại lần 2, vẫn là **product gap** (UI không có nút sắp xếp).
3. **`study_days` = 2** — không phải rác; là hoạt động học thật của admin + student hôm nay (0 orphan).

## 5. Rủi ro còn lại

| Rủi ro | Mức | Ghi chú |
|---|---|---|
| F147 authz chưa quyết | MEDIUM | Cần owner chọn hướng; đã đo blast-radius |
| F150 CLS chưa fix | LOW | 0.104, một route, cần đổi kiến trúc render |
| F148 API mồ côi | LOW | Không ảnh hưởng người dùng |
| C6 dep thừa 15MB | LOW | Chỉ ảnh hưởng kích thước build |

## Verdict

**ANALYZE: PASS.** Không có artifact nào mâu thuẫn mà không giải thích; mọi requirement có bằng chứng; các hạng mục
chưa đóng đều được ghi rõ và phân loại. Một lệch số (FK 25 vs 26) được ghi là **phát hiện**, không bị che.

---

## Addendum Phase 10 (2026-09-21) — số liệu mới

| Hạng mục | Phase 5 | **Phase 10** | Ghi chú |
|---|---|---|---|
| Backend test | 485 | **496** | +4 `SrsDueWordsAuthzTest` (F151), +6 `VocabularyServiceTest` (F147), +4 `FlashcardServiceStudyActivityTest` viết lại (F148), −3 thay thế |
| Frontend test | 119 / 24 file | **128 / 26 file** | +3 `srsService.test.js`, +4 `DueReview.test.js`; +2 file |
| Build entry | 177.44 kB | **177.64 kB** | +chunk `DueReview` 8.41 kB (lazy) |
| API sweep | 133/0/0 | **137/0/0** | +4 assert F147 (harness nay tự cấp phát deck) |
| **Parity** | `1471\|43735\|72\|127\|28\|15\|4\|126\|14\|5` | **`1470\|43734\|72\|118\|28\|15\|4\|126\|10\|5`** | **Đổi có chủ ý** — dọn 9 vocab rác + 4 deck test + 1 lesson |

**Finding mới:** F151 (IDOR `/api/srs/due/{deckId}`) — không có trong audit gốc, phát hiện khi làm Item A. Đã fix + verify.
