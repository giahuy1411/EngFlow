/**
 * Tim cau hinh chrome-devtools MCP trong ~/.claude.json va settings.json.
 * Chi DOC, khong sua.
 */
const fs = require("fs");
const os = require("os");
const path = require("path");

const HOME = os.homedir();
const FILES = [
  path.join(HOME, ".claude.json"),
  path.join(HOME, ".claude", "settings.json"),
  path.join(HOME, ".claude", "settings.local.json"),
  path.join(process.cwd(), ".mcp.json"),
  path.join(process.cwd(), ".claude", "settings.json"),
  path.join(process.cwd(), ".claude", "settings.local.json"),
];

function scan(obj, where, out, depth) {
  if (depth > 6 || obj == null || typeof obj !== "object") return;
  for (const [k, v] of Object.entries(obj)) {
    if (/chrome-devtools|chrome_devtools|chromedevtools/i.test(k)) {
      out.push({ where, key: k, value: v });
    }
    if (v && typeof v === "object") scan(v, where + "." + k, out, depth + 1);
  }
}

for (const f of FILES) {
  let raw;
  try { raw = fs.readFileSync(f, "utf8"); } catch (e) { continue; }
  console.log("=== " + f + " (" + raw.length + " bytes) ===");
  let j;
  try { j = JSON.parse(raw); } catch (e) {
    console.log("  KHONG parse duoc JSON: " + e.message.slice(0, 80));
    continue;
  }
  const found = [];
  scan(j, "", found, 0);
  if (!found.length) {
    console.log("  (khong co chrome-devtools)");
    // Van in ra mcpServers o top-level neu co
    const top = j.mcpServers ? Object.keys(j.mcpServers) : null;
    if (top) console.log("  mcpServers top-level: " + JSON.stringify(top));
    if (j.projects) {
      for (const [p, v] of Object.entries(j.projects)) {
        if (v && v.mcpServers && Object.keys(v.mcpServers).length) {
          console.log("  project " + p + " mcpServers: " + JSON.stringify(Object.keys(v.mcpServers)));
        }
      }
    }
  } else {
    for (const x of found) {
      console.log("  tai: " + (x.where || "(root)") + " -> " + x.key);
      console.log("    " + JSON.stringify(x.value, null, 2).split("\n").join("\n    "));
    }
  }
  console.log("");
}
