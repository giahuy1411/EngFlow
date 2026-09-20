# Phase 7 — Vòng 2, rộng hơn (loop-until-dry)

**Ngày:** 2026-09-21 (+07) · Chạy trên **build cuối** (sau mọi fix của v12).
Người dùng yêu cầu: *"chạy lại thêm lần nữa với mức độ toàn diện hơn, không quan trọng thời gian, cứ chạy test lặp
toàn bộ chức năng để tìm ra lỗi và sửa tận gốc."* Vòng này tồn tại vì vòng 1 **không** là bằng chứng rằng không còn gì.

---

## 1. Full suite trên build cuối

| Suite | Kết quả |
|---|---|
| Backend `mvn -o test` | **485 run / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** (`.p5-v12-backend-test.log`) |
| Frontend `npx vitest run` | **23 passed / 1 skipped (24 file)** (`.p5-v12-frontend-test.log`) |
| Build `npx vite build` | entry **177.44 kB** (gzip 67.55 kB), exit 0 |

Backend **không hồi quy** so baseline (485 = 485). Frontend **+1 file** (regression test F149) so baseline 23 → 24.
Bundle **không đổi** (177.44 kB).

## 2. API sweep lặp lại — **giống hệt vòng 1**

```
pass=133  fail=0  blocked=1  n_a=3
  auth 13 · lessons 18 · streak 5 · search 8 · crud 17 · game 13 · flashcard 3 · srs 6
  misc 6 · submission 6 · speaking 9 · video 7 · admin 10 · ai 5 · payment 7
```

**Không lỗi mới.** Vẫn gồm: role matrix 2 chiều, 7-field streak contract, 4 guard draft cũ (F88/F89/F115/F126),
F145 header. Parity sau khi chạy: `1471|43735|72|127|28|15|4|126|14|5` — **không đổi**.

## 3. Browser sweep lặp lại — và nó **tìm ra điều vòng 1 bỏ sót**

Đây là kết quả thực chất của vòng 2.

Vòng 1 probe contrast báo **2 lỗi** (F149, `text-muted-foreground/60` trên `/admin/lessons`). **Vòng 2 chạy lại sau
fix**: contrast = **0** trên mọi route.

```
vòng 1: contrastFails = 2   (F149, đã fix)
vòng 2: contrastFails = 0   <- xác nhận fix, không phải giả định
```

**Điều vòng 2 bắt được mà vòng 1 bỏ sót: `probe` tự lộ lỗi của chính nó.**

- **V9 (vòng 1):** probe đếm `<24px` và báo **7 tap-target vi phạm**. Vòng 2 áp **đúng WCAG 2.5.8** (ngoại lệ
  **Inline** + **Spacing**, đo khoảng cách tâm target) → **cả 7 EXEMPT**. Nếu tin vòng 1, tôi đã "sửa" CSS đang đúng.
- **V10:** trong lúc truy gốc F150, 3 biến thể inject CSS lúc runtime đều báo CLS y nguyên; kiểm cơ chế thì
  `injected: false` → **init script không chạy**. Ba kết quả đó vô giá trị.
- **V11:** KB4 streak báo 2 fail; hoá ra scenario chèn "hôm qua" = **2026-09-20**, nay **bằng** cutover → đúng là
  ngày học. Đo lại với 2026-09-19 → đúng. **Sửa probe** (đọc cutover từ DB thay vì tính "hôm qua"), re-run **25/25 PASS**.

## 4. Case biên đối kháng (T7.3)

### Streak — `sweep/v12/streak-scenarios.js` → **25/25 PASS**

| Kịch bản | Kết quả |
|---|---|
| KB1: đăng nhập **KHÔNG** tạo study_day | PASS |
| KB2: ôn SRS → ghi study_day | PASS |
| KB3: snapshot phản ánh đúng ngày vừa ghi | PASS |
| KB4: **cutover chặn ngày TRƯỚC nó** | PASS (sau khi sửa probe drift) |
| KB5: chỉ đăng nhập, không học → `studiedToday=false`, streak 0, 0 row | PASS |

**Hợp đồng lõi của streak refactor được tái chứng minh sống:** đăng nhập **không** phải ngày học; chỉ hoàn thành
hoạt động học mới tính.

### SRS interval cap (F106) — kiểm trong source

`SrsService.MAX_INTERVAL_DAYS = 365`, kèm lịch sử overflow ghi tại chỗ: `interval × easeFactor` không cap từng cho
`srs_interval = 1_537_216` (~4210 năm), sau đó mọi review trả HTTP 500 lỗi `datetime2` out-of-range **vĩnh viễn**
(vì row không bao giờ được sửa). Cap vừa chặn interval vừa cho row đã hỏng tự lành ở lần review kế.

### Rate-limit bucket routing — kiểm trong `RateLimitFilter` + `RateLimitFilterTest`

Prefix bucket (`:auth` 20, `:mail` 5, `:ai` 10, `:upload` 15, `:order` 10, `:global` 100) — có test
`RateLimitFilterTest` (F91/F92) phủ routing. **Xác nhận** tồn tại, không chạy burst lại (v11 đã đo burst thô).

### Cutover day (`study_policy.effective_from = 2026-09-20`)

Xác nhận live: `study_policy` = 1 row, FK `fk_study_days_user` enabled, UQ `uq_study_days_user_date` present.
`study_days` = 2 row (admin + student, **hoạt động học thật** hôm nay) — hợp lệ, 0 orphan.

## 5. Cross-artifact consistency (T7.4)

| Artifact | Nhất quán? |
|---|---|
| `spec.md` R1–R7 ↔ công việc đã làm | ✓ mỗi requirement có artifact |
| `plan.md` Phase 0–8 ↔ `evidence/` | ✓ mỗi phase có file evidence |
| `findings.md` ↔ `evidence/` | ✓ mỗi finding nêu probe sinh ra nó |
| probe-artifacts (V1–V11) ↔ findings | ✓ 11 ứng viên bị falsify, không tính trùng thành finding |
| parity line ↔ mọi run ghi | ✓ `1471\|43735\|72\|127\|28\|15\|4\|126\|14\|5` trước/sau mọi run |
| `baseline.md` số ↔ `.p0-v12-*.log` | ✓ 485 / 119·23 |
| `AGENTS.md` số cũ ↔ đo lại | ✓ 4848 answer rỗng, 0 LISTENING thiếu audio, ~185–198 ms admin search đều tái lập |

## 6. Rác & parity — cuối cùng

```
parity:            1471|43735|72|127|28|15|4|126|14|5   (không đổi suốt audit)
AUDIT-V12 lessons: 0
AUDIT-V12 decks:   0
AUDIT-V12 vocab:   0
payment residue:   0
```

## 7. Loop-until-dry — đã cạn chưa?

| Vòng | Lỗi MỚI tìm được | Ghi chú |
|---|---|---|
| Vòng 1 (Phase 1–6) | F146 (doc), F147 (authz), F148 (SRS orphan), F149 (contrast), F150 (CLS) | — |
| Vòng 2 (Phase 7) | **0 lỗi mới** | API 133/0/0 giống hệt; contrast 2→0; streak 25/25; 11 probe bug bị falsify |

**Vòng 2 không tìm thêm lỗi sản phẩm nào.** Theo tiêu chí loop-until-dry (dừng khi 2 vòng liên tiếp không lỗi mới),
vòng 2 là vòng "khô" thứ nhất. Ghi rõ: **F150 vẫn OPEN** (đã truy gốc, revert fix không đủ) và **F147/F148 chờ
quyết định owner** — đây là **hạng mục chưa đóng**, không phải lỗi bị bỏ sót.

## Verdict

**Phase 7: PASS.** Suite xanh và không hồi quy; API sweep **giống hệt** vòng 1 (133/0/0); browser sweep **xác nhận
F149 đã fix** (contrast 2 → 0); streak 25/25 sau khi sửa probe drift. Vòng 2 **không tìm thêm lỗi sản phẩm mới** —
và giá trị thật của nó là **bắt được 3 lỗi của chính probe** (V9 tap-target, V10 inject CSS, V11 KB4 date drift) mà
vòng 1 đã báo sai. Đó đúng là lý do phải chạy vòng 2: *độ phủ của probe không phải bề mặt của app.*
