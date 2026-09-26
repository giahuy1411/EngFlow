# EngFlow — Nền tảng tự học tiếng Anh

Nền tảng học tiếng Anh toàn diện: bài học tương tác, luyện tập thông minh, AI chấm phát âm, và cộng đồng sôi động.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 4.0.6, Java 25 |
| **Database** | Microsoft SQL Server 2019 |
| **Cache / Queue** | Redis |
| **Storage** | MinIO (local S3), Cloudinary |
| **Frontend** | Vue 3 + Vite + Tailwind CSS + Pinia |
| **AI** | Ollama (`qwen2.5:1.5b` / `qwen2.5:3b`) |
| **Speech** | Faster-Whisper (`engflow-whisper`) |
| **Runtime** | Docker Compose |

---

## Prerequisites

- **Docker Desktop** (only required dependency)

---

## Quick Start

```bash
# 1. Create .env from example
cp .env.example .env

# 2. Edit .env — set DB_PASSWORD and other secrets

# 3. Start everything
docker compose up -d

# Wait ~1–2 min for first-time init, then access:
#   Frontend:  http://localhost:5173
#   Backend:   http://localhost:8080
```

### Ports

| Service | Host Port |
|---------|-----------|
| Frontend (Vite dev) | 5173 |
| Backend (Spring Boot) | 8080 |
| SQL Server | 1433 |
| Redis | 6379 |
| MinIO | 9000 / 9001 (console) |

---

## Development

### Backend (Spring Boot)

```bash
# Runs inside Docker container (JDK 25) — no local JDK needed
docker compose up -d backend

# Or locally (requires JDK 25):
./mvnw clean install
./mvnw spring-boot:run
```

### Frontend (Vue 3)

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
npm run build        # production build
npm run test         # Vitest (178 tests / 29 files)
```

---

## Default Accounts

| Email | Password | Role |
|-------|----------|------|
| `user@gmail.com` | `123456` | Student |
| `admin@gmail.com` | `123456` | Administrator |

---

## Architecture

```
engflow/
├── src/main/java/com/datn/engflow/   # Spring Boot backend
│   ├── controller/   # 26 REST controllers
│   ├── service/      # Business logic
│   ├── repository/   # JPA repositories
│   ├── model/        # Entities + DTOs
│   └── config/       # Security, CORS, Redis
├── frontend/src/                      # Vue 3 SPA
│   ├── views/        # 46 .vue files (lessons, games, admin, speaking, videos, premium)
│   ├── components/   # UI primitives + decorators
│   ├── services/     # API client layer
│   ├── store/        # Pinia state
│   └── assets/       # design-system.css (Playful Geometric tokens)
├── docker-compose.yml
└── .specify/         # SpecKit artifacts (specs, plans, reports)
```

### Key Features

- **Lessons**: Structured lessons with 4 levels (Elementary → Upper-Intermediate), 7 skills, content HTML + exercises
- **Exercises**: Fill-in-blank, multiple-choice, matching (with AI grading)
- **AI Exercise Generation**: `POST /api/admin/exercises/ai/generate` → Ollama `qwen2.5:1.5b`
- **Speaking**: Recording → Whisper transcription → Ollama rubric scoring (`qwen2.5:3b`)
- **Games**: Quiz, Memory Match, Typing, Flashcard, Listening, Mixed — Redis-backed session
- **Decks**: Public vocabulary decks (Oxford 3000/5000, TOEIC, IELTS, etc.)
- **SRS**: Spaced repetition with SM-2 algorithm
- **Payments**: SePay gateway with webhook
- **Leaderboard + Streaks**: Gamification with daily streak tracking

---

## Commands Reference

```bash
# Backend tests (512 tests)
cmd /c "mvnw.cmd test"

# Frontend tests (178 tests, 29 files)
cd frontend && npx vitest run

# Frontend build
cd frontend && npx vite build

# Rebuild backend container (after code change)
docker compose up -d --build backend

# DB access from host
docker exec engflow-sqlserver \
  /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'YourPassword123' \
  -d english_learning -C -W
```

---

## Environment Variables

Copy `.env.example` → `.env` and configure:

```env
DB_PASSWORD=...           # SQL Server sa password
JWT_SECRET=...            # JWT signing key (min 256-bit)
MINIO_ROOT_USER=...
MINIO_ROOT_PASSWORD=...
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
CLOUDINARY_CLOUD_NAME=... # Cloudinary CDN (avatar + listening audio)
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
OPENROUTER_BASE_URL=http://localhost:11434/v1   # Ollama OpenAI-compatible endpoint
OPENROUTER_MODEL=qwen2.5
OPENROUTER_API_KEY=ollama
AI_SPEAKING_OLLAMA_BASE_URL=http://localhost:11434
AI_SPEAKING_OLLAMA_MODEL=qwen2.5:3b
MAIL_USERNAME=...         # Gmail SMTP (streak reminders)
MAIL_PASSWORD=...
SEPAY_BANK_ACCOUNT=...
SEPAY_WEBHOOK_SECRET=...
SEPAY_API_TOKEN=...
```

---

## Database Schema

18 tables: `users`, `lessons`, `lesson_submissions`, `exercises`, `exercise_attempts`, `vocabulary`, `decks`, `deck_words`, `user_vocabulary_progress`, `study_days`, `study_policy`, `speaking_prompts`, `speaking_submissions`, `video_lessons`, `video_attempts`, `payment_transactions`, `user_progress`, `user_streaks`.

> `user_streaks` is a **legacy table** (1 row, no entity, 0 code readers) kept only so `ddl-auto=update` never drops it; streak state now lives in `study_days`. `flashcards` / `flashcard_reviews` / `leaderboard_entries` do **not** exist — flashcards and the leaderboard are computed from `user_vocabulary_progress` and `study_days`.

---

## AI Models (Local, RTX 2050 4GB)

- `qwen2.5:1.5b` — exercise generation (82 tok/s GPU, fast, but doesn't grade Vietnamese rubrics)
- `qwen2.5:3b` — speaking rubric (46.5 tok/s GPU, accurate scoring)
- `whisper-base` — speech-to-text via sidecar

`OLLAMA_MAX_LOADED_MODELS=1` is set host-persistent (GPU 4GB chỉ chứa 1 model). Model swap khi đổi tính năng: ~5.7s.

---

## License

Private capstone project.
