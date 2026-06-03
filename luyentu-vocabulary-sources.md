# Nguồn Tài Liệu Từ Vựng Tiếng Anh — luyentu.com

> Phân tích các bộ tài liệu tiếng Anh mà luyentu.com đang dùng, kèm nguồn gốc, cách truy cập và hướng dẫn tích hợp cho dự án của bạn.

---

## Tổng Quan Các Nguồn

Luyentu.com tổ chức từ vựng thành các **nhóm lộ trình** chính:
1. TOEIC (ETS, Hackers, level-based)
2. IELTS (topic-based, Academic/General)
3. Level CEFR (A1 → C2 — Destination series)
4. SAT / Học thuật
5. THPT (Global Success — chương trình Việt Nam)
6. Oxford 3000 / Cambridge

---

## 1. Oxford Vocabulary Lists

### 1.1 Oxford 3000 (A1–B2)
**Là gì:** Danh sách 3.000 từ tiếng Anh quan trọng nhất do Oxford University Press tuyển chọn dựa trên tần suất xuất hiện trong British National Corpus và Oxford Corpus Collection.

**Link chính thức (Free PDF từ Oxford):**
- Oxford 3000 đầy đủ: https://www.oxfordlearnersdictionaries.com/external/pdf/wordlists/oxford-3000-5000/The_Oxford_3000.pdf
- Oxford 3000 phân theo CEFR: https://www.oxfordlearnersdictionaries.com/external/pdf/wordlists/oxford-3000-5000/The_Oxford_3000_by_CEFR_level.pdf
- Oxford 3000 (American English): https://www.oxfordlearnersdictionaries.com/external/pdf/wordlists/oxford-3000-5000/American_Oxford_3000.pdf

**GitHub (đã parse thành TXT/JSON):**
- https://github.com/sapbmw/The-Oxford-3000 — wordlist dạng plain text
- https://github.com/jnoodle/English-Vocabulary-Word-List — Oxford 3000 + nhiều list khác

**Nội dung mỗi từ:**
```
word | part of speech | CEFR level (A1/A2/B1/B2)
```
> Lưu ý: Oxford 3000 chỉ cung cấp list, KHÔNG kèm definition/IPA. Cần kết hợp với Dictionary API để lấy thêm dữ liệu.

---

### 1.2 Oxford 5000 (C1–C2)
**Là gì:** Mở rộng của Oxford 3000, thêm 2.000 từ nâng cao ở level C1 và C2.

**Link chính thức:**
- https://www.oxfordlearnersdictionaries.com/external/pdf/wordlists/oxford-3000-5000/The_Oxford_5000.pdf
- https://www.oxfordlearnersdictionaries.com/external/pdf/wordlists/oxford-3000-5000/The_Oxford_5000_by_CEFR_level.pdf

---

### 1.3 Oxford Learner's Dictionary API
**Là gì:** API trả về full data cho mỗi từ (definition, IPA, audio, examples, synonyms).

**Truy cập:**
- Trang đăng ký: https://developer.oxforddictionaries.com/
- Free plan: 1.000 request/tháng
- Trả về: IPA, audio MP3, definition, examples, word forms

**Ví dụ API call:**
```bash
GET https://od-api.oxforddictionaries.com/api/v2/entries/en-gb/ambitious
Headers: app_id: YOUR_ID, app_key: YOUR_KEY
```

---

## 2. Cambridge Vocabulary Lists

### 2.1 Cambridge English Vocabulary Lists (Official, Free)
**Là gì:** Cambridge cung cấp wordlist chính thức cho từng kỳ thi: A2 Key, B1 Preliminary, B2 First, C1 Advanced, C2 Proficiency.

**Link download PDF chính thức (Free):**

| Kỳ Thi | Level | Link |
|--------|-------|------|
| Pre A1 Starters | Pre-A1 | https://www.cambridgeenglish.org/images/149681-yle-flyers-word-list.pdf |
| B1 Preliminary | B1 | https://www.cambridgeenglish.org/Images/506887-b1-preliminary-vocabulary-list.pdf |
| B2 First (FCE) | B2 | https://www.cambridgeenglish.org/images/168442-b2-first-vocabulary-list.pdf |
| C1 Advanced (CAE) | C1 | https://www.cambridgeenglish.org/images/168447-c1-advanced-vocabulary-list.pdf |

**GitHub (đã extract thành structured data):**
- https://github.com/sharafabacery/Cambridge-Vocabulary-list

**Cambridge Dictionary Word Lists (Interactive):**
- https://dictionary.cambridge.org/plus/cambridgeWordlists — tạo, download, share word lists miễn phí

---

### 2.2 Destination B1, B2, C1&C2 (Macmillan)
**Là gì:** Đây là sách luyện thi tiếng Anh phổ biến tại Việt Nam. Luyentu sử dụng từ vựng từ các sách này.

**Cách tìm nguồn hợp pháp:**
- Destination B1 ISBN: 9780230035379
- Destination B2 ISBN: 9780230035409  
- Nội dung chương theo unit với topic-based vocabulary
- Không có free official API — cần tự build database từ sách hoặc dùng AI generate

**Alternative hợp pháp (cùng level, free):**
- British Council B1-B2: https://learnenglish.britishcouncil.org/vocabulary/b1-b2-vocabulary (chia theo topic)

---

## 3. IELTS Vocabulary Sources

### 3.1 Academic Word List (AWL) — Free
**Là gì:** Danh sách 570 từ tập hợp thành 60 nhóm (word families), xuất hiện thường xuyên nhất trong văn bản học thuật. Thiết yếu cho IELTS Academic và TOEFL.

**Tác giả:** Averil Coxhead (Victoria University of Wellington)

**Download miễn phí:**
- Official site: https://www.wgtn.ac.nz/lals/resources/academicwordlist/
- Sublists 1-10: https://www.victoria.ac.nz/lals/resources/academicwordlist/publications/awlsublists1.pdf
- GitHub JSON format: https://github.com/bcko/academic-word-list

**Cấu trúc:**
```json
{
  "sublist": 1,
  "word_families": ["analyze", "analysis", "analyst", "analytical", "analytically"]
}
```
> AWL là nguồn MIỄN PHÍ hoàn toàn và cực kỳ có giá trị cho IELTS. Đây là tài liệu học thuật công khai.

---

### 3.2 IELTS Vocabulary by Topic (Community & Curated Lists)
**Luyentu.com dùng các chủ đề IELTS phổ biến:**

| Chủ Đề | Số Từ Tiêu Biểu |
|--------|----------------|
| Environment & Climate | 150-200 từ |
| Technology & Innovation | 150-200 từ |
| Health & Medicine | 150-200 từ |
| Education | 100-150 từ |
| Society & Culture | 100-150 từ |
| Business & Economy | 150-200 từ |
| Science & Research | 100-150 từ |

**Nguồn free có thể dùng:**
- IELTS Liz vocabulary: https://ieltsliz.com/100-ielts-essay-vocabulary/ (giáo viên IELTS 8.5+)
- Magoosh IELTS vocabulary: https://magoosh.com/ielts/ielts-vocabulary/
- British Council IELTS vocab: https://learnenglish.britishcouncil.org/skills/writing/advanced-c1-writing/ielts-vocabulary

**Cách dùng AI để tạo IELTS vocab list:**
```
Prompt: "Generate 50 IELTS Academic vocabulary words for topic 'Environment & Climate Change' 
at C1-C2 level. Include: word, IPA, definition (English), Vietnamese meaning, 
IELTS example sentence, word family, collocations. Return as JSON array."
```

---

### 3.3 Collins COBUILD IELTS Vocabulary (Book Series)
**Là gì:** Sách từ vựng IELTS của Collins, sử dụng corpus-based definitions.

- Collins Vocabulary for IELTS (B1+ → Band 6+)
- Organized by topic units
- Mỗi từ có: definition, example from corpus, exam tips

**Không miễn phí**, nhưng có thể dùng AI generate từ vựng tương đương với cùng cấu trúc.

---

## 4. TOEIC Vocabulary Sources

### 4.1 ETS Official TOEIC Vocabulary
**Là gì:** Từ vựng từ sách ETS chính thức. Luyentu liệt kê "ETS 2026" trong lộ trình.

**Các sách ETS chính thức:**
- ETS TOEIC Official Test-Preparation Guide
- ETS TOEIC Speaking & Writing (business-focused vocabulary)
- Không có free API — cần tự curate

**GitHub tham khảo (community-curated):**
- https://github.com/topics/toeic — nhiều project open source liên quan TOEIC

---

### 4.2 Hackers TOEIC Vocabulary
**Là gì:** Cuốn sách Hackers TOEIC rất phổ biến tại Việt Nam và Hàn Quốc. Chia theo Part 5/6/7.

- Hackers TOEIC Vocabulary (RC + LC)
- Được tổ chức theo chủ đề kinh doanh

**Không free** — cần mua sách hoặc tự build database.

---

### 4.3 600 Essential Words for the TOEIC (Barron's)
**Là gì:** Cuốn sách luyện TOEIC nổi tiếng của Barron's, 50 units × 12 words/unit = 600 từ thiết yếu.

- Chia theo 10 loại chủ đề TOEIC: Contracts, Marketing, Personnel, Finance, Office, Travel, Dining, Entertainment, Health, Finance
- Có sẵn vocabulary list trên nhiều trang học tiếng Anh

**Tìm kiếm:** "Barron's 600 Essential Words TOEIC word list" → nhiều nguồn community đã chia sẻ

---

## 5. Free APIs Để Lấy Dữ Liệu Từ Điển

### 5.1 Free Dictionary API (Khuyến Nghị #1)
**URL:** `https://api.dictionaryapi.dev/api/v2/entries/en/{word}`

**Không cần API key. Miễn phí hoàn toàn.**

**Response trả về:**
```json
{
  "word": "ambitious",
  "phonetics": [
    { "text": "/æmˈbɪʃ.əs/", "audio": "https://...ambitious.mp3" }
  ],
  "meanings": [{
    "partOfSpeech": "adjective",
    "definitions": [{
      "definition": "Having a strong desire for success, achievement, or distinction.",
      "example": "She was very ambitious and worked long hours."
    }],
    "synonyms": ["aspiring", "determined"],
    "antonyms": ["unambitious"]
  }]
}
```

**GitHub:** https://github.com/meetDeveloper/freeDictionaryAPI

---

### 5.2 Merriam-Webster Dictionary API
**URL:** https://dictionaryapi.com/

**Free plan:** 1.000 requests/ngày  
**Trả về:** Definition, IPA (M-W notation), audio, examples, synonyms/antonyms  
**Đăng ký:** https://dictionaryapi.com/register/index

---

### 5.3 Wiktionary API (Free, Unlimited)
**URL:** `https://en.wiktionary.org/api/rest_v1/page/definition/{word}`

- Miễn phí, không giới hạn
- Data có thể không nhất quán nhưng coverage rất rộng
- Có IPA, etymology, multiple definitions

---

### 5.4 WordNet (Princeton — Academic, Free)
**Là gì:** Cơ sở dữ liệu từ vựng học thuật của Princeton, sử dụng rộng rãi trong NLP.

**Truy cập:**
- Web: https://wordnet.princeton.edu/
- Python NLTK: `import nltk; from nltk.corpus import wordnet`
- API: https://www.nltk.org/howto/wordnet.html

**Dữ liệu:** synsets, hypernyms/hyponyms, definitions, lemmas

---

### 5.5 Datamuse API (Miễn Phí, Không Key)
**URL:** `https://api.datamuse.com/words?rel_syn={word}`

- Tìm synonyms, antonyms, related words
- Hữu ích để tạo "distractor" trong game trắc nghiệm

---

## 6. Audio Pronunciation Sources

### 6.1 Forvo API (Native Speaker Recordings)
- https://api.forvo.com/ — recordings từ người bản ngữ thật
- Free plan: 500 requests/ngày

### 6.2 Merriam-Webster Audio
- Audio file MP3 từ Merriam-Webster Dictionary API (kèm theo API response)

### 6.3 Free Dictionary API Audio
- Audio URLs trả về trong response (từ Wikimedia/Wiktionary)

### 6.4 Web Speech API (Browser TTS — Miễn Phí)
```js
// Phát âm từ bằng trình duyệt — không cần API
const utterance = new SpeechSynthesisUtterance('ambitious');
utterance.lang = 'en-US';
utterance.rate = 0.8;
window.speechSynthesis.speak(utterance);
```
> Phương án nhanh nhất và miễn phí hoàn toàn, nhưng chất lượng giọng phụ thuộc vào trình duyệt.

### 6.5 ElevenLabs / Google TTS (Paid, High Quality)
- ElevenLabs API: https://elevenlabs.io/api — giọng tự nhiên nhất
- Google Cloud Text-to-Speech: https://cloud.google.com/text-to-speech

---

## 7. THPT Vietnam (Global Success)

### Nguồn
- **Bộ sách:** Tiếng Anh lớp 10, 11, 12 — chương trình Global Success (NXB Giáo dục)
- Luyentu dùng từ vựng theo unit từ các sách này

**Cách build database:**
- Không có free API chính thức
- Dùng AI generate từ vựng theo unit/topic tương ứng sách giáo khoa
- Cộng đồng Việt Nam đã share nhiều wordlist trên Google Drive/Facebook

---

## 8. Tổng Hợp: Roadmap Xây Dựng Database Từ Vựng

### Bước 1: Core vocabulary (miễn phí, bắt đầu ngay)
```
1. Download Oxford 3000 PDF → parse thành JSON (A1-B2, ~3000 từ)
2. Download Academic Word List → thêm 570 từ học thuật
3. Dùng Free Dictionary API để enrich: IPA + audio + definition
4. Dùng Claude API để thêm Vietnamese translation + example
```

### Bước 2: IELTS vocabulary (AI-generated)
```
5. Dùng Claude API generate ~200 từ cho mỗi trong 10 IELTS topic
   (Environment, Technology, Health, Education, Society,
    Business, Science, Crime, Arts, Food & Nutrition)
6. Kiểm tra chất lượng và deduplicate
```

### Bước 3: TOEIC vocabulary
```
7. Dùng Barron's 600 structure làm template
8. AI generate TOEIC-specific words theo 10 business topics
9. Cross-reference với ETS official content
```

### Bước 4: Level-based (A1-C2)
```
10. Oxford 3000 (A1-B2) đã có ở Bước 1
11. Oxford 5000 (C1-C2) — download PDF chính thức
12. Cambridge vocabulary lists theo exam level
```

---

## 9. Bảng Tóm Tắt Nhanh

| Nguồn | Type | Free? | Link |
|-------|------|-------|------|
| Oxford 3000 | Word list A1-B2 | ✅ Hoàn toàn | oxfordlearnersdictionaries.com |
| Oxford 5000 | Word list C1-C2 | ✅ Hoàn toàn | oxfordlearnersdictionaries.com |
| Cambridge B1/B2 Lists | Exam vocab | ✅ Hoàn toàn | cambridgeenglish.org |
| Academic Word List | Academic vocab | ✅ Hoàn toàn | wgtn.ac.nz/lals |
| Free Dictionary API | IPA + Definition | ✅ Hoàn toàn | dictionaryapi.dev |
| Wiktionary API | Definition + etymology | ✅ Hoàn toàn | en.wiktionary.org |
| WordNet | Semantic relations | ✅ Hoàn toàn | wordnet.princeton.edu |
| Datamuse API | Synonyms/Antonyms | ✅ Hoàn toàn | datamuse.com |
| Web Speech API | TTS Audio | ✅ Browser built-in | MDN Web Docs |
| Merriam-Webster API | Definition + Audio | ⚠️ 1000/day | dictionaryapi.com |
| Oxford Dictionaries API | Full data | ⚠️ 1000/month | developer.oxforddictionaries.com |
| Forvo API | Native audio | ⚠️ 500/day | api.forvo.com |
| Claude/GPT API | AI generation | 💰 Có phí | anthropic.com / openai.com |
| ElevenLabs TTS | High-quality audio | 💰 Có phí | elevenlabs.io |
| Hackers TOEIC | TOEIC vocab | 📕 Mua sách | Nhà sách |
| Destination B1/B2 | CEFR vocab | 📕 Mua sách | Nhà sách |

---

## 10. Code Snippet: Build Vocab Entry Đầy Đủ Tự Động

```js
// Hàm tự động tạo 1 vocab entry đầy đủ từ 1 từ tiếng Anh
async function buildVocabEntry(word) {
  // 1. Lấy IPA + audio + definition từ Free Dictionary API
  const dictData = await fetch(`https://api.dictionaryapi.dev/api/v2/entries/en/${word}`)
    .then(r => r.json()).then(d => d[0]);
  
  const ipa = dictData?.phonetics?.find(p => p.text)?.text || '';
  const audio = dictData?.phonetics?.find(p => p.audio)?.audio || '';
  const definition_en = dictData?.meanings?.[0]?.definitions?.[0]?.definition || '';
  const example_en = dictData?.meanings?.[0]?.definitions?.[0]?.example || '';
  const pos = dictData?.meanings?.[0]?.partOfSpeech || '';

  // 2. Dùng AI để thêm Vietnamese translation + example mới
  const aiRes = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: 'claude-sonnet-4-20250514',
      max_tokens: 500,
      messages: [{
        role: 'user',
        content: `For the word "${word}" (${pos}):
Definition: "${definition_en}"
1. Provide Vietnamese translation (1-2 words max)
2. Write a natural IELTS-level example sentence
3. List 2 common collocations
Return as JSON: { "definition_vi": "", "example_new": "", "collocations": [] }`
      }]
    })
  }).then(r => r.json());
  
  const aiData = JSON.parse(aiRes.content[0].text);
  
  return {
    word, ipa, audio, pos, definition_en,
    definition_vi: aiData.definition_vi,
    example_en: aiData.example_new || example_en,
    collocations: aiData.collocations
  };
}

// Dùng: 
const entry = await buildVocabEntry('ambitious');
// Lưu vào DB
```

---

*Tổng hợp bởi phân tích luyentu.com — May 2026*
