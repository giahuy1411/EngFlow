const H = require("../ui/lib.js");

// helper: click, then report which API calls the click produced
async function act(page, label, fn) {
  const calls = [];
  const errs = [];
  const onReq = r => { if (r.url().startsWith(H.API)) calls.push(r.method() + " " + r.url().replace(H.API, "")); };
  const onResp = r => { if (r.url().startsWith(H.API) && r.status() >= 400) calls.push("->" + r.status()); };
  const onCon = m => { if (m.type() === "error") errs.push(m.text().slice(0, 120)); };
  page.on("request", onReq); page.on("response", onResp); page.on("console", onCon);
  let note = "";
  try { note = await fn() || ""; } catch (e) { note = "THREW " + e.message.slice(0, 80); }
  page.off("request", onReq); page.off("response", onResp); page.off("console", onCon);
  console.log(("  " + label).padEnd(44) + (note ? note : "-") + (calls.length ? "  | " + calls.slice(0, 6).join(" ") : "")
    + (errs.length ? "  CONSOLE_ERR " + JSON.stringify(errs.slice(0, 2)) : ""));
  return note;
}

async function login(page) {
  await page.goto(H.APP + "/login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1200);
  await page.fill("input[type=email]", "user@gmail.com");
  await page.fill("input[type=password]", "123456");
  await page.click("button[type=submit]");
  await page.waitForTimeout(3500);
  return page.evaluate(() => location.pathname);
}

(async () => {
  const browser = await H.pw.chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, permissions: [] });
  const page = await ctx.newPage();
  console.log("login lands at", await login(page));

  // ---- SEARCH: type + submit, expect /api/lessons?q= ----
  await page.goto(H.APP + "/search", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1500);
  await act(page, "search: type travel + Enter", async () => {
    const inp = await page.$("input[type=search], input[type=text], input[placeholder]");
    if (!inp) return "NO INPUT";
    await inp.click(); await inp.fill("travel");
    const p = page.waitForResponse(r => r.url().includes("/api/"), { timeout: 8000 }).catch(() => null);
    await inp.press("Enter");
    const res = await p;
    return "api=" + (res ? res.url().replace(H.API, "").slice(0, 60) : "none");
  });
  await page.waitForTimeout(1200);
  const sres = await page.evaluate(() => document.body.innerText.length);
  console.log("     after search textLen=" + sres);

  // ---- LESSONS list: filter by level ----
  await page.goto(H.APP + "/lessons", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1800);
  await act(page, "lessons: click a level filter chip", async () => {
    const btns = await page.$$("button, a");
    for (const b of btns) {
      const t = ((await b.innerText()) || "").trim().toUpperCase();
      if (t === "SƠ CẤP" || t === "TRUNG CẤP" || t === "ELEMENTARY" || t === "INTERMEDIATE" || t === "SƠ CẤP (A)") {
        await b.click(); await page.waitForTimeout(1500);
        return "clicked " + t;
      }
    }
    return "no chip found";
  });

  // ---- LESSON detail: answer an exercise, then grade ----
  await page.goto(H.APP + "/lessons/445", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2500);
  await act(page, "lesson: click option + kiem tra", async () => {
    const opts = await page.$$("button, label");
    let clicked = 0;
    for (const o of opts) {
      const t = ((await o.innerText()) || "").trim();
      if (t.length > 0 && t.length < 60 && !/kiểm tra|nộp|lưu|trả lời đúng|tiếp/i.test(t)) {
        try { await o.click({ timeout: 1500 }); clicked++; if (clicked >= 1) break; } catch (e) {}
      }
    }
    const btns = await page.$$("button");
    for (const b of btns) {
      const t = ((await b.innerText()) || "").toLowerCase();
      if (t.includes("kiểm tra") || t.includes("nộp") || t.includes("chấm")) {
        const p = page.waitForResponse(r => r.url().includes("/exercises/"), { timeout: 9000 }).catch(() => null);
        await b.click();
        const res = await p;
        return "opts=" + clicked + " graded=" + (res ? res.status() + " " + res.url().replace(H.API, "").slice(0, 50) : "no-resp");
      }
    }
    return "opts=" + clicked + " no grade button";
  });

  // ---- DECK play: quiz game start -> submit through UI ----
  await page.goto(H.APP + "/decks/10006/play/quiz", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2500);
  await act(page, "quiz: answer 1 question", async () => {
    const cards = await page.$$("button");
    for (const c of cards) {
      const t = ((await c.innerText()) || "").trim();
      if (t && t.length < 50) { await c.click(); await page.waitForTimeout(900); return "clicked " + t.slice(0, 22); }
    }
    return "no option";
  });

  // ---- FLASHCARD: flip + rate ----
  await page.goto(H.APP + "/decks/10006/play/flashcard", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2200);
  await act(page, "flashcard: rate known/unknown", async () => {
    const bs = await page.$$("button");
    for (const b of bs) {
      const t = ((await b.innerText()) || "").toLowerCase();
      if (t.includes("thuộc") || t.includes("chưa") || t.includes("đúng") || t.includes("sai")) {
        const p = page.waitForResponse(r => r.url().includes("/flashcards/"), { timeout: 6000 }).catch(() => null);
        await b.click();
        const res = await p;
        return "btn=" + t.slice(0, 16) + " api=" + (res ? res.status() : "none");
      }
    }
    return "no rate button";
  });

  // ---- SPEAKING history (premium-bypass admin not needed; user has history) ----
  await page.goto(H.APP + "/speaking/history", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2000);
  await act(page, "speaking history: open first item", async () => {
    const links = await page.$$("a, article button, [role=button]");
    for (const l of links) {
      const t = ((await l.innerText()) || "").trim();
      if (t.length > 25 && t.length < 300) { await l.click(); await page.waitForTimeout(1600); return "opened: " + t.slice(0, 30); }
    }
    return "no item";
  });

  // ---- PROFILE: streak + progress render ----
  await page.goto(H.APP + "/profile", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(2200);
  const prof = await page.evaluate(() => {
    const t = document.body.innerText;
    return { streak: /chuỗi|streak/i.test(t), len: t.length, hasNumber: /\d/.test(t) };
  });
  console.log("  profile render:", JSON.stringify(prof));

  // ---- PREMIUM page (SePay order is NOT clicked: avoids real order creation) ----
  await page.goto(H.APP + "/premium", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1800);
  console.log("  premium textLen=", (await page.evaluate(() => document.body.innerText.length)));

  // ---- LOGOUT through the header ----
  await act(page, "logout via header", async () => {
    const bs = await page.$$("button");
    for (const b of bs) {
      const t = ((await b.innerText()) || "").trim().toLowerCase();
      if (t.includes("thoát") || t.includes("logout") || t.includes("đăng xuất")) {
        await b.click(); await page.waitForTimeout(2000);
        return "path=" + page.evaluate ? "" : "";
      }
    }
    return "no logout button";
  });
  const after = await page.evaluate(() => ({ path: location.pathname, tok: !!localStorage.getItem("token") }));
  console.log("  after logout:", JSON.stringify(after));

  await browser.close();
})().catch(e => { console.log("FATAL", e.message); process.exit(1); });
