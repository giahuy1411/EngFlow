# Nghiên cứu: Chấm điểm luyện nói tiếng Anh (EngFlow)

> Research ngày 30/08/2025 — mọi kết luận kèm URL chính thức (docs nhà cung cấp).

## 1. Trả lời 4 câu hỏi

### 1) Azure AI Speech Pronunciation Assessment (REST, không SDK)

- **Endpoint REST (short audio)**: `POST https://{resource}.cognitiveservices.azure.com/stt/speech/recognition/conversation/cognitiveservices/v1?language=en-US`
  - Header `Pronunciation-Assessment`: Base64 của JSON config (`ReferenceText`, `GradingSystem`, `Granularity`, `Dimension`, `EnableMiscue`, `EnableProsodyAssessment`, `ScenarioId`).
  - Header `Content-type`: `audio/wav; codecs=audio/pcm; samplerate=16000` hoặc `audio/ogg; codecs=opus`.
  - Giới hạn: ≤ 60s audio; **riêng pronunciation assessment ≤ 30s**.
  - Nguồn: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/rest-speech-to-text-short
- **Reference text**: Bắt buộc trên REST short API (scripted). Trên SDK, `ReferenceText` là **optional** — không có text thì chạy "unscripted assessment" (speaking scenario): trả Accuracy/Fluency/Prosody/PronScore, **không có CompletenessScore**, và dùng model STT riêng; docs khuyên nếu cần text chuẩn thì gọi Azure STT trước rồi chạy scripted. Nguồn: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/how-to-pronunciation-assessment
- **Chấm được gì**: AccuracyScore (đến **phoneme/syllable/word level** — syllable chỉ en-US), FluencyScore, CompletenessScore (chỉ scripted), ProsodyScore (**chỉ en-US**), PronScore (trọng số), ErrorType per word (None/Omission/Insertion/Mispronunciation/UnexpectedBreak/MissingBreak/Monotone), NBestPhonemes. Nguồn: how-to-pronunciation-assessment (trên).
- **Giá** (https://azure.microsoft.com/en-us/pricing/details/speech/):
  - **Free F0**: **5 audio giờ STT miễn phí/tháng** (shared giữa Standard/Custom; batch không tính). PA tính phí như STT Standard.
  - **S1 (pay-as-you-go, us-east)**: real-time STT = **$1.00/giờ** (~$0.0167/phút); add-on **Pronunciation Assessment (prosody) +$0.30/giờ**.
  - → ~100 bài × 1 phút ≈ 1.7 giờ ≈ **$1.7–2.2/tháng**, hoặc **$0 nếu nằm trong 5h free F0**.
- **Format REST**: chỉ **WAV/PCM 16kHz mono** và **OGG/OPUS**. **KHÔNG có webm** trên REST short (webm/opus chỉ qua Speech SDK). → MediaRecorder webm phải transcode (ffmpeg) trước khi gọi REST. Nguồn: rest-speech-to-text-short (trên).

### 2) Whisper STT + LLM rubric

- **OpenAI transcription**: file nhận `flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, webm` → **webm/opus OK**. Nguồn: https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create
  - Giá (https://developers.openai.com/api/docs/pricing): **whisper-1 = $0.006/phút**; `gpt-4o-transcribe` $0.006/phút; **`gpt-4o-mini-transcribe` $0.003/phút**.
- **LLM chấm rubric**: **không có chuẩn chính thức nào "khuyến nghị rubric"**. Mẫu tham khảo chính thức duy nhất là **Azure "Content assessment"** (đã retire khỏi SDK, docs hướng dẫn thay bằng Azure OpenAI): prompt chấm `{vocabulary, grammar, topic}` 0–100 từ transcript, kèm hướng dẫn bỏ filler ("uh"), thêm dấu câu. Nguồn: how-to-pronunciation-assessment#content-assessment. **Giới hạn**: LLM chỉ thấy text → **không đánh giá được phát âm** (chỉ grammar/vocab/fluency-suy-diễn từ độ dài, filler, lặp từ).
- **Gemini**: input audio hỗ trợ `WAV, MP3, AIFF, AAC, OGG, FLAC, MPEG, M4A, L16, Opus, ALAW, MULAW, WebM` → **WebM/Opus OK** (https://ai.google.dev/gemini-api/docs/audio). Giá **Gemini 2.5 Flash** (2.0 Flash đã rời bảng giá): input **$0.30/1M tokens (text), $1.00/1M (audio)**, output $2.50/1M; audio tốn ~32 token/s ≈ 1.9k token/phút → 100 phút ≈ **~$0.2–0.3/tháng** (https://ai.google.dev/gemini-api/docs/pricing). Có **free tier** (giá "Free of charge", giới hạn theo tier — https://ai.google.dev/gemini-api/docs/rate-limits). Có API key miễn phí, không cần thẻ.
- **Self-host phát âm**: 
  - **Kaldi GOP** chính thức: `compute-gop.cc` — https://kaldi-asr.org/doc/compute-gop_8cc.html (C++, chạy Docker `kaldiasr/kaldi`, cần model + forced alignment).
  - **wav2vec2** (docs chính thức https://huggingface.co/docs/transformers/model_doc/wav2vec2) + forced alignment (https://docs.pytorch.org/audio/stable/tutorials/forced_alignment_tutorial.html); repo mẫu (không chính thức, chất lượng thấp): OpenPronounce (Wav2Vec2+DTW, 43★), lark (FastAPI+HF, 14★).
  - **Montreal Forced Aligner** (Kaldi-based, 1.9k★) — https://github.com/MontrealCorpusTools/Montreal-Forced-Aligner.

### 3) Nhà cung cấp chuyên dụng (tóm tắt nhanh)

- **ETS SpeechRater** (https://www.ets.org/speechrater): engine chấm speaking của TOEFL iBT — **chỉ license B2B, không công bố giá** → không phù hợp MVP cá nhân.
- **SpeechAce** (https://www.speechace.com): API chấm phát âm/fluency thương mại (phoneme-level) — **pricing contact-sales, B2B** → không phù hợp MVP cá nhân.
- **"Sonda", "Docear"**: không phải nhà cung cấp pronunciation assessment (Sonda = data lineage; Docear = quản lý tài liệu học thuật) — có lẽ nhầm tên; bỏ qua.

### 4) Thuật toán thuần (không AI) trong Java/Spring

- **GOP**: cần forced alignment (acoustic model) — **không có thư viện JVM native**. Thực tế: chạy Kaldi/MFA như **sidecar Docker** + Spring gọi qua CLI/HTTP, hoặc cURL → độ phức tạp cao (4–5/5). Nguồn: https://kaldi-asr.org/doc/compute-gop_8cc.html
- **DTW trên MFCC**: tự viết được trong Java (~100–200 dòng: MFCC + DP), không cần thư viện nặng; smile (JVM, 6.4k★) có công cụ ML nhưng không có STT/MFCC sẵn dùng cho bài này — https://github.com/haifengl/smile. Nhưng DTW thuần chỉ so khay giọng mẫu, **không tạo điểm phát âm có ý nghĩa giáo dục** mà không có baseline chuẩn → chỉ phù hợp demo so-sánh-giọng-mẫu (2–3/5).
- **WER từ STT transcript**: đơn giản nhất — **Apache Commons Text `LevenshteinDistance`** (chính thức): https://commons.apache.org/proper/commons-text/javadocs/api-release/org/apache/commons/text/similarity/LevenshteinDistance.html → so reference text vs transcript (1/5, thêm ~50 dòng code). CMU Sphinx4 (pure Java, có aligner nhưng model cũ, tiếng Anh) — https://github.com/cmusphinx/sphinx4.

## 2. Bảng so sánh (giả định ~100 bài/tháng, ~1 phút audio/bài)

| Phương án | Chấm được gì | Chi phí/tháng (~100 bài) | Độ phức tạp (1–5) | Rủi ro chính |
|---|---|---|---|---|
| **A. Thuần WER** (Commons Text so reference ↔ transcript từ STT bất kỳ) | Nội dung (từ đúng/thiếu/thừa). Không phát âm | $0 (thuật toán) + STT | 1 | Cần STT; WER ≠ điểm nói tự nhiên |
| **B. Ollama qwen2.5:1.5b chấm transcript** (stack hiện có) | Grammar/vocab rất hạn chế; model 1.5b yếu | $0 | 1 | Model nhỏ → điểm không ổn định/không tin cậy |
| **C. Gemini 2.5 Flash** (audio thẳng vào model) | Nội dung + rubric (grammar/vocab/fluency suy từ transcript); không phoneme | **~$0–0.3** (free tier đủ 100 bài) | 1 | Không chấm phát âm thật; cần key Gemini (free, không cần thẻ) |
| **D. Whisper API + GPT-4o-mini** | Nội dung; không phát âm | ~$0.6 (STT) + ~$0.1 (LLM) | 1 | Cần key OpenAI trả phí; WER với accent Việt |
| **E. Azure PA REST (scripted)** | **Phát âm phoneme/word-level + fluency + prosody + completeness** | **$0 với F0 (5h free)**; S1 ~$1.7–2.2 | 2 | REST chỉ nhận WAV/PCM hoặc OGG/Opus 16k mono → **phải transcode webm**; ≤30s/bài; en-US cho prosody |
| **F. Kaldi GOP self-host (Docker)** | Phát âm phoneme-level (GOP score) | $0 license + server RAM/CPU | 4–5 | Cần acoustic model, xây pipeline alignment; nhiều thời gian kỹ thuật |
| **G. SpeechRater / SpeechAce** | Full speaking assessment thương mại | Không công bố (contact sales) | 3 | Không phù hợp cá nhân; chi phí B2B |

## 3. Khuyến nghị 2 giai đoạn

### (a) MVP tuần này — stack hiện có (Ollama text-only, chưa key nào)

1. **Frontend** (Vue): MediaRecorder ghi **webm/opus**, upload thẳng vào **MinIO** (đã có), lưu object key vào SQL Server.
2. **Backend** (Spring Boot): lấy reference text của bài nói; gọi Ollama (OpenRouter-compatible endpoint hiện có) với prompt rubric JSON `{grammar, vocab, fluency_estimate, feedback}` **chỉ khi có transcript** — nếu chưa có STT, để user dán transcript tự ghi (đăng ký miễn phí) HOẶC dùng C (Gemini free) nếu chịu tạo key.
3. **Chấm phát âm "giả lập" MVP**: tính **WER + độ phủ từ (recall)** bằng Apache Commons Text giữa reference text và transcript → điểm "nội dung"; **không claim** là điểm phát âm.
4. **Không gọi AI audio nào** trong MVP → chi phí $0, không phụ thuộc key.

> Nếu chấp nhận tạo 1 key miễn phí (không cần thẻ): thay B bằng **Gemini 2.5 Flash free tier** — gửi thẳng audio + reference text trong 1 request, nhận transcript + rubric JSON (C) — vẫn $0 và chất lượng cao hơn hẳn qwen2.5:1.5b.

### (b) Bản nâng cấp khi có key Azure (và/hoặc Gemini trả phí)

1. **Azure Pronunciation Assessment REST (scripted)** cho điểm phát âm thật:
   - Transcode webm/opus → **OGG/Opus 16kHz mono** bằng ffmpeg (sidecar Docker trong cùng pod/máy) hoặc client ghi WAV 16k PCM.
   - `POST /stt/speech/recognition/conversation/cognitiveservices/v1?language=en-US` + header `Pronunciation-Assessment` (Base64 JSON), audio ≤ 30s/bài.
   - Lưu kết quả JSON (phoneme/word/fluency/prosody) vào SQL Server, gắn với bài luyện + audio MinIO.
   - Chi phí: **bắt đầu F0 5h free**; khi vượt mới lên S1 (~$1.7/tháng/100 bài).
2. **Gemini hoặc GPT-4o-mini** chấm phần nội dung (grammar/vocab/topic) theo mẫu Azure content-assessment → hợp nhất thành report: phát âm (Azure) + nội dung (LLM).
3. Giữ pipeline MVP (WER + Ollama) làm **fallback** khi Azure/LLM key hết hạn quota.

## 4. URL nguồn (đều là docs chính thức của nhà cung cấp)

- Azure PA (SDK + unscripted + prosody + PronScore): https://learn.microsoft.com/en-us/azure/ai-services/speech-service/how-to-pronunciation-assessment
- Azure STT REST short audio (endpoint, header `Pronunciation-Assessment`, formats WAV/OGG, ≤30s): https://learn.microsoft.com/en-us/azure/ai-services/speech-service/rest-speech-to-text-short
- Azure Speech pricing (F0 5h free, S1 $1/h, prosody +$0.30/h): https://azure.microsoft.com/en-us/pricing/details/speech/
- Azure language learning (grammar/vocab trong Speech Studio, en-US, preview): https://learn.microsoft.com/en-us/azure/ai-services/speech-service/language-learning-with-pronunciation-assessment
- OpenAI Create transcription (formats, incl. webm): https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create
- OpenAI pricing (whisper-1 $0.006/min, gpt-4o-mini-transcribe $0.003/min, gpt-4o-mini token giá): https://developers.openai.com/api/docs/pricing
- Gemini audio formats (WebM/Opus OK): https://ai.google.dev/gemini-api/docs/audio
- Gemini pricing (2.5 Flash: $0.30/1M text in, $1.00/1M audio in, $2.50/1M out; free tier): https://ai.google.dev/gemini-api/docs/pricing
- Gemini rate limits: https://ai.google.dev/gemini-api/docs/rate-limits
- Kaldi GOP (compute-gop.cc): https://kaldi-asr.org/doc/compute-gop_8cc.html
- Montreal Forced Aligner: https://github.com/MontrealCorpusTools/Montreal-Forced-Aligner
- wav2vec2 (HF docs): https://huggingface.co/docs/transformers/model_doc/wav2vec2
- PyTorch forced alignment tutorial: https://docs.pytorch.org/audio/stable/tutorials/forced_alignment_tutorial.html
- Apache Commons Text LevenshteinDistance (WER): https://commons.apache.org/proper/commons-text/javadocs/api-release/org/apache/commons/text/similarity/LevenshteinDistance.html
- CMU Sphinx4 (pure Java STT/aligner): https://github.com/cmusphinx/sphinx4
- ETS SpeechRater (B2B): https://www.ets.org/speechrater
- SpeechAce (B2B): https://www.speechace.com
- Ollama qwen2.5 (model text-only): https://ollama.com/library/qwen2.5
