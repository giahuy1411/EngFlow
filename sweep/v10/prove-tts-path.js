/**
 * audit-v10 Phase 3.8 — chung minh duong TTS -> Cloudinary co chay that khong.
 *
 * QUYET DINH C7: chi backfill 9 audio neu duong nay duoc chung minh bang mot
 * file media THAT. AGENTS.md ghi rang byte gia tao ra 400/500 va tung dan toi
 * ket luan sai ve "demo credentials" — nen phai do, khong duoc suy doan.
 *
 * Script nay chay tung buoc va DUNG ngay khi mot buoc that bai, ghi ro buoc nao.
 * KHONG ghi vao DB. Chi tra ve byte + URL de nguoi doc quyet dinh.
 *
 * Chay: node sweep/v10/prove-tts-path.js
 */
const fs = require("fs");
const path = require("path");
const TTS = "http://localhost:8001";
const API = "http://localhost:8080";

async function login(email, pass) {
  const r = await fetch(API + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password: pass }),
  });
  if (r.status !== 200) return null;
  const j = await r.json();
  return j.token || j.data?.token || null;
}

(async () => {
  console.log("=== chung minh duong TTS -> Cloudinary ===");
  console.log("");

  // ---------- BUOC 1: TTS sidecar song ----------
  console.log("BUOC 1: TTS sidecar /health");
  const h = await fetch(TTS + "/health");
  console.log("  status = " + h.status);
  if (h.status !== 200) { console.log("  >>> DUNG: sidecar khong song"); process.exit(1); }
  console.log("  OK");
  console.log("");

  // ---------- BUOC 2: sinh WAV that ----------
  console.log("BUOC 2: POST /synthesize -> WAV bytes");
  const TEXT = "The quick brown fox jumps over the lazy dog.";
  const t0 = Date.now();
  const sr = await fetch(TTS + "/synthesize", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text: TEXT, voice: "M1", lang: "en" }),
  });
  const ms = Date.now() - t0;
  console.log("  status = " + sr.status + "   thoi gian = " + ms + "ms");
  if (sr.status !== 200) {
    console.log("  body: " + (await sr.text()).slice(0, 300));
    console.log("  >>> DUNG: synthesize that bai");
    process.exit(1);
  }
  const buf = Buffer.from(await sr.arrayBuffer());
  console.log("  so byte = " + buf.length);

  // WAV that phai bat dau bang "RIFF"...."WAVE"
  const riff = buf.slice(0, 4).toString("ascii");
  const wave = buf.slice(8, 12).toString("ascii");
  console.log("  magic  = '" + riff + "' + '" + wave + "'  (WAV that phai la RIFF/WAVE)");
  if (riff !== "RIFF" || wave !== "WAVE") {
    console.log("  >>> DUNG: khong phai WAV that");
    process.exit(1);
  }
  // Doc header de biet sample rate / channels
  const channels = buf.readUInt16LE(22);
  const sampleRate = buf.readUInt32LE(24);
  const bits = buf.readUInt16LE(34);
  console.log("  channels=" + channels + " sampleRate=" + sampleRate + " bits=" + bits);
  if (buf.length < 1000) {
    console.log("  >>> DUNG: file qua nho, nghi la rong");
    process.exit(1);
  }
  console.log("  OK — day la WAV THAT");

  const outWav = path.join(__dirname, "tts-proof.wav");
  fs.writeFileSync(outWav, buf);
  console.log("  da ghi: " + outWav);
  console.log("");

  // ---------- BUOC 3: upload qua API that ----------
  console.log("BUOC 3: POST /api/admin/audio-upload (multipart)");
  const token = await login("admin@gmail.com", "123456");
  if (!token) { console.log("  >>> DUNG: khong dang nhap duoc admin"); process.exit(1); }
  console.log("  co token admin");

  const fd = new FormData();
  fd.append("file", new Blob([buf], { type: "audio/wav" }), "tts-proof.wav");
  const up = await fetch(API + "/api/admin/audio-upload", {
    method: "POST", headers: { Authorization: `Bearer ${token}` }, body: fd,
  });
  const upText = await up.text();
  console.log("  status = " + up.status);
  console.log("  body   = " + upText.slice(0, 300));
  if (up.status !== 200) {
    console.log("  >>> DUNG: upload that bai. Duong TTS->Cloudinary CHUA duoc chung minh.");
    process.exit(1);
  }
  let url = null;
  try { url = JSON.parse(upText).url; } catch (e) {}
  if (!url) { console.log("  >>> DUNG: response khong co url"); process.exit(1); }
  console.log("  URL = " + url);
  console.log("");

  // ---------- BUOC 4: URL phai that su phuc vu duoc audio ----------
  console.log("BUOC 4: GET URL vua tra ve");
  const g = await fetch(url);
  const ct = g.headers.get("content-type");
  const len = g.headers.get("content-length");
  const body = await g.arrayBuffer();
  console.log("  status       = " + g.status);
  console.log("  content-type = " + ct);
  console.log("  content-len  = " + len);
  console.log("  so byte nhan = " + body.byteLength);
  if (g.status !== 200 || body.byteLength < 1000) {
    console.log("  >>> DUNG: URL khong phuc vu duoc audio that");
    process.exit(1);
  }
  console.log("  OK — URL phuc vu audio that");
  console.log("");

  console.log("============================================");
  console.log("  DUONG TTS -> CLOUDINARY DA DUOC CHUNG MINH");
  console.log("============================================");
  console.log("");
  console.log("Co the backfill 9 row LISTENING. NHUNG script nay CO Y KHONG ghi DB —");
  console.log("can mot quyet dinh rieng vi no sua du lieu bai hoc that.");
  console.log("");
  console.log("URL mau: " + url);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
