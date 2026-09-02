# Spec: Comprehensive Audit — Backend APIs + Frontend UI + DB + Performance

## 1. Concept & Vision

EngFlow là nền tảng học tiếng Anh capstone. Task này thực hiện audit toàn diện toàn bộ hệ thống: backend APIs (CRUD, AI), frontend UI (theo Playful Geometric + Be Vietnam Pro), cơ sở dữ liệu Docker, và hiệu năng. Tất cả bug phát hiện phải được sửa tận gốc. Tất cả thay đổi phải giữ baseline test xanh.

## 2. Design System — Playful Geometric (Font: Be Vietnam Pro)

### Color Tokens (Light Mode)
```
background:        #FFFDF5  (Warm Cream/Off-White)
foreground:       #1E293B  (Slate 800)
muted:            #F1F5F9  (Slate 100)
mutedForeground:  #64748B  (Slate 500)
accent:           #7C3AED  (Vivid Violet — WCAG AA ✅)
secondary:        #DB2777  (Hot Pink — text-safe ✅)
tertiary:         #B45309  (Amber — text-safe ✅)
quaternary:       #34D399  (Emerald/Mint)
border:           #E2E8F0  (Slate 200)
```

### Typography
- **Font**: Be Vietnam Pro (thay thế Outfit / Plus Jakarta Sans trong prompt gốc)
- **Headings**: Be Vietnam Pro Bold (700) / ExtraBold (800)
- **Body**: Be Vietnam Pro Regular (400) / Medium (500)

### Shadows & Effects
```
box-shadow: 4px 4px 0px 0px #1E293B;  // Hard shadow (no blur)
box-shadow-hover: 6px 6px 0px 0px #1E293B;
box-shadow-active: 2px 2px 0px 0px #1E293B;
```

### Radius
```
radius-sm:   8px
radius-md:   16px
radius-lg:   24px
radius-full: 9999px
border-width: 2px
```

## 3. Scope

### 3.1 Backend Audit
- **26 @RestController**: Auth, Lesson, Exercise, Admin (6), Game, Deck, Flashcard, SRS, Vocabulary, Speaking (2), Video (2), Payment, Leaderboard, Progress, Streak, Dashboard, AiVocab, MediaProxy
- **36 @Service**: Exercise/AI/Lesson/Speaking/Vocabulary/Gamification/Payment/Video
- **19 @Repository**
- **AI Features**: Ollama (qwen2.5:1.5b exercises, qwen2.5:3b speaking rubric), Whisper (faster-whisper base)
- **CRUD**: verify all endpoints — auth, lessons, exercises, vocabulary, decks, flashcards, speaking, video, payment, leaderboard, progress, streak
- **Performance**: N+1 queries, missing indexes, slow endpoints

### 3.2 Frontend Audit
- **89 Vue files**: Home, Auth, Lessons (13), Luyện tập (10), Speaking (4), Videos (2), Premium (2), Admin (10)
- **18 UI components**: AppButton, AppInput, AppModal, AppTable, AppToast...
- **Design system compliance**: font Be Vietnam Pro, colors, hard shadows, border-2, border-foreground
- **Responsive**: mobile-first, tappable buttons 48px+
- **Interactions**: bouncy transitions, hover effects

### 3.3 Database Audit
- SQL Server in Docker (`engflow-sqlserver`)
- Schema managed by Hibernate `ddl-auto=update`
- Key tables: users, lessons, exercises, exercise_attempts, lessons, lesson_blocks, lesson_sections, decks, flashcards, vocabulary, user_vocabulary_progress, speaking_prompts, speaking_submissions, video_lessons, video_attempts, payments, streaks, leaderboard

### 3.4 Performance Optimization
- Identify N+1 queries via repository analysis + runtime observation
- Check missing indexes on FK columns and frequently queried fields
- Verify Redis usage for game sessions and AI progress
- Check Ollama call latency and retry logic

## 4. Acceptance Criteria

- [ ] Tất cả backend API endpoints trả về đúng HTTP status và response shape
- [ ] Tất cả frontend views render đúng, tương tác với backend API thành công
- [ ] Font Be Vietnam Pro thay thế hoàn toàn Outfit/Plus Jakarta Sans
- [ ] Design tokens (màu, shadow, border, radius) khớp spec Playful Geometric
- [ ] Không còn lỗi contrast WCAG AA
- [ ] Backend test suite: 207/207 pass
- [ ] Frontend test suite: 73/73 pass
- [ ] Database: không có orphan records, FK constraints hoạt động, indexes hiệu quả
- [ ] Performance: thời gian response API < 500ms cho read ops, < 2s cho write ops
- [ ] AI features: exercise generation và speaking assessment hoạt động end-to-end
- [ ] Container health: tất cả Docker services ổn định

## 5. Out of Scope

- CI/CD pipeline setup
- Deployment to production
- New feature development
- Test coverage increase (chỉ verify existing tests pass)
- Video/audio streaming stress test

## 6. Risks & Unknowns

- Duplicate router registration for speaking-prompts có thể gây conflict
- Speaking submission audio URL phụ thuộc MCP supertonic — nếu MCP không chạy, audio sẽ trống
- TTS backend gọi Google Cloud TTS không có key — fail-soft nhưng không có audio thật cho in-app TTS
- Premium subscription gate có thể block audit một số endpoints
