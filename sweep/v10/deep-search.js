/**
 * audit-v10 Phase 4.9 — deep check Tim kiem / Sap xep.
 *
 * Kiem: sort theo nhieu cot, huong tang/giam, phan trang, tham so bien
 * (page am, size qua lon, sort cot khong ton tai), tim kiem rong/co ket qua/
 * khong ket qua/ky tu dac biet.
 */
const API = "http://localhost:8080";
const R = { pass: 0, fail: 0, fails: [] };
function check(name, cond, detail) {
  if (cond) { R.pass++; console.log("  PASS  " + name); }
  else { R.fail++; console.log("  FAIL  " + name + "  -> " + detail); R.fails.push(name + " -> " + detail); }
}
async function get(p, token) {
  const h = {}; if (token) h.Authorization = `Bearer ${token}`;
  const r = await fetch(API + p, { headers: h });
  const t = await r.text();
  let d = null; try { d = JSON.parse(t); } catch { d = t.slice(0, 200); }
  return { status: r.status, data: d };
}
async function login(e, p) {
  const r = await fetch(API + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: e, password: p }),
  });
  const j = await r.json();
  return j.token || j.data?.token;
}
function contentOf(d) { return Array.isArray(d) ? d : (d?.content || d?.data?.content || d?.data || []); }

(async () => {
  console.log("=== Phase 4.9 — deep check Tim kiem / Sap xep ===");
  console.log("");
  const admin = await login("admin@gmail.com", "123456");

  // ---------- PHAN TRANG ----------
  console.log("--- phan trang ---");
  const p0 = await get("/api/lessons?page=0&size=5");
  check("page=0&size=5 -> 200", p0.status === 200, `nhan ${p0.status}`);
  const c0 = contentOf(p0.data);
  check("tra ve dung 5 phan tu", c0.length === 5, `nhan ${c0.length}`);
  check("co metadata phan trang", !!(p0.data?.totalElements !== undefined || p0.data?.totalPages !== undefined),
        `keys: ${Object.keys(p0.data || {}).join(",")}`);

  const p1 = await get("/api/lessons?page=1&size=5");
  const c1 = contentOf(p1.data);
  check("page=1 -> 200", p1.status === 200, `nhan ${p1.status}`);
  const ids0 = c0.map((x) => x.id);
  const ids1 = c1.map((x) => x.id);
  check("page 0 va page 1 KHONG trung phan tu", !ids0.some((i) => ids1.includes(i)),
        `trung: ${JSON.stringify(ids0.filter((i) => ids1.includes(i)))}`);

  const big = await get("/api/lessons?page=0&size=100000");
  check("size=100000 khong 500", big.status !== 500, `nhan ${big.status}`);

  const negPage = await get("/api/lessons?page=-1&size=10");
  check("page=-1 khong 500", negPage.status !== 500, `nhan ${negPage.status}`);

  const negSize = await get("/api/lessons?page=0&size=-5");
  check("size=-5 khong 500", negSize.status !== 500, `nhan ${negSize.status}`);

  // ---------- SAP XEP ----------
  //
  // SU THAT DO DUOC: KHONG controller nao nhan `sort` tu client.
  //
  //   LessonController:38   Sort.by("orderIndex").ascending().and(Sort.by("id"))
  //   DeckController:34,50  Sort.by("name").ascending().and(Sort.by("id"))
  //   AdminExerciseController:39  Sort.by("orderIndex").ascending().and(Sort.by("id"))
  //
  // Tat ca hard-code. Va frontend cung KHONG gui `sort`:
  //   Lessons.vue:173  const params = { page, size }
  //   (chi them `level` va `q`)
  //
  // Nen "sap xep" khong phai tinh nang nguoi dung — no la mot tham so URL bi
  // BO QUA IM LANG. Phien ban dau cua probe nay doi asc/desc tra ve khac nhau
  // va bao 3 FAIL; do la GIA DINH SAI cua probe, khong phai loi app.
  //
  // Cai DUNG can kiem: thu tu hard-code phai on dinh (on dinh la dieu kien de
  // phan trang khong bo sot / lap row), va tham so `sort` la khong duoc lam 500.
  console.log("");
  console.log("--- sap xep (thu tu hard-code, KHONG nhan tu client) ---");
  const first = await get("/api/lessons?page=0&size=10");
  const again = await get("/api/lessons?page=0&size=10");
  check("goi lai cung tham so -> 200", first.status === 200 && again.status === 200,
        `${first.status}/${again.status}`);
  const fIds = contentOf(first.data).map((x) => x.id);
  const gIds = contentOf(again.data).map((x) => x.id);
  check("thu tu ON DINH giua 2 lan goi (phan trang khong bo sot row)",
        fIds.join(",") === gIds.join(","), `${JSON.stringify(fIds.slice(0, 5))} vs ${JSON.stringify(gIds.slice(0, 5))}`);

  // Tham so `sort` gui len phai bi bo qua EM, khong duoc 500.
  const withSort = await get("/api/lessons?page=0&size=5&sort=id,desc");
  check("gui sort=id,desc -> 200 (tham so bi bo qua, khong loi)", withSort.status === 200, `nhan ${withSort.status}`);

  const badSort = await get("/api/lessons?page=0&size=5&sort=cot_khong_ton_tai,asc");
  check("sort cot khong ton tai -> khong 500", badSort.status !== 500, `nhan ${badSort.status}`);

  // Ghi nhan ro: day KHONG phai tinh nang. Neu sau nay muon sap xep that thi
  // phai them @PageableDefault hoac doc `sort` tu request — va do la thay doi
  // SAN PHAM, khong phai sua loi.
  console.log("  GHI NHAN: `sort` la tham so URL bi bo qua. Frontend khong gui no.");
  console.log("            Sap xep that su la MOT TINH NANG CHUA CO, khong phai loi.");

  // ---------- TIM KIEM ----------
  console.log("");
  console.log("--- tim kiem ---");
  const s1 = await get("/api/vocabulary/search?q=hello");
  check("vocab/search?q=hello -> 200", s1.status === 200, `nhan ${s1.status}`);

  const s2 = await get("/api/vocabulary/search?q=");
  check("q rong -> khong 500", s2.status !== 500, `nhan ${s2.status}`);

  const s3 = await get("/api/vocabulary/search?q=zzzzkhongtontai" + Date.now());
  check("q khong ket qua -> khong 500", s3.status !== 500, `nhan ${s3.status}`);
  const s3c = contentOf(s3.data);
  check("q khong ket qua -> mang rong", Array.isArray(s3c) && s3c.length === 0,
        `nhan ${JSON.stringify(s3.data).slice(0, 120)}`);

  const s4 = await get("/api/vocabulary/search?q=" + encodeURIComponent("%_'\"--"));
  check("q co ky tu dac biet -> khong 500", s4.status !== 500, `nhan ${s4.status}`);

  const s5 = await get("/api/vocabulary/search?q=" + encodeURIComponent("a".repeat(500)));
  check("q rat dai (500 ky tu) -> khong 500", s5.status !== 500, `nhan ${s5.status}`);

  // SQL injection co ban — phai khong lam lo gi va khong 500
  const s6 = await get("/api/vocabulary/search?q=" + encodeURIComponent("' OR 1=1--"));
  check("q kieu SQL injection -> khong 500", s6.status !== 500, `nhan ${s6.status}`);

  // ---------- ADMIN SEARCH ----------
  console.log("");
  console.log("--- tim kiem admin ---");
  const a1 = await get("/api/admin/exercises?q=the&page=0&size=5", admin);
  check("admin/exercises?q=the -> 200", a1.status === 200, `nhan ${a1.status}`);

  const a2 = await get("/api/admin/exercises?q=&page=0&size=5", admin);
  check("admin q rong -> 200", a2.status === 200, `nhan ${a2.status}`);

  const a3 = await get("/api/admin/exercises?q=" + encodeURIComponent("%"), admin);
  check("admin q='%' (wildcard) -> khong 500", a3.status !== 500, `nhan ${a3.status}`);

  const a4 = await get("/api/admin/exercises?q=" + encodeURIComponent("_"), admin);
  check("admin q='_' (wildcard 1 ky tu) -> khong 500", a4.status !== 500, `nhan ${a4.status}`);

  const a5 = await get("/api/admin/lessons?q=grammar&page=0&size=5", admin);
  check("admin/lessons?q=grammar -> 200", a5.status === 200, `nhan ${a5.status}`);

  const a6 = await get("/api/admin/users?q=admin&page=0&size=5", admin);
  check("admin/users?q=admin -> 200", a6.status === 200, `nhan ${a6.status}`);

  console.log("");
  console.log(`  PASS=${R.pass}  FAIL=${R.fail}`);
  if (R.fail) { console.log(""); console.log("FAILURES:"); R.fails.forEach((f) => console.log("  - " + f)); }
  process.exit(R.fail ? 1 : 0);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
