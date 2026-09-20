/**
 * PHEP THU QUYET DINH: chay @playwright/mcp voi --executable-path tro toi Brave,
 * roi bat tay JSON-RPC that (initialize -> tools/list), va goi thu mot tool
 * that su mo browser.
 *
 * Khong suy doan "chac la chay duoc" — phai thay no tra loi.
 */
const { spawn } = require("child_process");
const BRAVE = "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe";

// Chay TRUC TIEP bang `node` tren file entry cua MCP, khong qua npx.
//
// BA CACH DA THU VA HONG (ghi lai de khong ai thu lai):
//  1. spawn("npx", args, {shell:true})  -> shell tach "C:\Program Files\..."
//     thanh hai doi so tai dau cach -> "too many arguments. Expected 0
//     arguments but got 1: Files\BraveSoftware\...".
//  2. spawn("npx", args) khong shell    -> ENOENT: Windows can npx.cmd.
//  3. spawn("npx.cmd", args) khong shell -> EINVAL: Node khong chay duoc
//     .cmd truc tiep khi khong co shell.
//
// Cach dung: goi thang node + duong dan file JS. Khong co shell nao xen vao,
// nen dau cach trong duong dan Brave khong con la van de.
const MCP_ENTRY = process.env.PLAYWRIGHT_MCP_ENTRY
  || "C:\\Users\\ASUS\\AppData\\Local\\npm-cache\\_npx\\9833c18b2d85bc59\\node_modules\\@playwright\\mcp\\cli.js";

const args = [
  MCP_ENTRY,
  "--executable-path", BRAVE,
  "--headless",
  "--isolated",
];

console.log("lenh: node " + args.join(" "));
console.log("");

const child = spawn(process.execPath, args, {
  stdio: ["pipe", "pipe", "pipe"],
});

let buf = "";
const responses = [];
const stderrLines = [];

child.stdout.on("data", (d) => {
  buf += d.toString();
  let idx;
  while ((idx = buf.indexOf("\n")) >= 0) {
    const line = buf.slice(0, idx).trim();
    buf = buf.slice(idx + 1);
    if (!line) continue;
    try { responses.push(JSON.parse(line)); }
    catch (e) { /* khong phai JSON */ }
  }
});

child.stderr.on("data", (d) => stderrLines.push(d.toString().slice(0, 300)));

function send(obj) {
  child.stdin.write(JSON.stringify(obj) + "\n");
}

function waitFor(id, ms) {
  return new Promise((resolve) => {
    const t0 = Date.now();
    const tick = setInterval(() => {
      const r = responses.find((x) => x.id === id);
      if (r) { clearInterval(tick); resolve(r); }
      else if (Date.now() - t0 > ms) { clearInterval(tick); resolve(null); }
    }, 200);
  });
}

(async () => {
  await new Promise((r) => setTimeout(r, 3000));

  console.log("--- 1. initialize ---");
  send({
    jsonrpc: "2.0", id: 1, method: "initialize",
    params: {
      protocolVersion: "2024-11-05",
      capabilities: {},
      clientInfo: { name: "audit-v10-probe", version: "1.0" },
    },
  });
  const init = await waitFor(1, 30000);
  if (!init) {
    console.log("  KHONG tra loi initialize");
    console.log("  stderr: " + stderrLines.join(" | ").slice(0, 500));
    child.kill(); process.exit(1);
  }
  console.log("  server:", JSON.stringify(init.result?.serverInfo || {}));
  console.log("  protocol:", init.result?.protocolVersion);

  send({ jsonrpc: "2.0", method: "notifications/initialized", params: {} });

  console.log("");
  console.log("--- 2. tools/list ---");
  send({ jsonrpc: "2.0", id: 2, method: "tools/list", params: {} });
  const tools = await waitFor(2, 30000);
  if (!tools) { console.log("  KHONG tra loi tools/list"); child.kill(); process.exit(1); }
  const names = (tools.result?.tools || []).map((t) => t.name);
  console.log("  so tool: " + names.length);
  console.log("  " + names.slice(0, 12).join(", ") + (names.length > 12 ? " ..." : ""));

  console.log("");
  console.log("--- 3. GOI THAT: browser_navigate toi app ---");
  const navTool = names.find((n) => /navigate/i.test(n));
  if (!navTool) { console.log("  khong co tool navigate"); child.kill(); process.exit(1); }
  console.log("  dung tool: " + navTool);

  send({
    jsonrpc: "2.0", id: 3, method: "tools/call",
    params: { name: navTool, arguments: { url: "http://localhost:5173/" } },
  });
  const nav = await waitFor(3, 90000);
  if (!nav) {
    console.log("  KHONG tra loi navigate");
    console.log("  stderr: " + stderrLines.join(" | ").slice(0, 800));
    child.kill(); process.exit(1);
  }
  if (nav.error) {
    console.log("  LOI: " + JSON.stringify(nav.error).slice(0, 400));
    child.kill(); process.exit(1);
  }
  const text = (nav.result?.content || []).map((c) => c.text || "").join(" ").slice(0, 500);
  console.log("  ket qua: " + text.replace(/\s+/g, " ").slice(0, 400));

  console.log("");
  console.log("============================================");
  console.log("  PLAYWRIGHT MCP + BRAVE: CHAY DUOC");
  console.log("============================================");
  console.log("  server : " + JSON.stringify(init.result?.serverInfo?.name || "?"));
  console.log("  tools  : " + names.length);
  console.log("  browser: Brave (qua --executable-path)");

  child.kill();
  process.exit(0);
})().catch((e) => {
  console.error("ERR", e.message);
  try { child.kill(); } catch (x) {}
  process.exit(1);
});
