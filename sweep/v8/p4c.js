const lib = require("./lib.js");
const { probe } = lib;
const J = JSON.stringify;
(async () => {
  await lib.initTokens();
  const st = Date.now();
  const two = [{ start: 0, end: 2, textEn: "Hello there." }, { start: 2, end: 4, textEn: "One coffee please." }];

  let r = await probe("VL create", "POST", "/api/v1/admin/video-lessons", "admin", [200, 201], {
    title: "ZZ v8 vid " + st, description: "probe", youtubeUrl: "https://www.youtube.com/watch?v=2VeQTuSSiI0",
    level: "ELEMENTARY", category: "PROBE", isPublished: false, transcript: two });
  let vid = null; try { vid = JSON.parse(r.txt).id; } catch (e) { console.log("  resp " + r.txt.slice(0, 140)); }
  console.log("  videoId=" + vid);
  if (vid) {
    // audit-v8 F88: fixture la NHAP -> guest 404, admin 200 (xem p4b cho nhanh published).
  await probe("VL draft detail guest 404", "GET", "/api/v1/video-lessons/" + vid, "none", 404);
  r = await probe("VL draft detail admin 200", "GET", "/api/v1/video-lessons/" + vid, "admin", 200);
    let d = null; try { d = JSON.parse(r.txt); } catch (e) {}
    console.log("  yt=" + (d && d.youtubeVideoId) + " lines=" + (d && d.transcript && d.transcript.length));
    await probe("VL update", "PUT", "/api/v1/admin/video-lessons/" + vid, "admin", [200], {
      title: "ZZ v8 vid " + st + "b", description: "p2", youtubeUrl: "https://youtu.be/2VeQTuSSiI0",
      level: "INTERMEDIATE", category: "PROBE", isPublished: true, transcript: two });
    await probe("VL bad level 400", "PUT", "/api/v1/admin/video-lessons/" + vid, "admin", [400], {
      title: "x", youtubeUrl: "https://youtu.be/2VeQTuSSiI0", level: "B1", transcript: two });
    await probe("VL 1 line 400", "PUT", "/api/v1/admin/video-lessons/" + vid, "admin", [400], {
      title: "x", youtubeUrl: "https://youtu.be/2VeQTuSSiI0", level: "B1", transcript: [two[0]] });
    await probe("VL user create 403", "POST", "/api/v1/admin/video-lessons", "user", 403, {
      title: "x", youtubeUrl: "https://youtu.be/2VeQTuSSiI0", level: "ELEMENTARY", transcript: two });
    await probe("VL delete", "DELETE", "/api/v1/admin/video-lessons/" + vid, "admin", [200, 204]);
    await probe("VL gone 404", "GET", "/api/v1/video-lessons/" + vid, "none", 404);
  }

  const b = "----p4c" + Math.random().toString(16).slice(2);
  const meta = { title: "ZZ v8 up " + st, youtubeUrl: "https://www.youtube.com/watch?v=reKgQh0E9kg", level: "PRE_INTERMEDIATE", category: "PROBE", isPublished: false };
  const body = Buffer.concat([
    Buffer.from("--" + b + "\r\nContent-Disposition: form-data; name=\"meta\"\r\nContent-Type: application/json\r\n\r\n" + J(meta) + "\r\n"),
    Buffer.from("--" + b + "\r\nContent-Disposition: form-data; name=\"transcriptFile\"; filename=\"c.srt\"\r\nContent-Type: application/x-subrip\r\n\r\n"),
    Buffer.from("1\n00:00:00,000 --> 00:00:02,000\nHello.\n\n2\n00:00:02,000 --> 00:00:04,000\nCoffee.\n\n", "utf8"),
    Buffer.from("\r\n--" + b + "--\r\n")]);
  const up = await fetch("http://localhost:8080/api/v1/admin/video-lessons/upload", {
    method: "POST", headers: { Authorization: "Bearer " + lib.getAdmin(), "Content-Type": "multipart/form-data; boundary=" + b }, body: body });
  const ut = await up.text(); let uid = null; try { uid = JSON.parse(ut).id; } catch (e) {}
  console.log("  multipart upload -> " + up.status + " id=" + (uid || ut.slice(0, 120)));
  if (uid) {
    const dd = await (await fetch("http://localhost:8080/api/v1/video-lessons/" + uid, { headers: { Authorization: "Bearer " + lib.getAdmin() } })).json();
    console.log("  SRT parsed lines=" + (dd.transcript || []).length + " yt=" + dd.youtubeVideoId + " level=" + dd.level);
    await probe("upload cleanup", "DELETE", "/api/v1/admin/video-lessons/" + uid, "admin", [200, 204]);
  }
  lib.report("PHASE4C-VIDEOS");
  lib.dump("p4c.json");
})().catch(e => { console.error("FATAL", e); process.exit(1); });
