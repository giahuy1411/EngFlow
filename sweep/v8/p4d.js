const lib = require("./lib.js");
const fs = require("fs");
const B = "http://localhost:8080";
const J = JSON.stringify;

async function mp(path, token, parts) {
  const b = "----d" + Math.random().toString(16).slice(2);
  const chunks = [];
  for (const p of parts) {
    let head = "--" + b + "\r\nContent-Disposition: form-data; name=\"" + p.name + "\"";
    if (p.file) head += "; filename=\"" + p.file + "\"";
    head += "\r\n";
    if (p.ctype) head += "Content-Type: " + p.ctype + "\r\n";
    head += "\r\n";
    chunks.push(Buffer.from(head));
    chunks.push(Buffer.isBuffer(p.value) ? p.value : Buffer.from(String(p.value)));
    chunks.push(Buffer.from("\r\n"));
  }
  chunks.push(Buffer.from("--" + b + "--\r\n"));
  try {
    const r = await fetch(B + path, { method: "POST", body: Buffer.concat(chunks),
      headers: { Authorization: "Bearer " + token, "Content-Type": "multipart/form-data; boundary=" + b } });
    return { code: r.status, txt: await r.text() };
  } catch (e) { return { code: -1, txt: String(e.message) }; }
}

function rec(name, r, expect) {
  const ok = expect.indexOf(r.code) >= 0;
  lib.rows.push({ name: name, method: "POST", path: "/api/v1/admin/video-lessons/upload", as: "tok",
    code: r.code, ms: 0, expect: expect.join("/"), pass: ok, snippet: (r.txt || "").slice(0, 160) });
  console.log(("  " + name).padEnd(26) + "-> " + r.code + " " + r.txt.slice(0, 130));
  return r;
}

(async () => {
  await lib.initTokens();
  const admin = lib.getAdmin(), user = lib.getUser();
  const st = Date.now();
  const meta = { title: "ZZ v8 mp " + st, description: "probe", youtubeUrl: "https://www.youtube.com/watch?v=4EtXW3nnfPI",
    level: "ELEMENTARY", category: "PROBE", isPublished: false };

  const srt = "1\n00:00:00,000 --> 00:00:02,000\nHello there.\n\n2\n00:00:02,000 --> 00:00:04,000\nOne coffee please.\n\n";
  let r = rec("A meta+SRT file", await mp("/api/v1/admin/video-lessons/upload", admin,
    [{ name: "meta", ctype: "application/json", value: J(meta) },
     { name: "transcriptFile", file: "cap.srt", ctype: "application/x-subrip", value: Buffer.from(srt, "utf8") }]), [201, 200, 400]);
  let idA = null; try { idA = JSON.parse(r.txt).id; } catch (e) {}

  const metaB = J(Object.assign({}, meta, { title: "ZZ v8 txt " + st }));
  r = rec("B meta+transcriptText", await mp("/api/v1/admin/video-lessons/upload", admin,
    [{ name: "meta", ctype: "application/json", value: metaB }, { name: "transcriptText", value: "1|0|2|First line.\n2|2|4|Second line." }]),
    [201, 200, 400]);
  let idB = null; try { idB = JSON.parse(r.txt).id; } catch (e) {}

  const metaBad = J(Object.assign({}, meta, { title: "ZZ v8 bad " + st, youtubeUrl: "https://example.com/nope" }));
  rec("C bad youtube url", await mp("/api/v1/admin/video-lessons/upload", admin,
    [{ name: "meta", ctype: "application/json", value: metaBad }, { name: "transcriptText", value: "1|0|2|a\n2|2|4|b" }]), [400]);

  const metaNoLevel = J({ title: "no level", youtubeUrl: "https://youtu.be/2VeQTuSSiI0", transcript: [] });
  rec("D meta missing level", await mp("/api/v1/admin/video-lessons/upload", admin,
    [{ name: "meta", ctype: "application/json", value: metaNoLevel }, { name: "transcriptText", value: "1|0|2|a\n2|2|4|b" }]), [400]);

  rec("E non-admin", await mp("/api/v1/admin/video-lessons/upload", user,
    [{ name: "meta", ctype: "application/json", value: metaB }, { name: "transcriptText", value: "1|0|2|a\n2|2|4|b" }]), [403]);

  for (const [tag, id] of [["A", idA], ["B", idB]]) {
    if (!id) continue;
    const d = await (await fetch(B + "/api/v1/video-lessons/" + id)).json();
    console.log("  detail " + tag + " id=" + id + " lines=" + ((d.transcript || []).length) + " yt=" + d.youtubeVideoId + " level=" + d.level);
    await lib.probe("cleanup " + tag, "DELETE", "/api/v1/admin/video-lessons/" + id, "admin", [200, 204]);
  }
  lib.report("PHASE4D-UPLOAD");
  lib.dump("p4d.json");
})().catch(e => { console.error("FATAL", e); process.exit(1); });
