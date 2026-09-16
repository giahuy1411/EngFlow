/**
 * p10 — verify the F90 "ordering" concern does NOT create an exploitable bypass,
 * and pin the exact reason, so the finding is classified honestly rather than
 * left as a scary-sounding HIGH.
 *
 * Claim to falsify: "an attacker can bypass the limiter on expensive endpoints".
 * An anonymous request to an authenticated endpoint is rejected by Spring
 * Security BEFORE the limiter, so it never charges a bucket — but it also never
 * executes controller code. To show a real bypass we would need an endpoint that
 * (a) is reachable without authentication AND (b) does expensive work AND
 * (c) never charges a bucket. Test each permitAll surface for (b)+(c).
 */
const { execSync } = require("child_process");
const BASE = "http://localhost:8080";
const redis = (c) => execSync("docker exec engflow-redis redis-cli " + c, { encoding: "utf8" }).trim();
const keys = () => redis("--scan --pattern rate_limit:*").split(/\r?\n/).filter(Boolean);
const flush = () => {
  const k = keys();
  if (k.length) execSync("docker exec engflow-redis redis-cli del " + k.map(x => '"' + x + '"').join(" "), { encoding: "utf8" });
};

// Every permitAll surface from SecurityConfig, plus the bucket it lands in.
const PERMITALL = [
  ["POST", "/api/auth/login"],
  ["POST", "/api/auth/register"],
  ["POST", "/api/auth/forgot-password"],
  ["POST", "/api/auth/reset-password"],
  ["GET", "/api/vocabulary/search?q=test"],
  ["GET", "/api/vocabulary/dictionary/travel"],
  ["GET", "/api/leaderboard"],
  ["GET", "/api/lessons"],
  ["GET", "/api/lessons/445"],
  ["GET", "/api/v1/speaking-prompts"],
  ["GET", "/api/v1/video-prompts"],
  ["GET", "/api/v1/video-lessons"],
  ["GET", "/api/decks"],
  ["POST", "/api/webhook/sepay"],
  ["GET", "/api/resources/none.bin"],
  ["GET", "/api/v1/media/none.bin"],
  ["GET", "/audio/none.bin"],
];

(async () => {
  console.log("=== F90 falsification: every permitAll surface, does it charge a bucket? ===");
  console.log("(điều kiện khai thác thật = permitAll + việc đắt + KHÔNG tốn bucket)\n");
  const out = [];
  for (const [m, p] of PERMITALL) {
    flush();
    let status = 0;
    try {
      const r = await fetch(BASE + p, {
        method: m, headers: { "Content-Type": "application/json" },
        body: m === "POST" ? "{}" : undefined
      });
      status = r.status;
    } catch (e) { status = -1; }
    await new Promise(x => setTimeout(x, 150));
    const k = keys();
    const bucket = k.length ? k[0].split(":").pop() : "<NONE>";
    const exploited = (bucket === "<NONE>");
    out.push({ method: m, path: p, status, bucket, noBucket: exploited });
    console.log("  " + m.padEnd(5) + p.padEnd(42) + " status=" + String(status).padEnd(5) +
      " bucket=:" + String(bucket).padEnd(9) + (exploited ? "  <-- NO BUCKET" : ""));
  }
  flush();
  require("fs").writeFileSync("p10.json", JSON.stringify(out, null, 1));
  const bad = out.filter(x => x.noBucket);
  console.log("\npermitAll KHÔNG tốn bucket: " + bad.length +
    (bad.length ? " -> " + JSON.stringify(bad.map(x => x.method + " " + x.path)) : " (không có bypass)"));
  console.log("wrote p10.json  (rate-limit keys cleared)");
})().catch(e => { console.error("ERR", e); process.exit(1); });
