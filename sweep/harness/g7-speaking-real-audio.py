#!/usr/bin/env python
"""
audit-v17 G7 — speaking assessment with REAL audio (end-to-end, local).

v17 recorded this as BLOCKED ("needs a real mic/file; a fake mic yields silence -> FAILED by
design"). It is testable: the local TTS sidecar (:8001) synthesizes real speech, so we can
upload genuine audio of the prompt's own reference text and drive the real assessment pipeline
(MinIO -> Whisper :9002 -> Ollama rubric).

Flow: synthesize referenceText -> upload (premium) -> assess -> assert a real transcript/score
-> DELETE the row by id + remove the MinIO object. Self-cleaning; parity re-asserted.

Usage: python sweep/harness/g7-speaking-real-audio.py
"""
import json
import os
import re
import subprocess
import sys
import time
import urllib.request

API = "http://localhost:8080"
TTS = "http://localhost:8001/synthesize"
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
PROMPT_ID = 50007  # READ_ALOUD, has referenceText
USER = ("user@gmail.com", "123456")


def http(method, url, data=None, headers=None, token=None, timeout=180):
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
    boundary = "----auditv17boundary"
    body = b""
    for k, v in fields.items():
        body += ("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n" % (boundary, k, v)).encode()
    body += ("--%s\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\nContent-Type: %s\r\n\r\n"
             % (boundary, filename, content_type)).encode()
    body += filebytes + b"\r\n"
    body += ("--%s--\r\n" % boundary).encode()
    return body, "multipart/form-data; boundary=" + boundary


def sql(query):
    tmp = os.path.join(ROOT, "sweep", "harness", "_g7-speaking.sql")
    with open(tmp, "w", encoding="utf-8") as fh:
        fh.write(query + "\n")
    out = subprocess.run(["python", "sweep/v8/sqlrun.py", "sweep/harness/_g7-speaking.sql"],
                         cwd=ROOT, capture_output=True, text=True)
    return (out.stdout or "") + (out.stderr or "")


def minio_rm(key):
    """Remove the media object from the MinIO container (env expands inside the container)."""
    cmd = ("mc alias set local http://localhost:9000 \"$MINIO_ROOT_USER\" \"$MINIO_ROOT_PASSWORD\" >/dev/null 2>&1; "
           "mc rm --force local/speaking-uploads/%s" % key)
    r = subprocess.run(["docker", "exec", "engflow-minio", "sh", "-c", cmd],
                       capture_output=True, text=True)
    return (r.stdout or "") + (r.stderr or "")


def main():
    # 1) reference text of the prompt
    st, raw = http("GET", API + "/api/v1/speaking-prompts/%d" % PROMPT_ID)
    prompt = json.loads(raw.decode("utf-8", "replace"))
    ref = prompt.get("referenceText") or ""
    print("prompt %d mode=%s ref_len=%d" % (PROMPT_ID, prompt.get("mode"), len(ref)))
    assert ref, "prompt has no referenceText"

    # 2) synthesize REAL speech of that text
    st, raw = http("POST", TTS, json.dumps({"text": ref, "lang": "en"}).encode(),
                   {"Content-Type": "application/json"})
    print("tts:", st, "bytes=", len(raw), "riff=", raw[:4])
    assert st == 200 and raw[:4] == b"RIFF", "TTS did not return a WAV"

    # 3) login + upload
    st, body = post_json("/api/auth/login", {"email": USER[0], "password": USER[1]})
    tok = (json.loads(body).get("data") or json.loads(body)).get("token")
    assert tok, "login failed"
    mp_body, ct = multipart({"promptId": str(PROMPT_ID)}, "audit-v17-tts.wav", raw)
    st, raw2 = http("POST", API + "/api/v1/speaking-submissions/upload", mp_body, {"Content-Type": ct}, tok)
    print("upload:", st, raw2[:200])
    sub = json.loads(raw2.decode("utf-8", "replace"))
    sub_id = sub.get("id")
    media_key = sub.get("mediaObjectKey")
    print("submission id=%s mediaKey=%s" % (sub_id, media_key))
    assert st == 200 and sub_id, "upload failed"

    # 4) assess (Whisper + Ollama rubric). Everything from here on runs inside try/finally so the
    #    row + MinIO object + study_days row are cleaned even if assessment throws (F-17-25).
    #    (media_key is re-read from the DB in cleanup, so it is not carried over from the response.)
    status = "?"
    transcript = ""
    score = None
    deleted = "?"
    had_err = False
    sd_deleted = "?"
    gone = False
    try:
        st, raw3 = http("POST", API + "/api/v1/speaking-submissions/%d/assess" % sub_id, b"", {}, tok, timeout=300)
        print("assess:", st)
        assessed = json.loads(raw3.decode("utf-8", "replace")) if st == 200 else {}
        transcript = (assessed.get("transcript") or "").strip()
        score = assessed.get("scoreTotal")
        status = assessed.get("status")
        print("status=%s transcript_len=%d scoreTotal=%s" % (status, len(transcript), score))
        print("transcript head:", transcript[:120])
    finally:
        # 5) CLEANUP — read the object key BEFORE deleting the row (the response exposes mediaUrl,
        #    not the raw key, so the DB is the source of truth for what to remove from MinIO).
        out = sql("SET NOCOUNT ON; SELECT 'KEY=' + ISNULL(media_object_key,'NONE') FROM speaking_submissions WHERE id=%d;" % sub_id)
        media_key = (re.search(r"KEY=(\S+)", out) or [None, "NONE"])[1]
        print("db media_object_key:", media_key)

        # F-17-24 (cross-review): the study_days window must use the VN clock, not SQL Server's.
        # `study_date` is written in Asia/Ho_Chi_Minh (StudyActivityService), but the SQL Server
        # container runs UTC — so `CAST(GETDATE() AS DATE)` is the WRONG day between 17:00-24:00 UTC
        # (= 00:00-07:00 VN the next day) and the DELETE would match 0 rows, leaving residue.
        vn_today = time.strftime("%Y-%m-%d", time.gmtime(time.time() + 7 * 3600))  # VN = UTC+7
        print("cleanup window (VN date):", vn_today)

        rm_out = minio_rm(media_key) if media_key and media_key != "NONE" else "(no key)"
        out = sql("SET QUOTED_IDENTIFIER ON; DELETE FROM speaking_submissions WHERE id=%d; "
                  "SELECT 'DELETED=' + CAST(@@ROWCOUNT AS varchar(5));" % sub_id)
        deleted = (re.search(r"DELETED=(\d+)", out) or [None, "?"])[1]
        had_err = bool(re.search(r"Msg \d+", out))
        print("cleanup: row deleted=%s sqlError=%s ; minio: %s" % (deleted, had_err, rm_out.strip()[:120]))

        # assess() calls SpeakingSubmissionService.recordStudy -> a study_days row for the probe user.
        # It MUST be cleaned too (same residue class as F-16-01), scoped to the probe accounts + the
        # VN date computed above (NOT GETDATE()).
        sd_out = sql("SET QUOTED_IDENTIFIER ON; "
                     "DELETE FROM study_days WHERE study_date = '%s' "
                     "AND user_id IN (SELECT user_id FROM users WHERE email IN ('user@gmail.com','admin@gmail.com')); "
                     "SELECT 'SD_DELETED=' + CAST(@@ROWCOUNT AS varchar(5));" % vn_today)
        sd_deleted = (re.search(r"SD_DELETED=(\d+)", sd_out) or [None, "?"])[1]
        print("cleanup: study_days rows removed=%s" % sd_deleted)

        # verify the object is really gone
        verify = minio_rm(media_key) if media_key and media_key != "NONE" else ""
        gone = ("does not exist" in verify) or ("Removed" in rm_out) or (media_key == "NONE")
        print("minio object gone:", gone)

    # PASS requires BOTH the real pipeline ran AND everything it created was cleaned
    # (F-17-25: the first version's `ok` ignored the study_days + MinIO results, so it could
    # report PASS while leaving residue behind).
    ok = (status == "COMPLETED") and len(transcript) > 0 \
        and (deleted == "1") and not had_err and gone and (sd_deleted in ("0", "1"))
    print("G7 RESULT:", "PASS" if ok else "FAIL",
          "(real transcript produced => the pipeline ran on REAL audio; cleanup verified)")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
