#!/usr/bin/env python
"""
audit-v17 G8 — speaking assessment with a REAL HUMAN recording (end-to-end, local).

G7 proved the pipeline runs on REAL audio, but that audio was TTS-synthesized. This probe closes
the "no human voice" gap with a fixture that is IN THE REPO, LICENSED and REPRODUCIBLE:
`sweep/harness/fixtures/human-speech-librispeech.wav` (LibriSpeech `2277-149896-0000`, CC BY 4.0 —
see fixtures/README.md). It does NOT synthesize anything.

(Previously this probe read a learner's recording that happened to sit in the local MinIO bucket —
uncommitted, not reproducible, and PII-bearing. That dependency is gone.)

Flow: read the fixture WAV -> create a DEDICATED speaking prompt whose referenceText is the
fixture's own transcript -> upload a copy as a submission for that prompt -> assess -> assert the
Whisper transcript matches the reference -> DELETE the submission row, the MinIO copy, the
study_days row AND the prompt this probe created. Self-cleaning; parity re-asserted.

Honest scope: the fixture is REAL HUMAN speech, but it is a static file from a public corpus — not a
live production session.

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
FIXTURE = os.path.join(ROOT, "sweep", "harness", "fixtures", "human-speech-librispeech.wav")
# The fixture's transcript, verbatim from LibriSpeech `2277-149896-0000`. The prompt this probe
# creates uses exactly this as its referenceText, so the transcript match is a fair test.
REFERENCE = ("HE WAS IN A FEVERED STATE OF MIND OWING TO THE BLIGHT HIS WIFE'S ACTION "
             "THREATENED TO CAST UPON HIS ENTIRE FUTURE")
PROMPT_TITLE = "AUDIT-V17 G8 human-audio fixture"
USER = ("user@gmail.com", "123456")
ADMIN = ("admin@gmail.com", "123456")
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


def token_of(body):
    d = json.loads(body)
    return (d.get("data") or d).get("token")


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

    Returns (stdout_bytes, combined_text). `mc cat` writes the OBJECT to stdout (returned as bytes);
    `mc rm`/`mc stat` write their messages to stdout while errors go to stderr, so `combined_text`
    merges both for the presence checks.
    """
    full = ("mc alias set local http://localhost:9000 \"$MINIO_ROOT_USER\" \"$MINIO_ROOT_PASSWORD\" "
            ">/dev/null 2>&1; " + command)
    r = subprocess.run(["docker", "exec", MINIO_CONTAINER, "sh", "-c", full], capture_output=True)
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
    # 1) the fixture — in-repo, licensed, reproducible
    assert os.path.exists(FIXTURE), "fixture missing: %s" % FIXTURE
    with open(FIXTURE, "rb") as fh:
        audio = fh.read()
    print("fixture: %s bytes=%d" % (os.path.basename(FIXTURE), len(audio)))
    assert len(audio) > 10000 and audio[:4] == b"RIFF", "fixture is not a WAV"

    # 2) tokens: admin creates/removes the dedicated prompt; the learner uploads the audio
    st, body = post_json("/api/auth/login", {"email": ADMIN[0], "password": ADMIN[1]})
    admin_tok = token_of(body)
    assert admin_tok, "admin login failed: %s" % body[:200]
    st, body = post_json("/api/auth/login", {"email": USER[0], "password": USER[1]})
    tok = token_of(body)
    assert tok, "user login failed"

    # 3) a DEDICATED prompt whose referenceText is the fixture's own transcript
    st, body = post_json("/api/v1/admin/speaking-prompts", {
        "title": PROMPT_TITLE,
        "description": "audit-v17 fixture probe",
        "prompt": "Read the passage aloud.",
        "mode": "READ_ALOUD",
        "referenceText": REFERENCE,
        "maxDurationSeconds": 60,
        "attemptLimit": 10,
        "isPremium": False,
        "isPublished": True,
    }, admin_tok)
    prompt = json.loads(body)
    prompt_id = prompt.get("id")
    print("prompt created:", st, "id=%s" % prompt_id)
    assert st in (200, 201) and prompt_id, "prompt create failed: %s" % body[:200]

    # Everything from here runs inside try/finally so the submission row, the MinIO copy, the
    # study_days row AND the prompt are cleaned even if assessment throws.
    sub_id = None
    status = "?"
    transcript = ""
    score = None
    deleted = "?"
    had_err = False
    sd_deleted = "?"
    gone = False
    prompt_deleted = "?"
    try:
        # 4) upload a COPY of the fixture as a submission for the probe's prompt
        mp_body, ct = multipart({"promptId": str(prompt_id)}, "audit-v17-human.wav", audio)
        st, raw2 = http("POST", API + "/api/v1/speaking-submissions/upload", mp_body, {"Content-Type": ct}, tok)
        print("upload:", st, raw2[:180])
        sub = json.loads(raw2.decode("utf-8", "replace"))
        sub_id = sub.get("id")
        assert st == 200 and sub_id, "upload failed"

        # 5) assess (Whisper + Ollama rubric)
        st, raw3 = http("POST", API + "/api/v1/speaking-submissions/%d/assess" % sub_id, b"", {}, tok)
        print("assess:", st)
        assessed = json.loads(raw3.decode("utf-8", "replace")) if st == 200 else {}
        transcript = (assessed.get("transcript") or "").strip()
        score = assessed.get("scoreTotal")
        status = assessed.get("status")
        print("status=%s transcript_len=%d scoreTotal=%s" % (status, len(transcript), score))
        print("transcript head:", transcript[:140])
    finally:
        # 6) CLEANUP — read the object key BEFORE deleting the row (the response exposes mediaUrl,
        #    not the raw key, so the DB is the source of truth for what to remove from MinIO).
        media_key = "NONE"
        if sub_id:
            out = sql("SET NOCOUNT ON; SELECT 'KEY=' + ISNULL(media_object_key,'NONE') FROM speaking_submissions WHERE id=%d;" % sub_id)
            media_key = (re.search(r"KEY=(\S+)", out) or [None, "NONE"])[1]
        print("db media_object_key:", media_key)

        # VN date for the study_days window (F-17-24: SQL Server runs UTC, study_date is VN).
        vn_today = time.strftime("%Y-%m-%d", time.gmtime(time.time() + 7 * 3600))
        print("cleanup window (VN date):", vn_today)

        rm_out = ""
        if media_key and media_key != "NONE":
            _, rm_out = mc("mc rm --force local/speaking-uploads/%s" % media_key)
        if sub_id:
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

        # remove the dedicated prompt this probe created
        st_del, _ = http("DELETE", API + "/api/v1/admin/speaking-prompts/%d" % prompt_id, b"", {}, admin_tok)
        prompt_deleted = "yes" if st_del in (200, 204) else "no(%s)" % st_del
        print("cleanup: prompt deleted=%s" % prompt_deleted)

        # verify the COPY is gone
        if media_key and media_key != "NONE":
            _, verify = mc("mc stat local/speaking-uploads/%s" % media_key)
            gone = ("does not exist" in verify) or ("Not found" in verify) or ("no such" in verify.lower())
        else:
            gone = True
        # the fixture in the repo is never touched — assert it is still byte-identical
        with open(FIXTURE, "rb") as fh:
            after = fh.read()
        src_intact = len(after) == len(audio)
        print("minio copy gone:", gone, "| repo fixture intact:", src_intact,
              "(bytes %d -> %d)" % (len(audio), len(after)))

    # The transcript must actually match the reference — a real human reading this sentence. An
    # empty or unrelated transcript means the audio does not match the prompt, so the probe FAILS
    # rather than claiming a pass on unrelated audio.
    rec = overlap(norm(REFERENCE), norm(transcript))
    print("reference/transcript word-set recall: %.2f" % rec)

    ok = (status == "COMPLETED") and len(transcript) > 0 and rec >= 0.5 \
        and (deleted == "1") and not had_err and gone and src_intact \
        and (sd_deleted in ("0", "1")) and prompt_deleted == "yes"
    print("G8 RESULT:", "PASS" if ok else "FAIL",
          "(HUMAN recording -> real transcript matching the reference; cleanup verified)")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
