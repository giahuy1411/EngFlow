#!/usr/bin/env python
"""
L2 end-to-end proof — a CONFIRMED 404 is negative-cached; a FAILURE is not.

The public upstream (dictionaryapi.dev) currently answers HTTP 522 (Cloudflare origin-timeout) for
unknown words, so a real 404 cannot be produced against it. This stub stands in for the upstream and
answers deterministically, letting us prove the cache behaviour over the REAL Redis:

  word "realword"  -> 200 + a payload            (positive cache)
  word "missing"   -> 404                        (negative cache)
  word "flaky"     -> 500 on the first call, 200 afterwards (a failure must NOT be cached)

It starts a tiny HTTP server, points the backend at it via DICTIONARY_UPSTREAM_URL, restarts the
backend, and measures the second lookup of each word. Self-contained; restores the backend env.

Usage: python sweep/harness/l2-negative-cache-proof.py
"""
import json
import os
import re
import subprocess
import sys
import threading
import time
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

API = "http://localhost:8080"
STUB_PORT = 8099
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
COMPOSE = os.path.join(ROOT, "docker-compose.yml")

CALLS = {}


class Stub(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"   # keep-alive; avoids the Windows connection-abort seen with 1.0

    def log_message(self, *a):
        pass

    def _send(self, code, payload):
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def do_GET(self):
        word = self.path.rstrip("/").split("/")[-1].split("?")[0]
        CALLS[word] = CALLS.get(word, 0) + 1
        if word == "missing":
            self._send(404, b'{"title":"No Definitions Found"}')
        elif word == "flaky":
            if CALLS[word] == 1:
                self._send(500, b"boom")
            else:
                self._send(200, json.dumps([{"word": word, "phonetics": [{"text": "/x/"}]}]).encode())
        else:
            self._send(200, json.dumps([{"word": word, "phonetics": [{"text": "/x/", "audio": "a.mp3"}]}]).encode())


def lookup(word):
    t0 = time.time()
    try:
        with urllib.request.urlopen(API + "/api/vocabulary/dictionary/" + word, timeout=60) as r:
            body = r.read().decode("utf-8", "replace")
            return r.status, body, time.time() - t0
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace"), time.time() - t0


def redis_del(pattern):
    # Use redis-cli EVAL so the pattern is not mangled by the shell (an earlier `xargs` form failed
    # with "unmatched single quote").
    script = ("local k=redis.call('keys',ARGV[1]); for i=1,#k do redis.call('del',k[i]) end; "
              "return #k")
    subprocess.run(["docker", "exec", "engflow-redis", "redis-cli", "EVAL", script, "0", pattern],
                   capture_output=True, text=True)


def main():
    srv = ThreadingHTTPServer(("0.0.0.0", STUB_PORT), Stub)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    print("stub listening on :%d" % STUB_PORT)

    # Point the backend at the stub. Docker Desktop: host is reachable as host.docker.internal.
    os.environ["DICTIONARY_UPSTREAM_URL"] = "http://host.docker.internal:%d/api" % STUB_PORT
    redis_del("dictionary*")
    print("restarting backend with DICTIONARY_UPSTREAM_URL=...")
    subprocess.run(["docker", "compose", "up", "-d", "backend"], cwd=ROOT,
                   capture_output=True, text=True, env=os.environ)
    for _ in range(40):
        try:
            urllib.request.urlopen(API + "/api/lessons?size=1", timeout=3).read()
            break
        except Exception:
            time.sleep(3)

    ok = True
    try:
        # 1) 404 -> negative-cached: 2nd lookup is instant and does NOT reach the stub again.
        before = CALLS.get("missing", 0)
        st1, b1, t1 = lookup("missing")
        st2, b2, t2 = lookup("missing")
        after = CALLS.get("missing", 0)
        hit_stub_second = (after - before) >= 2
        print("missing: 1st %.2fs body=%s | 2nd %.2fs body=%s | stub calls=%d"
              % (t1, b1[:30], t2, b2[:30], after - before))
        neg_ok = (st1 == 200 and st2 == 200 and b1 == "[]" and b2 == "[]" and not hit_stub_second)
        print("  negative-cached (2nd does not re-hit upstream):", neg_ok)
        ok = ok and neg_ok

        # 2) failure (500) -> NOT cached: the 2nd lookup must reach the stub and get the real payload.
        before = CALLS.get("flaky", 0)
        st1, b1, t1 = lookup("flaky")
        st2, b2, t2 = lookup("flaky")
        after = CALLS.get("flaky", 0)
        print("flaky: 1st body=%s | 2nd body=%s | stub calls=%d" % (b1[:30], b2[:30], after - before))
        pos_ok = (b1 == "[]" and b2.startswith("[") and b2 != "[]" and (after - before) >= 2)
        print("  failure NOT cached (2nd re-hits upstream and succeeds):", pos_ok)
        ok = ok and pos_ok

        # 3) positive payload -> cached: the 2nd lookup does NOT reach the stub.
        before = CALLS.get("realword", 0)
        st1, b1, t1 = lookup("realword")
        st2, b2, t2 = lookup("realword")
        after = CALLS.get("realword", 0)
        hit2 = (after - before) >= 2
        print("realword: 1st %s | 2nd %s | stub calls=%d" % (b1[:30], b2[:30], after - before))
        pos2_ok = (b1 == b2 and b1.startswith("[") and not hit2)
        print("  positive payload cached (2nd does not re-hit upstream):", pos2_ok)
        ok = ok and pos2_ok
    finally:
        # RESTORE: leave the backend pointing at the REAL upstream again (the probe must not change
        # the running system's behaviour after it exits), and drop the stub-only cache entries.
        real = "https://api.dictionaryapi.dev/api/v2/entries/en"
        os.environ["DICTIONARY_UPSTREAM_URL"] = real
        redis_del("dictionary*")
        subprocess.run(["docker", "compose", "up", "-d", "backend"], cwd=ROOT,
                       capture_output=True, text=True, env=os.environ)
        for _ in range(40):
            try:
                urllib.request.urlopen(API + "/api/lessons?size=1", timeout=3).read()
                break
            except Exception:
                time.sleep(3)
        print("restored backend upstream ->", real)

    print("L2 RESULT:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
