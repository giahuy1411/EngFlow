"""add_token_only_seed_finding.py — record the token-only seed false pass.

Run:  python .specify/specs/audit-v8-full/add_token_only_seed_finding.py
Idempotent: re-running updates the same finding instead of appending a copy.

SCOPE NOTE (measured, not assumed): an A/B run of the OLD token-only seed vs the
NEW full-session seed, walking routes-all.js's exact route order, showed BOTH
produce identical admin results (0/9 bounced). The app self-hydrates `user` from
`/api/auth/me` during earlier navigations, so an admin route reached LATE in a
long walk renders correctly. Only the FRESH-CONTEXT harnesses — which navigate to
an admin route as their first action — are affected. The impact statement below
reflects that narrower, measured scope.
"""
import io
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PATH = os.path.join(HERE, "findings.json")

with io.open(PATH, encoding="utf-8") as fh:
    doc = json.load(fh)

NEW = {
    "id": "HARNESS-TOKEN-ONLY-SEED",
    "severity": "HIGH",
    "class": "AUDIT-HARNESS",
    "title": ("Harness seed chỉ localStorage.token (không seed localStorage.user) "
              "→ harness dùng context MỚI cho mỗi case render nhầm trang chủ ở 9 route admin"),
    "reproduction": [
        "`ui/lib.js:seedToken()` chỉ gọi `localStorage.setItem('token', t)`.",
        "`store/modules/auth.js:22` tính `isAdmin = user.value?.isAdmin`; `user` khởi tạo từ `localStorage.getItem('user')` — key chưa từng được ghi.",
        "`main.js` KHÔNG gọi `fetchUser()` lúc boot ⇒ trang load với token mà không có user ⇒ `isAdmin === undefined`.",
        "Router guard `index.js:220` `if (to.meta.requiresAdmin && !auth.isAdmin)` → redirect `/`.",
        "`p18_redirect_audit.js` (context mới cho mỗi case): 18 case → 14 redirect; cả 9 route admin → `/`, h1 'HỌC TIẾNG ANH VUI VẺ'.",
        "`p17_screenshots.js` (context mới cho mỗi shot): 9 shot admin đều có text=1524 = độ dài trang chủ.",
    ],
    "rootCause": ("Harness chỉ seed token. Trang chủ là trang hoàn toàn khỏe mạnh nên "
                  "`mounted=Y, err=0, api4xx=0, ovf=-15` — không chỉ số nào phân biệt được "
                  "'render đúng route' với 'bị đá sang trang khác'. Harness không ghi `page.url()` "
                  "sau điều hướng, và field `redirected` cũ chỉ được set khi `role === 'anon'`, "
                  "nên redirect của role admin bị ẩn hoàn toàn."),
    "affectedSurface": ("sweep/v8/p17_screenshots.js (9/36 shot), sweep/v8/p18_redirect_audit.js — "
                        "tức các harness dùng CONTEXT MỚI cho mỗi case. "
                        "KHÔNG ảnh hưởng sweep/v8/ui/routes-all.js và ui/design-v2.js (đã đo bằng A/B, xem impact)."),
    "impact": ("Phạm vi ĐÃ ĐO, không suy đoán: `p20_routes_all_old_seed_ab.js` chạy A/B seed cũ vs seed mới "
               "theo đúng thứ tự route của routes-all.js → **cả hai đều 0/9 bounce**, text giống hệt nhau "
               "(721/2800/2471/1151/921/4167/588/942/365). Lý do: app tự hydrate `user` qua `/api/auth/me` "
               "trong lúc đi các route trước đó, nên route admin ở CUỐI hành trình vẫn render đúng. "
               "`p19_why_design_v2_was_fine.js` case C xác nhận tương tự cho design-v2 (60 tổ hợp vẫn hợp lệ). "
               "Vậy thiệt hại thật: 9/36 ảnh screenshot là ảnh trang chủ đội lốt trang admin — bằng chứng "
               "hình ảnh sai, và bảng 'before/after' của PLAN.md Phase 5 sẽ nộp ảnh sai. "
               "Con số 152/228 lượt của routes-all.js KHÔNG bị ảnh hưởng."),
    "disposition": ("Thêm `loginFull()` + `mapUser()` + `seedAuth()` vào `ui/lib.js` để seed ĐÚNG shape mà "
                    "`mapUser()` sinh ra (kèm comment giải thích). `p17_screenshots.js` nay seed token+user, "
                    "từ chối chạy nếu `adminS.user.isAdmin` falsy, và đánh dấu `landedElsewhere` mỗi shot. "
                    "`routes-all.js` nay chạy 3 role (admin/user/anon), ghi `finalPath` cho MỌI lượt, và "
                    "assert hợp đồng landing guard × role ở CẢ HAI chiều (stay vs bounce). "
                    "`design-v2.js` nay ghi `wrongPage` và exit 1 nếu lệch."),
    "regressionCheck": ("`node ui/routes-all.js` → 228 lượt (38 route × 2 viewport × 3 role), 0 console error, "
                        "0 API ≥400, 0 overflow, **0 wrong landing**, **18/18 lượt admin render trang admin thật**. "
                        "`p17_screenshots.js` → 36/36 shot, 0 landed-on-wrong-page, admin shots nay text=721/2800/... "
                        "(trước: 1524). `p18b_admin_after_seed_fix.js` → 9/9 route admin render, có bảng thật "
                        "(20 lesson row, 20 exercise row, 10 user row). Đây là lỗi HARNESS, KHÔNG phải lỗi app: "
                        "guard từ chối user không xác định là hành vi ĐÚNG."),
    "evidence": [
        "sweep/v8/p18_redirect_audit.json",
        "sweep/v8/p18_redirect_audit.txt",
        "sweep/v8/p18b_admin_after_seed_fix.json",
        "sweep/v8/p18b.txt",
        "sweep/v8/p19_why_design_v2_was_fine.js",
        "sweep/v8/p19.txt",
        "sweep/v8/p20_ab.json",
        "sweep/v8/p20.txt",
        "sweep/v8/ui/routes-all.txt",
        "sweep/v8/p17_screenshots.txt",
    ],
    "status": "FIXED-VERIFIED",
}

found = False
for i, f in enumerate(doc["findings"]):
    if f["id"] == NEW["id"]:
        doc["findings"][i] = NEW
        found = True
        break
if not found:
    doc["findings"].append(NEW)

doc["round"] = 3
doc["note"] = (doc.get("note", "") +
               " | round 3: +HARNESS-TOKEN-ONLY-SEED (HIGH) — seed chỉ token làm 9/36 ảnh screenshot "
               "chụp nhầm trang chủ; A/B đã chứng minh routes-all.js KHÔNG bị ảnh hưởng.")

with io.open(PATH, "w", encoding="utf-8", newline="\n") as fh:
    json.dump(doc, fh, ensure_ascii=False, indent=1)
    fh.write("\n")

print(("updated" if found else "appended") + ": " + NEW["id"])
print("total findings: %d" % len(doc["findings"]))
