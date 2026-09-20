/** Doc --help cua ca hai MCP de biet co tro browser duoc khong. */
const { execFileSync } = require("child_process");

function run(label, cmd, args) {
  console.log("========== " + label + " ==========");
  try {
    const out = execFileSync(cmd, args, {
      encoding: "utf8",
      timeout: 120000,
      stdio: ["ignore", "pipe", "pipe"],
      shell: true,
    });
    console.log(out);
  } catch (e) {
    const out = String(e.stdout || "") + String(e.stderr || "");
    console.log(out.slice(0, 6000));
    if (!out) console.log("(khong co output) " + e.message.slice(0, 200));
  }
  console.log("");
}

run("playwright mcp --help", "npx", ["-y", "@playwright/mcp@latest", "--help"]);
