const lib = require("./lib.js");
const { probe } = lib;
const J = JSON.stringify;
(async () => {
  await lib.initTokens();
  const st = Date.now();

  // ---- video-lesson admin CRUD ----
  let r = await probe("VL create", "POST", "/api/v1/admin/video-lessons", "admin", [200, 201], {
    title: "ZZ v8 vid " + st, description: "probe", youtubeUrl: "https://www.youtube.com/watch?v=2VeQTuSSiI0",
    level: "PRE_INTERMEDIATE", category: "PROBE", isPublished: false,
    transcript: [{ start: 0, end: 2, textEn: "Hello there." }, { start: 2, end: 4, textEn: "One coffee please." }] });
  let vid = null; try { vid = JSON.parse(r.txt).id; } catch (e) { console.log("  create resp " + r.txt.slice(0, 120)); }
  console.log("  videoId=" + vid);
  if (vid) {
    // audit-v8 F88: fixture nay la NHAP (isPublished:false) -> guest phai 404, admin van 200.
    await probe("VL draft detail guest 404", "GET", "/api/v1/video-lessons/" + vid, "none", 404);
    r = await probe("VL draft detail admin 200", "GET", "/api/v1/video-lessons/" + vid, "admin", 200);
    let tc = null; try { tc = JSON.parse(r.txt).transcript.length; } catch (e) {}
    console.log("  transcript lines=" + tc);
    await probe("VL update", "PUT", "/api/v1/admin/video-lessons/" + vid, "admin", [200], {
      title: "ZZ v8 vid " + st + "b", description: "p2", youtubeUrl: "https://youtu.be/2VeQTuSSiI0",
      level: "INTERMEDIATE", category: "PROBE", isPublished: true, transcript: [{ start: 0, end: 2, textEn: "Updated." }, { start: 2, end: 4, textEn: "Second line." }] });
    await probe("VL user create 403", "POST", "/api/v1/admin/video-lessons", "user", 403,
      { title: "x", youtubeUrl: "https://youtu.be/2VeQTuSSiI0", level: "ELEMENTARY", transcript: [] });
    await probe("VL published detail guest 200", "GET", "/api/v1/video-lessons/" + vid, "none", 200);
    await probe("VL bad level 400", "POST", "/api/v1/admin/video-lessons", "admin", 400,
      { title: "x", youtubeUrl: "https://youtu.be/2VeQTuSSiI0", level: "B1", transcript: [{ start: 0, end: 2, textEn: "a" }] });
    await probe("VL delete", "DELETE", "/api/v1/admin/video-lessons/" + vid, "admin", [200, 204]);
    await probe("VL gone 404", "GET", "/api/v1/video-lessons/" + vid, "none", 404);
  }

  // ---- multipart video upload (meta part + srt file) ----
  const b = "----p4b" + Math.random().toString(16).slice(2);
  const meta = { title: "ZZ v8 up " + st, youtubeUrl: "https://www.youtube.com/watch?v=reKgQh0E9kg", level: "ELEMENTARY", category: "PROBE", isPublished: false, transcript: [] };
  const body = Buffer.concat([
    Buffer.from("--" + b + "\r\nContent-Disposition: form-data; name=\"meta\"\r\nContent-Type: application/json\r\n\r\n" + J(meta) + "\r\n"),
    Buffer.from("--" + b + "\r\nContent-Disposition: form-data; name=\"transcriptFile\"; filename=\"c.srt\"\r\nContent-Type: application/x-subrip\r\n\r\n"),
    Buffer.from("1\n00:00:00,000 --> 00:00:02,000\nHello.\n\n2\n00:00:02,000 --> 00:00:04,000\nCoffee.\n\n", "utf8"),
    Buffer.from("\r\n--" + b + "--\r\n")]);
  r = await fetch("http://localhost:8080/api/v1/admin/video-lessons/upload", {
    method: "POST", headers: { Authorization: "Bearer " + lib.getAdmin(), "Content-Type": "multipart/form-data; boundary=" + b }, body: body });
  const txt = await r.text();
  let upId = null; try { upId = JSON.parse(txt).id; } catch (e) {}
  console.log("  upload ->", r.status, upId ? "id=" + upId : txt.slice(0, 120));
  if (upId) {
    const d = await (await fetch("http://localhost:8080/api/v1/video-lessons/" + upId, { headers: { Authorization: "Bearer " + lib.getAdmin() } })).json();
    console.log("  SRT parsed lines=" + ((d.transcript || []).length) + " yt=" + d.youtubeVideoId);
    await probe("upload cleanup", "DELETE", "/api/v1/admin/video-lessons/" + upId, "admin", [200, 204]);
  }

  // ---- speaking manual grading (fresh submission, then SQL cleanup) ----
  const wav = require("fs").readFileSync("C:/Users/ASUS/Documents/LAPTRINH/engflow/frontend/public/e2e-tts.wav");
  const bb = "----g" + Math.random().toString(16).slice(2);
  const up = Buffer.concat([
    Buffer.from("--" + bb + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"g.wav\"\r\nContent-Type: audio/wav\r\n\r\n"),
    wav, Buffer.from("\r\n--" + bb + "--\r\n")]);
  r = await fetch("http://localhost:8080/api/v1/speaking-submissions/upload?promptId=60014", {
    method: "POST", headers: { Authorization: "Bearer " + lib.getUser(), "Content-Type": "multipart/form-data; boundary=" + bb }, body: up });
  const gj = await r.text(); let sid = null; try { sid = JSON.parse(gj).id; } catch (e) {}
  console.log("  submission=" + sid + " (" + r.status + ")");
  if (sid) {
    await probe("SM user 403", "PATCH", "/api/v1/admin/speaking-submissions/" + sid + "/grade", "user", 403, { score: 7, feedback: "no" });
    await probe("SM score 11 400", "PATCH", "/api/v1/admin/speaking-submissions/" + sid + "/grade", "admin", [400], { score: 11, feedback: "x" });
    await probe("SM 2 decimals 400", "PATCH", "/api/v1/admin/speaking-submissions/" + sid + "/grade", "admin", [400], { score: 7.55, feedback: "x" });
    await probe("SM blank feedback 400", "PATCH", "/api/v1/admin/speaking-submissions/" + sid + "/grade", "admin", [400], { score: 7.5, feedback: "" });
    r = await probe("SM valid grade 200", "PATCH", "/api/v1/admin/speaking-submissions/" + sid + "/grade", "admin", [200], { score: 8.5, feedback: "zz p4 fb", privateNote: "zz p4 note" });
    let g = null; try { g = JSON.parse(r.txt); } catch (e) {}
    console.log("  graded score=" + (g && g.score) + " status=" + (g && g.status) + " SUBMISSION_ID " + sid);
  }
  await probe("SM unknown 404", "PATCH", "/api/v1/admin/speaking-submissions/99999999/grade", "admin", [404, 400], { score: 8.5, feedback: "z" });

  lib.report("PHASE4B");
  lib.dump("p4b.json");
})().catch(e => { console.error("FATAL", e); process.exit(1); });
