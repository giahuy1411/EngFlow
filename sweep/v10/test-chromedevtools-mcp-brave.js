/**
 * PHEP THU: chrome-devtools-mcp voi --executablePath tro toi Brave.
 *
 * Dung cung cach da chung minh voi Playwright MCP: goi thang node tren cli.js,
 * khong qua npx/shell.
 */
const { spawn } = require("child_process");
const fs = require("fs");
const path = require("path");

const BRAVE = "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe";

// Plugin trong .claude/plugins/cache CHI CO `src/`, KHONG co `build/` — no
// chay qua `npx chrome-devtools-mcp@1.9.0`, tuc tai package tu npm. Ban da
// tai nam trong cache _npx. Tim ban MOI NHAT co build/ san.
const NPX_CACHE = "C:\\Users\\ASUS\\AppData\\Local\\npm-cache\\_npx";

function findEntry() {
  let best = null;
  let bestTime = 0;
  let dirs = [];
  try { dirs = fs.readdirSync(NPX_CACHE); } catch (e) { return null; }
  for (const d of dirs) {
    const p = path.join(NPX_CACHE, d, "node_modules", "chrome-devtools-mcp");
    const entry = path.join(p, "build", "src", "bin", "chrome-devtools-mcp.js");
    try {
      if (fs.existsSync(entry)) {
        const t = fs.statSync(entry).mtimeMs;
        if (t > bestTime) { bestTime = t; best = entry; }
      }
    } catch (e) {}
  }
  return best;
}

const ENTRY = findEntry();
console.log("entry     :", ENTRY || "(KHONG TIM THAY)");
if (!ENTRY) {
  console.log("");
  console.log("Khong tim thay chrome-devtools-mcp da build trong " + NPX_CACHE);
  console.log("Thu chay: npx chrome-devtools-mcp@1.9.0 --help  (de tai ve)");
  process.exit(1);
}

const args = [
  ENTRY,
  "--executablePath=" + BRAVE,
  "--headless=true",
  "--isolated",
];

console.log("");
console.log("lenh: node " + args.join(" "));
console.log("");

const child = spawn(process.execPath, args, { stdio: ["pipe", "pipe", "pipe"] });

let buf = "";
const responses = [];
const stderrLines = [];

child.stdout.on("data", (d) => {
  buf += d.toString();
  let i;
  while ((i = buf.indexOf("\n")) >= 0) {
    const line = buf.slice(0, i).trim();
    buf = buf.slice(i + 1);
    if (!line) continue;
    try { responses.push(JSON.parse(line)); } catch (e) {}
  }
});
child.stderr.on("data", (d) => stderrLines.push(d.toString().slice(0, 400)));

function send(o) { child.stdin.write(JSON.stringify(o) + "\n"); }
function waitFor(id, ms) {
  return new Promise((res) => {
    const t0 = Date.now();
    const t = setInterval(() => {
      const r = responses.find((x) => x.id === id);
      if (r) { clearInterval(t); res(r); }
      else if (Date.now() - t0 > ms) { clearInterval(t); res(null); }
    }, 200);
  });
}

(async () => {
  await new Promise((r) => setTimeout(r, 4000));

  console.log("--- 1. initialize ---");
  send({
    jsonrpc: "2.0", id: 1, method: "initialize",
    params: {
      protocolVersion: "2024-11-05", capabilities: {},
      clientInfo: { name: "audit-v10-probe", version: "1.0" },
    },
  });
  const init = await waitFor(1, 40000);
  if (!init) {
    console.log("  KHONG tra loi");
    console.log("  stderr: " + stderrLines.join(" | ").slice(0, 700));
    child.kill(); process.exit(1);
  }
  console.log("  server:", JSON.stringify(init.result?.serverInfo || {}));

  send({ jsonrpc: "2.0", method: "notifications/initialized", params: {} });

  console.log("");
  console.log("--- 2. tools/list ---");
  send({ jsonrpc: "2.0", id: 2, method: "tools/list", params: {} });
  const tools = await waitFor(2, 40000);
  if (!tools) {
    console.log("  KHONG tra loi");
    console.log("  stderr: " + stderrLines.join(" | ").slice(0, 700));
    child.kill(); process.exit(1);
  }
  const names = (tools.result?.tools || []).map((t) => t.name);
  console.log("  so tool: " + names.length);
  console.log("  " + names.slice(0, 15).join(", "));

  console.log("");
  console.log("--- 3. GOI THAT: new_page (tao tab) ---");
  const newTool = names.find((n) => /^new_page$/i.test(n));
  if (!newTool) { console.log("  khong co tool new_page"); child.kill(); process.exit(1); }
  console.log("  dung tool: " + newTool);
  send({
    jsonrpc: "2.0", id: 3, method: "tools/call",
    params: { name: newTool, arguments: { url: "http://localhost:5173/" } },
  });
  const created = await waitFor(3, 120000);
  if (!created) {
    console.log("  KHONG tra loi");
    console.log("  stderr: " + stderrLines.join(" | ").slice(0, 900));
    child.kill(); process.exit(1);
  }
  if (created.error) {
    console.log("  LOI: " + JSON.stringify(created.error).slice(0, 400));
    child.kill(); process.exit(1);
  }
  const ctxt = (created.result?.content || []).map((c) => c.text || "").join(" ");
  console.log("  ket qua: " + ctxt.replace(/\s+/g, " ").slice(0, 350));

  // Lay pageId cua tab DANG DUOC CHON.
  //
  // Hai lan sai cua probe, ghi lai:
  //  1. Goi `navigate_page` khong co pageId -> "Invalid arguments ... Required
  //     at pageId". Server bat --pageIdRouting mac dinh nen MOI tool theo trang
  //     deu bat buoc co no.
  //  2. Bat so dau tien trong ket qua -> duoc `1`, nhung do la tab
  //     `about:blank`; tab app la `2`. Nen `evaluate_script` chay tren trang
  //     trong va tra ve title="" + font Times New Roman.
  //
  // Cach dung: tim cum NGAY TRUOC "[selected]".
  //
  // Lan thu ba cua probe cung sai: regex `(\d+):[^:]*\[selected\]` khong khop
  // vi URL chua dau ':' (`http://localhost:5173/`) nen `[^:]*` dung som.
  // Lay phan truoc "[selected]" roi tim so CUOI CUNG trong do — cach nay khong
  // phu thuoc vao noi dung ben trong.
  const before = ctxt.split("[selected]")[0] || "";
  const all = [...before.matchAll(/(\d+)\s*:/g)].map((x) => parseInt(x[1], 10));
  const pageId = all.length ? all[all.length - 1] : null;
  console.log("  pageId cua tab dang chon: " + pageId + "  (tat ca: " + JSON.stringify(all) + ")");
  if (pageId === null) { console.log("  khong xac dinh duoc pageId"); child.kill(); process.exit(1); }

  console.log("");
  console.log("--- 4. GOI THAT: list_pages (kiem tra browser song) ---");
  send({ jsonrpc: "2.0", id: 4, method: "tools/call", params: { name: "list_pages", arguments: {} } });
  const pages = await waitFor(4, 60000);
  if (!pages || pages.error) {
    console.log("  LOI: " + JSON.stringify(pages?.error || "khong tra loi").slice(0, 300));
    child.kill(); process.exit(1);
  }
  const ptxt = (pages.result?.content || []).map((c) => c.text || "").join(" ");
  console.log("  " + ptxt.replace(/\s+/g, " ").slice(0, 400));

  console.log("");
  console.log("--- 5. GOI THAT: evaluate_script (doc du lieu trang) ---");
  send({
    jsonrpc: "2.0", id: 5, method: "tools/call",
    params: {
      name: "evaluate_script",
      arguments: { pageId, function: "() => ({ title: document.title, url: location.href, font: getComputedStyle(document.body).fontFamily })" },
    },
  });
  const ev = await waitFor(5, 60000);
  if (!ev || ev.error) {
    console.log("  LOI: " + JSON.stringify(ev?.error || "khong tra loi").slice(0, 300));
  } else {
    const etxt = (ev.result?.content || []).map((c) => c.text || "").join(" ");
    console.log("  " + etxt.replace(/\s+/g, " ").slice(0, 400));
  }

  console.log("");
  console.log("============================================");
  console.log("  CHROME DEVTOOLS MCP + BRAVE: CHAY DUOC");
  console.log("============================================");
  console.log("  server : " + JSON.stringify(init.result?.serverInfo?.name || "?"));
  console.log("  tools  : " + names.length);
  console.log("  browser: Brave (qua --executablePath)");

  child.kill();
  process.exit(0);
})().catch((e) => {
  console.error("ERR", e.message);
  try { child.kill(); } catch (x) {}
  process.exit(1);
});
