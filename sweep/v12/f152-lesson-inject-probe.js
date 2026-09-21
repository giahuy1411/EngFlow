// audit-v12 F152 verification probe — non-admin must not be able to attach a vocabulary row
// to a lesson. Creates real rows and removes them by enumerated id.
const BASE = process.env.BASE || "http://localhost:8080";

async function req(method, p, { token, body } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  const r = await fetch(BASE + p, { method, headers, body: body ? JSON.stringify(body) : undefined });
  let data = null;
  try { data = await r.json(); } catch {}
  return { status: r.status, data };
}

const login = async (email) => {
  const r = await req("POST", "/api/auth/login", { body: { email, password: "123456" } });
  return r.status === 200 ? (r.data?.data?.token || r.data?.token) : null;
};

let fail = 0;
const check = (n, ok, d) => { console.log(`${ok ? "PASS" : "FAIL"}  ${n}${ok ? "" : "  <- " + d}`); if (!ok) fail++; };

(async () => {
  const userToken = await login("user@gmail.com");
  const adminToken = await login("admin@gmail.com");
  console.log("tokens:", !!userToken, !!adminToken);

  const lessons = await req("GET", "/api/lessons?size=5", { token: userToken });
  const lessonId = (lessons.data?.content || lessons.data || [])[0]?.id;
  const deck = await req("POST", "/api/decks", { token: userToken, body: { name: "AUDIT-V12-API-F152", description: "f152", isPublic: false } });
  const deckId = deck.data?.id ?? deck.data?.deckId;
  console.log("lessonId:", lessonId, "deckId:", deckId);

  const FRESH = "zzf152fresh";
  const EXISTING = "negotiate"; // already in the shared dictionary -> exercises the dedupe branch

  // V1: student + lessonId on a FRESH word -> 400
  const v1 = await req("POST", `/api/vocabulary?deckId=${deckId}`, { token: userToken, body: { word: FRESH, meaning: "probe", lessonId } });
  check("V1 student + lessonId (fresh word) rejected 400", v1.status === 400, `got ${v1.status}`);

  // V2: student + lessonId on an EXISTING word -> must ALSO be 400 (the dedupe-bypass case)
  const v2 = await req("POST", `/api/vocabulary?deckId=${deckId}`, { token: userToken, body: { word: EXISTING, meaning: "probe", lessonId } });
  check("V2 student + lessonId (word already in dictionary) rejected 400", v2.status === 400, `got ${v2.status}`);

  // V3a: student WITHOUT lessonId -> still 200 (fix must not break the normal save)
  const v3a = await req("POST", `/api/vocabulary?deckId=${deckId}`, { token: userToken, body: { word: FRESH, meaning: "probe" } });
  check("V3a student without lessonId still 200", [200, 201].includes(v3a.status), `got ${v3a.status}`);
  const createdId = v3a.data?.id ?? v3a.data?.vocabId;

  // V3b: admin + lessonId -> 200 (fix must not lock the admin path)
  const v3b = await req("POST", "/api/admin/vocabulary", { token: adminToken, body: { word: "zzf152admin", meaning: "probe", lessonId } });
  check("V3b admin + lessonId still 200", [200, 201].includes(v3b.status), `got ${v3b.status}`);
  const adminVocabId = v3b.data?.id ?? v3b.data?.vocabId;

  // V4: nothing leaked into the public lesson payload
  const anon = await req("GET", `/api/lessons/${lessonId}`);
  const words = (anon.data?.vocabularies || []).map(v => v.word);
  check("V4 no injected word in anonymous lesson payload", !words.includes(FRESH), `words=${JSON.stringify(words)}`);

  console.log(JSON.stringify({ deckId, vocabId: createdId, adminVocabId, lessonId }));
  console.log(fail === 0 ? "\nALL PASS" : `\n${fail} FAILURE(S)`);
  process.exit(fail === 0 ? 0 : 1);
})();
