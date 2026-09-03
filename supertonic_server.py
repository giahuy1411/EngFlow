"""Supertonic TTS sidecar for EngFlow.

Runs supertonic's OWN server (supertonic.server.create_app — battle-tested
/v1/tts, /v1/health, ...) and adds a thin adapter at the exact contract the
Java SupertonicProxyTtsService expects:

    POST /synthesize  {"text","voice","lang"} -> raw WAV bytes
    GET  /health      200 once the model is ready

The adapter calls supertonic's internal _do_synthesize/_audio_response helpers
directly (voice resolution + synth lock + WAV encoding stay in the library's
hands). It deliberately does NOT proxy through the /v1/tts HTTP route: that
route's Pydantic body parsing under FastAPI 0.141 + `from __future__ import
annotations` rejects the Java client's request with 422, while a direct
TTSRequest.model_validate + helper call works (verified in-container).

supertonic's lifespan loads the model into its ServerState; uvicorn only runs
lifespan for the app it is handed, so we wrap it (see `app` below) to keep
that initialization when this adapter is the served app.

Runs locally on CPU (onnxruntime CPUExecutionProvider) so it never contends
with Ollama for the 4GB GPU. Single worker: the ~380MB ONNX model loads once
per process and synthesis is serialized behind supertonic's own synth_lock.
"""

import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field

MODEL = os.environ.get("SUPERTONIC_MODEL", "supertonic-3")
DEFAULT_VOICE = os.environ.get("SUPERTONIC_VOICE", "M1")
TOTAL_STEPS = int(os.environ.get("SUPERTONIC_STEPS", "6"))

from supertonic.server import create_app  # noqa: E402
from supertonic.server.routes import _audio_response, _do_synthesize  # noqa: E402

inner = create_app(model=MODEL)

app = FastAPI(title="engflow-tts-sidecar")


@asynccontextmanager
async def lifespan(_outer):
    # Drive supertonic's own lifespan (model load + readiness) from ours.
    async with inner.router.lifespan_context(inner):
        yield


app.router.lifespan_context = lifespan


class SynthesizeRequest(BaseModel):
    text: str = Field(min_length=1)
    voice: str | None = None
    lang: str | None = None


@app.get("/health")
async def health():
    state = inner.state.server_state
    if not state.is_ready or state.tts is None:
        raise HTTPException(status_code=503, detail="model not loaded")
    return {"status": "ok", "model": state.model, "sample_rate": state.tts.sample_rate}


@app.post("/synthesize")
def synthesize(req: SynthesizeRequest):
    state = inner.state.server_state
    if state.tts is None:
        raise HTTPException(status_code=503, detail="model not loaded")
    try:
        wav, dur = _do_synthesize(
            state,
            text=req.text,
            voice=req.voice or DEFAULT_VOICE,
            lang=req.lang or "en",
            speed=None,
            steps=TOTAL_STEPS,
            max_chunk_length=None,
            silence_duration=None,
        )
    except Exception as exc:  # noqa: BLE001 - surface synthesis failures to caller
        raise HTTPException(status_code=500, detail=f"synthesis failed: {exc}") from exc
    return _audio_response(state, wav, "wav", dur)


if __name__ == "__main__":  # pragma: no cover - uvicorn is the production entrypoint
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8001)
