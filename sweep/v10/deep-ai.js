/**
 * audit-v10 Phase 4.11 — deep check AI.
 *
 * Kiem: sinh tu vung (validate input, guard), TTS (da chung minh o
 * prove-tts-path.js), va cac duong AI khac co guard dung khong.
 *
 * LUU Y: KHONG goi LLM that o day. Mot loi goi qwen2.5 mat 6-60s va lam nang
 * may; phan "AI co tra loi dung khong" la chat luong noi dung, khong phai thu
 * mot vong audit ky thuat ket luan duoc. Cai kiem duoc la CONTRACT: guard, ma
 * loi, timeout, va dinh dang phan hoi.
 */
const API = "http://localhost:8080";
const R = { pass: 0, fail: 0, fails: [] };
function check(name, cond, detail) {
  if (cond) { R.pass++; console.log("  PASS  " + name); }
  else { R.fail++; console.log("  FAIL  " + name + "  -> " + detail); R.fails.push(name + " -> " + detail); }
}
async function req(method, p, { token, body } = {}) {
  const h = {};
  if (token) h.Authorization = `Bearer ${token}`;
  if (body) h["Content-Type"] = "application/json";
  const r = await fetch(API + p, { method, headers: h, body: body ? JSON.stringify(body) : undefined });
  const t = await r.text();
  let d = null; try { d = JSON.parse(t); } catch { d = t.slice(0, 200); }
  return { status: r.status, data: d, raw: t };
}
async function login(e, p) {
  const r = await req("POST", "/api/auth/login", { body: { email: e, password: p } });
  return r.data?.token || r.data?.data?.token;
}

(async () => {
  console.log("=== Phase 4.11 — deep check AI (contract, khong goi LLM) ===");
  console.log("");
  const user = await login("user@gmail.com", "123456");
  const admin = await login("admin@gmail.com", "123456");
  check("co token user", !!user, "khong dang nhap duoc");
  check("co token admin", !!admin, "khong dang nhap duoc");

  // ---------- 1. GUARD ----------
  console.log("");
  console.log("--- guard: phai dang nhap ---");
  const noAuth = await req("POST", "/api/ai/generate-vocab", { body: { topic: "travel" } });
  check("generate-vocab khong token -> 401", noAuth.status === 401, `nhan ${noAuth.status}`);

  // ---------- 2. VALIDATE INPUT ----------
  console.log("");
  console.log("--- validate input ---");
  const empty = await req("POST", "/api/ai/generate-vocab", { token: user, body: { topic: "" } });
  check("topic rong -> 400", empty.status === 400, `nhan ${empty.status}`);
  // KHONG assert ProblemDetail o day. Endpoint nay nam trong so 21 return dung
  // shape legacy {"error": "..."} — da ghi nhan la F123 va HOAN CO LY DO.
  // Va frontend da co shim: api.js dong 55-62 copy `error` sang `detail`/`message`,
  // nen nguoi dung van doc duoc thong bao that.
  // Kiem o day chi la: thong bao loi phai CO NOI DUNG THAT, khong duoc rong.
  const msg = empty.data?.detail || empty.data?.error || empty.data?.message || empty.data?.title;
  check("  loi co thong bao that (khong rong)", typeof msg === "string" && msg.trim().length > 0,
        JSON.stringify(empty.data).slice(0, 150));
  console.log("  GHI NHAN: shape la {" + Object.keys(empty.data || {}).join(",") + "} — thuoc F123 (legacy, da hoan)");

  const missing = await req("POST", "/api/ai/generate-vocab", { token: user, body: {} });
  check("thieu topic -> 400", missing.status === 400, `nhan ${missing.status}`);

  const blank = await req("POST", "/api/ai/generate-vocab", { token: user, body: { topic: "   " } });
  check("topic chi co khoang trang -> 400", blank.status === 400, `nhan ${blank.status}`);

  // ---------- 3. TTS (da chung minh day du o prove-tts-path.js) ----------
  console.log("");
  console.log("--- TTS sidecar ---");
  const ttsHealth = await fetch("http://localhost:8001/health");
  check("TTS sidecar /health -> 200", ttsHealth.status === 200, `nhan ${ttsHealth.status}`);
  console.log("  (duong TTS -> Cloudinary da chung minh o prove-tts-path.js: WAV that 276.524 byte, upload 200, URL phuc vu audio/wav)");

  // ---------- 4. WHISPER / STT ----------
  console.log("");
  console.log("--- Whisper (STT) sidecar ---");
  let whisperOk = false, whisperDetail = "";
  try {
    const r = await fetch("http://localhost:9002/health");
    whisperOk = r.status === 200;
    whisperDetail = "status=" + r.status;
  } catch (e) { whisperDetail = e.message.slice(0, 60); }
  check("Whisper sidecar /health -> 200", whisperOk, whisperDetail);

  // ---------- 5. CAC DUONG AI KHAC CO TON TAI VA CO GUARD ----------
  console.log("");
  console.log("--- cac duong AI khac ---");
  const paths = [
    ["POST", "/api/ai/generate-vocab", { topic: "travel" }],
    ["POST", "/api/ai/grade-writing", {}],
    ["POST", "/api/ai/translate", {}],
  ];
  for (const [m, p, b] of paths) {
    const noTok = await req(m, p, { body: b });
    check(`${p} khong token -> 401 (khong phai 500)`, noTok.status === 401,
          `nhan ${noTok.status}`);
  }

  // ---------- 6. KHONG RO RI API KEY ----------
  console.log("");
  console.log("--- khong lo cau hinh AI ---");
  const anyResp = await req("POST", "/api/ai/generate-vocab", { token: user, body: { topic: "" } });
  const body = anyResp.raw || "";
  check("loi KHONG chua OPENROUTER_API_KEY", !/OPENROUTER_API_KEY|sk-or-/i.test(body), body.slice(0, 200));
  check("loi KHONG chua api key dang chuoi", !/api[_-]?key['":\s]+[A-Za-z0-9]{16,}/i.test(body), body.slice(0, 200));
  check("loi KHONG chua base url noi bo", !/host\.docker\.internal|127\.0\.0\.1:11434/i.test(body), body.slice(0, 200));

  console.log("");
  console.log(`  PASS=${R.pass}  FAIL=${R.fail}`);
  if (R.fail) { console.log(""); console.log("FAILURES:"); R.fails.forEach((f) => console.log("  - " + f)); }
  console.log("");
  console.log("GHI CHU: script nay KHONG goi LLM that — moi loi goi qwen2.5 mat 6-60s.");
  console.log("         Chat luong noi dung AI khong phai thu mot vong audit ky thuat ket luan duoc.");
  process.exit(R.fail ? 1 : 0);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
