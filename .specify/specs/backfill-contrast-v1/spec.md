# Spec: Backfill đáp án rỗng bằng AI + Accent đạt WCAG AA

## Why

V2 audit phát hiện 7,076/43,738 bài tập seed có `correct_answer` rỗng (1,171 FILL_BLANK + 5,905 MULTIPLE_CHOICE, id 662174–704728) — học viên chọn đúng cũng bị chấm SAI. Đồng thời accent #8B5CF6 cho contrast 4.23:1 với text trắng (AA cần 4.5) và stat number màu #F472B6 (2.6:1) không đạt AA.

## Scope

### In Scope
1. **Backfill**: service + endpoint admin `POST /api/admin/exercises/ai/backfill-answers` chạy Ollama `qwen2.5:1.5b`, xử lý MULTIPLE_CHOICE + FILL_BLANK có `correct_answer` rỗng, theo lesson order (ASC), batch per lesson, resumable checkpoint.
2. **Validation answer** (untrusted AI output — LLM05): MC answer PHẢI ∈ options hiện có (case-insensitive trim match, ghi lại đúng variant trong options); FB đáp án ngắn (≤120 chars, không rỗng, không placeholder "left|right"/"answer"/"..."); reject → đếm vào unfillable, không tự chế.
3. **Accent swap**: `--geo-accent` #8B5CF6 → #7C3AED (design-system.css) + `geo.accent` + mọi alias trong tailwind.config.js. Stat-number #F472B6 → text #DB2777 (giữ nền pink-100 như design). KHÔNG đổi thứ gì khác của design system (borders #1E293B, cream #FFFDF5, blob radii, bounce bezier, Be Vietnam Pro, shadow-pop — nguyên vẹn).
4. Baseline gates: backend 197 tests + frontend 73 tests phải xanh sau mọi thay đổi.

### Out of Scope
- Không sửa MATCHING rỗng (loại khỏi tập backfill — contract "l=r" quá khó cho 1.5b).
- Không ẨN/xóa bài rỗng (user chọn backfill, không phải hide).
- Không fix 3 vocab dups, không index DB, không commit git.
- Không đổi palette khác (cream/borders/shadows) hay font.

## Success Criteria

- [ ] Số bài `correct_answer` rỗng giảm từ 7,076; bài có đáp án hợp lệ được ghi (MC ∈ options, FB ngắn); phần không backfill được được đếm + báo cáo rõ (không ghi rác).
- [ ] Contrast trắng trên accent ≥ 4.5:1 (verify Lighthouse/computed), stat-num text đạt ≥4.5:1 trên nền pink-100.
- [ ] Vitest 73/73 xanh; mvnw test 197/197 xanh; UI không đổi layout/chỉ đổi màu accent.
- [ ] Endpoint mới có: dry-run mode, limit param, checkpoint resume (chạy lại từ điểm dừng không xử lý trùng), progress pollable.

## Clarifications

- User chọn: backfill bằng AI (không phải hide/filter) — "còn về các bài tập bị đáp án rỗng thì Backfill bằng AI".
- User chốt: "vẫn phải tuân thủ nguyên tắc thiết kế giao diện trên" = chỉ đổi token màu accent, mọi nguyên tắc Playful Geometric khác giữ nguyên.
- Không commit (mục #1 không được chọn).
