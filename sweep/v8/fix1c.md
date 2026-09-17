1. **Cloudinary: key ĐANG DÙNG, không phải demo.** `.env` có `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET`
   thật (đã mask-kiểm chứng 2026-09-15) và container nhận đủ. Với file THẬT:
   `POST /api/auth/avatar/upload` → **200** + URL `.../engflow/avatars/<pubid>.png`;
   `POST /api/admin/audio-upload` (WAV thật 563 kB) → **200** + URL `.../engflow/audio/....wav` —
   đúng đường MCP listening tạo audio. Kết luận "demo creds" ở bản báo cáo đầu là **SAI** (tôi
   probe bằng bytes giả, rồi đọc fallback `demo` trong `application.properties` mà không kiểm `.env`)
   và đã được sửa ở cả AGENTS.md + REPORT này.
   CÒN MỘT LỆCH NHỎ ĐANG MỞ: file ảnh **corrupt** → avatar **500** (Cloudinary từ chối server-side,
   handler avatar gộp Exception chung → 500) trong khi audio-upload trả **400 "Unsupported video format
   or file"**. Hai endpoint không nhất quán; muốn siết thì bắt `IllegalArgumentException`/`ClientError`
   trong `uploadAvatar` → 400 như `uploadAudio`. Không phải mất chức năng upload.
