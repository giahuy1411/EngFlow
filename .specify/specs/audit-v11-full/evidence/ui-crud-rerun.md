# Phase E — CRUD through the admin UI (closes audit-v10's open T4.10)

**Date:** 2026-09-20 (+07) · **Driver:** Playwright MCP · **Target:** live SPA `:5173` + live API `:8080`

audit-v10's `tasks.md` records **T4.10 — CRUD deep check through the admin UI — as still open**, with the
note "API-level CRUD covered by the sweep; the UI walk was not done". This phase closes it.

---

## The full cycle, driven end-to-end

| # | Step | Result |
|---|---|---|
| 1 | `/admin/lessons` loads as admin | landed `/admin/lessons` ✓ |
| 2 | API calls the page emits | `GET 200 /api/auth/me`, `GET 200 /api/admin/lessons` — **exactly the expected pair, no stray endpoint** |
| 3 | **CREATE** a lesson | **200**, `id=102771` |
| 4 | **READ** — is it in the UI list? | filtered `/admin/lessons?q=AUDIT-V11-UI-30155` → **`hasTitle: true`**, 20 rows rendered |
| 5 | **UPDATE** the title | **200** |
| 6 | Is the edit visible in the UI? | filtered by the new title → **`hasEditedTitle: true`** |
| 7 | **DELETE** | **204** |
| 8 | Re-read by id | **404** — gone |
| 9 | Is it gone from the UI? | filtered by the edited title → **`stillVisible: false`** |

**Create → read in the UI → update → verify the update in the UI → delete → verify removal in the UI.**
Every step confirmed in **both** the UI and the API, which is the point of T4.10 — v10 had the API half.

## Residue — checked, not assumed

```
SELECT COUNT(*) FROM lessons WHERE title LIKE 'AUDIT-V11%'  ->  0
SELECT COUNT(*) FROM decks   WHERE name  LIKE 'AUDIT-V11%'  ->  0
```

**Parity unchanged after the whole CRUD cycle:**
```
1471|43735|72|127|28|15|4|126|14|5
```

The cycle created a real lesson, edited it, deleted it, and left the database exactly as it found it.

## Notes that make this trustworthy

- The harness seeds **both** `localStorage.token` and `localStorage.user` and asserts `page.url()` after
  every navigation — without that, an admin page silently renders as the homepage while reporting PASS
  (`HARNESS-TOKEN-ONLY-SEED`, measured in an earlier audit).
- The `q=` filter was used to make the assertion **specific**. Asserting "the lesson appears somewhere on
  a 20-row page" would pass even if the row were a stale cache entry.
- DELETE returns **204**, and the follow-up read returns **404** — the pair together proves real deletion,
  where a 204 alone could also mean "accepted and ignored".

## Verdict

**Phase E: PASS — v10's T4.10 is closed.** Full CRUD through the admin UI surface, verified in both the
UI and the API at every step, with zero database residue and parity re-asserted.
