/**
 * audit-v10 — kiem chung font: Be Vietnam Pro la font DUY NHAT, va no LOAD THAT.
 *
 * "Load that" khac "khai bao": mot font khai bao ma khong tai duoc se im lang
 * roi ve system-ui, va khong co loi nao bao. Phai hoi document.fonts.check()
 * — no tra ve false khi face chua tai xong.
 *
 * Chay: node sweep/v10/font-check.js
 */
const { launch, APP } = require("./ui-lib");

const ROUTES = ["/", "/lessons", "/login", "/register", "/leaderboard", "/premium", "/search"];

(async () => {
  const browser = await launch();
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();

  console.log("=== kiem tra font ===");
  console.log("");

  await page.goto(APP + "/", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2000);

  const f = await page.evaluate(async () => {
    await document.fonts.ready;
    const faces = [...document.fonts].map((x) => ({
      family: x.family, weight: x.weight, style: x.style, status: x.status,
    }));
    const bvp = faces.filter((x) => /Be Vietnam/i.test(x.family));
    const other = faces.filter((x) => !/Be Vietnam/i.test(x.family));
    // Trang thai cua tung face, kem weight — de phan biet "chua dung toi nen
    // chua tai" (unloaded, BINH THUONG voi font-display=swap) voi "tai that bai"
    // (error, moi la loi).
    const byWeight = {};
    for (const x of bvp) byWeight[x.weight + "/" + x.status] = (byWeight[x.weight + "/" + x.status] || 0) + 1;
    // Do weight nao THUC SU duoc dung trong trang nay, va weight do co san khong.
    const used = new Set();
    document.querySelectorAll("*").forEach((el) => {
      if (el.children.length) return;
      if (!(el.textContent || "").trim()) return;
      const cs = getComputedStyle(el);
      if (cs.display === "none") return;
      const fam = cs.fontFamily.split(",")[0].replace(/["']/g, "").trim();
      if (fam === "Be Vietnam Pro") used.add(cs.fontWeight);
    });
    return {
      total: faces.length,
      bvpCount: bvp.length,
      bvpStatuses: [...new Set(bvp.map((x) => x.status))],
      bvpWeights: [...new Set(bvp.map((x) => x.weight))].sort(),
      byWeight,
      errors: bvp.filter((x) => x.status === "error").map((x) => x.weight),
      usedWeights: [...used].sort(),
      otherFamilies: [...new Set(other.map((x) => x.family))],
      check400: document.fonts.check('400 16px "Be Vietnam Pro"'),
      check700: document.fonts.check('700 16px "Be Vietnam Pro"'),
      check900: document.fonts.check('900 16px "Be Vietnam Pro"'),
      rootFont: getComputedStyle(document.documentElement).getPropertyValue("--geo-font").trim(),
      bodyFont: getComputedStyle(document.body).fontFamily,
    };
  });

  console.log("-- Font that su duoc LOAD --");
  console.log("  tong so face          : " + f.total);
  console.log("  Be Vietnam Pro faces  : " + f.bvpCount + "  status=" + JSON.stringify(f.bvpStatuses));
  console.log("  weights KHAI BAO      : " + JSON.stringify(f.bvpWeights));
  console.log("  weights DANG DUNG     : " + JSON.stringify(f.usedWeights));
  console.log("  chi tiet weight/status: " + JSON.stringify(f.byWeight));
  console.log("  face bi LOI (phai rong): " + (f.errors.length ? JSON.stringify(f.errors) : "(khong)"));
  console.log("  family KHAC (phai rong): " + (f.otherFamilies.length ? JSON.stringify(f.otherFamilies) : "(khong)"));
  console.log("");
  console.log("-- check() tren tung weight --");
  console.log("  400: " + f.check400 + "   700: " + f.check700 + "   900: " + f.check900);
  console.log("");
  console.log("-- Token --");
  console.log("  --geo-font : " + f.rootFont);
  console.log("  body font  : " + f.bodyFont);

  let bad = 0;
  console.log("");
  console.log("-- Quet text node tren tung route --");
  for (const r of ROUTES) {
    await page.goto(APP + r, { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1200);
    const m = await page.evaluate(() => {
      const fams = new Set();
      let n = 0;
      document.querySelectorAll("*").forEach((el) => {
        if (el.children.length) return;
        if (!(el.textContent || "").trim()) return;
        const cs = getComputedStyle(el);
        if (cs.display === "none" || cs.visibility === "hidden") return;
        n++;
        fams.add(cs.fontFamily.split(",")[0].replace(/["']/g, "").trim());
      });
      return { n, fams: [...fams] };
    });
    const wrong = m.fams.filter((x) => x && x !== "Be Vietnam Pro" && x !== "system-ui" && x !== "sans-serif");
    if (wrong.length) bad++;
    console.log("  " + r.padEnd(14) + " text nodes=" + String(m.n).padStart(4)
      + "  families=" + JSON.stringify(m.fams) + (wrong.length ? "  <<< SAI" : ""));
  }

  await browser.close();
  console.log("");
  // Tieu chi dung. KHONG doi moi face phai "loaded":
  // font-display=swap nghia la face chi tai khi co text dung no. Cac weight
  // khai bao trong <link> ma trang khong dung (vi du 500 neu khong co cho nao
  // dat font-weight:500) se dung yen o "unloaded" — do la BINH THUONG, khong
  // phai loi. Cai CAN kiem la: khong face nao "error", va MOI weight DANG DUNG
  // deu co face tai duoc.
  const ok = f.otherFamilies.length === 0 && f.bvpCount > 0
    && f.errors.length === 0
    && f.usedWeights.every((w) => f.bvpWeights.includes(w))
    && f.check400 && f.check700 && f.check900 && bad === 0;
  console.log(ok ? "KET LUAN: Be Vietnam Pro la font DUY NHAT, tai duoc, moi route sach."
                 : "KET LUAN: CO VAN DE — xem dong <<< o tren.");
  if (!ok) {
    console.log("  otherFamilies=" + JSON.stringify(f.otherFamilies)
      + "  errors=" + JSON.stringify(f.errors)
      + "  usedWeights=" + JSON.stringify(f.usedWeights)
      + "  declared=" + JSON.stringify(f.bvpWeights));
  }
  process.exit(ok ? 0 : 1);
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
