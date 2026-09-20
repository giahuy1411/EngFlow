/**
 * p9_perf_probe_v9.js — audit-v9 performance probe.
 *
 * Two independent measurements per hot endpoint:
 *  1. wall-clock: N sequential HTTP calls, report min/median/max (ms);
 *  2. server-side: sys.dm_exec_query_stats for the statement text, reporting
 *     executions + average logical reads + average elapsed time. Logical reads
 *     are the honest metric here: they do not depend on a warm client cache.
 *
 * Usage: node p9_perf_probe_v9.js <label>   (label = before|after)
 */
const { execFileSync } = require("child_process");

const BASE = "http://localhost:8080";
const LABEL = process.argv[2] || "unlabelled";

function sql(q) {
  return execFileSync("docker", ["exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
    "-S", "localhost", "-U", "sa", "-P", "YourPassword123", "-d", "english_learning",
    "-C", "-I", "-h", "-1", "-W", "-Q", "SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; " + q],
    { encoding: "utf8" }).trim();
}

const med = (a) => a.slice().sort((x, y) => x - y)[Math.floor(a.length / 2)];
const min = (a) => Math.min(...a);
const max = (a) => Math.max(...a);

async function timing(label, path, token, n, init) {
  const times = [];
  for (let i = 0; i < n; i++) {
    const t0 = Date.now();
    const r = await fetch(BASE + path, { headers: token ? { Authorization: "Bearer " + token } : {} });
    await r.text();
    times.push(Date.now() - t0);
    if (i === 0 && init) init(r);
  }
  return { label, path, n, min: min(times), median: med(times), max: max(times) };
}

async function token(email) {
  const r = await fetch(BASE + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password: "123456" })
  });
  const j = await r.json();
  return (j.data && j.data.token) || j.token;
}

(async () => {
  const admin = await token("admin@gmail.com");
  const user = await token("user@gmail.com");

  const results = [];
  results.push(await timing("lesson exercises (guest)", "/api/lessons/445/exercises", null, 10));
  results.push(await timing("lesson content (guest)", "/api/lessons/445/exercises/content", null, 10));
  results.push(await timing("lesson detail (guest)", "/api/lessons/445", null, 10));
  results.push(await timing("lesson list page0", "/api/lessons?page=0&size=12", null, 10));
  results.push(await timing("admin lessons page", "/api/admin/lessons?page=0&size=20", admin, 10));
  results.push(await timing("admin exercises q", "/api/admin/exercises?page=0&size=20", admin, 10));
  results.push(await timing("admin exercises keyword", "/api/admin/exercises?page=0&size=20&q=grammar", admin, 10));
  results.push(await timing("vocab search", "/api/vocabulary/search?q=travel", null, 10));
  results.push(await timing("streak", "/api/streak/current", user, 10));

  console.log("=== HTTP TIMINGS (" + LABEL + ") ===");
  for (const r of results) {
    console.log(String(r.median).padStart(6) + " ms median  (min " + String(r.min).padStart(4)
      + " max " + String(r.max).padStart(4) + ")  " + r.label);
  }

  console.log("\n=== TOP STATEMENTS BY AVG LOGICAL READS ===");
  const rows = sql(`SELECT TOP 10
      qs.execution_count e,
      qs.total_logical_reads / NULLIF(qs.execution_count,0) avg_reads,
      qs.total_elapsed_time / NULLIF(qs.execution_count,0) / 1000 avg_ms,
      SUBSTRING(st.text, (qs.statement_start_offset/2)+1,
        ((CASE qs.statement_end_offset WHEN -1 THEN DATALENGTH(st.text) ELSE qs.statement_end_offset END
          - qs.statement_start_offset)/2)+1) q
    FROM sys.dm_exec_query_stats qs
    CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
    WHERE st.text NOT LIKE '%sys.%' AND st.text NOT LIKE '%dm_exec%'
    ORDER BY avg_reads DESC;`);
  console.log(rows);

  const exerciseQuery = sql(`SELECT TOP 1
      qs.execution_count e,
      qs.total_logical_reads / NULLIF(qs.execution_count,0) avg_reads,
      qs.total_elapsed_time / NULLIF(qs.execution_count,0) / 1000 avg_ms
    FROM sys.dm_exec_query_stats qs
    CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
    WHERE st.text LIKE '%from exercises e1_0 join lessons l1_0%'
    ORDER BY avg_reads DESC;`);
  console.log("exercise-list statement (exercise_id + lesson LOB columns): " + exerciseQuery);

  require("fs").writeFileSync(__dirname + "/p9_perf_v9_" + LABEL + ".json",
    JSON.stringify({ label: LABEL, timings: results, topReads: rows, exerciseListStatement: exerciseQuery }, null, 1));
  console.log("wrote p9_perf_v9_" + LABEL + ".json");
})().catch((e) => { console.log("ERR", e.message); process.exit(1); });