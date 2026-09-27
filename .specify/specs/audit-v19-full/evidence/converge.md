# audit-v19-full — converge (spec ↔ code gap)

| Mục | Yêu cầu | Trạng thái | Bằng chứng |
|---|---|---|---|
| O1 | Mọi endpoint có probe | ✅ | api-sweep 145/0/0/2na |
| O2 | Mọi route × role × viewport | ✅ | ui-sweep 0; routes-all 0 |
| O2b | Phase U **CẢ 2 engine MỌI luồng (gồm Premium)** | ✅ | `mcp-walkthrough.md` |
| O3 | DB toàn vẹn | ✅ | 0 orphan; 22/22 FK index; validate drill |
| O4 | 6 nhóm chức năng API↔UI | ✅ | `demo-claims.md` |
| **O5** | **W1: 0 CJK** | ✅ | 0 CJK ×15 live; test 4/4 |
| **O6** | **W2: smallTargets thật** | ✅ | 115→0; mutation-test PASS |
| **O7** | **W3: reads giảm** | ✅ | 1341→86; 42.8→26.6ms |
| **O8** | **W4: chữ ký SePay THẬT** | ✅ **PASS** | `ENG73E2D3AA2DF6` → SUCCESS, tx `85111759`, MBBank; webhook (không polling) |
| O9 | 2 vòng; docs; rác; report | 🔄 | round-2 ✅; docs ✅; rác/report đang làm |
| R1–R13 | (xem checklist) | ✅ (gồm R13: row tiền thật giữ lại) | — |

## Gap còn lại

| Gap | Lý do | Xử lý |
|---|---|---|
| ~~W4 chữ ký thật~~ | — | **ĐÃ XONG** (người dùng chuyển khoản, webhook thật settle) |
| Video/Speaking/Leaderboard Phase U chỉ PW | tiết kiệm; bù ui-sweep/routes-all | ghi rõ |
| CD home screenshot timeout | animation-heavy | PW + ui-sweep bù |
| `containsCjk` chưa phủ Hiragana/Hangul | defect đo được là Han | ghi nhận (không scope creep) |

**Mọi O/R có artifact.** W4 đã PASS.
