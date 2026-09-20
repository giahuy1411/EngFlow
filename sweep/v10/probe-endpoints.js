/**
 * audit-v10 — probe live backend de xac dinh container dang chay code cu hay moi.
 * Chay: node sweep/v10/probe-endpoints.js
 */
const BASE = "http://localhost:8080";

async function probe(path, method = "GET", body = null) {
  try {
    const res = await fetch(BASE + path, {
      method,
      headers: body ? { "Content-Type": "application/json" } : {},
      body: body ? JSON.stringify(body) : undefined,
    });
    let snippet = "";
    try {
      const text = await res.text();
      snippet = text.slice(0, 120).replace(/\s+/g, " ");
    } catch { /* ignore */ }
    return { path, status: res.status, snippet };
  } catch (e) {
    return { path, status: "ERR", snippet: e.message.slice(0, 80) };
  }
}

(async () => {
  console.log("=== audit-v10 endpoint probe ===");
  console.log("Target:", BASE);
  console.log("");

  const targets = [
    ["/api/streak/snapshot", "GET", null],   // MỚI: chỉ có ở code mới
    ["/api/streak/current",  "GET", null],   // cũ + mới
    ["/api/streak/history",  "GET", null],
    ["/actuator/health",     "GET", null],
  ];

  for (const [p, m, b] of targets) {
    const r = await probe(p, m, b);
    console.log(`${String(r.status).padEnd(5)} ${p.padEnd(28)} ${r.snippet}`);
  }

  console.log("");
  console.log("=== Cach doc ket qua ===");
  console.log("  /api/streak/snapshot -> 404  = container chay CODE CU (chua rebuild)");
  console.log("  /api/streak/snapshot -> 401/200 = container da chay CODE MOI");
  console.log("  (401 vi chua dang nhap, 200 neu co token)");
})();
