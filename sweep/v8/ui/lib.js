const pw = require("playwright-core");
const fs = require("fs");
const APP = "http://localhost:5173";
const API = "http://localhost:8080";

async function login(email, pass) {
  const r = await fetch(API + "/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: email, password: pass })
  });
  const j = await r.json();
  return (j.data && j.data.token) || j.token;
}

const results = [];

function mkContext(browser, token, viewport) {
  return browser.newContext({ viewport: viewport || { width: 1440, height: 900 } });
}

async function seedToken(page, token) {
  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((t) => localStorage.setItem("token", t), token);
}

// Collect console errors + failed requests while visiting one route.
async function visit(page, route, label, opts) {
  opts = opts || {};
  const errors = [], warns = [], failed = [];
  const onConsole = m => {
    if (m.type() === "error") errors.push(m.text().slice(0, 200));
    else if (m.type() === "warning") warns.push(m.text().slice(0, 160));
  };
  const onReq = r => { if (!r.url().startsWith(API)) return; };
  const onResp = r => {
    const u = r.url();
    if (!u.startsWith(API)) return;
    if (r.status() >= 500) failed.push(r.status() + " " + u.replace(API, "") + " [" + label + "]");
    else if (r.status() === 404 && !opts.expect404) failed.push("404 " + u.replace(API, ""));
  };
  page.on("console", onConsole);
  page.on("request", onReq);
  page.on("response", onResp);
  let nav = "ok";
  try {
    await page.goto(APP + route, { waitUntil: "domcontentloaded", timeout: 30000 });
    await page.waitForTimeout(opts.wait || 2200);
  } catch (e) { nav = "NAV-ERR " + e.message.slice(0, 90); }
  page.off("console", onConsole);
  page.off("response", onResp);
  const info = await page.evaluate(() => {
    const b = document.body;
    const cs = getComputedStyle(b);
    const heads = [...document.querySelectorAll("h1,h2,h3,.font-extrabold,button")].slice(0, 60);
    const fams = new Set([cs.fontFamily.split(",")[0].replace(/"/g, "")]);
    heads.forEach(h => fams.add(getComputedStyle(h).fontFamily.split(",")[0].replace(/"/g, "")));
    return {
      title: document.title,
      textLen: (b.innerText || "").trim().length,
      h1: (document.querySelector("h1") || {}).innerText || null,
      fonts: [...fams],
      scrollW: document.documentElement.scrollWidth,
      clientW: document.documentElement.clientWidth,
      bg: cs.backgroundColor,
      hasApp: !!document.querySelector("#app") && document.querySelector("#app").children.length > 0
    };
  }).catch(e => ({ err: e.message }));
  const rec = { route: route, label: label, nav: nav, errors: errors, failed: failed, info: info, warns: warns.length };
  results.push(rec);
  return rec;
}

function summarize(title) {
  const bad = results.filter(r => r.nav !== "ok" || r.errors.length || r.failed.length || !r.info.hasApp);
  console.log("=== UI " + title + " routes=" + results.length + " PROBLEM=" + bad.length);
  for (const r of bad) {
    console.log("  ! " + r.route + " [" + r.label + "] nav=" + r.nav
      + " err=" + JSON.stringify(r.errors.slice(0, 3))
      + " failed=" + JSON.stringify(r.failed.slice(0, 3))
      + " mounted=" + (r.info.hasApp === undefined ? "?" : r.info.hasApp)
      + " text=" + r.info.textLen);
  }
  const fontBad = results.filter(r => r.info.fonts && r.info.fonts.some(f => !/Be Vietnam Pro/.test(f)));
  console.log("  non-BVP fonts on: " + (fontBad.length ? fontBad.map(r => r.route + "=" + JSON.stringify(r.info.fonts)).join(" ; ") : "none"));
  const overflow = results.filter(r => r.info.scrollW > r.info.clientW + 2);
  console.log("  horizontal overflow: " + (overflow.length ? overflow.map(r => r.route + "(" + r.info.scrollW + ">" + r.info.clientW + ")").join(" ; ") : "none"));
}

module.exports = { pw, APP, API, login, results, visit, summarize, seedToken, mkContext };
