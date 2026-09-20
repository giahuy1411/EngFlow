# Phase 1 — Audit DB trong Docker

**Ngày:** 2026-09-21 (+07) · **DB:** `english_learning` trên `engflow-sqlserver` (:1433)
**Probe:** `sweep/v12/db-audit.sql` → `sweep/v12/db-audit-full.out`; `sweep/v12/constraint-effect.sql` → `.out`
**Kết quả:** **0 `Msg`** trên cả hai (exit code 0 một mình không chứng minh gì — xem §0).

---

## 0. Lỗi probe tự phát hiện (ghi lại, vì lỗi probe cũng là lỗi)

| Lần | Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|---|
| 1 | 5 × `Msg 207 Invalid column name 'vocabulary_id'` | Tôi giả định `deck_words` dùng `vocabulary_id`; thực tế là **`vocab_id`** | Truy `sys.columns` trước, sửa mọi join |
| 2 | `Msg 245 Conversion failed ... '2026-09-20' to data type int` | `UNION ALL` trộn `COUNT(*)` (int) với `CONVERT(varchar)` | Bọc `CAST(... AS varchar(30))` cho **mọi** nhánh UNION |
| 3 | `Msg 1934 CREATE INDEX failed ... QUOTED_IDENTIFIER` | Filtered index cần `SET QUOTED_IDENTIFIER ON` — **đúng bài học AGENTS.md** | Thêm `SET QUOTED_IDENTIFIER ON` vào batch scratch |

Cả ba lần `sqlcmd` đều **exit 0**. Chỉ có **quét `Msg \d+`** bắt được. Đây là lần thứ N quy tắc đó chứng minh giá trị.

---

## 1. T1.1 — HIỆU LỰC của constraint (không chỉ sự tồn tại)

Chạy trong DB scratch `english_learning_v12probe`, **DROP sau khi xong** (live DB không bị chạm).

| Probe | Kỳ vọng | Kết quả đo |
|---|---|---|
| Insert hợp lệ | thành công | ✓ 1 row |
| **FK vi phạm** (`parent_id=999`) | **bị chặn** | ✓ `The INSERT statement conflicted with the FOREIGN KEY constraint "fk_child_parent"` |
| **UNIQUE vi phạm** (email trùng) | **bị chặn** | ✓ `Cannot insert duplicate key row ... with unique index 'uq_child_email'` |
| Filtered unique + nhiều NULL | cho phép | ✓ 2 row NULL cùng tồn tại |
| FK nullable | cho phép | ✓ row với FK NULL chèn được |

**Kết luận:** constraint **có hiệu lực thật**, không chỉ tồn tại trên giấy. Đây là điều v11 để `OPEN` (T1.1).

## 2. T1.2 — Orphan scan (NULL FK đếm RIÊNG)

| Quan hệ | Orphans | NULL FK |
|---|---|---|
| `exercise_attempts → lessons` (không có FK DB — kiểm tay) | **0** | 0 |
| `user_vocabulary_progress → vocabulary` | **0** | 0 |
| `user_vocabulary_progress → users` | **0** | 0 |
| `deck_words → decks` | **0** | 0 |
| `deck_words → vocabulary` | **0** | 0 |
| `vocabulary → lessons` | **0** | **126** |
| `study_days → users` | **0** | 0 |

**126 NULL ở `vocabulary.lesson_id` là hợp lệ** — từ vựng thuộc deck (không thuộc lesson). Đây đúng là tập row mà
truy vấn orphan ngây thơ sẽ đếm nhầm. **0 orphan, 0 vi phạm toàn vẹn.**

## 3. T1.3 — Streak schema

| Kiểm | Kết quả |
|---|---|
| `study_policy` rows | **1** |
| `study_policy.effective_from` | **2026-09-20** |
| `study_days` rows | **1** |
| FK `fk_study_days_user` | **1, enabled** |
| UQ `uq_study_days_user_date` | **1** |

### `study_days` = 1 là XÁC NHẬN hành vi đúng, không phải lỗi

v11 ghi 0 row vì cutover **đúng ngày hôm đó**. Giờ đo được **1 row**:

```
id=10026  user_id=3  email=admin@gmail.com  study_date=2026-09-21
```

`recordStudy` chỉ chạy khi có **hoạt động học thật** (`ExerciseService:441`, `FlashcardService:70`,
`SpeakingSubmissionService:134`, `SrsService:110`) — **không** chạy khi login, và **không** chạy ở `save-vocab`.
⇒ Đây là hoạt động học thật của admin trong ngày 2026-09-21, không phải rác probe. Xác nhận cutover hoạt động
đúng và **login không tạo ngày học** (hợp đồng lõi của streak refactor).

## 4. T1.4 — Row-level sanity (đo lại, không kế thừa)

| Chỉ số | Giá trị |
|---|---|
| lessons total / published / draft | **1471 / 1465 / 6** |
| exercises total | **43735** |
| exercises `correct_answer` rỗng | **4848** |
| users total / premium | **72 / 6** |
| decks total / system (no owner) | **14 / 10** |
| vocabulary total | **127** |
| payment_transactions total | **126** |
| — PENDING với `transaction_id` NULL | **109** |
| speaking_submissions | **28** |
| video_attempts | **15** |
| lesson_submissions | **4** |
| lesson_snapshots | **5** |

### LISTENING thiếu `audio_url` = **0** (đo lại, không kế thừa con số 9)

```
exercises type LISTENING          = 358
LISTENING missing audio_url       = 0
```

`AGENTS.md` ghi 9/367 (đo 2026-09-12). v10 F128 (gắn nhãn lại 7 + xoá 2) giải thích chênh lệch. Ghi là **xác nhận
F128**, không phải việc mới. Tổng số LISTENING cũng đổi 367 → 358 (khớp việc xoá/đổi nhãn).

### 4848 `correct_answer` rỗng là giới hạn ĐÃ BIẾT

`AGENTS.md`: 586/5434 đã backfill, phần còn lại là "MC-fragment scrape failures … ungradeable-by-design". Đo lại
**4848** — khớp mô tả (5434 − 586 = 4848). Không phải finding mới.

## 5. T1.5 — Trùng khoá / toàn vẹn

| Kiểm | Offenders |
|---|---|
| dup `users.email` | **0** |
| dup `users.username` | **0** |
| dup `study_days(user, date)` | **0** |
| dup `deck_words(deck, vocab)` | **0** |
| dup `payment_transactions.transaction_id` | **0** |

**Sạch.** (Lưu ý: nhóm `(word, lesson_id)` **không** dùng được làm khoá vì `lesson_id` NULL cho mọi row thuộc deck —
đó là lỗi probe P6 mà v11 đã ghi; v12 không lặp lại.)

## 6. T1.6 — Index trên hot table

| Table | Indexes |
|---|---|
| lessons | PK, `idx_lessons_level`, `idx_lessons_order_index`, `idx_lessons_pub_level_order` |
| exercises | PK, `idx_exercises_lesson_order`, `idx_exercises_lesson_type_order`, `idx_exercises_type`, `idx_exercises_difficulty`, `IX_exercises_lesson_type_diff_order` |
| vocabulary | PK, `idx_vocabulary_lesson`, `idx_vocabulary_word_cefr` |
| deck_words | PK, UK, `idx_deck_words_deck`, `idx_deck_words_vocab` |
| payment_transactions | PK, `idx_payment_transactions_user`, **filtered UQ** `UKlsp8jh693lih2txq7dl4bdnpx` (`transaction_id IS NOT NULL`) |
| study_days | PK, `uq_study_days_user_date` |

Hot read path đã được phủ. `idx_vocabulary_word_cefr` **không** cứu được `LIKE '%kw%'` (leading wildcard) — khớp
`AGENTS.md` (~185 ms trên `/api/admin/exercises?q=`) và lý do **không** thêm index cho nó.

**25 FK** trên 22 bảng người dùng (đo trực tiếp: `sys.foreign_keys` = 25, tất cả `is_disabled=0`,
`is_not_trusted=0`) ⇒ constraint **đang được tin**.

> **Lệch với v11:** v11 ghi **26 FK**. Đo lại chỉ **25**. Đếm tay từ inventory T1.1 cũng ra 25
> (deck_words 2 · decks 1 · exercise_attempts 1 · exercises 1 · lesson_blocks 1 · lesson_sections 1 ·
> lesson_snapshots 1 · lesson_submissions 2 · payment_transactions 1 · speaking_prompts 1 ·
> speaking_submissions 3 · study_days 1 · user_progress 2 · user_streaks 1 · user_vocabulary_progress 2 ·
> video_attempts 3 · vocabulary 1). Ghi là **phát hiện** (v11 đếm dư 1), không phải thay đổi schema —
> không có DDL nào chạy giữa hai phiên. Điều này **không** ảnh hưởng kết luận: mọi FK hiện có đều enabled/trusted.

## 7. T1.7 — Fragmentation: **TỪ CHỐI tối ưu, có số**

| Table | Index | Pages | Frag % |
|---|---|---|---|
| lessons | clustered PK | **121** | **11.57** |
| exercises | `IX_exercises_lesson_type_diff_order` | **293** | **10.92** |

Cả hai chỉ hơn ngưỡng 10 % một chút, trên index **121 page** và **293 page**. Rebuild sẽ chạm ~414 page và
**không thể** làm thay đổi độ trễ đọc một cách hợp lý (11.57 % trên 121 page ≈ 14 page lệch). Theo **P5**:
**không tối ưu khi chưa đo được lợi ích** ⇒ **TỪ CHỐI**, ghi số.

---

## Verdict

**Phase 1: PASS, 0 finding chống lại sản phẩm.** DB nhất quán nội tại: 0 orphan, 0 khoá trùng, mọi constraint
**enabled + trusted + có hiệu lực thật**, streak schema deploy đúng và đang ghi nhận hoạt động thật, index phủ hot
path. Hai con số lệch tài liệu cũ (4848 answer rỗng; 0 LISTENING thiếu audio) đều **khớp fix/giới hạn đã biết**,
không phải lỗi mới. Ba lỗi probe do chính tôi gây ra đã bị **quét `Msg \d+`** bắt và sửa.
