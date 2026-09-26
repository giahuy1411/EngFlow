#!/usr/bin/env python
"""
audit-v17 G6 — SePay webhook with a REAL HMAC signature (end-to-end, local).

Creates a real PENDING order, signs the payload with the secret from .env exactly the way
PaymentService.isSignatureValid expects (sha256= + HMAC over "<ts>.<rawBody>"), posts it to
/webhook/sepay, asserts the transaction settles to SUCCESS, then DELETES the row it created.

This is a LOCAL test with the LOCAL secret: no real money, no production. The row is removed
in the same run and parity is re-asserted.

Usage: python sweep/harness/g6-sepay-signed.py
"""
import hashlib
import hmac
import json
import os
import re
import subprocess
import sys
import time
import urllib.request

API = "http://localhost:8080"
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
USER = ("user@gmail.com", "123456")


def env_secret():
    """Read SEPAY_WEBHOOK_SECRET from .env without printing it."""
    path = os.path.join(ROOT, ".env")
    with open(path, encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.strip()
            if line.startswith("SEPAY_WEBHOOK_SECRET="):
                return line.split("=", 1)[1].strip().strip('"').strip("'")
    raise SystemExit("SEPAY_WEBHOOK_SECRET not found in .env")


def post(path, body, headers=None, token=None):
    data = body.encode("utf-8") if isinstance(body, str) else json.dumps(body).encode()
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(API + path, data=data, headers=h, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def sql(query):
    """Run SQL via the shared runner (it takes a file path, not stdin)."""
    tmp = os.path.join(ROOT, "sweep", "harness", "_g6-sepay.sql")
    with open(tmp, "w", encoding="utf-8") as fh:
        fh.write(query + "\n")
    out = subprocess.run(
        ["python", "sweep/v8/sqlrun.py", "sweep/harness/_g6-sepay.sql"],
        cwd=ROOT, capture_output=True, text=True,
    )
    return (out.stdout or "") + (out.stderr or "")


def main():
    secret = env_secret()
    print("secret loaded: length=%d (value not printed)" % len(secret))

    # 1) login
    st, body = post("/api/auth/login", {"email": USER[0], "password": USER[1]})
    tok = (json.loads(body).get("data") or json.loads(body)).get("token")
    assert tok, "login failed: %s" % body[:200]
    print("login:", st)

    # 2) create a real PENDING order
    st, body = post("/api/v1/payment/create-order", {"planType": "MONTH"}, token=tok)
    print("create-order:", st, body[:200])
    m = re.search(r"ENG[0-9A-Z]{12}", body.upper())
    assert m, "no orderCode in create-order response"
    order_code = m.group(0)
    print("orderCode:", order_code)

    # 3) sign the SePay payload: sha256=<hex> over "<ts>.<rawBody>"
    payload = {
        # SePay sends "id" as a JSON number; PaymentService reads body.get("id").
        "id": 991700001,
        "transferAmount": 50000,
        "content": "AUDIT-V17-SIGNED " + order_code,
        "gateway": "AUDIT-V17-GW",
        "referenceCode": "AUDITV17REF",
    }
    raw = json.dumps(payload, separators=(",", ":"))
    ts = str(int(time.time()))
    sig = "sha256=" + hmac.new(secret.encode(), (ts + "." + raw).encode(), hashlib.sha256).hexdigest()

    # Capture the user's premium state BEFORE settling, so cleanup can RESTORE it (F-17-22:
    # settling calls PaymentService.processSePayTransaction -> user.setIsPremium(true) +
    # setPremiumExpiry(...), which each run would otherwise push a month further out, invisibly).
    before = sql("SET NOCOUNT ON; SELECT 'PREM=' + CAST(ISNULL(CAST(is_premium AS int),0) AS varchar(2)) "
                 "+ '|EXP=' + ISNULL(CONVERT(varchar(19), premium_expiry, 126),'NULL') FROM users WHERE email='%s';" % USER[0])
    bm = re.search(r"PREM=(\d)\|EXP=(\S+)", before)
    prem_before = bm.group(1) if bm else "?"
    exp_before = bm.group(2) if bm else "NULL"
    print("premium before: isPremium=%s expiry=%s" % (prem_before, exp_before))

    status = "?"
    deleted = "?"
    had_err = False
    bad_rejected = False
    restored = "?"
    try:
        # 4) post the webhook with a VALID signature
        st, body = post("/api/webhook/sepay", raw,
                        headers={"X-Sepay-Signature": sig, "X-Sepay-Timestamp": ts})
        print("webhook(valid sig):", st, body[:220])

        # 5) assert the row settled to SUCCESS
        out = sql("SET NOCOUNT ON; SELECT 'STATUS=' + ISNULL(MAX(status),'NONE') FROM payment_transactions WHERE order_code='%s';" % order_code)
        status = (re.search(r"STATUS=(\w+)", out) or [None, "?"])[1]
        print("db status:", status)

        # 6) NEGATIVE control: a bad signature must be REFUSED. Asserted, not just printed
        #    (F-17-23: the first version only printed it, so a server that accepted the bad
        #    signature would still have PASSed).
        st_bad, body_bad = post("/api/webhook/sepay", raw,
                                headers={"X-Sepay-Signature": "sha256=" + "0" * 64, "X-Sepay-Timestamp": ts})
        bad_rejected = ("Invalid signature" in body_bad) or ('"success":false' in body_bad.replace(" ", ""))
        print("webhook(bad sig):", st_bad, body_bad[:160], "-> rejected:", bad_rejected)
    finally:
        # 7) CLEANUP (always) — delete exactly the row this probe created, by orderCode,
        #    and RESTORE the user's premium fields to their pre-run values.
        out = sql("SET QUOTED_IDENTIFIER ON; DELETE FROM payment_transactions WHERE order_code='%s'; SELECT 'DELETED=' + CAST(@@ROWCOUNT AS varchar(5));" % order_code)
        deleted = (re.search(r"DELETED=(\d+)", out) or [None, "?"])[1]
        had_err = bool(re.search(r"Msg \d+", out))
        print("cleanup: deleted=%s sqlError=%s" % (deleted, had_err))

        exp_sql = "NULL" if exp_before == "NULL" else "'%s'" % exp_before
        r = sql("SET QUOTED_IDENTIFIER ON; UPDATE users SET is_premium=%s, premium_expiry=%s WHERE email='%s'; "
                "SELECT 'RESTORED=' + CAST(@@ROWCOUNT AS varchar(5));" % (prem_before, exp_sql, USER[0]))
        restored = (re.search(r"RESTORED=(\d+)", r) or [None, "?"])[1]
        print("cleanup: premium restored=%s (isPremium=%s expiry=%s)" % (restored, prem_before, exp_before))

    ok = (status == "SUCCESS") and (deleted == "1") and not had_err and bad_rejected and (restored == "1")
    print("G6 RESULT:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
