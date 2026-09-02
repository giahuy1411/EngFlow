# EngFlow — Nền tảng tự học tiếng Anh

Nền tảng học tiếng Anh toàn diện: bài học tương tác, luyện tập thông minh, AI chấm phát âm, và cộng đồng sôi động.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3, Java 17 |
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
#   Swagger:   http://localhost:8080/swagger-ui.html
```

### Ports

| Service | Host Port |
|---------|-----------|
| Frontend (Vite dev) | 5173 |
| Backend (Spring Boot) | 8080 |
| SQL Server | 1434 |
| Redis | 6379 |
| MinIO | 9000 / 9001 (console) |

---

## Development

### Backend (Spring Boot)

```bash
# Runs inside Docker container (JDK 25) — no local JDK needed
docker compose up -d backend

# Or locally (requires JDK 17+):
./mvnw clean install
./mvnw spring-boot:run
```

### Frontend (Vue 3)

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
npm run build        # production build
npm run test         # Vitest (73 tests / 14 files)
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
│   ├── views/        # 87 .vue files (lessons, games, admin, speaking, videos, premium)
│   ├── components/   # UI primitives + decorators
│   ├── services/     # API client layer
│   ├── store/        # Pinia state
│   └── assets/       # design-system.css (Playful Geometric tokens)
├── docker-compose.yml
└── .specify/         # SpecKit artifacts (specs, plans, reports)
```

### Key Features

- **Lessons**: Structured lessons with 4 CEFR levels, 7 skills, content HTML + exercises
- **Exercises**: Fill-in-blank, multiple-choice, matching (with AI grading)
- **AI Exercise Generation**: `POST /api/admin/ai/generate-exercise` → Ollama `qwen2.5:1.5b`
- **Speaking**: Recording → Whisper transcription → Ollama rubric scoring (`qwen2.5:3b`)
- **Games**: Quiz, Memory Match, Typing, Flashcard, Mixed — Redis-backed session
- **Decks**: Public vocabulary decks (Oxford 3000/5000, TOEIC, IELTS, etc.)
- **SRS**: Spaced repetition with SM-2 algorithm
- **Payments**: SePay VNPAY gateway with webhook
- **Leaderboard + Streaks**: Gamification with daily streak tracking

---

## Commands Reference

```bash
# Backend tests (223 tests)
cmd /c "mvnw.cmd test"

# Frontend tests (73 tests, 14 files)
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
REDIS_PASSWORD=...        # Redis auth
MINIO_ROOT_USER=...
MINIO_ROOT_PASSWORD=...
CLOUDINARY_URL=...        # Cloudinary CDN
OLLAMA_BASE_URL=http://localhost:11434
Z_AI_API_KEY=...          # Google Cloud TTS (optional; supertonic MCP preferred)
```

---

## Database Schema

19 tables: `users`, `lessons`, `exercises`, `lesson_sections`, `lesson_blocks`, `lesson_submissions`, `lesson_snapshots`, `exercise_attempts`, `vocabulary`, `decks`, `deck_words`, `flashcards`, `flashcard_reviews`, `user_vocabulary_progress`, `speaking_prompts`, `speaking_submissions`, `video_lessons`, `video_attempts`, `payment_transactions`, `user_streaks`, `user_progress`, `leaderboard_entries`.

---

## AI Models (Local, RTX 2050 4GB)

- `qwen2.5:1.5b` — exercise generation (82 tok/s GPU, fast, but doesn't grade Vietnamese rubrics)
- `qwen2.5:3b` — speaking rubric (46.5 tok/s GPU, accurate scoring)
- `whisper-base` — speech-to-text via sidecar

`OLLAMA_MAX_LOADED_MODELS=1` is set host-persistent (GPU 4GB chỉ chứa 1 model). Model swap khi đổi tính năng: ~5.7s.

---

## License

Private capstone project.
