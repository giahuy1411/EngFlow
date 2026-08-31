FROM python:3.12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir faster-whisper==1.1.1 flask==3.0.3 requests==2.32.3 gunicorn==23.0.0

WORKDIR /app
COPY whisper_server.py /app/whisper_server.py

EXPOSE 9002

# Single worker: the Whisper model is loaded once per process and transcription
# is CPU-heavy; multiple workers would multiply RAM usage on a 8GB host.
CMD ["gunicorn", "--bind", "0.0.0.0:9002", "--workers", "1", "--threads", "2", "--timeout", "300", "whisper_server:app"]
