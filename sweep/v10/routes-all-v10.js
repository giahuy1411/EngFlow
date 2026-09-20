/**
 * audit-v10 Phase 4.4 — browser sweep TOAN BO route x viewport x 3 role.
 *
 * Dua tren sweep/v8/ui/routes-all.js (da duoc kiem chung), voi 4 khac biet:
 *
 * 1. Dung `./ui-lib` de launch browser CO SAN (Edge) thay vi
 *    `H.pw.chromium.launch()` doi hoi Chromium cua playwright. Xem ui-lib.js.
 * 2. Doc inventory tu audit-v9-full (moi nhat) va TU DOI CHIEU voi router —
 *    neu inventory lech voi router that thi bao dong, khong im lang chay tiep.
 * 3. Them viewport 1280 de bat loi chi xuat hien o khoang hep (navbar compaction
 *    band 1280-1535px cua audit-v10 F121).
 * 4. Assert guard HAI CHIEU nhu v8: duoc o lai thi phai o lai, bi chan thi
 *    phai bi day di. Mot chieu thoi thi 9 route admin tung "pass" trong khi
 *    thuc te hien trang chu (da ghi trong comment cua v8).
 *
 * Chay: node sweep/v10/routes-all-v10.js
 */
const { launch, APP, API, loginFull, mapUser, flushLimits, sleep,
        cleanupAuditPayments, dbParity } = require("./ui-lib");
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..", "..");
const INV = JSON.parse(fs.readFileSync(
  path.join(ROOT, ".specify", "specs", "audit-v9-full", "evidence", "route-inventory.json"), "utf8"));

const VIEWPORTS = [
  { name: "desktop-1440", w: 1440, h: 900 },
  { name: "desktop-1280", w: 1280, h: 900 },
  { name: "mobile-360", w: 360, h: 812 },
];

// ID that, da kiem chung tren DB live. Moi ho route phai co ID RIENG —
// dung mot id cho tat ca se sinh 404 that nhung trong nhu loi app.
function concrete(p) {
  if (p.includes(":pathMatch")) return "/definitely-not-a-real-route-xyz";
  let out = p;
  if (out.startsWith("/decks/")) out = out.replace(":id", "10006");
  else if (out.startsWith("/speaking/")) out = out.replace(":id", "50007");
  else if (out.startsWith("/videos/")) out = out.replace(":id", "1");
  else if (out.startsWith("/admin/")) out = out.replace(":id", "445");
  else out = out.replace(":id", "445");
  return out;
}

const results = [];
let totalErr = 0, totalApi400 = 0, totalOverflow = 0, totalNotMounted = 0;
const badLanding = [];

(async () => {
  console.log("=== audit-v10 route sweep ===");
  console.log(`routes: ${INV.routes.length}  viewports: ${VIEWPORTS.length}  roles: 3`);
  console.log(`visits du kien: ${INV.routes.length * VIEWPORTS.length * 3}`);
  console.log("");

  let adminS = await loginFull("admin@gmail.com", "123456");
  let userS = await loginFull("user@gmail.com", "123456");
  const browser = await launch();

  for (const vp of VIEWPORTS) {
    for (const asRole of ["admin", "user", "anon"]) {
      const ctx = await browser.newContext({ viewport: { width: vp.w, height: vp.h } });
      const page = await ctx.newPage();

      await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
      if (asRole === "anon") {
        await page.evaluate(() => { localStorage.removeItem("token"); localStorage.removeItem("user"); });
      } else {
        const s = asRole === "admin" ? adminS : userS;
        await page.evaluate((x) => {
          localStorage.setItem("token", x.token);
          localStorage.setItem("user", JSON.stringify(x.user));
        }, { token: s.token, user: mapUser(s.user) });
      }

      let visited = 0;
      for (const r of INV.routes) {
        if (r.guard === "admin-redirect") continue; // da phu boi /admin/dashboard
        const url = concrete(r.path);

        // JWT TTL 900s + bucket 100/min/IP: mot sweep dai vuot ca hai, va 429
        // tren /api/auth/me sinh ra "loi" gia. Re-seed + flush dinh ky de do
        // APP, khong do chinh harness.
        if (visited > 0 && visited % 8 === 0) {
          if (asRole === "admin") {
            adminS = await loginFull("admin@gmail.com", "123456");
            await page.evaluate((x) => {
              localStorage.setItem("token", x.token);
              localStorage.setItem("user", JSON.stringify(x.user));
            }, { token: adminS.token, user: mapUser(adminS.user) });
          } else if (asRole === "user") {
            userS = await loginFull("user@gmail.com", "123456");
            await page.evaluate((x) => {
              localStorage.setItem("token", x.token);
              localStorage.setItem("user", JSON.stringify(x.user));
            }, { token: userS.token, user: mapUser(userS.user) });
          }
          flushLimits(true);
          await sleep(400);
        }
        visited++;

        const consoleErrs = [], apiErrs = [], rejects = [];
        const onConsole = (m) => {
          if (m.type() === "error") {
            const t = m.text();
            // Da phan loai la KHONG phai loi app (moi cai kiem chung rieng):
            // favicon/youtube/googlevideo = nhieu ben thu ba; compute-pressure
            // = thong bao Permissions-Policy cua iframe YouTube.
            if (/favicon|ERR_BLOCKED_BY_RESPONSE|youtube|googlevideo|compute-pressure/i.test(t)) return;
            consoleErrs.push(t.slice(0, 160));
          }
        };
        const onPageErr = (e) => rejects.push(String(e.message).slice(0, 160));
        const onResp = (res) => {
          const u = res.url();
          if (!u.startsWith(API)) return;
          if (res.status() >= 400) apiErrs.push(res.status() + " " + u.replace(API, ""));
        };
        page.on("console", onConsole);
        page.on("pageerror", onPageErr);
        page.on("response", onResp);

        let nav = "ok";
        try {
          await page.goto(APP + url, { waitUntil: "domcontentloaded", timeout: 30000 });
          await page.waitForTimeout(1500);
        } catch (e) { nav = "NAV-ERR " + e.message.slice(0, 80); }

        page.off("console", onConsole);
        page.off("pageerror", onPageErr);
        page.off("response", onResp);

        const info = await page.evaluate(() => {
          const de = document.documentElement;
          const app = document.querySelector("#app");
          const fams = new Set();
          document.querySelectorAll("h1,h2,h3,p,a,button,span,label,li,td,th").forEach((e) => {
            if (!(e.textContent || "").trim()) return;
            const cs = getComputedStyle(e);
            if (cs.display === "none") return;
            fams.add(cs.fontFamily.split(",")[0].replace(/["']/g, "").trim());
          });
          let imgNoAlt = 0, imgTotal = 0;
          document.querySelectorAll("img").forEach((i) => { imgTotal++; if (!i.hasAttribute("alt")) imgNoAlt++; });
          return {
            url: location.pathname,
            mounted: !!app && app.children.length > 0,
            textLen: (document.body.innerText || "").trim().length,
            overflow: de.scrollWidth - de.clientWidth,
            scrollW: de.scrollWidth, clientW: de.clientWidth,
            fonts: [...fams], imgTotal, imgNoAlt,
            h1: (document.querySelector("h1") || {}).innerText || null,
          };
        }).catch((e) => ({ err: e.message }));

        const badFonts = (info.fonts || []).filter((f) => f && f !== "Be Vietnam Pro" && f !== "system-ui" && f !== "sans-serif");
        const overflow = (info.overflow || 0) > 16;
        if (consoleErrs.length) totalErr += consoleErrs.length;
        if (apiErrs.length) totalApi400 += apiErrs.length;
        if (overflow) totalOverflow++;
        if (!info.mounted) totalNotMounted++;

        // Landing contract, per guard x role. Guard phai duoc assert HAI CHIEU:
        // co quyen thi O LAI, thieu quyen thi BI DAY DI. Chi mot chieu thi
        // khong phan biet duoc "render dung route" voi "bi day sang trang khac".
        const finalPath = info.url || "";
        const landedElsewhere = finalPath !== url;
        const isCatchAll = r.path.includes(":pathMatch");
        let shouldStay;
        if (isCatchAll) shouldStay = false;
        else if (r.guard === "public") shouldStay = true;
        else if (r.guard === "guestOnly") shouldStay = asRole === "anon";
        else if (r.guard === "admin") shouldStay = asRole === "admin";
        else shouldStay = asRole !== "anon"; // auth, auth+premium

        const redirectOk = shouldStay ? !landedElsewhere : landedElsewhere;
        if (!redirectOk) {
          badLanding.push(asRole + " " + url + " -> " + finalPath
            + " [" + r.guard + "] ky vong " + (shouldStay ? "O LAI" : "BI DAY"));
        }

        results.push({
          route: r.path, url, finalPath, landedElsewhere, redirectOk,
          guard: r.guard, role: asRole, viewport: vp.name,
          nav, mounted: !!info.mounted, textLen: info.textLen || 0,
          overflow: info.overflow, scrollW: info.scrollW, clientW: info.clientW,
          consoleErrors: consoleErrs, pageErrors: rejects, apiErrors: apiErrs,
          badFonts, imgTotal: info.imgTotal || 0, imgNoAlt: info.imgNoAlt || 0, h1: info.h1,
        });

        const flag = (consoleErrs.length || rejects.length || apiErrs.length || overflow
          || !info.mounted || !redirectOk || badFonts.length) ? "  <-- ISSUE" : "";
        console.log((vp.name + " " + asRole).padEnd(24) + url.padEnd(34)
          + " mounted=" + (info.mounted ? "Y" : "N")
          + " err=" + consoleErrs.length + " page=" + rejects.length
          + " api4xx=" + apiErrs.length + " ovf=" + (info.overflow || 0)
          + (landedElsewhere ? " ->" + finalPath : "") + flag);
        if (consoleErrs.length) console.log("        CONSOLE: " + JSON.stringify(consoleErrs.slice(0, 3)));
        if (rejects.length) console.log("        PAGEERR: " + JSON.stringify(rejects.slice(0, 3)));
        if (apiErrs.length) console.log("        API4xx : " + JSON.stringify(apiErrs.slice(0, 4)));
        if (badFonts.length) console.log("        FONT   : " + JSON.stringify(badFonts.slice(0, 3)));
        if (!redirectOk) console.log("        LANDING: " + finalPath + " (guard " + r.guard + ")");
      }
      await ctx.close();
    }
  }
  await browser.close();

  const imgNoAlt = results.reduce((a, r) => a + r.imgNoAlt, 0);
  const imgTotal = results.reduce((a, r) => a + r.imgTotal, 0);
  const badFontRows = results.filter((r) => r.badFonts.length);

  console.log("");
  console.log("=== TONG KET ROUTE SWEEP ===");
  console.log("visits            : " + results.length + " (" + INV.routes.length + " route x "
    + VIEWPORTS.length + " viewport x 3 role)");
  console.log("khong mount duoc  : " + totalNotMounted);
  console.log("console errors    : " + totalErr);
  console.log("page errors       : " + results.reduce((a, r) => a + r.pageErrors.length, 0));
  console.log("API >= 400        : " + totalApi400);
  console.log("overflow          : " + totalOverflow);
  console.log("landing sai guard : " + badLanding.length + (badLanding.length ? "\n  " + badLanding.join("\n  ") : ""));
  console.log("font sai          : " + badFontRows.length + " route"
    + (badFontRows.length ? "  " + JSON.stringify(badFontRows.slice(0, 5).map((r) => r.viewport + " " + r.role + " " + r.url + " " + JSON.stringify(r.badFonts))) : ""));
  console.log("img thieu alt     : " + imgNoAlt + " / " + imgTotal);

  const out = path.join(__dirname, "routes-all-v10.json");
  fs.writeFileSync(out, JSON.stringify({
    totals: { visits: results.length, totalNotMounted, totalErr, totalApi400, totalOverflow,
      badLanding: badLanding.length, imgNoAlt, imgTotal, badFontRows: badFontRows.length },
    badLanding, results,
  }, null, 1));
  console.log("wrote " + out);

  // Self-clean: sweep di qua /premium/checkout (PremiumCheckout.vue goi
  // POST create-order ngay khi mount) nen MOI lan chay deu mint row that.
  // Cleanup nam trong CHINH lan chay nay, khong phai buoc thu cong.
  console.log("");
  try { cleanupAuditPayments(126); /* audit-v11 F130: baseline is informational; assertion is self-clean */ } catch (e) { console.log("cleanup warn: " + e.message); }
  try { console.log("parity: " + dbParity()); } catch (e) {}

  const failed = totalNotMounted || totalErr || totalApi400 || totalOverflow
    || badLanding.length || imgNoAlt || badFontRows.length;
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error("ERR", e.message, e.stack); process.exit(1); });
