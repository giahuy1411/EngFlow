const lib = require("./lib.js");
const { probe } = lib;
const J = JSON.stringify;
const B = "http://localhost:8080";

async function raw(method, path, token, body, ctype) {
  const headers = {};
  if (token) headers.Authorization = "Bearer " + token;
  let payload;
  if (body !== undefined && body !== null) {
    if (typeof body === "string" || Buffer.isBuffer(body)) { payload = body; if (ctype) headers["Content-Type"] = ctype; }
    else { payload = J(body); headers["Content-Type"] = "application/json"; }
  }
  try {
    const r = await fetch(B + path, { method, headers, body: payload });
    return { code: r.status, txt: await r.text(), hdr: r.headers };
  } catch (e) { return { code: -1, txt: String(e.message), hdr: null }; }
}

async function tok2(name, method, path, token, expect, body) {
  const r = await raw(method, path, token, body);
  const exp = Array.isArray(expect) ? expect : [expect];
  lib.rows.push({ name, method, path, as: "bearer", code: r.code, ms: 0,
    expect: exp.join("/"), pass: exp.indexOf(r.code) >= 0, snippet: r.txt.slice(0, 180) });
  return r;
}

function redisGet(key) {
  const { execSync } = require("child_process");
  try { return execSync("docker exec engflow-redis redis-cli get " + JSON.stringify(key), { encoding: "utf8" }).trim(); }
  catch (e) { return "ERR"; }
}

(async () => {
  await lib.initTokens();
  const stamp = Date.now();
  const email = "zzp4" + stamp + "@example.com";
  let r = await probe("A0 register disposable", "POST", "/api/auth/register", "none", 201,
    { username: "zzp4" + stamp, email, password: "Str0ngPass!", fullName: "ZZ P4" });
  let uid = null, dtok = null;
  try { const j = JSON.parse(r.txt); uid = j.id; dtok = j.token || (j.data || {}).token; } catch (e) {}
  console.log("  disposable id=" + uid + " token=" + !!dtok);

  console.log("-- change-password --");
  console.log("  wrong old -> " + (await tok2("CP wrong old", "POST", "/api/auth/change-password", dtok, [400, 401, 403], { oldPassword: "WRONGold1!", newPassword: "NewStr0ng!" })).code);
  console.log("  short new -> " + (await tok2("CP short new", "POST", "/api/auth/change-password", dtok, [400], { oldPassword: "Str0ngPass!", newPassword: "abc" })).code);
  console.log("  valid     -> " + (await tok2("CP valid", "POST", "/api/auth/change-password", dtok, [200, 204], { oldPassword: "Str0ngPass!", newPassword: "N3wStr0ng!" })).code);
  let lg = await raw("POST", "/api/auth/login", null, { email, password: "N3wStr0ng!" });
  console.log("  login NEW pw -> " + lg.code + " (200 = change took effect)");
  let nt = null; try { const j = JSON.parse(lg.txt); nt = j.token || (j.data || {}).token; } catch (e) {}
  lg = await raw("POST", "/api/auth/login", null, { email, password: "Str0ngPass!" });
  console.log("  login OLD pw -> " + lg.code + " (must be 400/401)");

  console.log("-- forgot -> OTP -> reset --");
  r = await raw("POST", "/api/auth/forgot-password", null, { email });
  console.log("  forgot -> " + r.code);
  const otp = redisGet("otp:reset:" + email);
  const hasOtp = /^[0-9]{6}$/.test(otp);
  console.log("  redis OTP " + (hasOtp ? "present " + otp.slice(0, 2) + "**" : "MISSING: " + otp.slice(0, 40)));
  if (hasOtp) {
    console.log("  wrong otp -> " + (await tok2("RP wrong otp", "POST", "/api/auth/reset-password", null, [400, 401], { email, otp: "000000", newPassword: "EvilN3w!" })).code);
    console.log("  valid otp -> " + (await tok2("RP valid otp", "POST", "/api/auth/reset-password", null, [200], { email, otp, newPassword: "R3setStr0ng!" })).code);
    lg = await raw("POST", "/api/auth/login", null, { email, password: "R3setStr0ng!" });
    console.log("  login after reset -> " + lg.code);
    try { const j = JSON.parse(lg.txt); nt = j.token || (j.data || {}).token || nt; } catch (e) {}
    console.log("  OTP consumed after use -> " + (redisGet("otp:reset:" + email) === "" ? "YES" : "NO (still valid!)"));
  }

  console.log("-- avatar PUT --");
  console.log("  set url -> " + (await tok2("AV set", "PUT", "/api/auth/avatar", nt, [200], { avatarUrl: "https://api.dicebear.com/7.x/adventurer/svg?seed=zzp4" })).code);
  console.log("  blank   -> " + (await tok2("AV blank", "PUT", "/api/auth/avatar", nt, [400], { avatarUrl: "" })).code);
  await probe("AV noauth 401", "PUT", "/api/auth/avatar", "none", 401, { avatarUrl: "https://x/y.png" });

  console.log("-- admin user toggles (on disposable) --");
  const du = String(uid);
  await probe("T active", "PUT", "/api/admin/users/" + du + "/toggle-active", "admin", 200);
  await probe("T active back", "PUT", "/api/admin/users/" + du + "/toggle-active", "admin", 200);
  await probe("T premium on", "PUT", "/api/admin/users/" + du + "/toggle-premium", "admin", 200);
  await probe("T premium off", "PUT", "/api/admin/users/" + du + "/revoke-premium", "admin", [200, 400]);
  await probe("T admin-flag", "PUT", "/api/admin/users/" + du + "/toggle-admin", "admin", [200]);
  await probe("T admin-flag back", "PUT", "/api/admin/users/" + du + "/toggle-admin", "admin", [200]);
  await probe("T user-role 403", "PUT", "/api/admin/users/" + du + "/toggle-active", "user", 403);
  await probe("T noauth 401", "PUT", "/api/admin/users/" + du + "/toggle-active", "none", 401);
  await probe("T unknown 404", "PUT", "/api/admin/users/99999999/toggle-active", "admin", [404, 400]);

  lib.report("PHASE4A-AUTH");
  lib.dump("p4a.json");
  console.log("DISPOSABLE_ID " + uid + " EMAIL " + email);
})().catch(e => { console.error("FATAL", e); process.exit(1); });
