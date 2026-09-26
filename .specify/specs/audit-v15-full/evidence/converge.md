# audit-v15-full — converge

**Ngày:** 2026-09-25 (+07) · Đối chiếu spec ↔ implementation ↔ evidence.

## Yêu cầu (spec) → Trạng thái

| # | Yêu cầu | Trạng thái | Bằng chứng |
|---|---|---|---|
| 1 | Cleanup C1 — xoá 67 user rác + row con | ✅ DONE | `evidence/cleanup-c1c2.log` (67 user, 42 child), parity `…5…` |
| 2 | Cleanup C2 — xoá 114 payment, giữ 12 SUCCESS | ✅ DONE | `evidence/cleanup-c1c2.log`, `parity-after-cleanup.txt` |
| 3 | Migrate 3 câu hỏi → `exercises` | ✅ DONE | `evidence/migration-verify.md` (id 787912–787914, grade 3/3 & 0/3) |
| 4 | Gỡ Đường B — code | ✅ DONE | 16 file backend + 5 file frontend xoá; compile + suite xanh |
| 5 | Gỡ Đường B — DB | ✅ DONE | `evidence/drop-route-b.log` (0 `Msg`), `route_b_tables_remaining=0` |
| 6 | F-13-09 verify + docs | ✅ DONE | `evidence/tz-audit.md` (3 đồng hồ, 0 future study_days, giữ nguyên) |
| R1 | Số đo phiên này | ✅ | `prior-hypotheses.md` 15 dòng (5 giả thuyết SAI đã sửa) |
| R2 | Backup + restore-drill trước DML | ✅ | `cleanup-c1c2.md` T1.1 (VERIFYONLY valid + drill parity khớp) |
| R3 | 5 user thật + 12 SUCCESS còn | ✅ | `cleanup-c1c2.md` T1.7 |
| R4 | tab "Nội dung" 0 section biên soạn | ✅ | `post-removal-live.md` T4.1 + `f15-route-b-removal-live.json` 18/18 |
| R5 | 3 câu hỏi làm được + tab Lịch sử 200 | ✅ | `post-removal-live.md` T4.2/T4.3 |
| R6 | `deleteLesson` vẫn chạy | ✅ | `LessonServicePaginationTest` (4/4) + backend suite 512/0 |
| R7 | Probe 2 mỗi finding + review chéo | ✅ | mỗi F-15-* có "Probe 2"; `review-v15.md` |
| R8 | Dọn rác + liệt kê file đã xoá | ✅ | `cleanup-manifest.md` + REPORT §file đã xoá |

## Mâu thuẫn spec ↔ thực tế (đã xử lý)

| Điểm | Kế hoạch ban đầu | Thực tế đo được | Xử lý |
|---|---|---|---|
| `LessonStructureController` | "xoá cả class" | chứa 3 endpoint **dùng chung** | **tách** → `AdminUploadController` (F-15-02) |
| `AdminExercises.vue` | không nhắc | phụ thuộc `lessonStructureService` | tạo `uploadService.js` (F-15-03) |
| Số lesson có Đường B | 7 | **8** (thêm 11300) | ghi lại trong `prior-hypotheses.md` H6 |
| Tên cột Route B | `id`/`position` | `section_id`/`block_id`/`order_index` | sửa (H5) |
| Tab "Lịch sử" | "snapshot history" | **attempt history** → GIỮ | H13 |
| Parity | `…15…5` (còn 3 bảng) | **`…43738…10`** (không còn snapshot) | script parity mới `sweep/v15/p16-parity.sql` |

## Chưa làm / có lý do

- **Migrate timezone** — quyết định người dùng "chỉ verify + ghi docs" (V2).
- **GitHub issues** — V3.
- **`/api/admin/audio-upload`** — giữ (dùng chung, có test rate-limit phủ) dù 0 consumer frontend hiện tại.

## Kết luận

**Tất cả yêu cầu trong spec đã DONE và có bằng chứng.** Không có yêu cầu nào bị bỏ dở.
