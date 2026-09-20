/**
 * audit-v10 Phase 5 — do hieu nang cac duong da biet la nang.
 * Chi DO, khong toi uu. Theo P5: khong doi gi khi chua do duoc loi ich.
 * Chay: node sweep/v10/perf-probe.js
 */
const BASE = "http://localhost:8080";
const ROUNDS = 5;

async function login(email, pass) {
  const res = await fetch(BASE + "/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password: pass }),
  });
  if (!res.ok) return null;
  const j = await res.json();
  return (j.data && j.data.token) || j.token;
}

async function timeIt(label, path, token, rounds = ROUNDS) {
  const times = [];
  let lastStatus = null, lastSize = 0;
  for (let i = 0; i < rounds; i++) {
    const t0 = performance.now();
    try {
      const res = await fetch(BASE + path, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      const text = await res.text();
      const ms = performance.now() - t0;
      times.push(ms);
      lastStatus = res.status;
      lastSize = text.length;
    } catch (e) {
      times.push(-1);
      lastStatus = "ERR";
    }
  }
  const ok = times.filter((t) => t >= 0);
  if (!ok.length) {
    console.log(`${label.padEnd(34)} ALL FAILED (${lastStatus})`);
    return;
  }
  ok.sort((a, b) => a - b);
  const min = ok[0], max = ok[ok.length - 1];
  const median = ok[Math.floor(ok.length / 2)];
  const avg = ok.reduce((a, b) => a + b, 0) / ok.length;
  console.log(
    `${label.padEnd(34)} status=${String(lastStatus).padEnd(4)} ` +
    `median=${median.toFixed(0).padStart(5)}ms  min=${min.toFixed(0).padStart(5)}ms  ` +
    `max=${max.toFixed(0).padStart(5)}ms  avg=${avg.toFixed(0).padStart(5)}ms  ` +
    `size=${(lastSize / 1024).toFixed(1)}KB`
  );
}

(async () => {
  console.log("=== audit-v10 perf probe ===");
  console.log(`target: ${BASE}   rounds: ${ROUNDS}`);
  console.log("");

  const admin = await login("admin@gmail.com", "123456");
  const user = await login("user@gmail.com", "123456");
  console.log(`admin token: ${admin ? "OK" : "FAIL"}   user token: ${user ? "OK" : "FAIL"}`);
  console.log("");

  console.log("--- Cac duong da biet la nang ---");
  // 43.737 exercise, LIKE '%kw%' khong dung duoc index (leading wildcard)
  await timeIt("admin/exercises?q=the", "/api/admin/exercises?q=the&page=0&size=20", admin);
  await timeIt("admin/exercises (khong q)", "/api/admin/exercises?page=0&size=20", admin);
  await timeIt("admin/vocabulary", "/api/admin/vocabulary?page=0&size=20", admin);
  await timeIt("admin/users", "/api/admin/users?page=0&size=20", admin);
  await timeIt("admin/stats", "/api/admin/stats", admin);
  await timeIt("admin/lessons", "/api/admin/lessons?page=0&size=20", admin);

  console.log("");
  console.log("--- Duong MOI, chua tung duoc do (streak snapshot) ---");
  // 404 neu container chua rebuild -> ghi nhan, khong ket luan ve hieu nang
  await timeIt("streak/snapshot (MOI)", "/api/streak/snapshot", user);
  await timeIt("streak/current", "/api/streak/current", user);
  await timeIt("streak/history?days=30", "/api/streak/history?days=30", user);

  console.log("");
  console.log("--- Duong doc cong khai ---");
  await timeIt("leaderboard", "/api/leaderboard", null);
  await timeIt("vocab/search?q=hello", "/api/vocabulary/search?q=hello", null);
  await timeIt("lessons (public)", "/api/lessons?page=0&size=20", null);

  console.log("");
  console.log("GHI CHU: neu streak/snapshot tra 404 thi container chua rebuild.");
  console.log("        So do cua no KHONG duoc dung lam ket luan hieu nang.");
})();
