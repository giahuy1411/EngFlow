/**
 * audit-v11 — exercise the SePay webhook settlement path with a REAL HMAC signature.
 *
 * The secret is read from the running container's own environment, so it is never
 * written into a file or passed on a command line.
 *
 * What this proves (and what it deliberately does not):
 *   PROVES   — signature verification accepts a correctly-signed payload and rejects a
 *              tampered one; a PENDING order settles to SUCCESS; the user's premium is
 *              activated; a replayed webhook is idempotent.
 *   DOES NOT — contact SePay. No real bank transfer happens; this is the receiver side.
 *
 * Run: node sweep/v11/sepay-webhook-test.js
 */
const { execFileSync } = require("child_process");
const crypto = require("crypto");

const API = "http://localhost:8080";
const R = { pass: 0, fail: 0, notes: [] };
function check(name, cond, detail) {
  if (cond) { R.pass++; console.log("  PASS  " + name); }
  else { R.fail++; console.log("  FAIL  " + name + "  -> " + detail); }
}

function containerSecret() {
  // Read from the container env; never print it.
  const out = execFileSync("docker", ["exec", "engflow-backend", "printenv", "SEPAY_WEBHOOK_SECRET"], { encoding: "utf8" }).trim();
  if (!out) throw new Error("SEPAY_WEBHOOK_SECRET empty in container");
  return out;
}

function sqlFile(name, sql) {
  require("fs").writeFileSync("sweep/v11/" + name, sql, "utf8");
  return execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/v11/" + name], { cwd: ".", encoding: "utf8" });
}

async function login(email, password) {
  const r = await fetch(API + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password }) });
  if (!r.ok) return null;
  const j = await r.json();
  return j.token || j.data?.token;
}

function sign(secret, ts, body) {
  return "sha256=" + crypto.createHmac("sha256", secret).update(ts + "." + body).digest("hex");
}

(async () => {
  const secret = containerSecret();
  console.log("=== SePay webhook settlement test (real HMAC) ===");
  console.log("secret loaded from container env (not printed)");

  const token = await login("user@gmail.com", "123456");
  check("login as user", !!token, "no token");

  // 1. Create a REAL pending order through the API the UI uses.
  const order = await fetch(API + "/api/v1/payment/create-order", {
    method: "POST", headers: { "Content-Type": "application/json", Authorization: "Bearer " + token },
    body: JSON.stringify({ planType: "MONTH" }),
  });
  const orderJson = await order.json().catch(() => null);
  const orderCode = orderJson?.orderCode || orderJson?.data?.orderCode || (JSON.stringify(orderJson).match(/ENG[0-9A-F]+/) || [])[0];
  check("create-order returns an ENG order code", !!orderCode, JSON.stringify(orderJson).slice(0, 120));
  console.log("      orderCode = " + orderCode);
  if (!orderCode) { console.log("\nABORT"); process.exit(1); }

  // 2. Confirm the row is PENDING before the webhook.
  const before = sqlFile("_wh_before.sql",
    "SET NOCOUNT ON;\nSELECT 'STATE=' + status FROM payment_transactions WHERE order_code = '" + orderCode + "';\n");
  const beforeState = (before.match(/STATE=(\w+)/) || [])[1];
  check("order row is PENDING before webhook", beforeState === "PENDING", "state=" + beforeState);

  // 3. Build a REAL SePay-shaped payload and sign it correctly.
  const txnId = "AUDITV11" + Date.now();
  const payload = JSON.stringify({
    id: txnId,
    gateway: "Vietcombank",
    transactionDate: new Date(Date.now() + 7 * 3600e3).toISOString().slice(0, 19).replace("T", " "),
    accountNumber: "0000000000",
    content: "AUDIT-V11 " + orderCode,
    transferAmount: 10000,
    referenceCode: "AUDITV11REF",
  });
  const ts = Math.floor(Date.now() / 1000).toString();
  const goodSig = sign(secret, ts, payload);

  // 4. TAMPERED signature must be rejected.
  const badRes = await fetch(API + "/api/webhook/sepay", {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Sepay-Signature": "sha256=" + "0".repeat(64), "X-Sepay-Timestamp": ts },
    body: payload,
  });
  const badJson = await badRes.json().catch(() => ({}));
  check("tampered signature is REJECTED", badJson.success === false, JSON.stringify(badJson).slice(0, 120));

  // 5. STALE timestamp (outside the 5-minute replay window) must be rejected.
  const staleTs = (Math.floor(Date.now() / 1000) - 3600).toString();
  const staleRes = await fetch(API + "/api/webhook/sepay", {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Sepay-Signature": sign(secret, staleTs, payload), "X-Sepay-Timestamp": staleTs },
    body: payload,
  });
  const staleJson = await staleRes.json().catch(() => ({}));
  check("stale timestamp (replay) is REJECTED", staleJson.success === false, JSON.stringify(staleJson).slice(0, 120));

  // 6. CORRECTLY signed payload must settle.
  const okRes = await fetch(API + "/api/webhook/sepay", {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Sepay-Signature": goodSig, "X-Sepay-Timestamp": ts },
    body: payload,
  });
  const okJson = await okRes.json().catch(() => ({}));
  check("correctly-signed webhook ACCEPTED", okJson.success === true, JSON.stringify(okJson).slice(0, 160));

  // 7. The row must now be SUCCESS with the transaction id recorded.
  const after = sqlFile("_wh_after.sql",
    "SET NOCOUNT ON;\nSELECT 'STATE=' + status + ' TXN=' + ISNULL(transaction_id,'NULL') FROM payment_transactions WHERE order_code = '" + orderCode + "';\n");
  const stateLine = (after.match(/STATE=(\w+) TXN=(\S+)/) || []);
  check("order row settled to SUCCESS", stateLine[1] === "SUCCESS", "line=" + stateLine[0]);
  check("transaction_id recorded from the webhook", stateLine[2] === txnId, "txn=" + stateLine[2]);

  // 8. Idempotency: replaying the SAME webhook must not double-apply.
  const replay = await fetch(API + "/api/webhook/sepay", {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Sepay-Signature": goodSig, "X-Sepay-Timestamp": ts },
    body: payload,
  });
  const replayJson = await replay.json().catch(() => ({}));
  const afterReplay = sqlFile("_wh_replay.sql",
    "SET NOCOUNT ON;\nSELECT 'N=' + CAST(COUNT(*) AS varchar(10)) FROM payment_transactions WHERE transaction_id = '" + txnId + "';\n");
  const n = (afterReplay.match(/N=(\d+)/) || [])[1];
  check("replay does not create a duplicate row", n === "1", "rows with txn=" + n);
  R.notes.push("replay response: " + JSON.stringify(replayJson).slice(0, 100));

  // 9. Did premium actually activate?
  const me = await fetch(API + "/api/auth/me", { headers: { Authorization: "Bearer " + token } });
  const meJson = await me.json();
  check("user is now premium", meJson.isPremium === true || meJson.hasPremiumAccess === true,
    "isPremium=" + meJson.isPremium + " hasPremiumAccess=" + meJson.hasPremiumAccess);
  console.log("      premiumExpiry = " + meJson.premiumExpiry);

  console.log("\n=== SUMMARY ===");
  console.log("pass=" + R.pass + " fail=" + R.fail);
  R.notes.forEach(n => console.log("  note: " + n));
  console.log("\nCLEANUP NEEDED: order " + orderCode + " (id/state left for the caller to revert)");
  process.exit(R.fail > 0 ? 1 : 0);
})();
