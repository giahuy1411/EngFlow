/**
 * SUA LAI phep thu truoc — no la TAUTOLOGY.
 *
 * Lan truoc toi gui CHINH correct_answer lam cau tra loi, roi ket luan
 * "cham duoc". Dieu do chi chung minh ham so sanh chuoi chay, KHONG chung minh
 * nguoi hoc bam lua chon se tao ra duoc chuoi do.
 *
 * Phep thu DUNG: bat chuo i CHUOI MA CLIENT SE GUI.
 *   LessonExerciseTab.vue dong 71:  @click="selectAnswer(ex.id, opt)"
 *   -> client gui NGUYEN VAN noi dung lua chon `opt`, khong gui chu cai.
 *
 * Nen phai:
 *   1. Lay options tu DB
 *   2. Mo phong parsedOptions() cua client de biet lua chon nao HIEN RA
 *   3. Gui dung chuoi do
 *   4. Xem co correct=true khong
 *
 * Neu options KHONG parse duoc thanh mang thi client roi vao o nhap tay, va
 * nguoi hoc phai go dung tung ky tu — do la van de that.
 */
const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");
const API = "http://localhost:8080";
const CONTAINER = "engflow-sqlserver";

function sql(query) {
  const tmp = path.join(os.tmpdir(), `fix-${Date.now()}.sql`);
  fs.writeFileSync(tmp, "SET NOCOUNT ON;\n" + query + "\n", "utf8");
  try {
    execFileSync("docker", ["cp", tmp, `${CONTAINER}:/tmp/fix.sql`], { encoding: "utf8" });
    return execFileSync("docker", ["exec", CONTAINER, "sh", "-c",
      'cd /opt/mssql-tools18/bin && ./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -h -1 -W -s "\u0001" -i /tmp/fix.sql'],
      { encoding: "utf8" });
  } finally { try { fs.unlinkSync(tmp); } catch (e) {} }
}
function rows(q) {
  const out = sql(q);
  if (/Msg \d+/.test(out)) throw new Error("SQL loi: " + out.trim().slice(0, 300));
  return out.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
}
const SEP = "\u0001";

async function post(p, body, token) {
  const h = { "Content-Type": "application/json" };
  if (token) h.Authorization = `Bearer ${token}`;
  const r = await fetch(API + p, { method: "POST", headers: h, body: JSON.stringify(body) });
  const t = await r.text();
  let d = null; try { d = JSON.parse(t); } catch { d = t.slice(0, 200); }
  return { status: r.status, data: d };
}
async function login(e, p) {
  const r = await post("/api/auth/login", { email: e, password: p });
  return r.data?.token || r.data?.data?.token;
}

/** Mo phong parsedOptions() cua LessonExerciseTab.vue */
function parsedOptions(optionsRaw) {
  if (!optionsRaw) return null;
  let opts = optionsRaw;
  if (typeof opts === "string") {
    try { opts = JSON.parse(opts); } catch (e) { return null; }
  }
  if (!Array.isArray(opts) || opts.length === 0) return null;
  // Client loc bo "placeholder" (tat ca deu la dau ___)
  const allPlaceholders = opts.every((o) => /^[_\s]*$/.test(String(o)));
  return allPlaceholders ? null : opts;
}

(async () => {
  console.log("=== PHEP THU DUNG: gui chuoi MA CLIENT SE GUI ===");
  console.log("");
  const tok = await login("user@gmail.com", "123456");

  const data = rows(`SELECT
      CAST(e.exercise_id AS varchar(20)) + '${SEP}' +
      CAST(e.lesson_id AS varchar(20)) + '${SEP}' +
      REPLACE(REPLACE(ISNULL(CAST(e.correct_answer AS varchar(500)),''), CHAR(13),' '), CHAR(10),' ') + '${SEP}' +
      REPLACE(REPLACE(REPLACE(ISNULL(CAST(e.options AS varchar(2500)),''), CHAR(13),' '), CHAR(10),' '), '${SEP}',' ')
    FROM exercises e JOIN lessons l ON l.lesson_id = e.lesson_id
    WHERE e.exercise_type='LISTENING'
      AND (e.audio_url IS NULL OR LTRIM(RTRIM(e.audio_url))='')
      AND l.is_published = 1
    ORDER BY e.exercise_id;`);

  let clickableOk = 0, clickableFail = 0, textInput = 0;

  for (const line of data) {
    const [exId, lessonId, ca, optsRaw] = line.split(SEP);
    const opts = parsedOptions(optsRaw);

    console.log(`--- exercise ${exId} (lesson ${lessonId}) ---`);
    console.log(`  correct_answer: ${ca.slice(0, 75)}`);

    if (!opts) {
      textInput++;
      console.log(`  CLIENT SE HIEN: o NHAP TAY (options khong parse duoc thanh mang)`);
      console.log(`     options RAW: ${optsRaw.slice(0, 130)}`);
      console.log(`  => nguoi hoc phai GO dung tung ky tu -> thuc te khong cham duoc`);
      console.log("");
      continue;
    }

    console.log(`  CLIENT SE HIEN: ${opts.length} nut bam`);
    // Tim lua chon khop voi correct_answer
    const norm = (s) => String(s == null ? "" : s).trim().toLowerCase().replace(/\s+/g, " ");
    const target = opts.find((o) => norm(o) === norm(ca));

    if (!target) {
      console.log(`  => KHONG lua chon nao khop correct_answer -> bam nut nao cung SAI`);
      console.log(`     cac lua chon: ${JSON.stringify(opts.map((o) => String(o).slice(0, 40)))}`);
      console.log("");
      clickableFail++;
      continue;
    }

    // Gui DUNG chuoi client se gui khi bam lua chon dung
    const r = await post(`/api/lessons/${lessonId}/exercises/grade`, {
      lessonId: parseInt(lessonId, 10),
      answers: [{ exerciseId: parseInt(exId, 10), userAnswer: target }],
    }, tok);
    const item = r.data?.results?.[0];
    const pass = item?.correct === true;
    if (pass) clickableOk++; else clickableFail++;
    console.log(`  bam lua chon dung -> correct=${item?.correct}`);
    console.log(`     da gui: ${String(target).slice(0, 70)}`);
    console.log("");
  }

  console.log("=== KET QUA ===");
  console.log(`  bam nut duoc va cham DUNG  : ${clickableOk}`);
  console.log(`  bam nut duoc nhung SAI     : ${clickableFail}`);
  console.log(`  roi vao o NHAP TAY         : ${textInput}`);
  console.log("");
  console.log("GHI CHU QUAN TRONG:");
  console.log("  /grade chi so khop CHUOI (tru MATCHING). Nen 'bam duoc va cham dung'");
  console.log("  nghia la DU LIEU da dung — van de chi la exercise_type sai, khien UI");
  console.log("  hien nut '🔊 Nghe' doc to cau lenh viet/doc.");
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
