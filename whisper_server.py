"""OpenAI-compatible Whisper sidecar backed by faster-whisper.

Exposes POST /v1/audio/transcriptions (multipart: file + optional language)
returning {"text": "..."} so the EngFlow backend can transcribe speaking
submissions without any cloud API key. Runs locally on CPU or NVIDIA GPU.

Audio is written to a temp file and passed to ctranslate2's decoder by path:
AVIOContext (stream mode) segfaults the worker on some webm/opus inputs,
while ffmpeg-by-path handles them reliably.
"""

import os
import tempfile

from flask import Flask, jsonify, request
from faster_whisper import WhisperModel

MODEL_SIZE = os.environ.get("WHISPER_MODEL", "base")
DEVICE = os.environ.get("WHISPER_DEVICE", "cpu")
COMPUTE_TYPE = os.environ.get("WHISPER_COMPUTE_TYPE", "int8")

app = Flask(__name__)
print(f"Loading faster-whisper model '{MODEL_SIZE}' on {DEVICE}/{COMPUTE_TYPE}...", flush=True)
model = WhisperModel(MODEL_SIZE, device=DEVICE, compute_type=COMPUTE_TYPE)
print("Whisper model loaded.", flush=True)


@app.get("/health")
def health():
    return jsonify({"status": "ok", "model": MODEL_SIZE, "device": DEVICE})


@app.post("/v1/audio/transcriptions")
def transcribe():
    audio = request.files.get("file")
    if audio is None:
        return jsonify({"error": "missing 'file' field"}), 400
    language = request.form.get("language") or None
    suffix = os.path.splitext(audio.filename or "")[1] or ".audio"
    fd, tmp_path = tempfile.mkstemp(suffix=suffix)
    os.close(fd)
    try:
        audio.save(tmp_path)
        segments, info = model.transcribe(
            tmp_path,
            language=language,
            vad_filter=True,
            beam_size=1,
        )
        text = " ".join(segment.text.strip() for segment in segments).strip()
    except Exception as exc:  # noqa: BLE001 - surface transcription failures to caller
        app.logger.exception("transcription failed")
        return jsonify({"error": f"transcription failed: {exc}"}), 500
    finally:
        try:
            os.remove(tmp_path)
        except OSError:
            pass
    return jsonify({"text": text, "language": info.language})


if __name__ == "__main__":  # pragma: no cover - gunicorn is the production entrypoint
    app.run(host="0.0.0.0", port=9002)
