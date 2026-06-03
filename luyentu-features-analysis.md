# Phân Tích Chức Năng luyentu.com & Hướng Dẫn Clone

> Dựa trên phân tích website luyentu.com — nền tảng học từ vựng tiếng Anh #1 Việt Nam với 100.000+ người dùng

---

## Tổng Quan Kiến Trúc

Luyentu.com là một **SPA (Single Page Application)** chạy trên trình duyệt, lấy cảm hứng từ Anki (phần mềm flashcard desktop nổi tiếng) nhưng chuyển hoàn toàn lên web. Yêu cầu JavaScript để hoạt động. Tech stack dự đoán: React/Vue + Node.js backend + database quan hệ.

---

## Nhóm 1: Core Features (Tính Năng Cốt Lõi)

### 1.1 Hệ Thống Flashcard
**Mô tả:** Mỗi flashcard gồm mặt trước (từ tiếng Anh + IPA + audio phát âm) và mặt sau (nghĩa tiếng Việt + ví dụ). Người dùng lật thẻ và tự đánh giá mức độ nhớ.

**Clone như thế nào:**
- Dùng React state để quản lý trạng thái lật thẻ (flip animation với CSS `transform: rotateY`)
- Mỗi card object lưu: `{ word, ipa, audio_url, definition_vi, example_en, example_vi, image_url }`
- Nút đánh giá: "Chưa nhớ" / "Nhớ một phần" / "Nhớ rõ" → truyền điểm 1–5 vào SRS algorithm

```js
// Card data structure
{
  id: "uuid",
  word: "ambitious",
  ipa: "/æmˈbɪʃəs/",
  audio: "https://...",
  pos: "adjective",
  definition: "Có tham vọng, đầy hoài bão",
  example: "She is ambitious and wants to become a doctor.",
  level: 0,          // SRS level: 0-5
  next_review: Date, // Ngày ôn tập tiếp theo
  ease_factor: 2.5   // SM-2 ease factor
}
```

---

### 1.2 Spaced Repetition System (SRS) — Ôn Tập Ngắt Quãng
**Mô tả:** Hệ thống tự động lên lịch ôn tập từ vựng dựa trên Đường Cong Quên Lãng (Forgetting Curve). Luyentu dùng 6 cấp độ (Lvl 0 → Lvl 5), tương tự Anki.

**Nguyên lý hoạt động:**
- Từ mới = Lvl 0 (xuất hiện ngay hôm nay)
- Trả lời đúng → tăng level → interval dài hơn (1 ngày → 3 ngày → 1 tuần → 2 tuần → 1 tháng)
- Trả lời sai → reset về Lvl 0, ôn lại ngay

**Clone như thế nào (thuật toán SM-2):**

```js
// SM-2 Algorithm implementation
function sm2(quality, repetitions, easeFactor, interval) {
  // quality: 0-5 (0=quên hoàn toàn, 5=nhớ hoàn hảo)
  if (quality >= 3) {
    if (repetitions === 0) interval = 1;
    else if (repetitions === 1) interval = 6;
    else interval = Math.round(interval * easeFactor);
    repetitions++;
  } else {
    repetitions = 0;
    interval = 1;
  }
  easeFactor = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
  if (easeFactor < 1.3) easeFactor = 1.3;
  
  const nextReview = new Date();
  nextReview.setDate(nextReview.getDate() + interval);
  
  return { interval, repetitions, easeFactor, nextReview };
}
```

**Thư viện có sẵn:**
- `npm install supermemo` — TypeScript/JS implementation của SM-2 ([github.com/maxvien/supermemo](https://github.com/maxvien/supermemo))
- `sm2-plus` — phiên bản cải tiến của SM-2 ([github.com/lo-tp/sm2-plus](https://github.com/lo-tp/sm2-plus))
- **FSRS** (Free Spaced Repetition Scheduler) — thuật toán mới hơn, chính xác hơn SM-2, được Anki sử dụng từ 2023

---

### 1.3 Hệ Thống 6 Game Luyện Từ Vựng
**Mô tả:** Gamification giúp tăng engagement, mỗi game thưởng coin khác nhau.

| Game | Cơ Chế | Coin | Cách Clone |
|------|---------|------|-----------|
| **Flashcard** | Lật thẻ, tự đánh giá | +5 | Component đơn giản nhất |
| **Trắc Nghiệm** | 1 từ + 4 đáp án (1 đúng, 3 sai random) | +10 | Fisher-Yates shuffle để random đáp án |
| **Nối Từ** (Memory Match) | Ghép đôi từ ↔ nghĩa trong grid | +10 | 2D grid, match-pair logic |
| **Gõ Từ** | Nhìn nghĩa, gõ từ tiếng Anh | +10 | Input + string comparison (toLowerCase) |
| **Nghe Viết** | Nghe audio, gõ từ | +15 | Web Speech API hoặc audio file |
| **Tổng Hợp** | Random mix các dạng bài | +20 | Wrapper random chọn game type |

**Clone game Trắc Nghiệm:**
```js
function generateQuiz(currentWord, allWords) {
  const wrongOptions = allWords
    .filter(w => w.id !== currentWord.id)
    .sort(() => Math.random() - 0.5)
    .slice(0, 3)
    .map(w => w.definition);
  
  const options = [...wrongOptions, currentWord.definition]
    .sort(() => Math.random() - 0.5);
  
  return { question: currentWord.word, options, answer: currentWord.definition };
}
```

---

### 1.4 Kho Lộ Trình Học Có Sẵn (Pre-built Decks)
**Mô tả:** Luyentu cung cấp sẵn hàng chục bộ từ vựng theo mục tiêu, không cần tự tìm tài liệu.

| Lộ Trình | Nội Dung |
|----------|---------|
| THPT | Lớp 10, 11, 12 — chương trình Global Success |
| IELTS | Từ vựng phân theo chủ đề (Environment, Technology, Health...) |
| TOEIC | ETS 2026, Hackers TOEIC, 0–500+, 500–650+ |
| Theo Level | A1 → B2 (Destination B1, B2, C1&C2) |
| SAT | Từ vựng học thuật nâng cao |
| Người nổi tiếng | HuyForum, Vy Vocab |

**Clone:** Xem File 2 để biết nguồn tài liệu cụ thể

---

### 1.5 Tự Tạo & Quản Lý Bộ Từ Vựng Cá Nhân
**Mô tả:** Người dùng có thể tạo deck riêng, thêm từ thủ công hoặc dùng AI, import CSV/Excel.

**Tính năng:**
- Dashboard từ vựng: Tổng / Đã thuộc / Chưa thuộc / % tiến độ
- Thêm từ thủ công (word + definition + IPA + example)
- Import CSV/Excel (tương thích Anki format)
- Export ra CSV

**Clone Import/Export:**
```js
// Parse CSV với PapaParse
import Papa from 'papaparse';
const result = Papa.parse(csvFile, { header: true });
// Expected columns: word, definition, ipa, example

// Export
const csv = Papa.unparse(wordList);
const blob = new Blob([csv], { type: 'text/csv' });
```

---

### 1.6 Tracking, Streak & Leaderboard
**Mô tả:** Streak (chuỗi ngày học liên tiếp) là tính năng giữ chân user hiệu quả nhất.

**Thống kê hiển thị:**
- Streak hàng ngày (tương tự Duolingo)
- Biểu đồ tiến độ học (số từ mới / ôn tập theo tuần)
- Bảng xếp hạng cộng đồng (Streak + Hoạt động)

**Clone Streak logic:**
```js
function updateStreak(user) {
  const today = new Date().toDateString();
  const lastStudy = new Date(user.last_study_date).toDateString();
  const yesterday = new Date(Date.now() - 86400000).toDateString();
  
  if (lastStudy === today) return user.streak; // Đã học hôm nay
  if (lastStudy === yesterday) return user.streak + 1; // Học ngày hôm qua
  return 1; // Streak bị reset
}
```

---

### 1.7 Coin System & Shop
**Mô tả:** Hệ thống gamification — kiếm coin khi học, tiêu coin mua hình nền/avatar.

**Clone:** Thêm `coins` vào user profile, cộng coin sau mỗi game session, tạo shop component với items unlock.

---

### 1.8 Community & Social Features
- Chat cộng đồng trong app
- Game giải ô chữ hàng ngày (Daily Crossword) — thưởng coin
- Bảng xếp hạng công khai

---

## Nhóm 2: AI Features (Tính Năng Trí Tuệ Nhân Tạo)

### 2.1 AI Tạo Bộ Từ Vựng Theo Chủ Đề (Tính Năng Chính)
**Mô tả:** Người dùng nhập chủ đề (ví dụ: "Technology", "Environment", "Business"), AI tự động sinh ra danh sách từ vựng kèm đầy đủ thông tin.

**Cách clone với Claude/GPT API:**

```js
async function generateVocabWithAI(topic, level = "B2", count = 20) {
  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-api-key": process.env.CLAUDE_API_KEY,
      "anthropic-version": "2023-06-01"
    },
    body: JSON.stringify({
      model: "claude-sonnet-4-20250514",
      max_tokens: 2000,
      messages: [{
        role: "user",
        content: `Generate ${count} English vocabulary words for topic: "${topic}" at CEFR level ${level}.
Return ONLY a JSON array with NO extra text. Each object must have:
{
  "word": "string",
  "pos": "noun|verb|adjective|adverb",
  "ipa": "/IPA transcription/",
  "definition_en": "English definition",
  "definition_vi": "Vietnamese translation",
  "example": "Example sentence in English",
  "example_vi": "Example sentence in Vietnamese",
  "level": "${level}"
}`
      }]
    })
  });
  
  const data = await response.json();
  const text = data.content[0].text;
  return JSON.parse(text.replace(/```json|```/g, '').trim());
}
```

**Prompt tốt cho IELTS vocabulary:**
```
Generate 15 IELTS Academic vocabulary words for topic "Climate Change" at C1 level.
Include: word, IPA, part of speech, definition (English), Vietnamese meaning, 
collocations, example sentence, IELTS band relevance (6.0/7.0/8.0+).
Return as JSON array only.
```

---

### 2.2 AI Gợi Ý Khi Thêm Từ Thủ Công
**Mô tả:** Khi user gõ một từ tiếng Anh vào ô thêm từ, AI tự động điền IPA, nghĩa, ví dụ.

**Cách clone:**
- Dùng **Free Dictionary API** (`https://api.dictionaryapi.dev/api/v2/entries/en/{word}`) để lấy IPA + audio + definition miễn phí
- Kết hợp AI để dịch definition sang tiếng Việt và tạo ví dụ phù hợp

```js
// Bước 1: Lấy data từ Free Dictionary API (miễn phí, không cần key)
async function getWordData(word) {
  const res = await fetch(`https://api.dictionaryapi.dev/api/v2/entries/en/${word}`);
  const data = await res.json();
  const entry = data[0];
  return {
    ipa: entry.phonetics.find(p => p.text)?.text || '',
    audio: entry.phonetics.find(p => p.audio)?.audio || '',
    definition: entry.meanings[0]?.definitions[0]?.definition || '',
    example: entry.meanings[0]?.definitions[0]?.example || ''
  };
}

// Bước 2: Dịch sang tiếng Việt bằng AI
async function translateWithAI(word, definition) {
  // Gọi Claude/GPT API để dịch definition sang tiếng Việt
  // ...
}
```

---

### 2.3 AI Gợi Ý Lộ Trình Học Cá Nhân Hóa
**Mô tả (tiềm năng mở rộng):** Dựa trên trình độ hiện tại và mục tiêu (IELTS 7.0, TOEIC 800...), AI đề xuất bộ từ vựng nên học theo thứ tự ưu tiên.

**Clone với AI:**
```js
const prompt = `
User profile:
- Current level: B1
- Goal: IELTS 7.0 in 6 months  
- Weak topics: Academic Writing vocabulary, Environmental terms
- Strong topics: Business English, Technology

Recommend a 12-week vocabulary study roadmap. 
List top 5 deck priorities per month with reason.
Return as structured JSON.
`;
```

---

### 2.4 AI Tạo Ví Dụ Câu Ngữ Cảnh
**Mô tả:** Khi học một từ, AI có thể tạo thêm nhiều ví dụ câu theo ngữ cảnh khác nhau (formal/informal, IELTS writing, daily conversation).

```js
async function generateContextExamples(word, contexts = ['IELTS writing', 'daily conversation', 'business email']) {
  // Gọi AI để sinh ví dụ theo từng ngữ cảnh
}
```

---

## Nhóm 3: Technical Architecture (Kiến Trúc Kỹ Thuật)

### Stack Đề Xuất Để Clone

**Frontend:**
```
React 18 + TypeScript
Tailwind CSS (styling)
Framer Motion (flip animation cho flashcard)
Recharts (biểu đồ tiến độ)
React Query / Zustand (state management)
```

**Backend:**
```
Node.js + Express / Next.js API Routes
PostgreSQL (user data, word progress)
Redis (caching, session, leaderboard)
```

**AI/APIs:**
```
Anthropic Claude API (tạo vocab, gợi ý nghĩa)
Free Dictionary API - dictionaryapi.dev (IPA, audio, definition)
Web Speech API / ElevenLabs (TTS cho audio)
```

**Database Schema cơ bản:**
```sql
-- Users
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR, streak INT DEFAULT 0,
  coins INT DEFAULT 0, last_study_date DATE
);

-- Words
CREATE TABLE words (
  id UUID PRIMARY KEY, word VARCHAR, ipa VARCHAR,
  audio_url VARCHAR, definition_vi TEXT,
  definition_en TEXT, example_en TEXT, pos VARCHAR,
  cefr_level VARCHAR, source VARCHAR -- 'oxford3000', 'ielts', 'toeic'...
);

-- User Word Progress (SRS data)
CREATE TABLE user_word_progress (
  user_id UUID, word_id UUID,
  level INT DEFAULT 0, -- 0-5 (SRS levels)
  repetitions INT DEFAULT 0,
  ease_factor FLOAT DEFAULT 2.5,
  interval INT DEFAULT 1, -- days
  next_review TIMESTAMPTZ,
  PRIMARY KEY (user_id, word_id)
);

-- Decks
CREATE TABLE decks (
  id UUID PRIMARY KEY, user_id UUID,
  name VARCHAR, description TEXT,
  is_public BOOLEAN DEFAULT false,
  source VARCHAR -- 'user', 'ielts', 'toeic', 'oxford3000'
);
```

---

## Checklist Clone Priority

| Tính Năng | Độ Phức Tạp | Ưu Tiên |
|-----------|-------------|---------|
| Flashcard cơ bản | Thấp | ⭐⭐⭐ Làm trước |
| SRS Algorithm (SM-2) | Trung bình | ⭐⭐⭐ Làm trước |
| Trắc nghiệm 4 đáp án | Thấp | ⭐⭐⭐ Làm trước |
| User Auth + Progress | Trung bình | ⭐⭐⭐ Làm trước |
| AI tạo vocab theo chủ đề | Thấp (dùng API) | ⭐⭐⭐ Làm trước |
| Import/Export CSV | Thấp | ⭐⭐ Làm sau |
| Game Memory Match | Trung bình | ⭐⭐ Làm sau |
| Game Nghe Viết | Trung bình | ⭐⭐ Làm sau |
| Streak System | Thấp | ⭐⭐ Làm sau |
| Leaderboard | Trung bình | ⭐ Tùy chọn |
| Coin & Shop | Cao | ⭐ Tùy chọn |
| Daily Crossword Game | Cao | ⭐ Tùy chọn |

---

## Tham Khảo & Nguồn Mở

- SM-2 JS Implementation: https://github.com/maxvien/supermemo
- FSRS Algorithm (Anki 2023+): https://github.com/open-spaced-repetition/fsrs4anki
- Free Dictionary API: https://dictionaryapi.dev
- Anki deck format (APKG): https://github.com/ankidroid/Anki-Android
- Duolingo clone patterns: https://github.com/nickthanasiu/duolingo-clone
