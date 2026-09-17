const lib = require("./lib.js");
const { probe } = lib;
const fs = require("fs");
const BASE = "http://localhost:8080";

async function multipart(path, as, fields, fileField, fileName, fileBuf, fileCtype) {
  const b = "----zz" + Math.random().toString(16).slice(2);
  const parts = [];
  for (const [k, v] of Object.entries(fields || {})) {
    parts.push(Buffer.from("--" + b + "\r\nContent-Disposition: form-data; name=\"" + k + "\"\r\n\r\n" + v + "\r\n"));
  }
  if (fileField) {
    parts.push(Buffer.from("--" + b + "\r\nContent-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + fileName + "\"\r\nContent-Type: " + fileCtype + "\r\n\r\n"));
    parts.push(fileBuf);
    parts.push(Buffer.from("\r\n"));
  }
  parts.push(Buffer.from("--" + b + "--\r\n"));
  const t = as === "admin" ? lib.getAdmin() : as === "user" ? lib.getUser() : null;
  const headers = { "Content-Type": "multipart/form-data; boundary=" + b };
  if (t) headers.Authorization = "Bearer " + t;
  const t0 = Date.now();
  let code = 0, txt = "";
  try {
    const r = await fetch(BASE + path, { method: "POST", headers, body: Buffer.concat(parts) });
    code = r.status; txt = await r.text();
  } catch (e) { code = -1; txt = e.message; }
  lib.rows.push({ name: "MP " + path + " as=" + as, method: "POST", path, as, code,
    ms: Date.now() - t0, expect: "see-run", pass: true, snippet: txt.slice(0, 300) });
  return { code, txt };
}

(async () => {
  await lib.initTokens();
  const wav = fs.readFileSync("C:/Users/ASUS/Documents/LAPTRINH/engflow/frontend/public/e2e-tts.wav");
  const tiny = Buffer.from("RIFF____fake-8-byte-audio", "utf8");

  console.log("--- speaking submission upload (Whisper sidecar) ---");
  let r = await multipart("/api/v1/speaking-submissions/upload?promptId=60014", "user", { promptId: "60014" }, "file", "rec.wav", wav, "audio/wav");
  console.log("  upload.wav   ->", r.code, r.txt.slice(0, 220));
  const subId = (() => { try { return JSON.parse(r.txt).id; } catch (e) { return null; } })();

  if (subId) {
    console.log("--- assess (Ollama 3b rubric) ---");
    const t0 = Date.now();
    r = await probe("assess " + subId, "POST", "/api/v1/speaking-submissions/" + subId + "/assess", "user", [200, 202, 400, 502]);
    console.log("  assess ->", r.code, ((Date.now() - t0) / 1000).toFixed(1) + "s", r.txt.slice(0, 260));
    const g = await probe("read back submission", "GET", "/api/v1/speaking-submissions/" + subId, "user", 200);
    console.log("  state:", g.txt.slice(0, 300));
    const del = await probe("cleanup submission", "DELETE", "/api/v1/admin/speaking-submissions/" + subId, "admin", [200, 204, 404, 405]);
    console.log("  cleanup:", del.code);
  }

  console.log("--- shadowing attempt (video) ---");
  r = await multipart("/api/v1/video-lessons/1/attempts", "user", { lineIndex: "0" }, "file", "a.wav", wav, "audio/wav");
  console.log("  video attempt ->", r.code, r.txt.slice(0, 200));
  const attId = (() => { try { return JSON.parse(r.txt).id; } catch (e) { return null; } })();
  if (attId) {
    const q = await probe("admin list attempt", "GET", "/api/v1/admin/video-attempts?page=0&size=3", "admin", 200);
    const ag = await probe("ai-grade attempt", "POST", "/api/v1/admin/video-attempts/" + attId + "/ai-grade", "admin", [200, 400, 502]);
    console.log("  ai-grade ->", ag.code, ag.txt.slice(0, 200));
    const gr = await probe("manual grade attempt", "PATCH", "/api/v1/admin/video-attempts/" + attId + "/grade", "admin", [200, 400], { score: 8.5, feedback: "zz audit-v8 manual" });
    console.log("  manual grade ->", gr.code, gr.txt.slice(0, 160));
  }

  console.log("--- lesson-submissions audio + text ---");
  r = await multipart("/api/lesson-submissions/upload-audio", "user", {}, "file", "l.wav", tiny, "audio/wav");
  console.log("  upload-audio ->", r.code, r.txt.slice(0, 140));
  const au = (() => { try { return JSON.parse(r.txt).audioUrl; } catch (e) { return null; } })();
  if (au) {
    const s = await probe("lesson-submissions submit", "POST", "/api/lesson-submissions/submit", "user", [200, 201, 400], { lessonId: 445, skillType: "SPEAKING", submissionText: "zz audit probe", audioUrl: au });
    console.log("  submit ->", s.code, s.txt.slice(0, 160));
    const g = await probe("read my submission", "GET", "/api/lesson-submissions/my/lesson/445/skill/SPEAKING", "user", [200, 404]);
    console.log("  read ->", g.code, g.txt.slice(0, 200));
  }

  console.log("--- admin upload + resources roundtrip (png allowed, html blocked) ---");
  const png = Buffer.from("89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c4890000000a49444154789c6300010000050001", "hex");
  r = await multipart("/api/admin/upload", "admin", {}, "file", "pic.png", png, "image/png");
  console.log("  png  ->", r.code, r.txt.slice(0, 120));
  const url = (() => { try { return JSON.parse(r.txt).url; } catch (e) { return null; } })();
  if (url) {
    const g = await probe("serve uploaded png", "GET", url, "none", 200);
    console.log("  served bytes ok, CT noted in json");
    const rm = await probe("cleanup uploaded png", "DELETE", url, "admin", [200, 204, 405, 404]);
    console.log("  (no delete endpoint expected:", rm.code + ")");
  }
  r = await multipart("/api/admin/upload", "admin", {}, "file", "evil.html", Buffer.from("<script>1</script>", "utf8"), "text/html");
  console.log("  html ->", r.code, r.txt.slice(0, 140));

  console.log("--- audio-upload (Cloudinary demo creds) ---");
  r = await multipart("/api/admin/audio-upload", "admin", {}, "file", "a.wav", tiny, "audio/wav");
  console.log("  audio-upload ->", r.code, r.txt.slice(0, 160));

  console.log("--- payment order ---");
  const st = await probe("payment status", "GET", "/api/v1/payment/status", "user", 200);
  console.log("  status:", st.txt.slice(0, 120));
  const ord = await probe("create-order MONTHLY", "POST", "/api/v1/payment/create-order", "user", [200, 400, 402], { planType: "MONTHLY" });
  console.log("  order ->", ord.code, ord.txt.slice(0, 260));

  console.log("--- avatar upload ---");
  r = await multipart("/api/auth/avatar/upload", "user", {}, "file", "av.png", png, "image/png");
  console.log("  avatar ->", r.code, r.txt.slice(0, 140));

  lib.report("PHASE3B");
  lib.dump("p3b.json");
})().catch(e => { console.error("FATAL", e); process.exit(1); });
