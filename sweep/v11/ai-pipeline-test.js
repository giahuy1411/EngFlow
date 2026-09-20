/**
 * audit-v11 — end-to-end exercise-generation pipeline against the real local Ollama.
 *
 * Safety: everything happens inside a THROWAWAY lesson created for this test and deleted
 * afterwards, so no existing lesson content is touched. The lesson is created through the
 * admin API (the same path the admin UI uses), not by SQL.
 *
 * This exercises the real pipeline: AiExerciseService -> Ollama qwen2.5:1.5b -> JSON salvage
 * -> schema validate -> MATCHING repair -> dedup -> persist.
 *
 * Run: node sweep/v11/ai-pipeline-test.js
 */
const { execFileSync } = require("child_process");
const fs = require("fs");

const API = "http://localhost:8080";
const R = { pass: 0, fail: 0, steps: [] };
function check(name, cond, detail) {
  if (cond) { R.pass++; console.log("  PASS  " + name); }
  else { R.fail++; console.log("  FAIL  " + name + "  -> " + detail); }
  R.steps.push({ name, cond: !!cond, detail });
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

function sqlScalar(sql) {
  fs.writeFileSync("sweep/v11/_ai_q.sql", "SET NOCOUNT ON;\n" + sql + "\n", "utf8");
  const out = execFileSync("python", ["sweep/v8/sqlrun.py", "sweep/v11/_ai_q.sql"], { cwd: ".", encoding: "utf8" });
  const m = out.match(/VALUE=(\S+)/);
  return m ? m[1] : "UNPARSED";
}

(async () => {
  console.log("=== AI exercise-generation pipeline (real Ollama) ===");
  const admin = await login("admin@gmail.com", "123456");
  check("admin login", !!admin, "no token");
  if (!admin) process.exit(1);

  // 1. throwaway lesson
  const title = "AUDIT-V11-AI-" + (Date.now() % 1000000);
  const created = await req("POST", "/api/admin/lessons", {
    token: admin,
    body: { title, description: "throwaway lesson for the audit AI pipeline test", level: "ELEMENTARY", skillType: "GRAMMAR", isPublished: false, content: "<p>Present simple: I ____ (go) to school every day.</p>" },
  });
  const lessonId = (created.data?.data || created.data)?.lessonId || (created.data?.data || created.data)?.id;
  check("throwaway lesson created", !!lessonId, JSON.stringify(created.data).slice(0, 120));
  console.log("      lessonId = " + lessonId);
  if (!lessonId) process.exit(1);

  const countBefore = parseInt(sqlScalar("SELECT 'VALUE=' + CAST(COUNT(*) AS varchar(10)) FROM exercises WHERE lesson_id = " + lessonId + ";"), 10);

  // 2. fire async generation (the admin UI path)
  const t0 = Date.now();
  const gen = await req("POST", "/api/admin/exercises/ai/generate-async", { token: admin, body: { lessonId, count: 3, exerciseType: "MULTIPLE_CHOICE" } });
  check("generate-async returns 202", gen.status === 202, "status=" + gen.status + " body=" + JSON.stringify(gen.data).slice(0, 120));
  const batchId = gen.data?.batchId;
  check("batchId returned", !!batchId, JSON.stringify(gen.data).slice(0, 120));
  console.log("      batchId = " + batchId);

  // 3. poll to completion (bounded — Ollama on a 4GB GPU, first call may load the model)
  let progress = null, done = false;
  const deadline = Date.now() + 8 * 60 * 1000;   // 8 min ceiling, generous on purpose
  while (Date.now() < deadline) {
    const st = await req("GET", "/api/admin/exercises/ai/status?batchId=" + encodeURIComponent(batchId || ""), { token: admin });
    progress = st.data;
    if (progress && progress.running === false) { done = true; break; }
    await new Promise(r => setTimeout(r, 3000));
  }
  const secs = ((Date.now() - t0) / 1000).toFixed(1);
  check("generation finished (running=false)", done, "last=" + JSON.stringify(progress).slice(0, 160));
  console.log("      elapsed = " + secs + "s");
  console.log("      progress = " + JSON.stringify(progress).slice(0, 200));

  // 4. rows actually persisted?
  const countAfter = parseInt(sqlScalar("SELECT 'VALUE=' + CAST(COUNT(*) AS varchar(10)) FROM exercises WHERE lesson_id = " + lessonId + ";"), 10);
  check("exercises persisted to the DB", countAfter > countBefore, "before=" + countBefore + " after=" + countAfter);
  console.log("      exercises: " + countBefore + " -> " + countAfter);

  // 5. do the generated rows satisfy the schema the UI needs?
  if (countAfter > countBefore) {
    const shape = sqlScalar(
      "SELECT 'VALUE=' + CAST(SUM(CASE WHEN question IS NOT NULL AND LTRIM(RTRIM(question)) <> '' THEN 1 ELSE 0 END) AS varchar(10)) " +
      "+ '/' + CAST(COUNT(*) AS varchar(10)) " +
      "+ ' withAnswer=' + CAST(SUM(CASE WHEN correct_answer IS NOT NULL AND LTRIM(RTRIM(correct_answer)) <> '' THEN 1 ELSE 0 END) AS varchar(10)) " +
      "+ ' withOptions=' + CAST(SUM(CASE WHEN options IS NOT NULL AND LTRIM(RTRIM(options)) <> '' THEN 1 ELSE 0 END) AS varchar(10)) " +
      "FROM exercises WHERE lesson_id = " + lessonId + ";");
    console.log("      shape: question=" + shape);
    const m = shape.match(/^(\d+)\/(\d+) withAnswer=(\d+) withOptions=(\d+)$/);
    if (m) {
      check("every generated exercise has a question", m[1] === m[2], m[1] + "/" + m[2]);
      check("every generated exercise has a correct answer", m[3] === m[2], m[3] + "/" + m[2]);
      check("MULTIPLE_CHOICE rows carry options", m[4] === m[2], m[4] + "/" + m[2]);
    }
  }

  // 6. cleanup — delete the throwaway lesson (cascades its exercises)
  const del = await req("DELETE", "/api/admin/lessons/" + lessonId, { token: admin });
  check("throwaway lesson deleted", [200, 204].includes(del.status), "status=" + del.status);
  const leftovers = sqlScalar("SELECT 'VALUE=' + CAST(COUNT(*) AS varchar(10)) FROM exercises WHERE lesson_id = " + lessonId + ";");
  check("its exercises are gone too", leftovers === "0", "left=" + leftovers);

  console.log("\n=== SUMMARY ===");
  console.log("pass=" + R.pass + " fail=" + R.fail + "  elapsed=" + secs + "s");
  process.exit(R.fail > 0 ? 1 : 0);
})();
