/**
 * Chan doan ham sql()/sqlNum() trong streak-scenarios.js.
 * Nghi van: JSON.stringify bien newline thanh \n literal trong shell.
 */
const { execFileSync } = require("child_process");
const CONTAINER = "engflow-sqlserver";

function raw(argv) {
  try {
    return { ok: true, out: execFileSync("docker", argv, { encoding: "utf8" }) };
  } catch (e) {
    return { ok: false, out: String(e.stdout || "") + String(e.stderr || "") + " | " + e.message };
  }
}

console.log("=== A. Cach toi dang dung: JSON.stringify(query) ===");
const q1 = "SET NOCOUNT ON; SELECT TOP 1 id FROM vocabulary ORDER BY id;";
const cmd1 = `cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -Q ${JSON.stringify(q1)}`;
console.log("lenh shell:", cmd1);
let r = raw(["exec", CONTAINER, "sh", "-c", cmd1]);
console.log("ok:", r.ok);
console.log("out:", JSON.stringify(r.out));

console.log("");
console.log("=== B. Query nhieu dong (nhu cleanup) ===");
const q2 = `SET QUOTED_IDENTIFIER ON;
  DELETE FROM study_days WHERE user_id = -99999;`;
const cmd2 = `cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -Q ${JSON.stringify(q2)}`;
console.log("lenh shell:", cmd2.replace(/\n/g, "\\n"));
r = raw(["exec", CONTAINER, "sh", "-c", cmd2]);
console.log("ok:", r.ok);
console.log("out:", JSON.stringify(r.out));

console.log("");
console.log("=== C. Cach dung: truyen query qua stdin (-i /dev/stdin) ===");
r = raw(["exec", "-i", CONTAINER, "sh", "-c",
  `cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -i /dev/stdin`]);
console.log("(khong co stdin thi se treo hoac bao loi — bo qua)");
console.log("ok:", r.ok, "out:", JSON.stringify(r.out).slice(0, 200));

console.log("");
console.log("=== D. Cach dung that: ghi query ra file roi -i file ===");
console.log("(se thu trong script chinh)");

console.log("");
console.log("=== E. Trang thai hien tai ===");
const q3 = "SET NOCOUNT ON; SELECT COUNT(*) FROM users;";
const cmd3 = `cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -Q ${JSON.stringify(q3)}`;
r = raw(["exec", CONTAINER, "sh", "-c", cmd3]);
console.log("users count ->", JSON.stringify(r.out));

const q4 = "SET NOCOUNT ON; SELECT id, email FROM users WHERE email LIKE 'zzauditv10%';";
const cmd4 = `cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -Q ${JSON.stringify(q4)}`;
r = raw(["exec", CONTAINER, "sh", "-c", cmd4]);
console.log("user rac con lai ->", JSON.stringify(r.out));
