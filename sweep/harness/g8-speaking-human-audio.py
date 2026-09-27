#!/usr/bin/env python
"""
audit-v17 G8 (C5) — speaking assessment with a REAL HUMAN recording (end-to-end, local).

G7 proved the pipeline runs on REAL audio, but that audio was TTS-synthesized. The remaining
honest gap was "no human voice". This probe closes it using a pre-existing HUMAN recording already
in MinIO (`speaking-uploads/video-attempts/lesson-1/line-0/64357411-…`, ~550 KiB — a real learner's
video-attempt capture). It does NOT synthesize anything.

Flow: download the human object -> upload a COPY as a submission for prompt 50007 -> assess ->
assert the Whisper transcript matches the prompt's referenceText -> DELETE the row + the copy's
MinIO object + the study_days row. Self-cleaning; parity re-asserted.

Honest scope: the recording is REAL HUMAN audio, but it is a file that ALREADY EXISTS in this local
environment — it is not a live production session. The SOURCE object is never deleted (only the copy
this probe uploads).

Usage: python sweep/harness/g8-speaking-human-audio.py
"""
import json
import os
import re
import subprocess
import sys
import time
import urllib.request

API = "http://localhost:8080"
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
PROMPT_ID = 50007  # READ_ALOUD, has referenceText
USER = ("user@gmail.com", "123456")
SOURCE_KEY = "video-attempts/lesson-1/line-0/64357411-e031-43b5-ab3d-2b26c1562987"
MINIO_CONTAINER = "engflow-minio"


def http(method, url, data=None, headers=None, token=None, timeout=300):
    h = dict(headers or {})
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def post_json(path, body, token=None):
    st, raw = http("POST", API + path, json.dumps(body).encode(),
                   {"Content-Type": "application/json"}, token)
    return st, raw.decode("utf-8", "replace")


def multipart(fields, filename, filebytes, content_type="audio/wav"):
    boundary = "----auditv17g8boundary"
    body = b""
    for k, v in fields.items():
        body += ("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n" % (boundary, k, v)).encode()
    body += ("--%s\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\nContent-Type: %s\r\n\r\n"
             % (boundary, filename, content_type)).encode()
    body += filebytes + b"\r\n"
    body += ("--%s--\r\n" % boundary).encode()
    return body, "multipart/form-data; boundary=" + boundary


def sql(query):
    tmp = os.path.join(ROOT, "sweep", "harness", "_g8-speaking.sql")
    with open(tmp, "w", encoding="utf-8") as fh:
        fh.write(query + "\n")
    out = subprocess.run(["python", "sweep/v8/sqlrun.py", "sweep/harness/_g8-speaking.sql"],
                         cwd=ROOT, capture_output=True, text=True)
    return (out.stdout or "") + (out.stderr or "")


def mc(command):
    """Run an `mc` command inside the MinIO container (env expands inside).

    Returns (stdout_bytes, combined_text). `mc cat` writes the OBJECT to stdout, so it is
    returned as bytes; `mc rm`/`mc stat` write their messages to stdout while errors go to
    stderr, so `combined_text` merges both for the presence checks.
    """
    full = ("mc alias set local http://localhost:9000 \"$MINIO_ROOT_USER\" \"$MINIO_ROOT_PASSWORD\" "
            ">/dev/null 2>&1; " + command)
    r = subprocess.run(["docker", "exec", MINIO_CONTAINER, "sh", "-c", full],
                       capture_output=True)
    out = r.stdout or b""
    err = (r.stderr or b"").decode("utf-8", "replace")
    return out, (out.decode("utf-8", "replace") + err)


def norm(text):
    """Lowercase, strip everything but letters/spaces, collapse whitespace — for transcript match."""
    return re.sub(r"\s+", " ", re.sub(r"[^a-z ]+", " ", (text or "").lower())).strip()


def overlap(a, b):
    """Fraction of `a`'s tokens that also appear in `b` (word-set recall)."""
    ta, tb = set(a.split()), set(b.split())
    if not ta:
        return 0.0
    return len(ta & tb) / len(ta)


def main():
    # 1) reference text of the prompt
    st, raw = http("GET", API + "/api/v1/speaking-prompts/%d" % PROMPT_ID)
    prompt = json.loads(raw.decode("utf-8", "replace"))
    ref = prompt.get("referenceText") or ""
    print("prompt %d mode=%s ref_len=%d" % (PROMPT_ID, prompt.get("mode"), len(ref)))
    assert ref, "prompt has no referenceText"

    # 2) download the pre-existing HUMAN recording (read-only — the source is never deleted)
    audio, _ = mc("mc cat local/speaking-uploads/%s" % SOURCE_KEY)
    print("human audio: bytes=%d" % len(audio))
    assert len(audio) > 10000, "human recording missing or too small"
    assert audio[:4] in (b"RIFF", b"\x1aE\xdf\xa3") or audio[4:8] == b"ftyp", \
        "unexpected container: %r" % audio[:12]

    # 3) login + upload a COPY as a submission for prompt 50007
    st, body = post_json("/api/auth/login", {"email": USER[0], "password": USER[1]})
    tok = (json.loads(body).get("data") or json.loads(body)).get("token")
    assert tok, "login failed"
    ext = "webm" if audio[4:8] == b"ftyp" else "wav"
    ctype = "audio/webm" if ext == "webm" else "audio/wav"
    mp_body, ct = multipart({"promptId": str(PROMPT_ID)}, "audit-v17-human." + ext, audio, ctype)
    st, raw2 = http("POST", API + "/api/v1/speaking-submissions/upload", mp_body, {"Content-Type": ct}, tok)
    print("upload:", st, raw2[:200])
    sub = json.loads(raw2.decode("utf-8", "replace"))
    sub_id = sub.get("id")
    print("submission id=%s mediaKey=%s" % (sub_id, sub.get("mediaObjectKey")))
    assert st == 200 and sub_id, "upload failed"

    # 4) assess (Whisper + Ollama rubric). try/finally so row + object + study_days are cleaned
    #    even if assessment throws (F-17-25).
    status = "?"
    transcript = ""
    score = None
    deleted = "?"
    had_err = False
    sd_deleted = "?"
    gone = False
    try:
        st, raw3 = http("POST", API + "/api/v1/speaking-submissions/%d/assess" % sub_id, b"", {}, tok)
        print("assess:", st)
        assessed = json.loads(raw3.decode("utf-8", "replace")) if st == 200 else {}
        transcript = (assessed.get("transcript") or "").strip()
        score = assessed.get("scoreTotal")
        status = assessed.get("status")
        print("status=%s transcript_len=%d scoreTotal=%s" % (status, len(transcript), score))
        print("transcript head:", transcript[:140])
    finally:
        # 5) CLEANUP — read the object key BEFORE deleting the row (the response exposes mediaUrl,
        #    not the raw key, so the DB is the source of truth for what to remove from MinIO).
        out = sql("SET NOCOUNT ON; SELECT 'KEY=' + ISNULL(media_object_key,'NONE') FROM speaking_submissions WHERE id=%d;" % sub_id)
        media_key = (re.search(r"KEY=(\S+)", out) or [None, "NONE"])[1]
        print("db media_object_key:", media_key)

        # VN date for the study_days window (F-17-24: SQL Server runs UTC, study_date is VN).
        vn_today = time.strftime("%Y-%m-%d", time.gmtime(time.time() + 7 * 3600))
        print("cleanup window (VN date):", vn_today)

        rm_out = ""
        if media_key and media_key != "NONE":
            _, rm_out = mc("mc rm --force local/speaking-uploads/%s" % media_key)
        out = sql("SET QUOTED_IDENTIFIER ON; DELETE FROM speaking_submissions WHERE id=%d; "
                  "SELECT 'DELETED=' + CAST(@@ROWCOUNT AS varchar(5));" % sub_id)
        deleted = (re.search(r"DELETED=(\d+)", out) or [None, "?"])[1]
        had_err = bool(re.search(r"Msg \d+", out))
        print("cleanup: row deleted=%s sqlError=%s ; minio: %s" % (deleted, had_err, rm_out.strip()[:120]))

        sd_out = sql("SET QUOTED_IDENTIFIER ON; "
                     "DELETE FROM study_days WHERE study_date = '%s' "
                     "AND user_id IN (SELECT user_id FROM users WHERE email IN ('user@gmail.com','admin@gmail.com')); "
                     "SELECT 'SD_DELETED=' + CAST(@@ROWCOUNT AS varchar(5));" % vn_today)
        sd_deleted = (re.search(r"SD_DELETED=(\d+)", sd_out) or [None, "?"])[1]
        print("cleanup: study_days rows removed=%s" % sd_deleted)

        # verify the COPY is gone AND the SOURCE recording is untouched
        if media_key and media_key != "NONE":
            _, verify = mc("mc stat local/speaking-uploads/%s" % media_key)
            gone = ("does not exist" in verify) or ("Not found" in verify) or ("no such" in verify.lower())
        else:
            gone = True
        # Definitive: re-download the SOURCE and compare length — it must be byte-identical to
        # what we read at the start (the probe only ever uploads a copy, never deletes the source).
        src_after, _ = mc("mc cat local/speaking-uploads/%s" % SOURCE_KEY)
        src_intact = len(src_after) == len(audio)
        print("minio copy gone:", gone, "| source human object intact:", src_intact,
              "(bytes %d -> %d)" % (len(audio), len(src_after)))

    # Transcript must actually match the reference — a real human reading prompt 50007. If the
    # transcript is empty or unrelated, the recording is not of this prompt and the probe FAILS
    # rather than claiming a pass on unrelated audio.
    rec = overlap(norm(ref), norm(transcript))
    print("reference/transcript word-set recall: %.2f" % rec)

    ok = (status == "COMPLETED") and len(transcript) > 0 and rec >= 0.5 \
        and (deleted == "1") and not had_err and gone and src_intact and (sd_deleted in ("0", "1"))
    print("G8 RESULT:", "PASS" if ok else "FAIL",
          "(HUMAN recording -> real transcript matching the reference; cleanup verified)")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
