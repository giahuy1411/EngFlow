# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EngFlow is an English learning platform with lessons, vocabulary decks, speaking practice, video lessons, and gamification (streaks, leaderboards, SRS flashcards). It has a premium tier gated by payment (SePay) and admin content management.

## Tech Stack

- **Backend**: Spring Boot 4.0.6, Java 25, Spring Data JPA, Spring Security (JWT), Lombok
- **Frontend**: Vue 3, Vite, Pinia (state), Vue Router, Tailwind CSS
- **Database**: Microsoft SQL Server (Hibernate `ddl-auto=update`)
- **Cache**: Redis (game sessions, rate limiting)
- **External**: Cloudinary (avatars), MinIO (speaking media), Whisper sidecar + Ollama (pronunciation assessment), OpenRouter/Ollama (AI), SePay (payments), Gmail SMTP (streak reminders)

## Build & Run Commands

### Backend (Maven)
```bash
mvn spring-boot:run                    # Start backend (default port 8080)
mvn test                              # Run all backend tests
mvn test -Dtest=StreakServiceTest     # Run single test class
mvn test -Dtest=StreakServiceTest#testMethod  # Run single test method
```

### Frontend (Vite)
```bash
cd frontend
npm install                           # Install dependencies
npm run dev                           # Start dev server (port 5173)
npm run build                         # Production build
npm run test                          # Run tests (vitest run)
npm run test:watch                    # Watch mode
```

## Environment Variables

All sensitive config is via env vars (or `.env` file via `spring.config.import`):
- `SPRING_DATASOURCE_URL`, `DB_PASSWORD` — SQL Server
- `JWT_SECRET` — JWT signing key (256-bit minimum)
- `OPENROUTER_API_KEY`, `OPENROUTER_MODEL`, `OPENROUTER_BASE_URL` — AI integration. These are the config key *names* the app reads; by default `base-url` points at the local Ollama (`http://localhost:11434/v1`) and `api-key` defaults to `ollama`, so no cloud key is required
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` — Avatar uploads
- `MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` — Speaking media storage
- `MAIL_USERNAME`, `MAIL_PASSWORD` — Gmail SMTP for streak reminders
- `SEPAY_BANK_ACCOUNT`, `SEPAY_WEBHOOK_SECRET`, `SEPAY_API_TOKEN` — Payment processing

## Architecture

### Backend Structure (`src/main/java/com/datn/engflow/`)
- `controller/` — REST endpoints (Admin*, Lesson*, Speaking*, Deck*, Game*, Streak*, Leaderboard*, Srs*)
- `service/` — Business logic (ExerciseService, StreakService, SrsService, SpeakingSubmissionService, etc.)
- `repository/` — Spring Data JPA interfaces
- `model/entity/` — JPA entities (Lesson, ExerciseAttempt, Vocabulary, Deck, SpeakingPrompt, etc.)
- `model/dto/` — Request/Response DTOs
- `model/enums/` — ExerciseType, SkillType, etc. (`BlockType`/`QuestionType` were removed with the Lesson Builder — see `docs/lesson-builder-removal.md`)
- `security/` — JWT filter, UserPrincipal, rate limiting (the `PremiumRequired` annotation is defined here but unused — premium is checked via `hasPremiumAccess()`)
- `config/` — Redis, Jackson, MinIO, data seeders, migrations

### Frontend Structure (`frontend/src/`)
- `views/` — Page components (Home, Profile, Lessons, Speaking, admin/*, luyentu/*, videos/*)
- `components/ui/` — Reusable UI components (AppButton, AppInput, AppModal, AppTable, etc.)
- `components/common/` — Header, Footer, StreakCalendar, Pagination
- `store/modules/` — Pinia stores (auth, lesson, premium)
- `router/index.js` — Route definitions with auth/premium/admin guards
- `services/` — API client modules (streakService.js, etc.)

### Key Patterns
- **JWT Auth**: Access tokens (15min TTL); re-login to obtain a new one (there is no refresh-token endpoint). Filter chain: `JwtAuthenticationFilter` → `CustomUserDetailsService`
- **Premium Gating**: Frontend route meta `requiresPremium: true` + backend manual check via `userService.hasPremiumAccess()` (the `@PremiumRequired` annotation exists but has no usages)
- **Admin Guard**: Frontend `requiresAdmin: true` meta + backend role checks
- **AI Integration**: OpenRouter/Ollama for exercise generation; pronunciation assessment via the local Whisper sidecar (:9002) + an Ollama rubric
- **Speaking Submissions**: MinIO storage for audio/video, Whisper+Ollama assessment, admin manual grading
- **SRS (Spaced Repetition)**: `SrsService` with interval caps, study activity tracking
- **Streak System**: Daily activity tracking, reminder scheduler (Spring `@Scheduled`), email notifications

## Testing

- **Backend**: JUnit 5 + Spring Boot Test + Mockito. Tests are in `src/test/java/` mirroring main package structure
- **Frontend**: Vitest + `@vue/test-utils`. Tests co-located with components (e.g., `StreakCalendar.test.js`)
- **Security Tests**: `AuditV8*`, `AuditV9*` test classes verify access controls, XSS protection, draft visibility

## Code Conventions

- **Lombok**: Use `@Data`, `@Builder`, `@AllArgsConstructor` on entities/DTOs
- **DTOs**: Separate request/response classes in `model/dto/request/` and `model/dto/response/`
- **Exception Handling**: Custom exceptions (`BadRequestException`, `ResourceNotFoundException`) + `GlobalExceptionHandler` returning ProblemDetail
- **Frontend Components**: PascalCase filenames (e.g., `AppButton.vue`), composed of `ui/` primitives
- **Styling**: Tailwind CSS utility classes, design tokens in `assets/design-system.css`

## Database Migrations

Flyway is disabled — schema managed by Hibernate `ddl-auto=update`. Manual SQL migrations in `src/main/resources/db/migration/` (V1-V8) for reference only.
