# audit-v15-full — checklist (unit test cho requirements)

- [ ] **R1** Mọi con số là đo của phiên này (baseline, parity, leak)
- [ ] **R2** DML: backup + restore-drill trước; xoá theo ID liệt kê
- [ ] **R3** 5 user thật + `admin@gmail.com` nguyên vẹn; 12 payment SUCCESS còn
- [ ] **R4** tab "Nội dung" `/lessons/447` = **0** section "Nội dung biên soạn"; `GET /structure` → 404
- [ ] **R5** 3 câu hỏi cũ (block 4/5/7) làm được ở tab "Bài tập"; tab "Lịch sử" 200
- [ ] **R6** `deleteLesson` vẫn chạy sau DROP (không FK 547)
- [ ] **R7** probe thứ 2 mỗi finding; review chéo mọi fix
- [ ] **R8** cleanup manifest đầy đủ; `git status` sạch; REPORT liệt kê file đã xoá
- [ ] **Shared endpoints** `/api/admin/upload` + `/api/resources/**` vẫn chạy (`AuditV8UploadXssTest` xanh)
- [ ] **AdminExercises** vẫn upload ảnh được sau khi gỡ Đường B
- [ ] **Bẫy AGENTS.md:** flush rate-limit; `QUOTED_IDENTIFIER ON` + quét `Msg`; backup trước DML; ID liệt kê
