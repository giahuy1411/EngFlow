# audit-v17-full (rerun) — G7: speaking assessment với AUDIO THẬT

**Trạng thái v17:** BLOCKED ("cần mic/file thật; mic giả → silence → FAILED đúng thiết kế").
**Rerun:** **VERIFIED** — dùng **TTS sidecar :8001** sinh giọng nói thật ⇒ chạy được pipeline thật.

## Vì sao test được

`whisper.Dockerfile`/`supertonic.Dockerfile` chạy local; `POST http://localhost:8001/synthesize {"text","lang"}`
trả **WAV thật** (đo: 1 044 524 bytes, `RIFF`/`WAVE`, 16-bit PCM mono 44100 Hz). Prompt **50007** (`READ_ALOUD`) có
`referenceText` 181 ký tự → ta đọc **chính đoạn tham chiếu đó** ⇒ pipeline có gì đó thật để chấm.

## Probe: `sweep/harness/g7-speaking-real-audio.py` (tự dọn)

```
prompt 50007 mode=READ_ALOUD ref_len=181
tts: 200 bytes=1044524 riff=b'RIFF'                 ← WAV thật
upload: 200 {"id":40048, ...}                        ← premium OK
assess: 200
status=COMPLETED transcript_len=181 scoreTotal=9.7   ← TRANSCRIPT THẬT + ĐIỂM THẬT
transcript head: I usually wake up at seven o'clock in the morning. After that, I have breakfast ...
db media_object_key: speaking-submissions/e3330796-...-_audit-v17-tts.wav
cleanup: row deleted=1 sqlError=False ; minio: Removed `local/speaking-uploads/speaking-submissions/e3330796-...`
minio object gone: True
G7 RESULT: PASS
```

→ Chứng minh **cả chuỗi**: MinIO upload → Whisper :9002 transcribe → alignment với reference → Ollama rubric → điểm.
Transcript khớp reference (181 ký tự) và `scoreTotal=9.7`.

## Probe 2 (độc lập) + bài học tự sửa

- Lần chạy đầu `G7 PASS` **nhưng** `mediaKey=None` ⇒ **object MinIO bị bỏ lại** (orphan 1020 KiB). Tôi phát hiện
  bằng cách **liệt kê object hôm nay** trong bucket, **đã xoá** object orphan, rồi **sửa probe**: đọc
  `media_object_key` **từ DB trước khi** xoá row (response chỉ trả `mediaUrl`, không trả key thô).
- Chạy lại: `minio: Removed …` + `minio object gone: True` ⇒ dọn **đủ cả row và object**.
- **Finding mới phát hiện khi assert parity: `STUDY_DAYS=5`.** `assess()` gọi
  `SpeakingSubmissionService.recordStudy()` (`:134`) ⇒ **ghi một hàng `study_days`** (đúng lớp F-16-01/F-17-07).
  Probe ban đầu chỉ dọn row submission ⇒ sót study day. → **Đã sửa probe** dọn thêm `study_days` (scoped 2 tài khoản
  probe + hôm nay). Chạy lại: `study_days rows removed=1` và parity về `STUDY_DAYS=4`.
- `speaking_submissions` về **29** (baseline), parity không đổi sau lần chạy tự-dọn.

## Ghi chú trung thực

- Đây là **audio tổng hợp (TTS)** — **không** phải giọng người thật. Nó chứng minh **pipeline chạy đúng trên audio
  thật** (khác hẳn "mic giả → silence"), nhưng không chứng minh chất lượng chấm với giọng người.
- Test **local**, tự dọn (row + MinIO object + study_days), chạy lại được nhiều lần.
- **Bài học cho harness:** mọi hành động **nộp speaking** phải dọn **cả** `speaking_submissions` **và**
  `study_days` — đã ghi vào `AGENTS.md`.

---

## Vòng review chéo 2 — defect trong chính probe này (đã sửa)

- **F-17-24 (`GETDATE()` UTC vs VN):** probe dọn `study_days` bằng `CAST(GETDATE() AS DATE)` — SQL Server chạy **UTC**
  còn `study_date` ghi theo **VN** ⇒ từ 17:00–24:00 UTC (= 00:00–07:00 VN hôm sau) DELETE khớp **0 hàng** ⇒ sót residue
  (đúng lớp lỗi ghi ở `lib.js:42-51`). → **Sửa:** tính ngày VN bằng `time.gmtime(now + 7h)`; in
  `cleanup window (VN date): 2026-09-26`.
- **F-17-25 (thiếu try/finally + verdict bỏ qua cleanup):** verdict cũ chỉ xét `assess`+`deleted` ⇒ có thể PASS khi
  còn object/study_days. → **Sửa:** bọc `finally` (đã gặp thật: khi `time` chưa import, row **40050** bị bỏ lại —
  **đã dọn tay** cả row + object) và verdict nay gồm `gone` + `sd_deleted`.

**Kết quả sau fix:** `cleanup window (VN date): 2026-09-26`, `row deleted=1`, `minio: Removed …`,
`study_days rows removed=1`, `minio object gone: True`, `G7 RESULT: PASS (… cleanup verified)`.
