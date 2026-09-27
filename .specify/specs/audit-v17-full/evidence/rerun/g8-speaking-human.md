# audit-v17-full (closing round) — G8: speaking với GIỌNG NGƯỜI THẬT

**Trạng thái trước:** G7 đã chứng minh pipeline chạy trên audio **thật** nhưng là **TTS tổng hợp**;
khoảng trống trung thực còn lại là "chưa có giọng người".

**Cách đóng:** dùng bản ghi **giọng người thật** đã có sẵn trong MinIO
(`speaking-uploads/video-attempts/lesson-1/line-0/64357411-e031-43b5-ab3d-2b26c1562987`, **563 524 B**,
RIFF/WAVE 22 050 Hz mono 16-bit). Không tổng hợp gì.

## Verify trước khi tin (đo trực tiếp)

Transcribe bản ghi bằng Whisper sidecar (:9002, đúng đường `SpeakingAssessmentService` dùng):
```
POST /v1/audio/transcriptions  ->  200, 3.67 s
text: "I usually wake up at 7 o'clock in the morning. After that, I have breakfast with my
       family and walk to school. In the evening, I do my homework and read a short book before bed."
```
Khớp **từng từ** với `referenceText` của prompt **50007** (`READ_ALOUD`) ⇒ **giọng người thật**, đọc đúng bài.

## Probe: `sweep/harness/g8-speaking-human-audio.py` (tự dọn)

```
prompt 50007 mode=READ_ALOUD ref_len=181
human audio: bytes=563524
upload: 200 id=40053
assess: 200 status=COMPLETED transcript_len=177 scoreTotal=9.7
transcript head: I usually wake up at 7 o'clock in the morning. After that, I have breakfast …
db media_object_key: speaking-submissions/…_audit-v17-human.wav
cleanup window (VN date): 2026-09-27
cleanup: row deleted=1 sqlError=False ; minio: Removed `local/speaking-uploads/…_audit-v17-human.wav`.
cleanup: study_days rows removed=1
minio copy gone: True | source human object intact: True (bytes 563524 -> 563524)
reference/transcript word-set recall: 0.97
G8 RESULT: PASS (HUMAN recording -> real transcript matching the reference; cleanup verified)
```

→ Chấm **giọng người thật** chạy hết chuỗi: MinIO → Whisper :9002 → alignment với reference → Ollama rubric.
Transcript 177 ký tự, **recall 0.97** so với reference 181 ký tự, `scoreTotal=9.7`.

## Tự dọn (bài học từ G7 + review chéo 2)

- Đọc `media_object_key` **từ DB trước khi** xoá row (response chỉ trả `mediaUrl`, không trả key thô).
- Dọn `study_days` theo **ngày VN** (`time.gmtime(now+7h)`) — SQL Server chạy UTC (F-17-24).
- Bọc `try/finally` (F-17-25): dọn cả khi assess lỗi.
- **Chỉ xoá bản COPY** probe upload; **tải lại object gốc** và so byte-length ⇒ chứng minh gốc nguyên vẹn
  (`563524 -> 563524`). Probe **không bao giờ** xoá bản ghi gốc.
- Verdict gồm cả kết quả cleanup + recall ≥ 0.5 (không PASS trên audio lạc đề).

## Ghi chú trung thực

- Bản ghi là **giọng người thật**, nhưng là file **đã có sẵn trong môi trường local** — **không phải**
  phiên production.
- Test local, tự dọn (row + object + study_days), chạy lại được nhiều lần; object gốc không đổi.

## Defect trong chính probe (tự bắt + sửa khi chạy)

- **`mc()` trả sai stream:** bản đầu lấy stderr làm giá trị trả về, nhưng `mc rm`/`mc stat` in ra **stdout**
  ⇒ `minio copy gone: True` và `source intact` là **may mắn**, còn `mc cat` bị bẩn bởi text. → Sửa: `mc()` trả
  `(stdout_bytes, stdout+stderr_text)`; kiểm tra object-gone bằng `mc stat`, kiểm tra gốc bằng **so byte-length**
  khi tải lại (`563524 -> 563524`). Chạy lại: `copy gone: True | source intact: True`.
- **`print` lỗi unicode** khi in stderr chứa byte nhị phân của `mc cat` (cp1252) → bỏ in stderr của `mc cat`.
