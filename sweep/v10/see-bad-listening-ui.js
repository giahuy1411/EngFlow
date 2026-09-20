/**
 * Xem UI THAT cua 1 row "LISTENING" thieu audio — bang mat, khong suy doan.
 *
 * Cau hoi: nguoi hoc nhin thay gi?
 *   - Nut "🔊 Nghe" doc to cau lenh?
 *   - O nhap tay?
 *   - Cac lua chon?
 */
const { launch, APP, loginFull, mapUser } = require("./ui-lib");

const LESSON = 11516; // lesson "Reading - VOCABULARY", chua row 745748

(async () => {
  const user = await loginFull("user@gmail.com", "123456");
  const browser = await launch();
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 1200 } });
  const page = await ctx.newPage();

  await page.goto(APP + "/login", { waitUntil: "domcontentloaded" });
  await page.evaluate((x) => {
    localStorage.setItem("token", x.token);
    localStorage.setItem("user", JSON.stringify(x.user));
  }, { token: user.token, user: mapUser(user.user) });

  await page.goto(APP + `/lessons/${LESSON}`, { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(3500);

  console.log("=== UI cua lesson " + LESSON + " ===");
  console.log("url:", page.url());

  // Tim tat ca the bai tap
  const info = await page.evaluate(() => {
    const out = [];
    // Cac nut "Nghe" (aria-label bat dau bang 'Phat am cau nghe')
    document.querySelectorAll("button[aria-label*='Phat am'], button[aria-label*='Phát âm']").forEach((b) => {
      out.push({ kind: "NUT NGHE", text: b.textContent.trim(), aria: b.getAttribute("aria-label") });
    });
    // Cac the audio
    document.querySelectorAll("audio").forEach((a) => {
      out.push({ kind: "THE AUDIO", src: a.getAttribute("src") });
    });
    // Nhan "Nghe & tra loi"
    document.querySelectorAll("*").forEach((el) => {
      if (el.children.length) return;
      const t = (el.textContent || "").trim();
      if (/Nghe & trả lời/.test(t)) out.push({ kind: "NHAN", text: t });
    });
    // Cac nut lua chon
    const btns = [...document.querySelectorAll("button")].filter((b) => {
      const t = (b.textContent || "").trim();
      return /^[A-D]\.\s/.test(t);
    });
    out.push({ kind: "SO NUT LUA CHON", text: String(btns.length) });
    btns.slice(0, 4).forEach((b) => out.push({ kind: "  lua chon", text: (b.textContent || "").trim().slice(0, 70) }));
    // O nhap tay
    out.push({ kind: "SO O NHAP TAY", text: String(document.querySelectorAll("input[type=text]").length) });
    return out;
  });

  for (const i of info) console.log(`  [${i.kind}] ${i.text}${i.aria ? "  (" + i.aria + ")" : ""}${i.src ? "  " + i.src : ""}`);

  // Chay thu speakListening: bam nut Nghe va xem no se doc gi
  const speech = await page.evaluate(() => {
    const available = typeof window !== "undefined" && "speechSynthesis" in window;
    // Tim cau hoi cua bai LISTENING
    let question = null;
    document.querySelectorAll("*").forEach((el) => {
      if (el.children.length) return;
      const t = (el.textContent || "").trim();
      if (/^I can understand a text about brothers/.test(t)) question = t;
    });
    return { available, question };
  });
  console.log("");
  console.log("  speechSynthesis kha dung:", speech.available);
  console.log("  cau hoi cua bai LISTENING:", speech.question);
  if (speech.question) {
    // Mo phong blankOutForSpeech + stripLeadingNumber
    const spoken = speech.question
      .replace(/`[^`]*`/g, " ... ")
      .replace(/_{2,}/g, " ... ")
      .replace(/\s+/g, " ")
      .trim()
      .replace(/^\s*\d+\s*[.)]?\s+/, "")
      .trim();
    console.log("  >>> NUT '🔊 Nghe' SE DOC TO: \"" + spoken + "\"");
    console.log("  >>> day KHONG phai noi dung bai nghe — do la CAU LENH.");
  }

  await browser.close();
})().catch((e) => { console.error("ERR", e.message); process.exit(1); });
