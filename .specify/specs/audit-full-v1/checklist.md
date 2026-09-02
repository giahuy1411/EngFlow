# Checklist: Comprehensive Audit EngFlow v1

## Backend Checklist
- [ ] Tất cả 26 controller trả về đúng status code
- [ ] Tất cả endpoints có JWT auth đúng (trừ public)
- [ ] Premium gate hoạt động (speaking endpoints)
- [ ] Admin gate hoạt động (admin endpoints)
- [ ] Error responses theo format ErrorResponse
- [ ] N+1 queries được fix (check LessonController, ExerciseController, DeckController)
- [ ] Missing indexes (FK columns)
- [ ] Ollama call có timeout + retry
- [ ] Redis được sử dụng cho game sessions
- [ ] Rate limit không chặn audit

## Frontend Checklist
- [ ] Font Be Vietnam Pro load đúng trên mọi page
- [ ] Không còn font Outfit / Plus Jakarta Sans / Inter
- [ ] Color tokens khớp design system (7C3AED, DB2777, B45309)
- [ ] Hard shadows `4px 4px 0px #1E293B` trên buttons/cards
- [ ] Border-2 border-foreground pattern
- [ ] Hover effects (translate + shadow) hoạt động
- [ ] WCAG AA contrast mọi text
- [ ] Responsive: mobile 375px, tablet 768px, desktop 1440px
- [ ] Buttons tối thiểu 48px height
- [ ] Form inputs có label rõ ràng
- [ ] Error states có UI message

## AI Features Checklist
- [ ] Exercise generation: Ollama 1.5b trả JSON đúng schema
- [ ] JSON salvage 3 layers (fences, array, object) hoạt động
- [ ] Few-shot prompt ổn định, không copy example
- [ ] Vocab generation: Ollama 3b trả JSON
- [ ] Speaking rubric: 3 dims, average, feedback tiếng Việt
- [ ] Backfill status endpoint trả progress
- [ ] Ollama timeout 60s không gây hang
- [ ] Không model swap không cần thiết

## Database Checklist
- [ ] Tất cả 21 bảng có schema đúng
- [ ] FK constraints hoạt động
- [ ] No orphan records
- [ ] Indexes trên FK columns
- [ ] Sample 100 rows mỗi bảng chính
- [ ] Count exercises (1,654 backfilled còn 5,422 trống)
- [ ] Count users, lessons, decks, videos, payments
- [ ] Streak data integrity

## Test Baselines
- [ ] Backend: 207/207 pass
- [ ] Frontend: 73/73 pass
- [ ] Không break test nào trong audit

## Performance
- [ ] Read APIs < 500ms
- [ ] Write APIs < 2s
- [ ] AI APIs < 30s
- [ ] Page load < 3s

## Docker Health
- [ ] All 8 services running
- [ ] SQL Server reachable
- [ ] Redis reachable
- [ ] Backend logs no errors
- [ ] Frontend HMR working
