# Phase 3.8 — LISTENING audio: đường TTS đã chứng minh, nhưng 9 row KHÔNG NÊN backfill

**Chủ dự án đã chọn:** "Thử chứng minh đường TTS trước." Đã làm. Kết quả: **đường TTS chạy thật, nhưng backfill 9 row là SAI** — và lý do quan trọng hơn cả việc backfill.

## Phần 1: đường TTS → Cloudinary — ĐÃ CHỨNG MINH

`sweep/v10/prove-tts-path.js` chạy 4 bước, dừng ngay nếu một bước hỏng:

| Bước | Kết quả |
|---|---|
| 1. TTS sidecar `/health` | **200** |
| 2. `POST /synthesize` | **200**, 6.083ms, **276.524 byte** |
| 3. `POST /api/admin/audio-upload` (multipart) | **200**, trả URL Cloudinary |
| 4. `GET` URL vừa trả | **200**, `content-type: audio/wav`, đúng 276.524 byte |

File là **WAV thật**, kiểm bằng magic bytes chứ không tin content-type:
```
magic = 'RIFF' + 'WAVE'   channels=1  sampleRate=44100  bits=16
```

URL mẫu: `https://res.cloudinary.com/dsuvw92hh/video/upload/v1789840412/engflow/audio/jqbfzbe3clxxcbhiytmg.wav`

**Kết luận phần 1:** điều kiện của `issues.md` ("chỉ regenerate nếu đo được pipeline") **đã được thoả**. Đường ống chạy thật với file media thật, không phải byte giả.

## Phần 2: nhưng 9 row đó KHÔNG PHẢI bài nghe

Đo nội dung 9 row (`sweep/v10/inspect-listening-9.js`):

| exercise_id | question | Thực chất là gì |
|---|---|---|
| 745643 | "**Write** a description of your new home." | bài **viết** |
| 745673 | "Please **read** these Terms and Conditions..." | bài **đọc** |
| 745708 | "**Write** a personal profile." | bài **viết** |
| 745748 | "I can understand **a text** about brothers and sisters." | bài **đọc** |
| 745878 | "I can understand **a text** about brothers and sisters." | bài **đọc** |
| 745904 | "What is the first snowfall like **in the article**?" | bài **đọc** |
| 745713 | "**Listen** to the passage and answer..." | nghe — nhưng **không có passage** |
| 745808 | "**Listen** to the following text..." | nghe — nhưng **không có text** |
| 745718 | "What is the best way to make friends?" | câu hỏi thảo luận |

### Bằng chứng định lượng

So sánh độ dài `question` giữa hai nhóm (`sweep/v10/compare-listening.js`):

| Nhóm | Độ dài `question` trung bình | Max |
|---|---|---|
| 358 row **CÓ** audio | **42,0** ký tự | 400 |
| 9 row **KHÔNG** audio | **114,0** ký tự | 624 |

Nhóm thiếu audio có câu hỏi **dài gấp 2,7 lần**. Điều đó hợp lý khi nhìn vào 5 row có audio:

```
id=650582  question: "Hi! My name is Isaac and I'm a big Manchester United fan..."  audio: lesson_11302_5.mp3
id=650587  question: "The man ___ a jacket."                                        audio: lesson_10890_5.mp3
id=650597  question: "He is a doctor."                                              audio: lesson_446_5.mp3
```

Row **có** audio có `question` **ngắn** — vì **audio chính là nội dung**, câu hỏi chỉ là hướng dẫn. Row **không** audio có `question` **dài** — vì **đoạn văn nằm trong câu hỏi**, tức đó là bài đọc/viết bị gắn nhãn sai thành `LISTENING`.

### Kiểm tra thêm: audio mẫu là file tĩnh theo lesson

`http://localhost:8080/audio/lesson_11302_5.mp3` — tên file là `lesson_<lessonId>_<orderIndex>.mp3`, không phải file sinh từ nội dung từng row.

## Phần 3: vì sao KHÔNG backfill

Sinh TTS cho 9 row này sẽ **đọc to chính câu lệnh**:

> 🔊 *"Write a description of your new home. Include features, layout, and your thoughts."*
> 🔊 *"Please read these Terms and Conditions carefully before..."*
> 🔊 *"I can understand a text about brothers and sisters."*

Đó không phải bài luyện nghe. Nó là **một bài viết/đọc được gắn nhãn sai, cộng thêm một file audio vô nghĩa**. Học sinh sẽ nghe một câu lệnh rồi được hỏi về nó.

Backfill ở đây **tệ hơn để nguyên**: một row thiếu audio là một khoảng trống nhìn thấy được; một row có audio sai là một lỗi im lặng trông như đã sửa xong.

## Phần 4: bản chất thật của 9 row

Đây **không phải** vấn đề audio. Đây là **9 row bị phân loại sai `exercise_type`**: nội dung của chúng là đọc/viết nhưng nhãn là `LISTENING`.

Sửa đúng là **đổi `exercise_type`** cho khớp nội dung, hoặc xoá nếu chúng là rác scrape — **cả hai đều là quyết định nội dung**, không phải việc kỹ thuật mà một vòng audit được phép tự làm.

Bằng chứng bổ sung: cả 9 row đều thuộc **9 lesson khác nhau**, mỗi lesson có **đúng 5 exercise** và **đúng 1 LISTENING** — và LISTENING đó luôn là row thiếu audio. Mẫu này lặp lại 9/9 lần, không phải ngẫu nhiên.

## Việc đã làm

- `sweep/v10/prove-tts-path.js` — chứng minh đường TTS, **cố ý không ghi DB**
- `sweep/v10/inspect-listening-9.js`, `sweep/v10/compare-listening.js` — đo nội dung 9 row
- `sweep/v10/tts-proof.wav` — file bằng chứng 276.524 byte
- **Không sửa row nào trong DB.**

## Việc cần chủ dự án quyết định

9 row `exercise_id`: `745643`, `745673`, `745708`, `745713`, `745718`, `745748`, `745808`, `745878`, `745904`.

Ba lựa chọn, đều là quyết định nội dung:
1. **Đổi `exercise_type`** sang đúng loại (READING/WRITING) — giữ nội dung, sửa nhãn
2. **Xoá 9 row** — nếu coi là rác scrape
3. **Giữ nguyên** và chấp nhận 9 bài "nghe" không có audio

**Không nên** backfill audio. Ghi lại đây để vòng sau không "sửa" nó theo hướng đó.

## Ghi chú kỹ thuật

Probe `compare-listening.js` truy vấn cột `content_original` và nhận `Msg 207 Invalid column name`. **Cột đó không tồn tại trong schema hiện tại** — danh sách cột thật là:
`exercise_id, audio_url, correct_answer, created_at, difficulty, exercise_type, explanation, image_url, options, order_index, question, updated_at, lesson_id`

Đây là một truy vấn hỏng trong probe, **không** phải một phát hiện về dữ liệu; ba mục trước của cùng script đã chạy đúng và đủ để kết luận. Ghi lại để không ai đọc log và tưởng có cột bị thiếu.
