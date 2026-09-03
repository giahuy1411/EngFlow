FROM python:3.12-slim

RUN pip install --no-cache-dir "supertonic[serve]==1.3.1"

WORKDIR /app
COPY supertonic_server.py /app/supertonic_server.py

EXPOSE 8001

# Single worker: the ~380MB ONNX model loads once per process and synthesis is
# serialized behind a lock; multiple workers would multiply RAM on the 8GB host.
CMD ["uvicorn", "supertonic_server:app", "--host", "0.0.0.0", "--port", "8001", "--workers", "1"]
