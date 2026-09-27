# Speaking-assessment test fixture — real human speech

`human-speech-librispeech.wav` is a **real human recording** used by
`sweep/harness/g8-speaking-human-audio.py` to exercise the speaking-assessment pipeline
(MinIO upload → Whisper :9002 transcription → Ollama rubric) on genuine human speech rather
than synthetic TTS.

It replaces the previous approach, which read a learner's recording that happened to sit in the
local MinIO bucket — uncommitted, not reproducible from the repo, and PII-bearing. This fixture is
**in the repo, licensed, and reproducible**.

## Provenance

| Field | Value |
|---|---|
| Dataset | LibriSpeech ASR corpus (OpenSLR SLR12) — `dev-clean` split |
| Utterance id | `2277-149896-0000` |
| Source | `openslr/librispeech_asr`, config `clean`, split `validation`, row 0 (HuggingFace datasets-server) |
| Content | a single English sentence read aloud |
| Transcript | `HE WAS IN A FEVERED STATE OF MIND OWING TO THE BLIGHT HIS WIFE'S ACTION THREATENED TO CAST UPON HIS ENTIRE FUTURE` |
| Format | 16 kHz mono 16-bit PCM WAV (210 958 bytes) |
| SHA-256 | `73c1489a5a5e8a37dafa77b3f72431aba73adc187a8c762733fb657f4a3444ac` |
| Retrieved | 2026-09-27 |
| License | **CC BY 4.0** — LibriSpeech (c) 2014 Vassil Panayotov et al., derived from public-domain LibriVox audiobooks. See <https://www.openslr.org/12> and <https://creativecommons.org/licenses/by/4.0/>. |
| Modification | decoded from the corpus's FLAC to canonical PCM WAV at the original 16 kHz; **no** speech was added or altered |

This audio is distributed under **CC BY 4.0**, separately from the application's own license.
Attribution is given above; keep it with the file.

## How the probe uses it

The probe creates a **dedicated speaking prompt** whose `referenceText` is the transcript above,
uploads a copy of this WAV, calls `assess`, and asserts the Whisper transcript matches the
reference (word-set recall ≥ 0.5). The prompt and every row/object the probe creates are deleted in
a `finally` block — the fixture file itself is only ever read.
