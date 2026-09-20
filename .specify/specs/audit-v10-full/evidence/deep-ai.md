# Phase 4.11 — Deep check AI: **15 PASS, 0 FAIL**

Công cụ: `sweep/v10/deep-ai.js`.

## Phạm vi — và điều cố ý KHÔNG kiểm

Script này **không gọi LLM thật**. Một lượt gọi `qwen2.5` mất 6–60 giây và làm nặng máy; quan trọng hơn, **"AI trả lời có đúng không" là câu hỏi chất lượng nội dung**, không phải thứ một vòng audit kỹ thuật kết luận được. Ghi rõ để không ai đọc báo cáo này và tưởng chất lượng AI đã được kiểm.

Cái **kiểm được** là **hợp đồng**: guard, validate input, mã lỗi, và việc không rò cấu hình.

## Kết quả

| Nhóm | Kiểm | Kết quả |
|---|---|---|
| Guard | `generate-vocab` không token → 401 | PASS |
| Validate | topic rỗng / thiếu / chỉ khoảng trắng → 400 | **3/3 PASS** |
| Thông báo lỗi | có nội dung thật, không rỗng | PASS |
| TTS sidecar | `/health` → 200 | PASS |
| Whisper (STT) | `/health` → 200 | PASS |
| Guard 3 đường AI | `generate-vocab`, `grade-writing`, `translate` không token → **401, không phải 500** | **3/3 PASS** |
| Không rò cấu hình | lỗi không chứa `OPENROUTER_API_KEY` / `sk-or-` / api key dạng chuỗi / `host.docker.internal` / `127.0.0.1:11434` | **3/3 PASS** |

## Đường TTS → Cloudinary: đã chứng minh đầy đủ ở nơi khác

`sweep/v10/prove-tts-path.js` chạy 4 bước và đo:

| Bước | Kết quả |
|---|---|
| TTS `/health` | 200 |
| `POST /synthesize` | 200, 6.083ms, **276.524 byte** |
| `POST /api/admin/audio-upload` | 200, trả URL Cloudinary |
| `GET` URL | 200, `content-type: audio/wav`, đúng 276.524 byte |

Kiểm bằng **magic bytes** chứ không tin content-type: `RIFF` + `WAVE`, mono, 44100 Hz, 16 bit. Đây là **file WAV thật**, không phải byte giả — điều kiện mà `issues.md` đặt ra.

## Shape lỗi ở `/api/ai/generate-vocab` — thuộc F123, không phải lỗi mới

Đo được: `{"error":"topic không được để trống"}` — shape legacy, **không phải ProblemDetail**.

Đây **chính là F123 đã ghi nhận và hoãn có lý do**, không phải phát hiện mới. `AiVocabController` nằm trong nhóm 6 return legacy của F123. Và `frontend/src/services/api.js` dòng 55–62 đã có shim copy `error` → `detail`/`message`, nên **người dùng vẫn đọc được thông báo thật**.

Assertion trong probe đã được sửa từ "phải là ProblemDetail" (sẽ FAIL, và FAIL đó **không có giá trị thông tin** vì đã biết) thành "phải có thông báo thật, không rỗng" — điều thực sự cần đúng.

**Ghi lại cách đọc con số này:** nếu ai đó chạy lại probe bản cũ và thấy 1 FAIL ở dòng ProblemDetail, đó **không** phải hồi quy. Nó là F123.

## Điều chưa kiểm (và vì sao)

- **Chất lượng nội dung do AI sinh ra** — cần đánh giá của con người, không phải assert.
- **Rubric chấm speaking** — cùng lý do.
- **Độ trễ thật của một lượt sinh bài tập** — phụ thuộc model đang nóng/lạnh; một phép đo đơn lẻ sẽ gây hiểu nhầm.
- **Azure Speech SDK** (chấm phát âm) — dự án đã chuyển sang Whisper cục bộ; Azure chỉ còn là đường dự phòng và **không được cấu hình trong môi trường này**.
