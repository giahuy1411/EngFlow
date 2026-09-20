const BASE = "http://localhost:5173";
const PATHS = ["/", "/index.html", "/src/main.js"];
const DURATION_MS = Number(process.argv[2] || 30 * 60 * 1000);
const INTERVAL_MS = 30 * 1000;
const started = Date.now();
const results = [];
let failures = 0;

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function probe(path) {
  const probeStarted = Date.now();
  try {
    const response = await fetch(BASE + path, { signal: AbortSignal.timeout(5000) });
    const body = await response.text();
    return {
      path,
      status: response.status,
      ms: Date.now() - probeStarted,
      ok: response.ok && body.length > 0,
    };
  } catch (error) {
    return {
      path,
      ms: Date.now() - probeStarted,
      ok: false,
      error: error.message,
    };
  }
}

(async () => {
  while (Date.now() - started < DURATION_MS) {
    const row = { at: new Date().toISOString(), probes: [] };
    for (const path of PATHS) row.probes.push(await probe(path));
    row.failures = row.probes.filter((probe) => !probe.ok).length;
    failures += row.failures;
    results.push(row);
    console.log(JSON.stringify(row));
    if (Date.now() - started < DURATION_MS) await sleep(INTERVAL_MS);
  }
  console.log(JSON.stringify({ VITE_SOAK_TOTAL: results.length, VITE_SOAK_FAILURES: failures }));
  process.exitCode = failures > 0 ? 1 : 0;
})();
