/**
 * audit-v11 — inspect the QUALITY of AI-generated exercises, not just their presence.
 *
 * The presence check ("3 rows, question non-null") is weak: a row can exist, have a
 * question, and still be unusable (placeholder text, a copied few-shot example, a
 * MATCHING row whose options the frontend cannot parse, or an answer that is not among
 * the options). This script prints the actual content and runs the frontend's own
 * parsing contract against it.
 *
 * Run: node sweep/v11/ai-quality-test.js
 */
const { execFileSync } = require("child_process");
const fs = require("fs");

const API = "http://localhost:8080";
const R = { pass: 0, fail: 0, notes: [] };
function check(name, cond, detail) {
  if (cond) { R.pass++; console.log("  PASS  " + name); }
  else { R.fail++; console.log("  FAIL  " + name + "  -> " + detail); }
}

async function login(email, password) {
  const r = await fetch(API + "/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password }) });
  if (!r.ok) return null;
  const j = await r.json();
  return j.token || j.data?.token;
}
async function req(method, path, { token, body } = {}) {
  const headers = {};
  if (token) headers.Authorization = "Bearer " + token;
  if (body) headers["Content-Type"] = "application/json";
  const res = await fetch(API + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const text = await res.text();
  let data = null; try { data = JSON.parse(text); } catch { data = text.slice(0, 200); }
  return { status: res.status, data };
}
function sqlJson(sql) {
  fs.writeFileSync("sweep/v11/_aiq.sql", "SET NOCOUNT ON;\n" + sql + "\n", "utf8");
  return execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/v11/_aiq.sql"], { cwd: ".", encoding: "utf8" });
}

(async () => {
  const admin = await login("admin@gmail.com", "123456");
  if (!admin) { console.log("no admin token"); process.exit(1); }

  const title = "AUDIT-V11-Q-" + (Date.now() % 1000000);
  const created = await req("POST", "/api/admin/lessons", {
    token: admin,
    body: { title, description: "quality probe", level: "ELEMENTARY", skillType: "GRAMMAR", isPublished: false,
            content: "<p>Present simple: She ____ (go) to school every day. They ____ (not like) coffee.</p>" },
  });
  const lessonId = (created.data?.data || created.data)?.lessonId || (created.data?.data || created.data)?.id;
  console.log("lessonId = " + lessonId + "  (" + title + ")");

  // generate ALL types, so MATCHING's slash-pair repair is exercised too
  const gen = await req("POST", "/api/admin/exercises/ai/generate-async", { token: admin, body: { lessonId, count: 2 } });
  const batchId = gen.data?.batchId;
  console.log("batchId  = " + batchId + "  (status " + gen.status + ", all types)");

  const t0 = Date.now();
  let p = null;
  const deadline = Date.now() + 10 * 60 * 1000;
  while (Date.now() < deadline) {
    const st = await req("GET", "/api/admin/exercises/ai/status?batchId=" + encodeURIComponent(batchId || ""), { token: admin });
    p = st.data;
    if (p && p.running === false) break;
    await new Promise(r => setTimeout(r, 3000));
  }
  console.log("elapsed  = " + ((Date.now() - t0) / 1000).toFixed(1) + "s   errors=" + (p?.errors ?? "?"));

  // pull the generated rows as the API serves them to the admin UI
  const list = await req("GET", "/api/admin/exercises?lessonId=" + lessonId + "&size=50", { token: admin });
  const rows = list.data?.content || list.data?.data?.content || [];
  console.log("\n--- generated content (" + rows.length + " rows) ---");
  for (const e of rows) {
    console.log("[" + e.exerciseType + "] Q: " + String(e.question || "").slice(0, 90));
    console.log("        A: " + String(e.correctAnswer || "").slice(0, 70));
    if (e.options) console.log("        O: " + String(e.options).slice(0, 100));
  }
  console.log("");

  check("generated at least one exercise", rows.length > 0, "0 rows");
  const withQ = rows.filter(e => (e.question || "").trim().length > 3);
  check("all have a substantive question", withQ.length === rows.length, withQ.length + "/" + rows.length);
  const withA = rows.filter(e => (e.correctAnswer || "").trim().length > 0);
  check("all have a correct answer", withA.length === rows.length, withA.length + "/" + rows.length);

  // Frontend contract: MatchingExercise.vue parses options "left|right" and answer "l=r,..."
  const matching = rows.filter(e => e.exerciseType === "MATCHING");
  if (matching.length) {
    const badOpt = matching.filter(e => !String(e.options || "").includes("|"));
    check("MATCHING options use the 'left|right' contract", badOpt.length === 0,
      badOpt.length + " row(s) missing '|'");
    const badAns = matching.filter(e => !String(e.correctAnswer || "").includes("="));
    check("MATCHING answers use the 'l=r' contract", badAns.length === 0,
      badAns.length + " row(s) missing '='");
  } else {
    R.notes.push("no MATCHING row generated this run");
  }

  // Multiple choice: the answer should be among the options
  const mc = rows.filter(e => e.exerciseType === "MULTIPLE_CHOICE" && e.options);
  if (mc.length) {
    let inOptions = 0;
    for (const e of mc) {
      let opts = [];
      try { opts = JSON.parse(e.options); } catch { opts = String(e.options).split(",").map(s => s.trim()); }
      const ans = String(e.correctAnswer || "").trim();
      if (opts.some(o => String(o).trim() === ans)) inOptions++;
    }
    check("MC correct answer appears among its options", inOptions === mc.length, inOptions + "/" + mc.length);
  }

  // no placeholder / copied-example text
  const junk = rows.filter(e => /lorem|example\.com|TODO|xxx/i.test(e.question || ""));
  check("no placeholder text in questions", junk.length === 0, junk.length + " row(s)");

  // cleanup
  const del = await req("DELETE", "/api/admin/lessons/" + lessonId, { token: admin });
  check("cleanup: throwaway lesson deleted", [200, 204].includes(del.status), "status=" + del.status);

  console.log("\n=== SUMMARY ===");
  console.log("pass=" + R.pass + " fail=" + R.fail);
  R.notes.forEach(n => console.log("  note: " + n));
  process.exit(R.fail > 0 ? 1 : 0);
})();
