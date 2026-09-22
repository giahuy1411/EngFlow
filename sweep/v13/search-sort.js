/**
 * audit-v13-full Phase 2 — SEARCH + SORT measured on real rows.
 * Run: node sweep/v13/search-sort.js
 */
const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const API = "http://localhost:8080";
const R = { probed: [], findings: [] };

function flush() {
  try {
    const out = execFileSync("docker", ["exec", "engflow-redis", "redis-cli", "--scan", "--pattern", "rate_limit:*"], { encoding: "utf8" }).trim();
    const keys = out.split(/\r?\n/).filter(Boolean);
    if (keys.length) execFileSync("docker", ["exec", "engflow-redis", "redis-cli", "del", ...keys], { encoding: "utf8" });
    return keys.length;
  } catch { return -1; }
}
async function req(m, p, { token, body } = {}) {
  const h = {}; if (token) h.Authorization = "Bearer " + token;
  if (body !== undefined) h["Content-Type"] = "application/json";
  const res = await fetch(API + p, { method: m, headers: h, body: body !== undefined ? JSON.stringify(body) : undefined });
  const t = await res.text(); let d; try { d = JSON.parse(t); } catch { d = t.slice(0, 200); }
  return { status: res.status, data: d };
}
const login = async (e, p) => (await req("POST", "/api/auth/login", { body: { email: e, password: p } })).data?.token;

(async () => {
  console.log("flushed:", flush());
  const U = await login("user@gmail.com", "123456");
  const A = await login("admin@gmail.com", "123456");
  console.log("tokens", !!U, !!A);

  // ── SEARCH: vocabulary keyword search (case-insensitive substring) ──
  const cases = [
    ["ab", "lowercase substring"],
    ["AB", "uppercase substring (case-insensitive?)"],
    ["Ab", "mixed case"],
    ["rise", "real word fragment"],
    ["zzzznotaword", "no match -> []"],
    ["a", "1-char -> [] by design"],
    ["", "empty -> [] by design"],
  ];
  for (const [kw, label] of cases) {
    const r = await req("GET", `/api/vocabulary/search?keyword=${encodeURIComponent(kw)}`);
    const len = Array.isArray(r.data) ? r.data.length : "n/a";
    const words = Array.isArray(r.data) ? r.data.slice(0, 5).map((v) => v.word) : [];
    console.log(`  SEARCH "${kw}" (${label}) -> ${r.status} len=${len} ${JSON.stringify(words)}`);
    R.probed.push({ probe: "vocab-search", keyword: kw, label, status: r.status, len, words });
  }
  // case-insensitivity check
  const lo = await req("GET", "/api/vocabulary/search?keyword=rise");
  const up = await req("GET", "/api/vocabulary/search?keyword=RISE");
  const same = JSON.stringify((lo.data || []).map((v) => v.word)) === JSON.stringify((up.data || []).map((v) => v.word));
  console.log(`  CASE-INSENSITIVE: rise==RISE -> ${same}`);
  R.probed.push({ probe: "vocab-search-case", same, lower: (lo.data || []).length, upper: (up.data || []).length });

  // ── SEARCH: lessons ?q= ──
  const lq = await req("GET", "/api/lessons?q=grammar&size=5");
  console.log(`  LESSON q=grammar -> ${lq.status} total=${lq.data?.totalElements} titles=${JSON.stringify((lq.data?.content || []).map((x) => x.title).slice(0, 3))}`);
  R.probed.push({ probe: "lesson-q", status: lq.status, total: lq.data?.totalElements, first: (lq.data?.content || [])[0]?.title });
  const lq2 = await req("GET", "/api/lessons?q=zzzznotaword&size=5");
  console.log(`  LESSON q=zzzznotaword -> ${lq2.status} total=${lq2.data?.totalElements}`);
  R.probed.push({ probe: "lesson-q-nomatch", status: lq2.status, total: lq2.data?.totalElements });

  // ── SEARCH: admin exercises ?q= ──
  const eq = await req("GET", "/api/admin/exercises?q=the&size=5", { token: A });
  console.log(`  ADMIN-EX q=the -> ${eq.status} total=${eq.data?.totalElements} first=${JSON.stringify((eq.data?.content || [])[0]?.question)?.slice(0, 60)}`);
  R.probed.push({ probe: "admin-ex-q", status: eq.status, total: eq.data?.totalElements });

  // ── SORT: lessons ?sort= (claimed ignored) ──
  const keys = (r) => (r.data?.content || []).map((x) => x.title);
  const la = await req("GET", "/api/lessons?size=8&sort=title,asc");
  const ld = await req("GET", "/api/lessons?size=8&sort=title,desc");
  const lIdentical = JSON.stringify(keys(la)) === JSON.stringify(keys(ld));
  const lDefault = await req("GET", "/api/lessons?size=8");
  const lIsDefaultOrder = JSON.stringify(keys(la)) === JSON.stringify(keys(lDefault));
  console.log(`  LESSONS sort=title,asc==desc -> ${lIdentical} (asc==no-sort -> ${lIsDefaultOrder})`);
  R.probed.push({ probe: "lesson-sort", identicalAscDesc: lIdentical, identicalToDefault: lIsDefaultOrder, asc: keys(la), desc: keys(ld) });

  // ── SORT: admin exercises ?sort= ──
  const ea = await req("GET", "/api/admin/exercises?size=8&sort=question,asc", { token: A });
  const ed = await req("GET", "/api/admin/exercises?size=8&sort=question,desc", { token: A });
  const eIdentical = JSON.stringify((ea.data?.content || []).map((x) => x.id)) === JSON.stringify((ed.data?.content || []).map((x) => x.id));
  console.log(`  ADMIN-EX sort=question,asc==desc -> ${eIdentical}`);
  R.probed.push({ probe: "admin-ex-sort", identicalAscDesc: eIdentical });

  // ── SORT: vocabulary ?sort= (uses PageableDefault -> spring honours it) ──
  const va = await req("GET", "/api/vocabulary?size=8&sort=word,asc", { token: A });
  const vd = await req("GET", "/api/vocabulary?size=8&sort=word,desc", { token: A });
  const vk = (r) => (r.data?.content || []).map((x) => x.word);
  const vIdentical = JSON.stringify(vk(va)) === JSON.stringify(vk(vd));
  const vAscOrdered = vk(va).every((w, i) => i === 0 || String(vk(va)[i - 1]).localeCompare(String(w)) <= 0);
  const vDescOrdered = vk(vd).every((w, i) => i === 0 || String(vk(vd)[i - 1]).localeCompare(String(w)) >= 0);
  console.log(`  VOCAB sort=word,asc==desc -> ${vIdentical} ascOrdered=${vAscOrdered} descOrdered=${vDescOrdered}`);
  R.probed.push({ probe: "vocab-sort", identicalAscDesc: vIdentical, vAscOrdered, vDescOrdered, asc: vk(va), desc: vk(vd) });

  // ── SORT: decks ?sort= ──
  const da = await req("GET", "/api/decks?size=8&sort=name,asc", { token: A });
  const dd = await req("GET", "/api/decks?size=8&sort=name,desc", { token: A });
  const dk = (r) => (r.data?.content || []).map((x) => x.name);
  console.log(`  DECKS sort=name,asc==desc -> ${JSON.stringify(dk(da)) === JSON.stringify(dk(dd))}`);
  R.probed.push({ probe: "deck-sort", identicalAscDesc: JSON.stringify(dk(da)) === JSON.stringify(dk(dd)) });

  // ── SORT: does an INVALID sort field 500 or fall back? ──
  const bad = await req("GET", "/api/vocabulary?size=5&sort=notacolumn,asc", { token: A });
  console.log(`  VOCAB sort=notacolumn -> ${bad.status} ${JSON.stringify(bad.data).slice(0, 120)}`);
  R.probed.push({ probe: "vocab-sort-invalid", status: bad.status });

  const OUT = path.join(__dirname, "..", "..", ".specify", "specs", "audit-v13-full", "evidence", "search-sort.json");
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, JSON.stringify(R, null, 2));
  console.log("written:", path.relative(path.join(__dirname, "..", ".."), OUT));
})();
