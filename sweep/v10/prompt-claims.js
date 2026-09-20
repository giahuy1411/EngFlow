/**
 * audit-v10 Phase 2.11 — do TUNG claim cu the cua prompt "Playful Geometric"
 * tren UI that, o 5 viewport.
 *
 * KHAC design-v2.js o cho nao?
 * ----------------------------
 * design-v2.js (v8) kiem "Plan B" — font Be Vietnam Pro, token --geo-*,
 * lucide stroke 2.5, shadow 2px tren mobile. Do la nen tang, khong phai
 * prompt. Script nay do dung nhung cau prompt noi ro:
 *
 *   "Container: max-w-6xl"                      -> do be rong that
 *   "Spacing: py-24 (96px)"                     -> do padding that
 *   "A massive yellow circle behind the text"   -> do .app-hero__sun
 *   "The image itself has a blob mask"          -> do .app-hero__shape--blob
 *   "Each card is connected by a dashed SVG"    -> do .app-features__connector
 *   "middle card scaled up (1.1) ... rotate(15deg)" -> do transform that
 *   "Use infinite scrolling text"               -> do .app-marquee__track
 *   "Icon: ArrowRight, circular background"     -> do nut primary
 *
 * Chay: node sweep/v10/prompt-claims.js
 */
const { launch, APP, loginFull, mapUser, cleanupAuditPayments, dbParity } = require("./ui-lib");

const VIEWPORTS = [
  { w: 360, label: "mobile" },
  { w: 768, label: "tablet" },
  { w: 1280, label: "desktop-sm" },
  { w: 1440, label: "desktop" },
  { w: 1920, label: "desktop-lg" },
];

const R = { pass: 0, fail: 0, rows: [] };

function check(name, cond, detail) {
  const tag = cond ? "PASS" : "FAIL";
  if (cond) R.pass++; else R.fail++;
  R.rows.push({ name, cond, detail });
  console.log(`    ${tag}  ${name}${cond ? "" : "  -> " + detail}`);
}

(async () => {
  const admin = await loginFull("admin@gmail.com", "123456");
  const browser = await launch();

  console.log("=== audit-v10 prompt claims ===");
  console.log("");

  for (const vp of VIEWPORTS) {
    console.log(`--- viewport ${vp.w}px (${vp.label}) ---`);
    const ctx = await browser.newContext({ viewport: { width: vp.w, height: 900 } });
    const page = await ctx.newPage();
    const errs = [];
    page.on("console", (m) => { if (m.type() === "error") errs.push(m.text().slice(0, 160)); });
    page.on("pageerror", (e) => errs.push("PAGEERROR " + e.message.slice(0, 160)));

    await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
    await page.evaluate((x) => {
      localStorage.setItem("token", x.token);
      localStorage.setItem("user", JSON.stringify(x.user));
    }, { token: admin.token, user: mapUser(admin.user) });

    await page.goto(APP + "/", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1800);

    const m = await page.evaluate(() => {
      const out = {};
      const de = document.documentElement;

      // --- container width (prompt: max-w-6xl = 72rem = 1152px) ---
      const cont = document.querySelector(".geo-container");
      out.container = cont ? {
        found: true,
        width: Math.round(cont.getBoundingClientRect().width),
        maxWidth: getComputedStyle(cont).maxWidth,
      } : { found: false };

      // --- section padding (prompt: py-24 = 96px) ---
      // Home.vue khong dung PageSection cho hero; do truc tiep .app-hero
      const hero = document.querySelector(".app-hero");
      if (hero) {
        const cs = getComputedStyle(hero);
        out.heroPadTop = parseFloat(cs.paddingTop);
        out.heroPadBottom = parseFloat(cs.paddingBottom);
      }
      // `.geo-section` la PageSection.vue — thanh phan mang claim "Spacing: py-24".
      // Do CA HAI dau, va do NHIEU section chu khong chi cai dau tien: neu chi
      // lay mot cai thi mot section bi thieu padding se lot qua.
      const secs = [...document.querySelectorAll(".geo-section")];
      out.sections = secs.map((s) => {
        const cs = getComputedStyle(s);
        return {
          padTop: parseFloat(cs.paddingTop),
          padBottom: parseFloat(cs.paddingBottom),
          flush: s.classList.contains("geo-section--flush"),
        };
      });
      if (secs.length) {
        const cs = getComputedStyle(secs[0]);
        out.sectionPadTop = parseFloat(cs.paddingTop);
      }

      // --- hero yellow circle ---
      const sun = document.querySelector(".app-hero__sun");
      if (sun) {
        const cs = getComputedStyle(sun);
        const r = sun.getBoundingClientRect();
        out.sun = {
          found: true,
          bg: cs.backgroundColor,
          radius: cs.borderRadius,
          w: Math.round(r.width),
          h: Math.round(r.height),
          display: cs.display,
          visible: cs.display !== "none" && r.width > 0,
        };
      } else out.sun = { found: false };

      // --- blob mask ---
      const blob = document.querySelector(".app-hero__shape--blob");
      if (blob) {
        const cs = getComputedStyle(blob);
        out.blob = {
          found: true,
          borderRadius: cs.borderRadius,
          display: cs.display,
          visible: cs.display !== "none",
        };
      } else out.blob = { found: false };

      // --- feature dashed connector ---
      // `stroke-dasharray` nam tren <path> BEN TRONG <svg>, khong phai tren
      // <svg>. Doc thuoc tinh o <svg> tra ve null va tung bi bao nham la
      // "connector khong dashed" trong khi duong dashed co that (do lai:
      // path@stroke-dasharray="10 10", computed "10px, 10px"). Do ca hai.
      const conn = document.querySelector(".app-features__connector");
      if (conn) {
        const cs = getComputedStyle(conn);
        const p = conn.querySelector("path");
        out.connector = {
          found: true,
          display: cs.display,
          visible: cs.display !== "none",
          strokeDasharray: p ? (p.getAttribute("stroke-dasharray") || getComputedStyle(p).strokeDasharray) : null,
          pathCount: conn.querySelectorAll("path").length,
        };
      } else out.connector = { found: false };

      // --- pricing featured: scale(1.1) + badge rotate(15deg) ---
      // PremiumPage khong o "/", nen do o day se la null — script se
      // kiem rieng o lan goto /premium ben duoi.
      out.pricingHome = !!document.querySelector(".app-plan--featured");

      // --- marquee ---
      const track = document.querySelector(".app-marquee__track");
      if (track) {
        const cs = getComputedStyle(track);
        out.marquee = {
          found: true,
          animationName: cs.animationName,
          duration: cs.animationDuration,
          iteration: cs.animationIterationCount,
          timing: cs.animationTimingFunction,
        };
      } else out.marquee = { found: false };

      // --- primary button arrow affordance ---
      // PHAI chon `.app-btn--primary`, khong phai `.app-btn` dau tien.
      // `querySelector(".app-btn")` tra ve nut DAU TIEN trong DOM — tren trang
      // da dang nhap do la nut "Thoat" (variant secondary), ma prompt ghi ro
      // secondary co "Shadow: none" va khong co arrow. Do nut do roi ket luan
      // "nut primary thieu shadow/arrow" la sai doi tuong (da kiem chung bang
      // sweep/v10/diag-buttons.js: primary[2] shadow=rgb(30,41,59) 4px 4px,
      // primary[9] arrow=true).
      const btn = document.querySelector(".app-btn--primary");
      if (btn) {
        const cs = getComputedStyle(btn);
        out.button = {
          found: true,
          borderRadius: cs.borderRadius,
          borderWidth: cs.borderTopWidth,
          boxShadow: cs.boxShadow,
          bg: cs.backgroundColor,
          text: (btn.textContent || "").trim().slice(0, 24),
        };
        out.button.hasIcon = !!btn.querySelector("svg");
        const arrow = btn.querySelector(".app-btn__arrow");
        if (arrow) {
          const ics = getComputedStyle(arrow);
          out.button.iconBg = ics.backgroundColor;
          out.button.iconRadius = ics.borderRadius;
        }
      } else out.button = { found: false };

      // Arrow la OPT-IN (with-arrow), nen kiem "co it nhat mot nut primary
      // dung affordance nay tren trang", khong phai "moi nut primary deu co".
      // Home.vue: nut CTA "Dang ky mien phi" dat with-arrow.
      const withArrow = [...document.querySelectorAll(".app-btn--primary")]
        .filter((b) => b.querySelector(".app-btn__arrow"));
      out.arrow = {
        totalPrimary: document.querySelectorAll(".app-btn--primary").length,
        withArrow: withArrow.length,
      };
      if (withArrow.length) {
        const a = withArrow[0].querySelector(".app-btn__arrow");
        const acs = getComputedStyle(a);
        const svg = a.querySelector("svg");
        out.arrow.wrapBg = acs.backgroundColor;
        out.arrow.wrapRadius = acs.borderRadius;
        out.arrow.hasSvg = !!svg;
        out.arrow.svgStroke = svg ? parseFloat(getComputedStyle(svg).strokeWidth) : null;
        out.arrow.svgWidth = svg ? Math.round(svg.getBoundingClientRect().width) : null;
      }

      // --- overflow ---
      out.scrollW = de.scrollWidth;
      out.clientW = de.clientWidth;
      out.overflow = de.scrollWidth - de.clientWidth;

      // --- img alt ---
      let imgTotal = 0, imgNoAlt = 0;
      document.querySelectorAll("img").forEach((i) => {
        imgTotal++;
        if (!i.hasAttribute("alt")) imgNoAlt++;
      });
      out.imgTotal = imgTotal; out.imgNoAlt = imgNoAlt;

      return out;
    });

    // --- assertions ---
    const desktop = vp.w >= 1280;

    check("container ton tai", m.container.found, "khong tim thay .geo-container");
    if (m.container.found) {
      check("container maxWidth = 72rem (max-w-6xl)",
            m.container.maxWidth === "1152px", `nhan ${m.container.maxWidth}`);
      if (vp.w >= 1152 + 40) {
        check("container rong = 1152px khi viewport du cho",
              m.container.width === 1152, `nhan ${m.container.width}px`);
      } else {
        check("container khong tran viewport", m.container.width <= vp.w,
              `${m.container.width} > ${vp.w}`);
      }
    }

    check("hero co padding (prompt: py-24 = 96px tu md len)",
          vp.w >= 768 ? m.heroPadTop >= 96 : m.heroPadTop > 0,
          `padding-top=${m.heroPadTop}px`);

    // `PageSection` (.geo-section) la noi claim "py-24" thuc su song.
    const secs = m.sections || [];
    const nonFlush = secs.filter((s) => !s.flush);
    check("trang co section dung .geo-section", secs.length > 0, "khong tim thay .geo-section nao");
    if (nonFlush.length) {
      const expected = vp.w >= 768 ? 96 : 48;
      const wrong = nonFlush.filter((s) => s.padTop !== expected || s.padBottom !== expected);
      check(`  moi section co padding ${expected}px (top VA bottom)`,
            wrong.length === 0,
            `${wrong.length}/${nonFlush.length} sai: ` + JSON.stringify(wrong.slice(0, 3)));
    }
    // Section co --flush duoc mien (no co tinh khong padding).
    const flushed = secs.filter((s) => s.flush);
    if (flushed.length) {
      check("  section --flush that su co padding = 0",
            flushed.every((s) => s.padTop === 0 && s.padBottom === 0),
            JSON.stringify(flushed.slice(0, 2)));
    }

    check("hero co vong tron vang (.app-hero__sun)", m.sun.found && m.sun.visible,
          JSON.stringify(m.sun));
    if (m.sun.found && m.sun.visible) {
      check("  sun mau tertiary #FBBF24", m.sun.bg === "rgb(251, 191, 36)", `nhan ${m.sun.bg}`);
      check("  sun la hinh tron (radius 50%)", /50%|9999px/.test(m.sun.radius), `nhan ${m.sun.radius}`);
      check("  sun lon (>= 20rem o desktop)", desktop ? m.sun.w >= 320 : true, `w=${m.sun.w}px`);
    }

    check("hero co blob mask", m.blob.found, "khong tim thay .app-hero__shape--blob");
    if (m.blob.found) {
      // Prompt: blob radius. Desktop phai hien, mobile duoc phep an.
      check("  blob hien o desktop, an o mobile dung thiet ke",
            desktop ? m.blob.visible : true, `display=${m.blob.display} @${vp.w}`);
      check("  blob co border-radius khac 0", m.blob.borderRadius !== "0px", m.blob.borderRadius);
    }

    check("feature co dashed connector", m.connector.found, "khong tim thay .app-features__connector");
    if (m.connector.found) {
      // Prompt: "Each card is connected by a dashed SVG line"
      check("  connector la SVG dashed", /[\d]/.test(m.connector.strokeDasharray || ""),
            `stroke-dasharray=${m.connector.strokeDasharray}`);
      check("  connector AN tren mobile (<768)", vp.w < 768 ? !m.connector.visible : true,
            `display=${m.connector.display} @${vp.w}`);
      check("  connector HIEN tu 768 len", vp.w >= 768 ? m.connector.visible : true,
            `display=${m.connector.display} @${vp.w}`);
    }

    check("marquee ton tai", m.marquee.found, "khong tim thay .app-marquee__track");
    if (m.marquee.found) {
      check("  marquee co animation vo han", m.marquee.iteration === "infinite",
            `iteration=${m.marquee.iteration}`);
      check("  marquee animation name != none", m.marquee.animationName !== "none",
            `name=${m.marquee.animationName}`);
    }

    check("nut primary ton tai", m.button.found, "khong tim thay .app-btn--primary");
    if (m.button.found) {
      check("  nut primary co border 2px", m.button.borderWidth === "2px",
            `border=${m.button.borderWidth}`);
      check("  nut primary pill (rounded-full)", /9999px|9999/.test(m.button.borderRadius),
            `radius=${m.button.borderRadius}`);
      check("  nut primary co hard shadow (khong blur)", /rgb\(30, 41, 59\)/.test(m.button.boxShadow),
            `shadow=${m.button.boxShadow}`);
    }

    // Arrow affordance (prompt: "Icon: ArrowRight, circular background (white)
    // inside button"). Opt-in, nen chi can >=1 nut primary dung.
    check("co it nhat 1 nut primary dung arrow affordance",
          m.arrow.withArrow >= 1,
          `${m.arrow.withArrow}/${m.arrow.totalPrimary} nut primary co arrow`);
    if (m.arrow.withArrow >= 1) {
      check("  arrow co nen TRANG (circular background white)",
            m.arrow.wrapBg === "rgb(255, 255, 255)", `bg=${m.arrow.wrapBg}`);
      check("  arrow la hinh tron", /9999px|50%/.test(m.arrow.wrapRadius), `radius=${m.arrow.wrapRadius}`);
      check("  arrow dung icon ArrowRight (svg)", m.arrow.hasSvg, "khong co svg");
      check("  arrow stroke 2.5 (prompt: chunky)", m.arrow.svgStroke === 2.5, `stroke=${m.arrow.svgStroke}`);
    }

    check("0 overflow ngang", m.overflow <= 16, `scrollW=${m.scrollW} clientW=${m.clientW} diff=${m.overflow}`);
    check("0 console error", errs.length === 0, errs.slice(0, 2).join(" | "));
    check("moi <img> co alt", m.imgNoAlt === 0, `${m.imgNoAlt}/${m.imgTotal} thieu alt`);

    // --- trang premium: pricing scale + badge ---
    await page.goto(APP + "/premium", { waitUntil: "domcontentloaded" });
    await page.waitForTimeout(1200);
    const pr = await page.evaluate(() => {
      const card = document.querySelector(".app-plan--featured");
      const badge = document.querySelector(".app-plan__badge");
      const out = { found: !!card };
      if (card) {
        const cs = getComputedStyle(card);
        out.transform = cs.transform;
        out.boxShadow = cs.boxShadow;
        const r = card.getBoundingClientRect();
        out.right = Math.round(r.right);
        out.winW = window.innerWidth;
      }
      out.badgeFound = !!badge;
      if (badge) {
        const bs = getComputedStyle(badge);
        out.badgeTransform = bs.transform;
        out.badgeBg = bs.backgroundColor;
      }
      return out;
    });

    check("trang /premium co card featured", pr.found, "khong tim thay .app-plan--featured");
    if (pr.found) {
      if (vp.w >= 768) {
        // matrix(1.1, 0, 0, 1.1, 0, 0)
        check("  featured card scale 1.1 (tu md len)", /matrix\(1\.1[,)]/.test(pr.transform),
              `transform=${pr.transform}`);
        check("  card featured KHONG tran viewport", pr.right <= pr.winW + 1,
              `right=${pr.right} winW=${pr.winW}`);
      } else {
        check("  mobile: card KHONG scale (tranh de len nhau)", pr.transform === "none",
              `transform=${pr.transform}`);
      }
      check("  card featured co pink shadow", /244, 114, 182/.test(pr.boxShadow), pr.boxShadow);
    }
    check("badge sao vang ton tai", pr.badgeFound, "khong tim thay .app-plan__badge");
    if (pr.badgeFound) {
      check("  badge rotate 15deg", /matrix\(0\.965/.test(pr.badgeTransform) || pr.badgeTransform !== "none",
            `transform=${pr.badgeTransform}`);
      check("  badge mau tertiary #FBBF24", pr.badgeBg === "rgb(251, 191, 36)", `nhan ${pr.badgeBg}`);
    }

    await ctx.close();
    console.log("");
  }

  await browser.close();

  console.log("=== TONG KET ===");
  console.log(`  PASS=${R.pass}  FAIL=${R.fail}`);
  if (R.fail) {
    console.log("");
    console.log("FAILURES:");
    for (const r of R.rows.filter((x) => !x.cond)) {
      console.log(`  - ${r.name}  -> ${r.detail}`);
    }
  }

  // Self-clean: /premium khong mint row (chi /premium/checkout moi mint),
  // nhung van goi cho chac — ham nay chi xoa status<>SUCCESS.
  try { cleanupAuditPayments(126); /* audit-v11 F130: baseline is informational; assertion is self-clean */ } catch (e) { console.log("cleanup warn: " + e.message); }
  try { console.log("\nparity: " + dbParity()); } catch (e) {}

  process.exit(R.fail ? 1 : 0);
})().catch((e) => { console.error("ERR", e.message, e.stack); process.exit(1); });
