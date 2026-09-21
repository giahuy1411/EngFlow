// audit-v12 F151 — IDOR on GET /api/srs/due/{deckId}.
// Evidence: before the fix, a student could read the word content of another user's PRIVATE
// deck through the SRS endpoint while GET /api/decks/{id} correctly refused. After the fix
// both must refuse identically.
const BASE = process.env.BASE || "http://localhost:8080";

const USER = { email: "user@gmail.com", password: "123456" };
const ADMIN = { email: "admin@gmail.com", password: "123456" };

const PRIVATE_DECK_OF_ADMIN = 30033;   // owner_id=3 (admin), is_public=0
const PUBLIC_DECK = 10006;             // is_public=1

async function req(method, p, { token, body } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  const r = await fetch(BASE + p, { method, headers, body: body ? JSON.stringify(body) : undefined });
  let data = null;
  try { data = await r.json(); } catch { /* empty body */ }
  return { status: r.status, data };
}

const login = async ({ email, password }) => {
  const r = await req("POST", "/api/auth/login", { body: { email, password } });
  return r.status === 200 ? (r.data?.data?.token || r.data?.token || null) : null;
};

let failures = 0;
function check(name, ok, detail) {
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${ok ? "" : "   <- " + detail}`);
  if (!ok) failures++;
}

(async () => {
  const userToken = await login(USER);
  const adminToken = await login(ADMIN);
  console.log("user token:", userToken ? "OK" : "MISSING", "| admin token:", adminToken ? "OK" : "MISSING");

  // --- F151: the private deck of ANOTHER user -----------------------------------------
  const decksRefused = await req("GET", `/api/decks/${PRIVATE_DECK_OF_ADMIN}`, { token: userToken });
  check("GET /api/decks/{other's private} refused (400)",
    decksRefused.status === 400, `got ${decksRefused.status}`);

  const srsRefused = await req("GET", `/api/srs/due/${PRIVATE_DECK_OF_ADMIN}`, { token: userToken });
  check("GET /api/srs/due/{other's private} refused (400)",
    srsRefused.status === 400, `got ${srsRefused.status} body=${JSON.stringify(srsRefused.data)?.slice(0, 120)}`);

  // The leak was CONTENT, not just metadata — assert no word text escaped.
  const leaked = JSON.stringify(srsRefused.data || {});
  check("no word content leaked in the refusal",
    !/determine|pronunciation|definitionVi/.test(leaked), `body=${leaked.slice(0, 200)}`);

  // --- F151 follow-on: nonexistent deck should be 404, not 200 [] ----------------------
  const missingSrs = await req("GET", "/api/srs/due/999999", { token: userToken });
  const missingDeck = await req("GET", "/api/decks/999999", { token: userToken });
  check("GET /api/srs/due/{nonexistent} 404",
    missingSrs.status === 404, `got ${missingSrs.status}`);
  check("GET /api/decks/{nonexistent} 404 (parity)",
    missingDeck.status === 404, `got ${missingDeck.status}`);

  // --- Regression: the valid paths must still work -------------------------------------
  const pub = await req("GET", `/api/srs/due/${PUBLIC_DECK}`, { token: userToken });
  check("GET /api/srs/due/{public} 200", pub.status === 200, `got ${pub.status}`);
  check("public due words are a non-empty array",
    Array.isArray(pub.data) && pub.data.length > 0, `got ${JSON.stringify(pub.data)?.slice(0, 80)}`);

  const owner = await req("GET", `/api/srs/due/${PRIVATE_DECK_OF_ADMIN}`, { token: adminToken });
  check("owner reading own private deck 200", owner.status === 200, `got ${owner.status}`);

  const stats = await req("GET", "/api/srs/stats", { token: userToken });
  check("GET /api/srs/stats 200", stats.status === 200, `got ${stats.status}`);

  console.log(`\n${failures === 0 ? "ALL PASS" : failures + " FAILURE(S)"}`);
  process.exit(failures === 0 ? 0 : 1);
})();
