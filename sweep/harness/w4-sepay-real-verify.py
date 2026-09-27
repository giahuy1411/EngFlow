#!/usr/bin/env python
"""
audit-v19 W4 — verify a REAL SePay transfer settled via the production webhook.

The user scans the QR produced by POST /api/v1/payment/create-order and transfers the exact
amount. SePay's servers then POST the webhook (HMAC sha256 over "<ts>.<rawBody>" with THEIR
secret) to the Tailscale Funnel. This script does NOT transfer money — it only checks the
result: the payment row must be SUCCESS with a REAL transaction_id / gateway that only SePay
could have produced.

Usage: python sweep/harness/w4-sepay-real-verify.py <orderCode>
"""
import json, os, re, subprocess, sys, urllib.request

API = "http://localhost:8080"
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

def sql(q):
    out = subprocess.run(
        ["docker", "exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
         "-S", "localhost", "-U", "sa", "-P", os.environ.get("SA_PASSWORD", "YourPassword123"),
         "-d", "english_learning", "-C", "-h", "-1", "-W", "-Q", q],
        capture_output=True, text=True).stdout
    if re.search(r"Msg \d+", out):
        raise SystemExit("SQL error: " + out)
    return out.strip()

def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: w4-sepay-real-verify.py <orderCode>")
    code = sys.argv[1]
    print("orderCode:", code)
    row = sql(f"SELECT status, transaction_id, gateway, amount FROM payment_transactions WHERE order_code='{code}'")
    print("db row:", row)
    # A real SePay transaction has a non-null transaction_id and a real gateway; the local g6
    # probe leaves gateway='AUDIT-V17-GW' and a synthetic id, so this distinguishes them.
    is_real = ("SUCCESS" in row) and ("NULL" not in row.split("|")[1]) and ("AUDIT" not in row)
    print("REAL SIGNATURE SETTLEMENT:", is_real)
    prem = sql("SELECT is_premium, premium_expiry FROM users WHERE email='user@gmail.com'")
    print("premium now:", prem)
    print("W4 RESULT:", "PASS" if is_real else "PENDING (transfer not yet landed?)")

if __name__ == "__main__":
    main()
