/**
 * audit-v10 — dang nhap that roi probe /api/streak/snapshot co token.
 * Muc dich: phan biet 404 (route khong ton tai = code cu) vs 200 (code moi).
 * Chay: node sweep/v10/probe-authed.js
 */
const BASE = "http://localhost:8080";

async function login(email, pass) {
  const res = await fetch(BASE + "/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password: pass }),
  });
  if (!res.ok) return { error: `login ${res.status}` };
  const j = await res.json();
  const token = (j.data && j.data.token) || j.token;
  const user = (j.data && j.data.user) || j.user;
  return { token, user };
}

async function authedGet(path, token) {
  const res = await fetch(BASE + path, { headers: { Authorization: `Bearer ${token}` } });
  let body = "";
  try { body = (await res.text()).slice(0, 300); } catch { /* ignore */ }
  return { status: res.status, body: body.replace(/\s+/g, " ") };
}

(async () => {
  console.log("=== audit-v10 authed probe ===");

  const creds = [
    ["user@gmail.com", "123456"],
    ["admin@gmail.com", "123456"],
  ];

  let token = null, who = null;
  for (const [e, p] of creds) {
    const r = await login(e, p);
    if (r.token) { token = r.token; who = e; break; }
    console.log(`login ${e}: ${r.error}`);
  }

  if (!token) {
    console.log("");
    console.log("KHONG DANG NHAP DUOC -> khong ket luan duoc gi ve code version.");
    console.log("Khong duoc coi 401 o tren la bang chung route ton tai.");
    return;
  }

  console.log(`logged in as ${who}`);
  console.log("");

  const snap = await authedGet("/api/streak/snapshot", token);
  console.log(`/api/streak/snapshot -> ${snap.status}`);
  console.log(`  body: ${snap.body}`);
  console.log("");

  const cur = await authedGet("/api/streak/current", token);
  console.log(`/api/streak/current  -> ${cur.status}`);
  console.log(`  body: ${cur.body}`);
  console.log("");

  console.log("=== KET LUAN ===");
  if (snap.status === 404) {
    console.log("  snapshot = 404  =>  CONTAINER CHAY CODE CU. Can rebuild.");
  } else if (snap.status === 200) {
    console.log("  snapshot = 200  =>  CONTAINER DA CHAY CODE MOI.");
    console.log("  Contract check:");
    try {
      const j = JSON.parse(snap.body);
      const need = ["today","currentStreak","studiedToday","effectiveFrom","studiedDays","legacyAccessDays","legacyHistoryAvailable"];
      for (const k of need) {
        console.log(`    ${need.includes(k) && k in j ? "OK  " : "THIEU"} ${k} = ${JSON.stringify(j[k])}`);
      }
    } catch { console.log("    (body khong phai JSON day du)"); }
  } else if (snap.status === 500) {
    console.log("  snapshot = 500  =>  route co nhung loi server. Xem log backend.");
  } else {
    console.log(`  snapshot = ${snap.status} => chua ket luan duoc.`);
  }
})();
