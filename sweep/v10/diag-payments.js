/**
 * Chan doan: 2 row payment_transactions moi la gi, va co phai rac sweep khong.
 * READ-ONLY.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `pay-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/pay.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "~" -i /tmp/pay.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 250));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}

console.log("=== payment_transactions: tong quan ===");
console.log("tong row: " + rows("SELECT CAST(COUNT(*) AS varchar(10)) FROM payment_transactions;")[0]);
console.log("");
console.log("theo status:");
rows("SELECT status + ' = ' + CAST(COUNT(*) AS varchar(10)) FROM payment_transactions GROUP BY status ORDER BY status;")
  .forEach((r) => console.log("  " + r));
console.log("");
console.log("=== 8 row MOI NHAT ===");
for (const l of rows(`SELECT TOP 8
    CAST(id AS varchar(20)) + ' | ' + ISNULL(status,'NULL')
    + ' | tx=' + ISNULL(CAST(transaction_id AS varchar(40)),'NULL')
    + ' | user=' + ISNULL(CAST(user_id AS varchar(20)),'NULL')
    + ' | ' + CONVERT(varchar(19), created_at, 120)
  FROM payment_transactions ORDER BY id DESC;`)) {
  console.log("  " + l);
}
console.log("");
console.log("=== row co status <> SUCCESS (rac sweep) ===");
console.log("so row: " + rows("SELECT CAST(COUNT(*) AS varchar(10)) FROM payment_transactions WHERE status <> 'SUCCESS' OR status IS NULL;")[0]);
console.log("  trong do tx IS NULL: " + rows("SELECT CAST(COUNT(*) AS varchar(10)) FROM payment_transactions WHERE (status <> 'SUCCESS' OR status IS NULL) AND transaction_id IS NULL;")[0]);
console.log("");
console.log("=== 6 row rac do (neu co) ===");
for (const l of rows(`SELECT TOP 10
    CAST(id AS varchar(20)) + ' | ' + ISNULL(status,'NULL')
    + ' | tx=' + ISNULL(CAST(transaction_id AS varchar(40)),'NULL')
    + ' | user=' + ISNULL(CAST(user_id AS varchar(20)),'NULL')
    + ' | ' + CONVERT(varchar(19), created_at, 120)
  FROM payment_transactions
  WHERE (status <> 'SUCCESS' OR status IS NULL) AND transaction_id IS NULL
  ORDER BY id DESC;`)) {
  console.log("  " + l);
}
