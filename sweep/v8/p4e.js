const lib = require("./lib.js");
const J = JSON.stringify;
const B = "http://localhost:8080";
const SRT = "1\n00:00:01,000 --> 00:00:03,000\nHello there.\n\n2\n00:00:03,500 --> 00:00:06,000\nThis is a test transcript.\n";
function mp(path, token, fields, boundary) {
  const b = boundary || ("----e" + Math.random().toString(16).slice(2));
  const chunks = [];
  for (const f of fields) {
    let head = "--" + b + "\r\nContent-Disposition: form-data; name=\"" + f.name + "\"";
    if (f.file) head += "; filename=\"" + f.file + "\"";
    head += "\r\n";
    if (f.ctype) head += "Content-Type: " + f.ctype + "\r\n";
    chunks.push(Buffer.from(head + "\r\n"), Buffer.from(String(f.val), "utf8"), Buffer.from("\r\n"));
  }
  chunks.push(Buffer.from("--" + b + "--\r\n"));
  return fetch(B + path, { method: "POST", headers: { Authorization: "Bearer " + token, "Content-Type": "multipart/form-data; boundary=" + b }, body: Buffer.concat(chunks) });
}
(async () => {
  await lib.initTokens();
  const admin = lib.getAdmin(), st = Date.now();
  const ids = [];
  const tally = { total: 0, pass: 0, fail: 0, failed: [] };
  async function run(label, meta, extra, expect) {
    const fields = [{ name: "meta", val: J(meta), ctype: "application/json" }].concat(extra);
    const r = await mp("/api/v1/admin/video-lessons/upload", admin, fields);
    const t = await r.text();
    let id = null; try { id = JSON.parse(t).id; } catch (e) {}
    if (id) ids.push(id);
    const ok = expect.indexOf(r.status) >= 0;
    tally.total++; if (ok) { tally.pass++ } else { tally.fail++; tally.failed.push(label) }
    console.log(("  " + label).padEnd(28) + r.status + (ok ? " ok " : " <<< UNEXPECTED ") + t.slice(0, 90).replace(/\n/g, " "));
    return { id: id, txt: t, status: r.status };
  }
  const base = (t) => ({ title: t, youtubeUrl: "https://www.youtube.com/watch?v=2VeQTuSSiI0", level: "ELEMENTARY", category: "PROBE", isPublished: false, transcript: [] });

  const a = await run("meta + SRT file", base("ZZ v8e srt " + st),
    [{ name: "transcriptFile", file: "s.srt", val: SRT, ctype: "text/plain" }], [201]);
  if (a.id) {
    const d = await (await fetch(B + "/api/v1/video-lessons/" + a.id)).json();
    console.log("      SRT -> lines=" + (d.transcript || []).length + " first=" + JSON.stringify((d.transcript || [])[0]) + " yt=" + d.youtubeVideoId);
  }
  await run("meta + transcriptText", base("ZZ v8e txt " + st),
    [{ name: "transcriptText", val: SRT }], [201]);
  await run("meta + bad youtube", { ...base("ZZ v8e badyt " + st), youtubeUrl: "https://example.com/nope" },
    [{ name: "transcriptText", val: SRT }], [400]);
  await run("1-line transcript", { ...base("ZZ v8e 1line " + st), transcript: undefined },
    [{ name: "transcriptText", val: "1\n00:00:01,000 --> 00:00:02,000\nOnly one.\n" }], [400]);
  await run("missing title (F: @Valid)", { youtubeUrl: "https://youtu.be/2VeQTuSSiI0", level: "ELEMENTARY", transcript: [] },
    [{ name: "transcriptFile", file: "s.srt", val: SRT, ctype: "text/plain" }], [400]);
  await run("admin create (hợp lệ)", base("ZZ v8e ok " + st),
    [{ name: "transcriptFile", file: "s.srt", val: SRT, ctype: "text/plain" }], [201]);

  const r2 = await mp("/api/v1/admin/video-lessons/upload", lib.getUser(),
    [{ name: "meta", val: J(base("ZZ v8e user " + st)), ctype: "application/json" }], "----fixedboundary");
  console.log("  non-admin (user token)    " + r2.status + (r2.status === 403 ? " ok" : " <<< UNEXPECTED"));

  for (const id of ids) await fetch(B + "/api/v1/admin/video-lessons/" + id, { method: "DELETE", headers: { Authorization: "Bearer " + admin } });
  const left = await (await fetch(B + "/api/v1/admin/video-lessons?page=0&size=50", { headers: { Authorization: "Bearer " + admin } })).json();
  const zz = (left.content || []).filter(x => (x.title || "").startsWith("ZZ v8e"));
  console.log("  cleanup: created=" + ids.length + " leftover-ZZ=" + zz.length);
  console.log("=== PHASE4E-UPLOAD total=" + tally.total + " FAIL=" + tally.fail + (tally.fail ? " :: " + tally.failed.join(", ") : ""));
})().catch(e => { console.error("FATAL", e.message); process.exit(1); });
