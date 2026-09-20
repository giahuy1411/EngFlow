/**
 * v9_perf_exercise_list.js <label> [lessonId]
 *
 * Focused before/after probe for audit-v9 F108: ExerciseRepository
 * .findByLessonIdOrderByOrderIndexAsc uses JOIN FETCH e.lesson, so every row of
 * the public GET /api/lessons/{id}/exercises response re-reads the lesson's
 * NVARCHAR(MAX) content + content_original. Lesson 651 (published, 99
 * exercises, content 26 kB + content_original 77 kB) is the worst case.
 *
 * Reports HTTP timings and, from sys.dm_exec_query_stats, the executions /
 * avg logical reads / avg ms of that exact statement.
 */
const { execFileSync } = require("child_process");
const BASE = "http://localhost:8080";
const LABEL = process.argv[2] || "unlabelled";
const LESSON = process.argv[3] || "651";

function sql(q) {
  return execFileSync("docker", ["exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
    "-S", "localhost", "-U", "sa", "-P", "YourPassword123", "-d", "english_learning",
    "-C", "-I", "-h", "-1", "-W", "-Q", "SET NOCOUNT ON; " + q], { encoding: "utf8" }).trim();
}
const stat = () => sql(`SELECT TOP 1 qs.execution_count e,
    qs.total_logical_reads / NULLIF(qs.execution_count,0) avg_reads,
    qs.total_elapsed_time / NULLIF(qs.execution_count,0) / 1000 avg_ms
  FROM sys.dm_exec_query_stats qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
  WHERE st.text LIKE '%from exercises e1_0%' AND st.text LIKE '%l1_0.content_original%'
    AND st.text LIKE '%@P0 bigint%' ORDER BY qs.execution_count DESC;`);

(async () => {
  const before = stat();
  const times = [];
  for (let i = 0; i < 8; i++) {
    const t0 = Date.now();
    await (await fetch(BASE + "/api/lessons/" + LESSON + "/exercises")).text();
    times.push(Date.now() - t0);
  }
  const after = stat();
  const s = times.slice().sort((a, b) => a - b);
  console.log("=== exercise-list probe (" + LABEL + ") lesson=" + LESSON + " ===");
  console.log("HTTP ms  min=" + s[0] + " median=" + s[Math.floor(s.length / 2)] + " max=" + s[s.length - 1] + "  n=" + times.length);
  console.log("statement BEFORE probe: " + (before.split("\n").pop() || before));
  console.log("statement AFTER  probe: " + (after.split("\n").pop() || after));
  require("fs").writeFileSync(__dirname + "/v9_perf_exercise_list_" + LABEL + ".json",
    JSON.stringify({ label: LABEL, lesson: LESSON, times, statementBefore: before, statementAfter: after }, null, 1));
  console.log("wrote v9_perf_exercise_list_" + LABEL + ".json");
})().catch((e) => { console.log("ERR", e.message); process.exit(1); });